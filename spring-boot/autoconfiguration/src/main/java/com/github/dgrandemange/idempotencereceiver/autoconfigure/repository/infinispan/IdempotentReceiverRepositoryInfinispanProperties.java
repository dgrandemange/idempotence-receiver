package com.github.dgrandemange.idempotencereceiver.autoconfigure.repository.infinispan;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.github.dgrandemange.idempotencereceiver.autoconfigure.IdempotentReceiverCommonProperties;
import com.github.dgrandemange.idempotencereceiver.infinispan.hotrod.model.IdempotentReceiverInfinispanHotrodConfiguration;
import com.github.dgrandemange.idempotencereceiver.infinispan.hotrod.service.support.RepositoryInfinispanCacheImpl;

@ConfigurationProperties(prefix = IdempotentReceiverRepositoryInfinispanProperties.PREFIX)
public class IdempotentReceiverRepositoryInfinispanProperties extends IdempotentReceiverInfinispanHotrodConfiguration {

	public static final String PREFIX = IdempotentReceiverCommonProperties.PREFIX + ".repository."
	        + RepositoryInfinispanCacheImpl.REPOSITORY_TYPE;

	private static final Logger LOGGER = LoggerFactory
	        .getLogger(IdempotentReceiverRepositoryInfinispanProperties.class);

	@PostConstruct
	public void postConstruct() {
		LOGGER.info(super.toString());
	}

}
