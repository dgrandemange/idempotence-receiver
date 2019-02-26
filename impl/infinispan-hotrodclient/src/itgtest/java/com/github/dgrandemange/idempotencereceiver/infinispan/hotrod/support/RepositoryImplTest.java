package com.github.dgrandemange.idempotencereceiver.infinispan.hotrod.support;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map.Entry;

import org.fest.assertions.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.github.dgrandemange.idempotencereceiver.api.exception.IdempotentReceiverException;
import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentMethodResult;
import com.github.dgrandemange.idempotencereceiver.infinispan.hotrod.model.IdempotentReceiverInfinispanHotrodConfiguration;
import com.github.dgrandemange.idempotencereceiver.infinispan.hotrod.service.support.RepositoryInfinispanCacheImpl;

/**
 * <h1>Pre-requisites :</h1>
 * 
 * <p>Launch an infinispan-server through docker (make sure to expose port 11222) :</p>
 *
 * {@code docker run --rm -p 11222:11222 -it jboss/infinispan-server}
 *
 * <p>NB : in case the docker host is a VM running on a Windows OS, ensure port 11222 is accessible through a SSH tunnel</p>
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class RepositoryImplTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryImplTest.class);

	final static private long DEFAULT_TTL_MS = 1000L;
	private static final String PREFIX_NAME = "idempotent";
	private static final String SERVICE_NAME = "itgtest";

	@Configuration
	public static class RepositoryImplTestConfiguration {

		@Bean
		public IdempotentReceiverInfinispanHotrodConfiguration getRepositoryConfiguration() {
			IdempotentReceiverInfinispanHotrodConfiguration repoConfig = new IdempotentReceiverInfinispanHotrodConfiguration();
			repoConfig.setHotrodClientConfigPath("file:./target/classes/itgtest-hotrodclientconfig.properties");
			repoConfig.setCacheName("default");
			repoConfig.setTtlMs(DEFAULT_TTL_MS);
			return repoConfig;
		}

		@Bean
		public RepositoryInfinispanCacheImpl getIdempotentRepository() {
			RepositoryInfinispanCacheImpl repo = new RepositoryInfinispanCacheImpl();
			repo.setRepositoryConfig(getRepositoryConfiguration());
			return repo;
		}
	}

	@Autowired
	private RepositoryInfinispanCacheImpl repo;

	@Test
	public void testRepo() throws InterruptedException, IdempotentReceiverException {
		String key = null;
		IdempotentMethodResult imr = null;

		for (int i = 0; i < 10; i++) {
			key = generateIdempotentKey("" + i);
			imr = IdempotentMethodResult.builder().startedAt(Instant.now()).withIdempotencyKey(key).build();
			repo.register(key, imr);
		}

		// Check last inserted entry is available in cache
		Assertions.assertThat(repo.find(key)).isEqualTo(imr);

		// List cache entries
		for (Iterator<Entry<String, IdempotentMethodResult>> it = repo.getCache().entrySet().iterator(); it
		        .hasNext();) {
			Entry<String, IdempotentMethodResult> e = it.next();
			LOGGER.info("{} -> {}", e.getKey(), e.getValue().getStartedAt());
		}

		// Wait for server to evict entries from the cache
		Thread.sleep(DEFAULT_TTL_MS + 1000);

		// Check last inserted entry is no more available in the cache
		Assertions.assertThat(repo.find(key)).isNull();
	}

	private String generateIdempotentKey(String uid) {
		return String.format("%s.%s.%s", PREFIX_NAME, SERVICE_NAME, uid);
	}

}
