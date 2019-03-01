package com.github.dgrandemange.idempotencereceiver.infinispan.hotrod.model;

public class IdempotentReceiverInfinispanHotrodConfiguration {

	private String hotrodClientConfigPath;

	private String cacheName;

	private long ttlMs;

	/**
	 * Location of the hotrod client configuration resource<br>
	 * Can either be a in the classpath (use the '{@code classpath:}' prefix), or in
	 * the filesystem (use the '{@code file:}' prefix)<br>
	 * 
	 * @see <a href=
	 *      "http://infinispan.org/docs/stable/user_guide/user_guide.html#hotrod_java_client">Infinispan
	 *      - userguide - Hotrod Java client</a>
	 * @see <a href=
	 *      "https://docs.jboss.org/infinispan/9.4/apidocs/org/infinispan/client/hotrod/configuration/package-summary.html#package.description">Hotrod
	 *      Java client configuration properties</a>
	 * @return Hot Rod client configuration path
	 */
	public String getHotrodClientConfigPath() {
		return hotrodClientConfigPath;
	}

	/**
	 * @param hotrodClientConfigPath
	 *            See {@link #getHotrodClientConfigPath()}
	 */
	public void setHotrodClientConfigPath(String hotrodClientConfigPath) {
		this.hotrodClientConfigPath = hotrodClientConfigPath;
	}

	/**
	 * @return name of a cache declared in the infinispan server
	 */
	public String getCacheName() {
		return cacheName;
	}

	/**
	 * @param cacheName
	 *            See {@link #getCacheName()}
	 */
	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	/**
	 * @return lifetime (Time To Live) in milliseconds of entry in cache; once entry
	 *         lifetime is reached, entry will be evicted
	 */
	public long getTtlMs() {
		return ttlMs;
	}

	/**
	 * @param ttlMs
	 *            See {@link #getTtlMs()}
	 */
	public void setTtlMs(long ttlMs) {
		this.ttlMs = ttlMs;
	}

	@Override
	public String toString() {
		return "IdempotentReceiverInfinispanHotrodConfiguration [hotrodClientConfigPath=" + hotrodClientConfigPath
		        + ", cacheName=" + cacheName + ", ttlMs=" + ttlMs + "]";
	}

}
