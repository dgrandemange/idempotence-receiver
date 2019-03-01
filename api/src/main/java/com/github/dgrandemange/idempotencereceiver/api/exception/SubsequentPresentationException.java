package com.github.dgrandemange.idempotencereceiver.api.exception;

import org.springframework.http.ResponseEntity;

public class SubsequentPresentationException extends IdempotentReceiverException {

	private static final long serialVersionUID = 1L;

	private final ResponseEntity<Object> responseEntity;

	public SubsequentPresentationException(ResponseEntity<Object> responseEntity) {
		super();
		this.responseEntity = responseEntity;
	}

	public ResponseEntity<Object> getResponseEntity() {
		return responseEntity;
	}

}
