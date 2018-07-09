package com.logging.service;

import java.security.InvalidKeyException;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.logging.utility.ErrorUtil;
import com.logging.utility.MyJobScheduler;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;

/**
 * @author ShaishavS Service class add logs to Azure
 */
@PropertySource(value = { "classpath:constant.properties", "classpath:application.properties" })
@Service
public class LogsServiceImpl implements LogService {

	@Value("${keyvault.secret.connectionString}")
	private String storageConnection;
	@Value("${error.logServiceError}")
	private String logServiceError;
	@Value("${error.emptyRequest}")
	private String emptyRequest;
	@Value("${error.jsonError}")
	private String jsonError;
	@Value("${queue.name}")
	private String queueName;
	@Value("${json.logs}")
	private String jsonLogs;
	@Value("${error.invalidKeyName}")
	private String invalidKeyName;
	@Value("${error.nullValueForKey}")
	private String nullValueForKey;
	@Value("${error.invalidStorageKey}")
	private String invalidStorageKey;
	@Value("${error.storageError}")
	private String storageException;

	@Value("${container.name}")
	private String containerName;
	@Value("${scheduler.timeInterval}")
	private int timeInterval;
	@Value("${container.folders.name}")
	private String containerFoldersName;
	@Value("${error.schedulerError}")
	private String schedulerError;
	@Value("${error.ioError}")
	private String ioError;
	@Value("${error.uriSyntaxError}")
	private String uriSyntaxError;
	@Value("${error.keyVaultError}")
	private String keyVaultError;
	@Autowired
	private ErrorUtil errorUtil;
	private String storageConnectionString;
	private static final Logger logger = LogManager.getLogger(LogsServiceImpl.class);
	JSONArray logArray = new JSONArray();
	/*
	 * This method will add given data in Azure
	 * 
	 * @param inputParam return response
	 * 
	 */
	@Override
	public String getLogs(String inputParam) {
		logger.info("Enter getLogs service");

		storageConnectionString = storageConnection;

		storageConnectionString = getSecretFromKeyVault(storageConnectionString);
		// instantiating Singleton class to start polling
		MyJobScheduler.getInstance(storageConnectionString, queueName, containerName, containerFoldersName,
				schedulerError, ioError, timeInterval, uriSyntaxError, invalidStorageKey, storageException);
		String logresponse = "";
		try {
			// Retrieve storage account from connection-string.
			CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

			// Create the queue client.
			CloudQueueClient queueClient = storageAccount.createCloudQueueClient();

			// Retrieve a reference to a queue.
			CloudQueue queue = queueClient.getQueueReference(queueName);

			// Create the queue if it doesn't already exist.
			queue.createIfNotExists();

			JSONObject jsonObject = new JSONObject(inputParam);
			JSONArray jsonArray = jsonObject.getJSONArray(jsonLogs);
			if (null != jsonArray && jsonArray.length() > 0) {
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject obj = (JSONObject) jsonArray.get(i);

					if (obj.has("Microservice") && obj.has("Class") && obj.has("Method") && obj.has("Status")
							&& obj.has("Message") && obj.has("DateTime")) {

						if (!obj.getString("Microservice").trim().isEmpty()) {
							if (!obj.getString("Class").trim().isEmpty()) {
								if (!obj.getString("Method").trim().isEmpty()) {
									if (!obj.getString("Status").trim().isEmpty()) {
										Object json = obj.get("Message");
										if ( ((json instanceof String) && !obj.getString("Message").trim().isEmpty()) || ((json instanceof JSONObject) && obj.getJSONObject("Message").length()!=0)) {
											if (!obj.getString("DateTime").trim().isEmpty()) {
												CloudQueueMessage message = new CloudQueueMessage(obj.toString());
												queue.addMessage(message);
											} else {
												return errorUtil.getLogError("empty or null value for key DateTime",
														nullValueForKey);
											}
										} else {
											return errorUtil.getLogError("empty or null value for key Message",
													nullValueForKey);
										}
									} else {
										return errorUtil.getLogError("empty or null value for key Status",
												nullValueForKey);
									}
								} else {
									return errorUtil.getLogError("empty or null value for key Method", nullValueForKey);
								}
							} else {
								return errorUtil.getLogError("empty or null value for key Class", nullValueForKey);
							}
						} else {
							return errorUtil.getLogError("empty or null value for key Microservice", nullValueForKey);
						}
					} else {
						return errorUtil.getLogError("invalid JSON key", invalidKeyName);
					}
				}
			} else {
				return errorUtil.getLogError("empty request", emptyRequest);
			}
		} catch (JSONException exception) {
			return errorUtil.getLogError(exception.getMessage(), jsonError);
		} catch (InvalidKeyException exception) {
			return errorUtil.getLogError(exception.getMessage(), invalidStorageKey);
		} catch (StorageException exception) {
			return errorUtil.getLogError(exception.getMessage(), storageException);
		} catch (Exception exception) {
			return errorUtil.getLogError(exception.getMessage(), logServiceError);
		}
		return logresponse;
	}

	/**
	 * This method will fetch the Epic App Orchard authorization secret from
	 * azure key vault
	 * 
	 * @return authSecret
	 */
	public String getSecretFromKeyVault(String key) {
		String authSecret = "";
		try {
			RestTemplate restTemplate = new RestTemplate();
			authSecret = restTemplate.getForObject(key, String.class);
			JSONObject jsonObject = new JSONObject(authSecret);
			String status = jsonObject.getString("status");
			if("success".equals(status))
			{
				authSecret = jsonObject.getString("response");
			}
			else
			{
				String errorResponse = errorUtil.getLogError("Error in  get secret from key vault : "+authSecret, keyVaultError);
				appendLog("getSecretFromKeyVault", "Exception", errorResponse);
				pushLogIntoLoginservice();
			}
		} catch (JSONException jsonException) {
			String errorResponse = errorUtil.getLogError("JSON Error: " + jsonException.getMessage(),
					jsonError);
			appendLog("getSecretFromKeyVault", "Exception", errorResponse);
			pushLogIntoLoginservice();
			System.exit(0);
		} catch (Exception e) {
			String errorResponse = errorUtil.getLogError("KeyVault Exception: " + e.getMessage(),
					logServiceError);
			appendLog("getSecretFromKeyVault", "Exception", errorResponse);
			pushLogIntoLoginservice();
			System.exit(0);
		}
		return authSecret;
	}
	public String pushLogIntoLoginservice() {
		String response = " ";
		JSONObject logJsonObject = new JSONObject();
		try {
			logJsonObject.put("Logs", logArray);
			/*RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> entity = new HttpEntity<String>(logJsonObject.toString(), headers);*/
			response = getLogs(logJsonObject.toString());
			logArray = new JSONArray();
		} catch (JSONException jsonException) {

		}
		return response;
	}

	public void appendLog(String methodName, String type, String message) {
		JSONObject logJsonObject = new JSONObject();
		try {
			logJsonObject.put("Microservice", "Log Service");
			logJsonObject.put("Class", "LogsServiceImpl");
			logJsonObject.put("Method", methodName);
			logJsonObject.put("Status", type);
			logJsonObject.put("Message", message);
			logJsonObject.put("DateTime", new Date());
			logArray.put(logJsonObject);
		} catch (JSONException e) {
		}
	}
}
