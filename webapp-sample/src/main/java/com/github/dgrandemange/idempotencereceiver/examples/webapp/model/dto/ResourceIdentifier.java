package com.github.dgrandemange.idempotencereceiver.examples.webapp.model.dto;

import java.io.Serializable;

public class ResourceIdentifier implements Serializable {

	private static final long serialVersionUID = 1L;

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
