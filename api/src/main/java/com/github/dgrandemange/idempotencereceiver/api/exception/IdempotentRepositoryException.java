package com.github.dgrandemange.idempotencereceiver.api.exception;

public class IdempotentRepositoryException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public IdempotentRepositoryException() {
		super();
	}

	public IdempotentRepositoryException(String message, Throwable cause, boolean enableSuppression,
	        boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public IdempotentRepositoryException(String message, Throwable cause) {
		super(message, cause);
	}

	public IdempotentRepositoryException(String message) {
		super(message);
	}

	public IdempotentRepositoryException(Throwable cause) {
		super(cause);
	}

}
