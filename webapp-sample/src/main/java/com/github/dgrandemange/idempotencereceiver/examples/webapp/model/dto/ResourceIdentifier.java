package com.github.dgrandemange.idempotencereceiver.examples.webapp.model.dto;

public class ResourceIdentifier {

	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "ResourceIdentifier [id=" + id + "]";
	}

}
