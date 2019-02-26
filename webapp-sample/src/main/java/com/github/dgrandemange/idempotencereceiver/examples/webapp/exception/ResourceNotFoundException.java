package com.github.dgrandemange.idempotencereceiver.examples.webapp.exception;

public class ResourceNotFoundException extends ResourceManagementException {

	private static final long serialVersionUID = 1L;

	public ResourceNotFoundException(String id) {
		super(String.format("No book found for provided id=%s", id));
	}

}
