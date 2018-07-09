package com.logging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

/**
 * @author ShaishavS Initializer class to initialize application as spring boot
 *         app
 */
@SpringBootApplication
@PropertySource(value={"classpath:constant.properties"})

public class AppInitializer {

	public static void main(String[] args) {
		SpringApplication.run(AppInitializer.class, args);
	}
}
