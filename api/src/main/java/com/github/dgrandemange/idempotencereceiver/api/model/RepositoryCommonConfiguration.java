package com.github.dgrandemange.idempotencereceiver.api.model;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

import com.github.dgrandemange.idempotencereceiver.api.service.IdempotentRepository;

public class RepositoryCommonConfiguration {

	private String type;

	@NestedConfigurationProperty
	private ResiliencyConfiguration resiliency = new ResiliencyConfiguration();

	/**
	 * @return nested repository resiliency configuration
	 */
	public ResiliencyConfiguration getResiliency() {
		return resiliency;
	}

	/**
	 * @param resiliency
	 *            See {@link #getResiliency()}
	 */
	public void setResiliency(ResiliencyConfiguration resiliency) {
		this.resiliency = resiliency;
	}

	/**
	 * <p>
	 * Indicates which repository implementation to use.
	 * </p>
	 * <p>
	 * See all available implementations of {@link IdempotentRepository} to get
	 * their respective type.
	 * </p>
	 * 
	 * @return type of the repository implementation to use
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 *            See {@link #getType()}
	 */
	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "RepositoryCommonConfiguration [type=" + type + ", resiliency=" + resiliency + "]";
	}

}
