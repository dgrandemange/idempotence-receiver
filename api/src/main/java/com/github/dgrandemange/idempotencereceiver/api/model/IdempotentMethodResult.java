package com.github.dgrandemange.idempotencereceiver.api.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

public class IdempotentMethodResult implements Serializable {

	private static final long serialVersionUID = 100L;

	private String idempotencyKey;

	private Instant startedAt;

	private ProcessingState state;

	private Serializable responseBody;
	private HttpHeaders responseHeaders;
	private HttpStatus responseStatus;

	private Exception exception;

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

		/**
		 * Note : mutually exclusive with {link {@link #hasRaised(Exception)}}
		 * 
		 * @param body
		 *            returned http entity body
		 * @param headers
		 *            returned http headers
		 * @param status
		 *            returned http status
		 * @return idempotent method result builder
		 */
		public IdempotentMethodResultBuilder hasReturned(Serializable body, HttpHeaders headers, HttpStatus status) {
			instance.state = ProcessingState.DONE;
			instance.responseBody = body;
			instance.responseHeaders = headers;
			instance.responseStatus = status;
			instance.exception = null;
			return this;
		}

		/**
		 * Note : mutually exclusive with {link
		 * {@link #hasReturned(Serializable, HttpHeaders, HttpStatus)}}
		 * 
		 * @param exception
		 *            the exception raised within idempotent method
		 * @return idempotent method result builder
		 */
		public IdempotentMethodResultBuilder hasRaised(Exception exception) {
			instance.state = ProcessingState.DONE;
			instance.responseBody = null;
			instance.responseHeaders = null;
			instance.responseStatus = null;
			instance.exception = exception;
			return this;
		}

		public IdempotentMethodResultBuilder from(IdempotentMethodResult imr) {
			instance.idempotencyKey = imr.idempotencyKey;
			instance.startedAt = imr.startedAt;
			instance.state = imr.state;
			instance.responseBody = imr.responseBody;
			instance.responseHeaders = imr.responseHeaders;
			instance.responseStatus = imr.responseStatus;
			instance.exception = imr.exception;
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

	public Serializable getResponseBody() {
		return responseBody;
	}

	public void setResponseBody(Serializable responseBody) {
		this.responseBody = responseBody;
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

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(Instant startedAt) {
		this.startedAt = startedAt;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((exception == null) ? 0 : exception.hashCode());
		result = prime * result + ((idempotencyKey == null) ? 0 : idempotencyKey.hashCode());
		result = prime * result + ((responseBody == null) ? 0 : responseBody.hashCode());
		result = prime * result + ((responseHeaders == null) ? 0 : responseHeaders.hashCode());
		result = prime * result + ((responseStatus == null) ? 0 : responseStatus.hashCode());
		result = prime * result + ((startedAt == null) ? 0 : startedAt.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IdempotentMethodResult other = (IdempotentMethodResult) obj;
		if (exception == null) {
			if (other.exception != null)
				return false;
		} else if (!exception.equals(other.exception))
			return false;
		if (idempotencyKey == null) {
			if (other.idempotencyKey != null)
				return false;
		} else if (!idempotencyKey.equals(other.idempotencyKey))
			return false;
		if (responseBody == null) {
			if (other.responseBody != null)
				return false;
		} else if (!responseBody.equals(other.responseBody))
			return false;
		if (responseHeaders == null) {
			if (other.responseHeaders != null)
				return false;
		} else if (!responseHeaders.equals(other.responseHeaders))
			return false;
		if (responseStatus != other.responseStatus)
			return false;
		if (startedAt == null) {
			if (other.startedAt != null)
				return false;
		} else if (!startedAt.equals(other.startedAt))
			return false;
		if (state != other.state)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "IdempotentMethodResult [idempotencyKey=" + idempotencyKey + ", startedAt=" + startedAt + ", state="
		        + state + ", responseBody=" + responseBody + ", responseHeaders=" + responseHeaders
		        + ", responseStatus=" + responseStatus + ", exception=" + exception + "]";
	}

}
