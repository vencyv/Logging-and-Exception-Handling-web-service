package com.logging.utility;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.stereotype.Component;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.BlobRequestOptions;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;

/**
 * @author ShaishavS Scheduler class to run at predefined time interval
 */
@Component
@Configuration
@PropertySource(value = { "classpath:constant.properties" })
public class MyJobScheduler implements org.quartz.Job {

	private static MyJobScheduler singleInstance = null;
	private static String storageConnectionString;
	private static String queueName;
	private static String containerName;
	private static int timeInterval;
	private static String containerFoldersName;
	private static String schedulerError;
	private static String invalidStorageKey;
	private static String ioError;
	private static String uriSyntaxError;
	private static String storageError;
	private static String timeZone = "US/Central";

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	public static MyJobScheduler getInstance(String storageString, String queue, String container,
			String containerFolders, String schedulerErrorString, String ioErrorString, int time,
			String uriSyntaxErrorString, String invalidStorageKeyString, String storageExceptionString) {
		storageConnectionString = storageString;
		containerName = container;
		timeInterval = time;
		queueName = queue;
		containerFoldersName = containerFolders;
		if (singleInstance == null) {
			singleInstance = new MyJobScheduler();
			schedulerError = schedulerErrorString;
			ioError = ioErrorString;
			invalidStorageKey = invalidStorageKeyString;
			uriSyntaxError = uriSyntaxErrorString;
			storageError = storageExceptionString;
			run();
		}
		return singleInstance;
	}

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		// code for polling.......

		getQueuedData();
	}

	public static String run() {
		Scheduler scheduler;
		try {
			scheduler = StdSchedulerFactory.getDefaultScheduler();

			JobDetail job = newJob(MyJobScheduler.class).withIdentity("job", "group").build();

			// Trigger the job to run now, and then repeat every timeInterval
			// seconds
			Trigger trigger = newTrigger().withIdentity("trigger", "group").startNow()
					.withSchedule(simpleSchedule().withIntervalInSeconds(timeInterval).repeatForever()).build();

			// Tell quartz to schedule the job using our trigger
			scheduler.scheduleJob(job, trigger);
			// and start it off
			scheduler.start();
		} catch (SchedulerException e) {
			callLog("run", schedulerError + " " + e.getMessage());
		}
		return "";
	}

	private String uploadTextBlob(String containerName, String fileName, String data) {
		try {
			CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

			CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
			CloudBlobContainer container = blobClient.getContainerReference(containerName);
			// Create the container if it does not exist with public access.
			container.createIfNotExists(BlobContainerPublicAccessType.CONTAINER, new BlobRequestOptions(),
					new OperationContext());
			// Get a blob reference for a text file.
			CloudBlockBlob blob = container.getBlockBlobReference(containerFoldersName + "/" + fileName);
			// Upload some text into the blob.
			if (!data.equalsIgnoreCase("connection")) {
				blob.uploadText(data);
			}
		} catch (InvalidKeyException e) {
			callLog("uploadTextBlob", invalidStorageKey + " " + e.getMessage());
		} catch (IOException e) {
			callLog("uploadTextBlob", ioError + " " + e.getMessage());
		} catch (URISyntaxException e) {
			callLog("uploadTextBlob", uriSyntaxError + " " + e.getMessage());
		} catch (StorageException e) {
			callLog("uploadTextBlob", storageError + " " + e.getMessage());
		}
		return "success";
	}

	public void getQueuedData() {
		CloudQueue queue;
		try {
			// Retrieve storage account from connection-string.
			CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

			// Create the queue client.
			CloudQueueClient queueClient = storageAccount.createCloudQueueClient();

			// Retrieve a reference to a queue.
			queue = queueClient.getQueueReference(queueName);

			// Create the queue if it doesn't already exist.
			queue.createIfNotExists();

			// Download the approximate message count from the server.
			queue.downloadAttributes();

			// Retrieve the newly cached approximate message count.
			int cachedMessageCount = (int) queue.getApproximateMessageCount();

			StringBuilder output = new StringBuilder();
			String fieName = (getBatchDateTime() + ".json");
			if (cachedMessageCount > 0
					&& "success".equalsIgnoreCase(uploadTextBlob(containerName, fieName, "connection"))) {

				readQueueData(queue, cachedMessageCount, output, fieName);

			}
		} catch (InvalidKeyException e) {
			callLog("getQueuedData", invalidStorageKey + " " + e.getMessage());
		} catch (URISyntaxException e) {
			callLog("getQueuedData", uriSyntaxError + " " + e.getMessage());
		} catch (StorageException e) {
			callLog("getQueuedData", storageError + " " + e.getMessage());
		} catch (Exception e) {
			callLog("getQueuedData", e.getMessage());
		}

	}

	private void readQueueData(CloudQueue queue, int cachedMessageCount, StringBuilder output, String fieName)
			throws URISyntaxException, StorageException {
		// Retrieve cachedMessageCount messages from the queue with a
		// visibility timeout
		// of 30 seconds.
		JSONArray jsonArray = new JSONArray();
		try {
			for (int i = 0; i < cachedMessageCount; i++) {
				// Retrieve the first visible message in the queue.
				CloudQueueMessage retrievedMessage = queue.retrieveMessage();

				if (retrievedMessage != null) {
					// Do processing for all messages in less than 30
					// seconds,
					String message = retrievedMessage.getMessageContentAsString();
					output.append(message).append(System.lineSeparator());
					// Process the message in less than 30 seconds, and
					// then delete the message.
					queue.deleteMessage(retrievedMessage);
					jsonArray.put(message);
				}
			}
		} catch (StorageException e) {
			callLog("readQueueData", storageError + " " + e.getMessage());
		} finally {
			JSONObject mainObj = new JSONObject();
			mainObj.put("Logs", jsonArray);
			uploadTextBlob(containerName, fieName, mainObj.toString());
		}
	}

	private static void callLog(String methodName, String errorMessage) {

		try {
			URL url = new URL("http://localhost:8589/log/saveLogs");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			String input = "{\"Logs\": [{ \"Microservice\": \"Log\", \"Class\": \"MyJobScheduler.class\", \"Method\":\" "
					+ methodName + "\", \"Status\": \"exception\", \"Message\": \"" + errorMessage
					+ "\", \"DateTime\": " + getBatchDateTime() + "}]}";

			OutputStream os = conn.getOutputStream();
			os.write(input.getBytes());
			os.flush();

			new BufferedReader(new InputStreamReader((conn.getInputStream())));

			conn.disconnect();

		} catch (Exception e) {
			e.getMessage();
		}
	}
	
	public static String getBatchDateTime()
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZZZZ");
		Date datetime = new Date();
		sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
		String batchDateTime =  sdf.format(datetime);
		return batchDateTime;
	}
}
