package com.github.dgrandemange.idempotencereceiver.api.web.dto;

import java.io.Serializable;

import org.springframework.http.HttpStatus;

public class HttpError implements Serializable {

	private static final long serialVersionUID = 1L;

	private HttpStatus status;

	private String message;

	public HttpError(HttpStatus status, String message) {
		super();
		this.status = status;
		this.message = message;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public void setStatus(HttpStatus status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
