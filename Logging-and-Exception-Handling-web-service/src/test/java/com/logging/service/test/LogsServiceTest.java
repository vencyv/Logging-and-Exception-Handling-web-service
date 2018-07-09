package com.logging.service.test;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.logging.AppInitializer;
import com.logging.service.LogsServiceImpl;

@ContextConfiguration(classes = { AppInitializer.class })
@WebAppConfiguration
public class LogsServiceTest extends AbstractTestNGSpringContextTests {

	@Autowired
	private LogsServiceImpl logsServiceImpl;

	@DataProvider(name = "logGetDataProvider")
	public static Object[][] epicGetDataProvide() {
		return new Object[][] { { "{\"Logs\": []}", "ERROR-4003" }, { "{\"Log\": []}", "ERROR-4002" },
				{ "{\"Logs\": [{\"1\":\"1\"}]}", "ERROR-4004" },
				{ "{\"Logs\": [{ \"Microservice\": \"Log\", \"Class\": \" – class name -- \", \"Method\": \" – method name -- \", \"Status\": \"success | error | exception\", \"Message\": \" -– log message -- \", \"DateTime\": \"2018-06-02T23:01:41\"}]}",
						"ERROR-4005" },
				{ "{\"Logs\": [{ \"Microservice\": \"Log\", \"Class\": \"\", \"Method\": \" – method name -- \", \"Status\": \"success | error | exception\", \"Message\": \" -– log message -- \", \"DateTime\": \"2018-06-02T23:01:41\"}]}",
						"ERROR-4005" },
				{ "{\"Logs\": [{ \"Microservice\": \"Log\", \"Class\": \" – class name -- \", \"Method\": \"\", \"Status\": \"success | error | exception\", \"Message\": \" -– log message -- \", \"DateTime\": \"2018-06-02T23:01:41\"}]}",
						"ERROR-4005" },
				{ "{\"Logs\": [{ \"Microservice\": \"Log\", \"Class\": \" – class name -- \", \"Method\": \" – method name -- \", \"Status\": \"\", \"Message\": \" -– log message -- \", \"DateTime\": \"2018-06-02T23:01:41\"}]}",
						"ERROR-4005" },
				{ "{\"Logs\": [{ \"Microservice\": \"Log\", \"Class\": \" – class name -- \", \"Method\": \" – method name -- \", \"Status\": \"success | error | exception\", \"Message\": \"\", \"DateTime\": \"2018-06-02T23:01:41\"}]}",
						"ERROR-4005" },
				{ "{\"Logs\": [{ \"Microservice\": \"Log\", \"Class\": \" – class name -- \", \"Method\": \" – method name -- \", \"Status\": \"success | error | exception\", \"Message\": \" -– log message -- \", \"DateTime\": \"\"}]}",
						"ERROR-4005" },
				{ "{\"Logs\": [{ \"Microservice\": \"Log\" \"Class\": \" – class name -- \", \"Method\": \" – method name -- \", \"Status\": \"success | error | exception\", \"Message\": \" -– log message -- \", \"DateTime\": \"\"}]}",
						"ERROR-4002" } };
	}

	@Test(dataProvider = "logGetDataProvider")
	public void testGetEpicData(String inputParam, String expectedResult) {
		try {
			String response = logsServiceImpl.getLogs(inputParam);
			JSONObject jsonResponse = new JSONObject(response);
			Assert.assertEquals(jsonResponse.get("code").toString(), expectedResult);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
