package com.company.ErrorHandlingToolRegistry;

import io.micrometer.core.util.internal.logging.Slf4JLoggerFactory;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.logback.LogbackLoggingSystem;

@SpringBootApplication
public class ErrorHandlingToolRegistryApplication {

	public static void main(String[] args) {
		SpringApplication.run(ErrorHandlingToolRegistryApplication.class, args);
	}

}
