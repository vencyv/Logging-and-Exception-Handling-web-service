package com.logging.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.logging.service.LogService;
import com.logging.utility.ErrorUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author ShaishavS
 * 
 *         Controller class to Get logs from other microservices
 * 
 */
@RestController

@Api(value = "LogController")
public class LogControllerImpl implements LogController {

	@Autowired
	LogService logService;

	@Autowired
	private ErrorUtil errorUtil;

	@Value("${error.emptyRequestParam}")
	private String emptyRequestParam;

	@Value("${error.logServiceError}")
	private String logServiceError;

	/*
	 * This method will accept the post request to get logs and add the given
	 * data to Azure
	 * 
	 * @param inputParam
	 * 
	 * @return success or error response
	 */
	@ApiOperation(value = "Get logs data from other microservices", httpMethod = "POST", tags = "getLogs", consumes = "application/json", produces = "application/json")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "inputParam", required = true, dataType = "application/json", paramType = "body") })
	@PostMapping(path = "/log/saveLogs", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)

	public @ResponseBody String getLogs(@RequestBody String inputParam) {

		String logData = "";
		try {
			if (inputParam != null && !inputParam.trim().isEmpty() && new JSONObject(inputParam).length() != 0) {
				logData = logService.getLogs(inputParam);
			} else {
				return errorUtil.getLogError("Invalid or empty request parameter", emptyRequestParam);
			}
		} catch (Exception exception) {
			return errorUtil.getLogError(exception.getMessage(), logServiceError);
		}
		return logData;
	}
}
