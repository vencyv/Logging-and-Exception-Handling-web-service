package com.logging.controller.test;

import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.logging.AppInitializer;
import com.logging.config.AppConfig;
import com.logging.service.LogService;


@ContextConfiguration(classes = { AppInitializer.class, AppConfig.class })
@WebMvcTest
public class LogControllerTest extends AbstractTestNGSpringContextTests {

	@Autowired
	MockMvc mockMvc;

	@SpyBean
	@Autowired
	LogService logDataService;

	@DataProvider(name = "logDataProvider")
	public static Object[][] logDataProvide() {
		return new Object[][] { { "{}", "ERROR-4001" }, { "{ } ", "ERROR-4001" } };
	}

	@Test(dataProvider = "logDataProvider")
	public void testGetLogData_InvalidInputParam(String inputParam, String expectedResult) {
		try {
			MvcResult mvcResult = mockMvc
					.perform(post("/log/saveLogs").contentType(MediaType.APPLICATION_JSON).content(inputParam))
					.andReturn();
			String response = mvcResult.getResponse().getContentAsString();
			JSONObject jsonResponse = new JSONObject(response);
			Assert.assertEquals(jsonResponse.get("code").toString(), expectedResult);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test()
	public void testGetLogData_ValidInputParam() {
		String inputParam = "{\"Logs\": [{ \"Microservice\": \" – Microservice name -- \", \"Class\": \" – class name -- \", \"Method\": \" – method name -- \", \"Status\": \"success | error | exception\", \"Message\": \" -– log message -- \", \"DateTime\": \"2018-06-02T23:01:41\"}]}";
				//"{\"Logs\":[\"{\"Status\":\"success | error | exception\",\"Microservice\":\" \\u2013 Microservice name -- \",\"Message\":\" -\\u2013 log message -- \",\"Class\":\" \\u2013 class name -- \",\"Method\":\" \\u2013 method name -- \",\"DateTime\":\"2018-06-02T23:01:41\"}\",\"{\"Status\":\" status \",\"Microservice\":\" \\u2013 Microservice name -- \",\"Message\":\" -\\u2013 log message -- \",\"Class\":\" \\u2013 class name -- \",\"Method\":\" \\u2013 method name -- \",\"DateTime\":\"2018-06-02T23:01:41\"}\",\"{\"Status\":\"success | error | exception\",\"Microservice\":\" \\u2013 Microservice name -- \",\"Message\":\" -\\u2013 log message -- \",\"Class\":\" \\u2013 class name -- \",\"Method\":\" \\u2013 method name -- \",\"DateTime\":\"2018-06-02T23:01:41\"}\",\"{\"Status\":\" status \",\"Microservice\":\" \\u2013 Microservice name -- \",\"Message\":\" -\\u2013 log message -- \",\"Class\":\" \\u2013 class name -- \",\"Method\":\" \\u2013 method name -- \",\"DateTime\":\"2018-06-02T23:01:41\"}\"]}";
		String expectedResult = "";
		try {
			doReturn(expectedResult).when(logDataService).getLogs(inputParam);
			MvcResult mvcResult = mockMvc
					.perform(post("/log/saveLogs").contentType(MediaType.APPLICATION_JSON).content(inputParam))
					.andReturn();
			Assert.assertEquals(mvcResult.getResponse().getContentAsString(), expectedResult);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
