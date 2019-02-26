package com.github.dgrandemange.idempotencereceiver.examples.webapp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.github.dgrandemange.idempotencereceiver.examples.webapp.exception.ResourceAlreadyExistsException;
import com.github.dgrandemange.idempotencereceiver.examples.webapp.exception.ResourceNotFoundException;

@ControllerAdvice
public class MyControllerAdvice {

	@ResponseBody
	@ExceptionHandler(ResourceNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	String employeeNotFoundHandler(ResourceNotFoundException ex) {
		return ex.getMessage();
	}

	@ResponseBody
	@ExceptionHandler(ResourceAlreadyExistsException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	String employeeNotFoundHandler(ResourceAlreadyExistsException ex) {
		return ex.getMessage();
	}
}
