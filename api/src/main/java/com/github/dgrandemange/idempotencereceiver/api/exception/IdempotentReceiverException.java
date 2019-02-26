package com.github.dgrandemange.idempotencereceiver.api.exception;

public class IdempotentReceiverException extends Exception {

	private static final long serialVersionUID = 1L;

	public IdempotentReceiverException() {
		super();
	}

	public IdempotentReceiverException(String message, Throwable cause, boolean enableSuppression,
	        boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public IdempotentReceiverException(String message, Throwable cause) {
		super(message, cause);
	}

	public IdempotentReceiverException(String message) {
		super(message);
	}

	public IdempotentReceiverException(Throwable cause) {
		super(cause);
	}

}
