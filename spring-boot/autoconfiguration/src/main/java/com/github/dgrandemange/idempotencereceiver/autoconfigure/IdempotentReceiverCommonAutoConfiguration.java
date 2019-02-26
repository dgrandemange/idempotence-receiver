package com.github.dgrandemange.idempotencereceiver.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.web.filter.GenericFilterBean;

import com.github.dgrandemange.idempotencereceiver.api.aspect.IdempotentReceiverAspect;
import com.github.dgrandemange.idempotencereceiver.api.service.support.ResilientIdempotentRepository;
import com.github.dgrandemange.idempotencereceiver.api.web.filter.CacheRequestContentFilter;
import com.github.dgrandemange.idempotencereceiver.autoconfigure.repository.infinispan.IdempotentReceiverRepositoryInfinispanAutoConfiguration;
import com.github.dgrandemange.idempotencereceiver.autoconfigure.repository.memory.IdempotentReceiverRepositoryMemoryAutoConfiguration;

@Configuration
@AutoConfigureAfter({ IdempotentReceiverRepositoryMemoryAutoConfiguration.class,
        IdempotentReceiverRepositoryInfinispanAutoConfiguration.class })
@ConditionalOnBean(name = "idempotentRepository")
@ComponentScan(basePackageClasses = { com.github.dgrandemange.idempotencereceiver.api.PackageMarker.class })
@EnableConfigurationProperties(IdempotentReceiverCommonProperties.class)
@EnableAspectJAutoProxy
public class IdempotentReceiverCommonAutoConfiguration {

	@Bean
	@Primary
	ResilientIdempotentRepository resilientIdempotencyRepository() {
		return new ResilientIdempotentRepository();
	}

	@Bean
	IdempotentReceiverAspect idempotentReceiverAspect() {
		return new IdempotentReceiverAspect();
	}

	@Bean
	@ConditionalOnProperty(prefix = IdempotentReceiverCommonProperties.PREFIX, name = "register-cache-request-content-filter", matchIfMissing = true)
	GenericFilterBean cacheRequestContentFilter() {
		return new CacheRequestContentFilter();
	}

	@Bean
	@Conditional(SpringfoxSwaggerPluginSupportCondition.class)
	public Object idempotencyKeyHeaderBuilder()
	        throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		com.github.dgrandemange.idempotencereceiver.api.springfox.ext.PackageMarker.class.getPackage().getName();
		Class<?> forName = Class.forName(String.format("%s.IdempotencyKeyHeaderBuilder",
		        com.github.dgrandemange.idempotencereceiver.api.springfox.ext.PackageMarker.class.getPackage().getName()));
		return forName.newInstance();
	}

}
