package com.github.dgrandemange.idempotencereceiver.api.model;

public class ResiliencyConfiguration {

	public static class RetryConfiguration {

		public static final int RETRY_POLICY_DEFAULT_MAX_RETRIES = 1;
		public static final int RETRY_POLICY_DEFAULT_DELAY_MS = 150;

		private int delayMs = RETRY_POLICY_DEFAULT_DELAY_MS;

		private int maxRetries = RETRY_POLICY_DEFAULT_MAX_RETRIES;

		/**
		 * @return Delay in milliseconds between retry attempts
		 * @see #RETRY_POLICY_DEFAULT_DELAY_MS
		 */
		public int getDelayMs() {
			return delayMs;
		}

		/**
		 * @param delayMs
		 *            See {@link #getDelayMs()}
		 */
		public void setDelayMs(int delayMs) {
			this.delayMs = delayMs;
		}

		/**
		 * @return Max number of retries
		 * @see #RETRY_POLICY_DEFAULT_MAX_RETRIES
		 */
		public int getMaxRetries() {
			return maxRetries;
		}

		/**
		 * @param maxRetries
		 *            See {@link #getMaxRetries()}
		 */
		public void setMaxRetries(int maxRetries) {
			this.maxRetries = maxRetries;
		}

		@Override
		public String toString() {
			return "RetryConfiguration [delayMs=" + delayMs + ", maxRetries=" + maxRetries + "]";
		}

	}

	/**
	 * @see <a href=
	 *      "https://martinfowler.com/bliki/CircuitBreaker.html">CircuitBreaker
	 *      pattern</a>
	 * @see <a href=
	 *      "https://www.hascode.com/wp-content/uploads/2017/02/circuit-breaker-state1.png">Circuit
	 *      breaker states</a>
	 */
	public static class CircuitBreakerConfiguration {

		public static final int CIRCUIT_BREAKER_POLICY_DEFAULT_FAILURE_THRESHOLD = 5;
		public static final int CIRCUIT_BREAKER_POLICY_DEFAULT_DELAY_MS = 30000;
		public static final int CIRCUIT_BREAKER_POLICY_DEFAULT_SUCCESS_THRESHOLD = 3;

		private int failureThreshold = CIRCUIT_BREAKER_POLICY_DEFAULT_FAILURE_THRESHOLD;

		private int delayMs = CIRCUIT_BREAKER_POLICY_DEFAULT_DELAY_MS;

		private int successThreshold = CIRCUIT_BREAKER_POLICY_DEFAULT_SUCCESS_THRESHOLD;

		/**
		 * Circuit breaker is configured to open when a successive number (here given by
		 * {@link #failureThreshold} property) of executions have failed.
		 * 
		 * @return failureThreshold
		 * @see #CIRCUIT_BREAKER_POLICY_DEFAULT_FAILURE_THRESHOLD
		 */
		public int getFailureThreshold() {
			return failureThreshold;
		}

		/**
		 * @param failureThreshold
		 *            See {@link #getFailureThreshold()}
		 */
		public void setFailureThreshold(int failureThreshold) {
			this.failureThreshold = failureThreshold;
		}

		/**
		 * Once in an opened state, a circuit breaker will delay for a specific amount
		 * of time (here given by {@link #delayMs} property) before entering a
		 * half-opened state and attempting to close again
		 * 
		 * @return delay in milliseconds
		 * @see #CIRCUIT_BREAKER_POLICY_DEFAULT_DELAY_MS
		 */
		public int getDelayMs() {
			return delayMs;
		}

		/**
		 * @param delayMs
		 *            See {@link #getDelayMs()}
		 */
		public void setDelayMs(int delayMs) {
			this.delayMs = delayMs;
		}

		/**
		 * Once in a half-opened state, the circuit breaker is configured to close again
		 * if a number (here indicated by {@link #successThreshold} property) of trial
		 * executions succeed, else it will re-open.
		 * 
		 * @return success threshold
		 * @see #CIRCUIT_BREAKER_POLICY_DEFAULT_SUCCESS_THRESHOLD
		 */
		public int getSuccessThreshold() {
			return successThreshold;
		}

		/**
		 * @param successThreshold
		 *            See {@link #getSuccessThreshold()}
		 */
		public void setSuccessThreshold(int successThreshold) {
			this.successThreshold = successThreshold;
		}

		@Override
		public String toString() {
			return "CircuitBreakerConfiguration [failureThreshold=" + failureThreshold + ", successThreshold="
			        + successThreshold + ", delayMs=" + delayMs + "]";
		}

	}

	private RetryConfiguration retry = new RetryConfiguration();

	private CircuitBreakerConfiguration circuitBreaker = new CircuitBreakerConfiguration();

	/**
	 * @return nested retry configuration
	 */
	public RetryConfiguration getRetry() {
		return retry;
	}

	/**
	 * @param retry
	 *            See {@link #getRetry()}
	 */
	public void setRetry(RetryConfiguration retry) {
		this.retry = retry;
	}

	/**
	 * @return Nested circuit breaker configuration
	 */
	public CircuitBreakerConfiguration getCircuitBreaker() {
		return circuitBreaker;
	}

	/**
	 * @param circuitBreaker
	 *            See {@link #getCircuitBreaker()}
	 */
	public void setCircuitBreaker(CircuitBreakerConfiguration circuitBreaker) {
		this.circuitBreaker = circuitBreaker;
	}

	@Override
	public String toString() {
		return "ResiliencyConfiguration [retry=" + retry + ", circuitBreaker=" + circuitBreaker + "]";
	}

}
