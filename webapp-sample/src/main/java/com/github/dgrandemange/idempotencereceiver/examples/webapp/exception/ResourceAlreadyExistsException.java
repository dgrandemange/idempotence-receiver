package com.github.dgrandemange.idempotencereceiver.examples.webapp.exception;

public class ResourceAlreadyExistsException extends ResourceManagementException {

	private static final long serialVersionUID = 1L;

	public ResourceAlreadyExistsException(String id) {
		super(String.format("Book already registered (id=%s)", id));
	}

}
