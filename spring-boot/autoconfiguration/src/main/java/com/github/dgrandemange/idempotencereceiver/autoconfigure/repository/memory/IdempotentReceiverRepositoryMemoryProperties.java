package com.github.dgrandemange.idempotencereceiver.autoconfigure.repository.memory;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.github.dgrandemange.idempotencereceiver.api.service.support.RepositoryInMemoryGcManagedImpl;
import com.github.dgrandemange.idempotencereceiver.autoconfigure.IdempotentReceiverCommonProperties;

@ConfigurationProperties(prefix = IdempotentReceiverRepositoryMemoryProperties.PREFIX)
public class IdempotentReceiverRepositoryMemoryProperties {

	public static final String PREFIX = IdempotentReceiverCommonProperties.PREFIX + ".repository."
	        + RepositoryInMemoryGcManagedImpl.REPOSITORY_TYPE;

	private static final Logger LOGGER = LoggerFactory.getLogger(IdempotentReceiverRepositoryMemoryProperties.class);

	@PostConstruct
	public void postConstruct() {
		LOGGER.info(super.toString());
	}

}
