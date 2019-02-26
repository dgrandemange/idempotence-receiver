package com.github.dgrandemange.idempotencereceiver.autoconfigure.repository.infinispan;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.dgrandemange.idempotencereceiver.autoconfigure.IdempotentReceiverCommonProperties;
import com.github.dgrandemange.idempotencereceiver.infinispan.hotrod.service.support.RepositoryInfinispanCacheImpl;

@Configuration
@ConditionalOnProperty(name = IdempotentReceiverCommonProperties.PREFIX
        + ".repository.type", havingValue = RepositoryInfinispanCacheImpl.REPOSITORY_TYPE)
@EnableConfigurationProperties(IdempotentReceiverRepositoryInfinispanProperties.class)
public class IdempotentReceiverRepositoryInfinispanAutoConfiguration {
	private static final String PROPERTY_NOT_CONFIGURED_ERRMSG = "%s not configured properly. Please check "
	        + IdempotentReceiverRepositoryInfinispanProperties.PREFIX + ".* properties settings in configuration file.";

	@Autowired
	IdempotentReceiverRepositoryInfinispanProperties properties;

	@Bean
	@ConditionalOnMissingBean
	public RepositoryInfinispanCacheImpl idempotentRepository() {
		// Check configured properties
		Objects.requireNonNull(properties.getHotrodClientConfigPath(),
		        String.format(PROPERTY_NOT_CONFIGURED_ERRMSG, "Hotrod client configuration path"));

		Objects.requireNonNull(properties.getTtlMs(),
		        String.format(PROPERTY_NOT_CONFIGURED_ERRMSG, "Entries lifetime in cache (Time To Live)"));

		Objects.requireNonNull(properties.getCacheName(),
		        String.format(PROPERTY_NOT_CONFIGURED_ERRMSG, "Infinispan cache name"));

		RepositoryInfinispanCacheImpl repo = new RepositoryInfinispanCacheImpl();
		repo.setRepositoryConfig(properties);
		return repo;
	}

}
