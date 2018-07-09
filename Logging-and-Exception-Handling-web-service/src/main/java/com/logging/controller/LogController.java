package com.logging.controller;

/**
 * @author ShaishavS Add logs and exception in Azure
 */
public interface LogController {

	/**
	 * Get logs data from other microservices
	 * 
	 * @param inputParam
	 * @return success or error response
	 */
	public String getLogs(String inputParam);

}
