package com.github.dgrandemange.idempotencereceiver.infinispan.hotrod.model;

public class IdempotentReceiverInfinispanHotrodConfiguration {

	/**
	 * Location of the hotrod client configuration resource<br>
	 * Can either be a in the classpath (use the '{@code classpath:}' prefix), or in
	 * the filesystem (use the '{@code file:}' prefix)<br>
	 * 
	 * @See <a href=
	 *      "http://infinispan.org/docs/stable/user_guide/user_guide.html#hotrod_java_client">Infinispan
	 *      - userguide - Hotrod Java client</a>
	 * @See <a href=
	 *      "https://docs.jboss.org/infinispan/9.4/apidocs/org/infinispan/client/hotrod/configuration/package-summary.html#package.description">Hotrod
	 *      Java client configuration properties</a>
	 */
	private String hotrodClientConfigPath;

	/**
	 * Should match a cache declared by infinispan server side
	 */
	private String cacheName;

	/**
	 * Time To Live : lifetime of an entry in the cache; once lifetime is reached,
	 * the entry will be evicted by the cache
	 */
	private long ttlMs;

	public String getHotrodClientConfigPath() {
		return hotrodClientConfigPath;
	}

	public void setHotrodClientConfigPath(String hotrodClientConfigPath) {
		this.hotrodClientConfigPath = hotrodClientConfigPath;
	}

	public String getCacheName() {
		return cacheName;
	}

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	public long getTtlMs() {
		return ttlMs;
	}

	public void setTtlMs(long ttlMs) {
		this.ttlMs = ttlMs;
	}

	@Override
	public String toString() {
		return "IdempotentReceiverInfinispanHotrodConfiguration [hotrodClientConfigPath=" + hotrodClientConfigPath
		        + ", cacheName=" + cacheName + ", ttlMs=" + ttlMs + "]";
	}

}
