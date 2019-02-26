package com.github.dgrandemange.idempotencereceiver.api.service.support;

import java.time.Duration;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.dgrandemange.idempotencereceiver.api.exception.IdempotentRepositoryException;
import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentMethodResult;
import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentReceiverCommonConfiguration;
import com.github.dgrandemange.idempotencereceiver.api.model.ResiliencyConfiguration.CircuitBreakerConfiguration;
import com.github.dgrandemange.idempotencereceiver.api.model.ResiliencyConfiguration.RetryConfiguration;
import com.github.dgrandemange.idempotencereceiver.api.service.IdempotentRepository;

import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import net.jodah.failsafe.RetryPolicy;

public class ResilientIdempotentRepository implements IdempotentRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResilientIdempotentRepository.class);

	@Autowired
	private IdempotentReceiverCommonConfiguration configuration;

	@Resource(name = "idempotentRepository")
	private IdempotentRepository idempotentRepository;

	private RetryPolicy<IdempotentMethodResult> retryPolicy;
	private CircuitBreaker<IdempotentMethodResult> circuitBreaker;

	@PostConstruct
	void postConstruct() {
		RetryConfiguration retryConfig = configuration.getRepository().getResiliency().getRetry();
		retryPolicy = new RetryPolicy<IdempotentMethodResult>().handle(IdempotentRepositoryException.class)
		        .withDelay(Duration.ofMillis(retryConfig.getDelayMs())).withMaxRetries(retryConfig.getMaxRetries());

		CircuitBreakerConfiguration circuitBreakerConfig = configuration.getRepository().getResiliency()
		        .getCircuitBreaker();
		circuitBreaker = new CircuitBreaker<IdempotentMethodResult>().handle(IdempotentRepositoryException.class)
		        .withFailureThreshold(circuitBreakerConfig.getFailureThreshold())
		        .withSuccessThreshold(circuitBreakerConfig.getSuccessThreshold())
		        .withDelay(Duration.ofMillis(circuitBreakerConfig.getDelayMs()))
		        .onClose(() -> LOGGER.info("The circuit breaker has been closed"))
		        .onOpen(() -> LOGGER.info("The circuit breaker has been opened"))
		        .onHalfOpen(() -> LOGGER.info("The circuit breaker has been half-opened"));
	}

	@Override
	public IdempotentMethodResult register(String idempotencyKey, IdempotentMethodResult imr) {
		try {
			return Failsafe.with(retryPolicy, circuitBreaker)
			        .get(() -> idempotentRepository.register(idempotencyKey, imr));
		} catch (FailsafeException e) {
			throw new IdempotentRepositoryException(e);
		}
	}

	@Override
	public IdempotentMethodResult unregister(String idempotencyKey) {
		try {
			return Failsafe.with(retryPolicy, circuitBreaker)
			        .get(() -> idempotentRepository.unregister(idempotencyKey));
		} catch (FailsafeException e) {
			throw new IdempotentRepositoryException(e);
		}
	}

	@Override
	public IdempotentMethodResult find(String idempotencyKey) {
		try {
			return Failsafe.with(retryPolicy, circuitBreaker).get(() -> idempotentRepository.find(idempotencyKey));
		} catch (FailsafeException e) {
			throw new IdempotentRepositoryException(e);
		}
	}

}
