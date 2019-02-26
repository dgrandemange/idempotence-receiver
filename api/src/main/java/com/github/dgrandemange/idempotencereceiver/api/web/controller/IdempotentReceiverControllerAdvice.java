package com.github.dgrandemange.idempotencereceiver.api.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.github.dgrandemange.idempotencereceiver.api.exception.IdempotentReceiverException;
import com.github.dgrandemange.idempotencereceiver.api.exception.MissingIdempotencyKeyHeaderException;
import com.github.dgrandemange.idempotencereceiver.api.web.dto.HttpError;

@ControllerAdvice
public class IdempotentReceiverControllerAdvice {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdempotentReceiverControllerAdvice.class);

	@ExceptionHandler(MissingIdempotencyKeyHeaderException.class)
	ResponseEntity<HttpError> employeeNotFoundHandler(MissingIdempotencyKeyHeaderException ex) {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		return new ResponseEntity<>(new HttpError(status,
		        String.format("request is missing mandatory idempotency key header '%s'", ex.getMissingHeaderName())),
		        status);
	}

	@ExceptionHandler(IdempotentReceiverException.class)
	ResponseEntity<HttpError> unhandled(IdempotentReceiverException ex) {
		LOGGER.error(ex.getMessage(), ex);
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		return new ResponseEntity<>(new HttpError(status, "unexpected error from idempotency receiver"), status);
	}
}
