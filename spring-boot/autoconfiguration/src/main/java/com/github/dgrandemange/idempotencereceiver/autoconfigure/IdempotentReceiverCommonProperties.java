package com.github.dgrandemange.idempotencereceiver.autoconfigure;

import java.util.Objects;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentReceiverCommonConfiguration;

@ConfigurationProperties(prefix = IdempotentReceiverCommonProperties.PREFIX)
public class IdempotentReceiverCommonProperties extends IdempotentReceiverCommonConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdempotentReceiverCommonProperties.class);

	public static final String PREFIX = "idempotence-receiver";

	private static final String SPRING_APPLICATION_NAME_PROPERTY = "spring.application.name";

	public static final String DEFAULT_NAMESPACE = "default";

	@Value("${" + SPRING_APPLICATION_NAME_PROPERTY + "}")
	private String springAppName;

	@PostConstruct
	public void postConstruct() {
		if (Objects.isNull(this.getNamespace()) || this.getNamespace().trim().isEmpty()) {
			// Try setting default namespace to spring boot app name property when defined
			if (Objects.isNull(springAppName) || springAppName.trim().isEmpty()) {
				// Spring application name not defined : set to default
				LOGGER.warn(
				        "Neither of '{}.namespace' and '{}' properties is defined : setting to default namespace '{}'",
				        PREFIX, SPRING_APPLICATION_NAME_PROPERTY, DEFAULT_NAMESPACE);

				this.setNamespace(DEFAULT_NAMESPACE);
			} else {
				LOGGER.info("'{}.namespace' property not defined : setting to spring application name '{}'",
				        PREFIX, springAppName);

				this.setNamespace(springAppName);
			}
		}
		
		LOGGER.info(super.toString());
	}
}
