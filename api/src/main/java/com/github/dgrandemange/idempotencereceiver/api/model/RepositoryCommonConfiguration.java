package com.github.dgrandemange.idempotencereceiver.api.model;

public class RepositoryCommonConfiguration {

	/**
	 * Indicates which repository implementation to use
	 */
	private String type;

	/**
	 * Nested repository resiliency configuration
	 */
	private ResiliencyConfiguration resiliency = new ResiliencyConfiguration();

	public ResiliencyConfiguration getResiliency() {
		return resiliency;
	}

	public void setResiliency(ResiliencyConfiguration resiliency) {
		this.resiliency = resiliency;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "RepositoryCommonConfiguration [type=" + type + ", resiliency=" + resiliency + "]";
	}

}

