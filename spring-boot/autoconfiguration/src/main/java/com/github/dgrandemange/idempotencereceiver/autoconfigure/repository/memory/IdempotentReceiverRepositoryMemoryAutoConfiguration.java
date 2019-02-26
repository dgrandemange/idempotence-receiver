package com.github.dgrandemange.idempotencereceiver.autoconfigure.repository.memory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.dgrandemange.idempotencereceiver.api.service.IdempotentRepository;
import com.github.dgrandemange.idempotencereceiver.api.service.support.RepositoryInMemoryGcManagedImpl;
import com.github.dgrandemange.idempotencereceiver.autoconfigure.IdempotentReceiverCommonProperties;

@Configuration
@ConditionalOnProperty(name = IdempotentReceiverCommonProperties.PREFIX
        + ".repository.type", havingValue = RepositoryInMemoryGcManagedImpl.REPOSITORY_TYPE)
public class IdempotentReceiverRepositoryMemoryAutoConfiguration {
	@Bean
	@ConditionalOnMissingBean
	IdempotentRepository idempotentRepository() {
		return new RepositoryInMemoryGcManagedImpl();
	}
}
