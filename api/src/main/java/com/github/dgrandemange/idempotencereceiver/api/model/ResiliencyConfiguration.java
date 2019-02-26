package com.github.dgrandemange.idempotencereceiver.api.model;

public class ResiliencyConfiguration {

	public static class RetryConfiguration {

		/**
		 * Delay (in ms) between attempts
		 */
		private int delayMs = 150;

		/**
		 * Max number of retries
		 */
		private int maxRetries = 1;

		public int getDelayMs() {
			return delayMs;
		}

		public void setDelayMs(int delayMs) {
			this.delayMs = delayMs;
		}

		public int getMaxRetries() {
			return maxRetries;
		}

		public void setMaxRetries(int maxRetries) {
			this.maxRetries = maxRetries;
		}

		@Override
		public String toString() {
			return "RetryConfiguration [delayMs=" + delayMs + ", maxRetries=" + maxRetries + "]";
		}

	}

	/**
	 * @see <a href="https://martinfowler.com/bliki/CircuitBreaker.html">CircuitBreaker pattern</a>
	 */
	public static class CircuitBreakerConfiguration {

		/**
		 * Circuit breaker is configured to open when a successive number (here given by
		 * {@link #failureThreshold} property) of executions have failed.
		 */
		private int failureThreshold = 5;

		/**
		 * Once in an opened state, a circuit breaker will delay for a specific amount
		 * of time (here given by {@link #delayMs} property) before entering a
		 * half-opened state and attempting to close again
		 */
		private int delayMs = 30000;

		/**
		 * Once in a half-opened state, the circuit breaker is configured to close again
		 * if a number (here indicated by {@link #successThreshold} property) of trial
		 * executions succeed, else it will re-open.
		 */
		private int successThreshold = 3;

		public int getFailureThreshold() {
			return failureThreshold;
		}

		public void setFailureThreshold(int failureThreshold) {
			this.failureThreshold = failureThreshold;
		}

		public int getSuccessThreshold() {
			return successThreshold;
		}

		public void setSuccessThreshold(int successThreshold) {
			this.successThreshold = successThreshold;
		}

		public int getDelayMs() {
			return delayMs;
		}

		public void setDelayMs(int delayMs) {
			this.delayMs = delayMs;
		}

		@Override
		public String toString() {
			return "CircuitBreakerConfiguration [failureThreshold=" + failureThreshold + ", successThreshold="
			        + successThreshold + ", delayMs=" + delayMs + "]";
		}

	}

	/**
	 * Nested retry configuration
	 */
	private RetryConfiguration retry = new RetryConfiguration();

	/**
	 * Nested circuit breaker configuration
	 */
	private CircuitBreakerConfiguration circuitBreaker = new CircuitBreakerConfiguration();

	public RetryConfiguration getRetry() {
		return retry;
	}

	public void setRetry(RetryConfiguration retry) {
		this.retry = retry;
	}

	public CircuitBreakerConfiguration getCircuitBreaker() {
		return circuitBreaker;
	}

	public void setCircuitBreaker(CircuitBreakerConfiguration circuitBreaker) {
		this.circuitBreaker = circuitBreaker;
	}

	@Override
	public String toString() {
		return "ResiliencyConfiguration [retry=" + retry + ", circuitBreaker=" + circuitBreaker + "]";
	}

}
