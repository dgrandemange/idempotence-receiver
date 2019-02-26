package com.github.dgrandemange.idempotencereceiver.api.exception;

public class MissingIdempotencyKeyHeaderException extends IdempotentReceiverException {

	private static final long serialVersionUID = 1L;

	private final String missingHeaderName;

	public MissingIdempotencyKeyHeaderException(String missingHeaderName) {
		super();
		this.missingHeaderName = missingHeaderName;
	}

	public String getMissingHeaderName() {
		return missingHeaderName;
	}

}
