package com.github.dgrandemange.idempotencereceiver.api.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;

public class IdempotentMethodResult implements Serializable {

	private static final long serialVersionUID = 100L;

	private String idempotencyKey;

	private Instant startedAt;

	private ProcessingState state;

	private byte[] body;
	private MediaType bodyContentType;
	private String returnTypeName;
	private String selectedConverterTypeName;

	private HttpHeaders responseHeaders;
	private HttpStatus responseStatus;

	public enum ProcessingState {
		RUNNING, DONE;
	}

	public static class IdempotentMethodResultBuilder {
		private IdempotentMethodResult instance;

		public IdempotentMethodResultBuilder() {
			super();
			instance = new IdempotentMethodResult();
			instance.state = ProcessingState.RUNNING;
		}

		public IdempotentMethodResultBuilder withIdempotencyKey(String idempotencyKey) {
			instance.idempotencyKey = idempotencyKey;
			return this;
		}

		public IdempotentMethodResultBuilder startedAt(Instant instant) {
			instance.startedAt = instant;
			return this;
		}

		public IdempotentMethodResultBuilder from(IdempotentMethodResult imr) {
			instance.idempotencyKey = imr.idempotencyKey;
			instance.startedAt = imr.startedAt;
			instance.state = imr.state;
			instance.responseHeaders = imr.responseHeaders;
			instance.responseStatus = imr.responseStatus;
			instance.body = imr.body;
			instance.bodyContentType = imr.bodyContentType;
			instance.selectedConverterTypeName = imr.selectedConverterTypeName;
			instance.returnTypeName = imr.returnTypeName;
			return this;
		}

		public IdempotentMethodResultBuilder withResponse(byte[] bodyAsByteArray, Class<?> returnType,
		        MediaType bodyContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType,
		        HttpHeaders httpHeaders, HttpStatus httpStatus) {
			instance.state = ProcessingState.DONE;
			instance.body = bodyAsByteArray;
			instance.bodyContentType = bodyContentType;
			instance.selectedConverterTypeName = Objects.isNull(selectedConverterType) ? null
			        : selectedConverterType.getName();
			instance.returnTypeName = Objects.isNull(returnType) ? null : returnType.getName();
			instance.responseHeaders = httpHeaders;
			instance.responseStatus = httpStatus;
			return this;
		}

		public IdempotentMethodResultBuilder withResponse(HttpHeaders httpHeaders, HttpStatus httpStatus) {
			instance.state = ProcessingState.DONE;
			instance.body = null;
			instance.bodyContentType = null;
			instance.selectedConverterTypeName = null;
			instance.returnTypeName = null;
			instance.responseHeaders = httpHeaders;
			instance.responseStatus = httpStatus;
			return this;
		}
		
		public IdempotentMethodResult build() {
			Objects.requireNonNull(instance.startedAt);
			Objects.requireNonNull(instance.idempotencyKey);
			return instance;
		}
	}

	public static IdempotentMethodResultBuilder builder() {
		return new IdempotentMethodResultBuilder();
	}

	public HttpHeaders getResponseHeaders() {
		return responseHeaders;
	}

	public void setResponseHeaders(HttpHeaders responseHeaders) {
		this.responseHeaders = responseHeaders;
	}

	public HttpStatus getResponseStatus() {
		return responseStatus;
	}

	public void setResponseStatus(HttpStatus responseStatus) {
		this.responseStatus = responseStatus;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public void setIdempotencyKey(String idempotencyKey) {
		this.idempotencyKey = idempotencyKey;
	}

	public ProcessingState getState() {
		return state;
	}

	public void setState(ProcessingState state) {
		this.state = state;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(Instant startedAt) {
		this.startedAt = startedAt;
	}

	public MediaType getBodyContentType() {
		return bodyContentType;
	}

	public void setBodyContentType(MediaType bodyContentType) {
		this.bodyContentType = bodyContentType;
	}

	public byte[] getBody() {
		return body;
	}

	public void setBody(byte[] body) {
		this.body = body;
	}

	public String getReturnTypeName() {
		return returnTypeName;
	}

	public void setReturnTypeName(String returnTypeName) {
		this.returnTypeName = returnTypeName;
	}

	public String getSelectedConverterTypeName() {
		return selectedConverterTypeName;
	}

	public void setSelectedConverterTypeName(String selectedConverterTypeName) {
		this.selectedConverterTypeName = selectedConverterTypeName;
	}

	@Override
	public String toString() {
		return "IdempotentMethodResult [idempotencyKey=" + idempotencyKey + ", startedAt=" + startedAt + ", state="
		        + state + ", bodyContentType=" + bodyContentType + ", returnTypeName=" + returnTypeName
		        + ", selectedConverterTypeName=" + selectedConverterTypeName + ", responseHeaders=" + responseHeaders
		        + ", responseStatus=" + responseStatus + "]";
	}

}
