package com.github.dgrandemange.idempotencereceiver.api.exception;

import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentMethodResult;

public class UnmarshallException extends IdempotentReceiverException {

	private static final long serialVersionUID = 1L;

	private final IdempotentMethodResult imr;	

	public UnmarshallException(IdempotentMethodResult imr, String message) {
		super(message);
		this.imr = imr;
	}

	public UnmarshallException(IdempotentMethodResult imr, Exception e) {
		super(e.getMessage(), e);
		this.imr = imr;
	}
	
	public IdempotentMethodResult getImr() {
		return imr;
	}

}
