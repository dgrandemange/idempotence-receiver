package com.github.dgrandemange.idempotencereceiver.examples.webapp.exception;

public class ResourceManagementException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ResourceManagementException() {
		super();
	}

	public ResourceManagementException(String message, Throwable cause, boolean enableSuppression,
	        boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ResourceManagementException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResourceManagementException(String message) {
		super(message);
	}

	public ResourceManagementException(Throwable cause) {
		super(cause);
	}

}
