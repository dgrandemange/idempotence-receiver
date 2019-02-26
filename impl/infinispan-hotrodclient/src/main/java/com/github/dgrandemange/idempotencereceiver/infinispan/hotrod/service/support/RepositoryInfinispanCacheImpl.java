package com.github.dgrandemange.idempotencereceiver.infinispan.hotrod.service.support;

import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.api.BasicCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.github.dgrandemange.idempotencereceiver.api.exception.IdempotentRepositoryException;
import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentMethodResult;
import com.github.dgrandemange.idempotencereceiver.api.service.IdempotentRepository;
import com.github.dgrandemange.idempotencereceiver.infinispan.hotrod.model.IdempotentReceiverInfinispanHotrodConfiguration;

public class RepositoryInfinispanCacheImpl implements IdempotentRepository, ResourceLoaderAware {

	public static final String REPOSITORY_TYPE = "infinispan-cache";

	private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryInfinispanCacheImpl.class);

	private IdempotentReceiverInfinispanHotrodConfiguration repositoryConfig;

	/**
	 * See <a href=
	 * "https://www.mkyong.com/spring/spring-resource-loader-with-getresource-example/">Spring
	 * Resource loader with getResource() example</a>
	 */
	private ResourceLoader resourceLoader;

	private RemoteCacheManager rcm;

	private RemoteCache<String, IdempotentMethodResult> cache;

	@PostConstruct
	public void initIt() throws Exception {
		String hotrodClientConfigPath = repositoryConfig.getHotrodClientConfigPath();

		LOGGER.info("Try initializing infinispan idempotent repository from following config : {} ...",
		        repositoryConfig);

		Resource resource = resourceLoader.getResource(hotrodClientConfigPath);
		try (InputStream is = resource.getInputStream()) {
			Properties props = new Properties();
			props.load(resource.getInputStream());
			Configuration hotrodClientConfig = new ConfigurationBuilder().withProperties(props).build();

			LOGGER.debug("Creating remote cache manager ...");
			this.rcm = new RemoteCacheManager(hotrodClientConfig, true);

			initCache();

			LOGGER.info("Infinispan idempotent repository successfully initialized");
		} catch (HotRodClientException e) {
			LOGGER.warn(
			        "CAUTION : idempotence may not be handled properly. Cannot initialize infinispan idempotent repository. Cause : {}",
			        e.getMessage());
		} catch (Exception e) {
			LOGGER.error("Cannot initialize infinispan idempotent repository. Cause : {}", e.getMessage(), e);
			throw e;
		}
	}

	@PreDestroy
	public void cleanUp() throws Exception {
		LOGGER.info("Try stopping infinispan idempotent repository ...");
		if (rcm != null) {
			rcm.stop();
			rcm.close();
		}
		LOGGER.info("Infinispan idempotent repository now stopped");
	}

	@Override
	public IdempotentMethodResult register(String idempotencyKey, IdempotentMethodResult imr) {
		try {
			checkCacheIsInitialized();
			getCache().put(idempotencyKey, imr, repositoryConfig.getTtlMs(), TimeUnit.MILLISECONDS);
			return imr;
		} catch (Exception e) {
			throw new IdempotentRepositoryException(e);
		}
	}

	@Override
	public IdempotentMethodResult unregister(String idempotencyKey) {
		try {
			checkCacheIsInitialized();
			return getCache().remove(idempotencyKey);
		} catch (Exception e) {
			throw new IdempotentRepositoryException(e);
		}
	}

	@Override
	public IdempotentMethodResult find(String idempotencyKey) {
		try {
			checkCacheIsInitialized();
			return getCache().get(idempotencyKey);
		} catch (Exception e) {
			throw new IdempotentRepositoryException(e);
		}
	}

	void checkCacheIsInitialized() {
		if (Objects.isNull(this.getCache())) {
			initCache();
		}
	}

	synchronized void initCache() {
		LOGGER.debug("Retrieving cache '{}' from remote cache manager ...", repositoryConfig.getCacheName());
		setCache(this.rcm.getCache(repositoryConfig.getCacheName()));
	}

	void setCache(RemoteCache<String, IdempotentMethodResult> cache) {
		this.cache = cache;
	}

	public BasicCache<String, IdempotentMethodResult> getCache() {
		return this.cache;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public void setRepositoryConfig(IdempotentReceiverInfinispanHotrodConfiguration repositoryConfig) {
		this.repositoryConfig = repositoryConfig;
	}
}
