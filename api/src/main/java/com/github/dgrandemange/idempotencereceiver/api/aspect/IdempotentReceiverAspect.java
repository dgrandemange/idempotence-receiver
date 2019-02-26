package com.github.dgrandemange.idempotencereceiver.api.aspect;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.github.dgrandemange.idempotencereceiver.api.annot.Idempotent;
import com.github.dgrandemange.idempotencereceiver.api.exception.MissingIdempotencyKeyHeaderException;
import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentMethodResult;
import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentReceiverCommonConfiguration;
import com.github.dgrandemange.idempotencereceiver.api.service.IdempotentRepository;
import com.github.dgrandemange.idempotencereceiver.api.service.support.InstantProviderImpl;

/**
 * <p>
 * Idempotence manual management aspect that will advise HTTP request handlers
 * methods annotated with the {@link Idempotent} annotation.
 * </p>
 * 
 * <p>
 * Limitation : for the idempotence management to work, annotated methods return
 * type must be {@link ResponseEntity} (a {@link UnsupportedOperationException}
 * will be thrown at runtime otherwise).
 * </p>
 */
@Aspect
public class IdempotentReceiverAspect implements Ordered {
	private static final Logger LOGGER = LoggerFactory.getLogger(IdempotentReceiverAspect.class);

	public static final String HTTP_HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";
	public static final String HTTP_HEADER_PROCESSING_DURATION = "Processing-Duration";

	private InstantProviderImpl instantProvider = new InstantProviderImpl();

	@Autowired
	private IdempotentReceiverCommonConfiguration configuration;

	@Autowired
	private IdempotentRepository repository;

	@Override
	public int getOrder() {
		return configuration.getOrder();
	}

	@Pointcut(value = "execution(* *(..))")
	public void anyMethod() {
		// Empty method, we need it to declare aspect pointcut
	}

	@Around("anyMethod() && @annotation(annot)")
	public Object core(ProceedingJoinPoint joinpoint, Idempotent annot) throws Throwable {
		checkMethodReturnType(joinpoint);

		HttpServletRequest request = retrieveCurrentHttpRequest();

		String idempotencyKeyHeader = request.getHeader(HTTP_HEADER_IDEMPOTENCY_KEY);

		if (Objects.isNull(idempotencyKeyHeader) || idempotencyKeyHeader.trim().isEmpty()) {
			LOGGER.trace("No idempotency key header '{}' provided in incoming request '{}' (origin : {})",
			        HTTP_HEADER_IDEMPOTENCY_KEY, request.getRequestURI(), request.getRemoteAddr());

			if (configuration.getIdempotencyKeyHeaderMandatory()) {
				throw new MissingIdempotencyKeyHeaderException(HTTP_HEADER_IDEMPOTENCY_KEY);
			}
		} else {
			LOGGER.trace("An idempotency key '{}' is provided in incoming request '{}' (origin : {})",
			        idempotencyKeyHeader, request.getRequestURI(), request.getRemoteAddr());
		}

		String requestHash = computeRequestHash(request);

		return handleIdempotency(joinpoint, annot, requestHash);
	}

	String computeRequestHash(HttpServletRequest request) throws IOException, NoSuchAlgorithmException {
		JsonObject builder = Json.object();

		builder.add("namespace", this.configuration.getNamespace());

		String idempotencyKeyHeader = request.getHeader(HTTP_HEADER_IDEMPOTENCY_KEY);
		builder.add("idempotencyKeyHeader", Objects.isNull(idempotencyKeyHeader) ? "" : idempotencyKeyHeader);

		builder.add("remoteAddr", request.getRemoteAddr());

		Principal userPrincipal = request.getUserPrincipal();
		builder.add("principalName", Objects.isNull(userPrincipal) ? "" : userPrincipal.getName());

		HttpSession session = request.getSession(false);
		builder.add("sessionId", Objects.isNull(session) ? "" : session.getId());

		builder.add("method", request.getMethod());

		builder.add("requestURI", request.getRequestURI());

		String queryString = request.getQueryString();
		builder.add("queryString", Objects.isNull(queryString) ? "" : queryString);

		byte[] bodyAr = retrieveBodyContents(request);

		// We don't log full body contents as it may include sensitive contents
		String resWithoutBody = builder.toString();

		builder.add("body", toHex(bodyAr));
		String res = builder.toString();

		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.update(res.getBytes());
		String hash = toHex(digest.digest());

		LOGGER.trace("Computed hash for request {} (with request body len={}, hashcode={}) = {}", resWithoutBody,
		        bodyAr.length, Arrays.hashCode(bodyAr), hash);

		return hash;
	}

	byte[] retrieveBodyContents(HttpServletRequest request) throws IOException {
		if (ContentCachingRequestWrapper.class.isAssignableFrom(request.getClass())) {
			return ((ContentCachingRequestWrapper) request).getContentAsByteArray();
		} else if (HttpServletRequestWrapper.class.isAssignableFrom(request.getClass())) {
			return retrieveBodyContents((HttpServletRequest) ((HttpServletRequestWrapper) request).getRequest());
		} else {
			return StreamUtils.copyToByteArray(request.getInputStream());
		}
	}

	String toHex(byte[] bytes) {
		if (bytes.length > 0) {
			BigInteger bigInteger = new BigInteger(1, bytes);
			return String.format("%0" + (bytes.length << 1) + "x", bigInteger);
		} else {
			return "";
		}
	}

	HttpServletRequest retrieveCurrentHttpRequest() {
		return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
	}

	Object handleIdempotency(ProceedingJoinPoint joinpoint, Idempotent annot, String requestHash) throws Throwable {
		// First, check if idempotency key matches one entry in idempotency repository
		IdempotentMethodResult imr;

		try {
			imr = repository.find(requestHash);
		} catch (Exception e) {
			LOGGER.trace(
			        "An exception occurred while looking for request identified by key '{}' in idempotent repository. Cause : {}. Idempotence handling will be skipped for current request",
			        requestHash, e.getMessage());

			// Proceed without handling idempotence
			return joinpoint.proceed();
		}

		Object result;
		if (Objects.isNull(imr)) {
			// No entry matched : deal with request's first presentation
			LOGGER.trace("No entry found matching hash {} : handling request as a first presentation", requestHash);
			result = handleRequestFirstPresentation(joinpoint, annot, requestHash);
		} else {
			LOGGER.trace("One entry found matching hash {} : handling request as a subsequent presentation {}",
			        requestHash, imr);
			result = handleRequestSubsequentPresentation(imr);
		}

		return result;

	}

	Object handleRequestFirstPresentation(ProceedingJoinPoint joinpoint, Idempotent annot, String requestHash)
	        throws Throwable {
		Object result;
		IdempotentMethodResult imr = IdempotentMethodResult.builder().startedAt(instantProvider.provide())
		        .withIdempotencyKey(requestHash).build();

		try {
			LOGGER.trace("Before request processing : init and register idempotent method result {}", imr);
			registerIdempotentImageResult(imr);

			// Process request
			result = joinpoint.proceed();

			// Method processing has returned smoothly
			ResponseEntity<?> re = (ResponseEntity<?>) result;
			IdempotentMethodResult updatedImr = IdempotentMethodResult.builder().from(imr)
			        .hasReturned((Serializable) re.getBody(), re.getHeaders(), re.getStatusCode()).build();
			LOGGER.trace("Request processing done : update registered idempotent method result {}", updatedImr);
			registerIdempotentImageResult(updatedImr);
		} catch (Exception e) {
			// Method processing has raised an exception

			String exceptionTypeName = e.getClass().getName();
			LOGGER.trace("Request processing has thrown exception {}. Cause : {}", exceptionTypeName, e.getMessage());

			if (isExceptionRegisterable(annot.registerableEx(), e)) {
				IdempotentMethodResult updatedImr = IdempotentMethodResult.builder().from(imr).hasRaised(e).build();
				LOGGER.trace(
				        "Exception type {} is configured registerable, update registered idempotent method result {}",
				        exceptionTypeName, updatedImr);
				registerIdempotentImageResult(updatedImr);
			} else {
				LOGGER.trace(
				        "Exception type {} not configured as registerable : unregister idempotent method result {}",
				        exceptionTypeName, imr);

				unregisterIdempotentImageResult(imr);
			}

			throw e;
		}

		return result;
	}

	boolean registerIdempotentImageResult(IdempotentMethodResult imr) {
		try {
			repository.register(imr.getIdempotencyKey(), imr);
			return true;
		} catch (Exception e) {
			LOGGER.warn("Unable to register request identified by key '{}' in idempotent repository. Cause : {}",
			        imr.getIdempotencyKey(), e.getMessage());
			return false;
		}
	}

	void unregisterIdempotentImageResult(IdempotentMethodResult imr) {
		try {
			repository.unregister(imr.getIdempotencyKey());
		} catch (Exception e) {
			LOGGER.warn("Unable to unregister request identified by key '{}' in idempotent repository. Cause : {}",
			        imr.getIdempotencyKey(), e.getMessage());
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	Object handleRequestSubsequentPresentation(IdempotentMethodResult imr) throws Exception {
		Objects.requireNonNull(imr);

		HttpHeaders headers = new HttpHeaders();

		switch (imr.getState()) {
		case RUNNING:
			// Handle case where first presentation request seems to be still processing

			long durationMS = Duration.between(imr.getStartedAt(), instantProvider.provide()).toMillis();
			LOGGER.trace(
			        "Idempotent method result {} indicates request processing started {} ms ago and is still running",
			        imr, durationMS);

			headers.add(HTTP_HEADER_PROCESSING_DURATION, Long.toString(durationMS));

			return new ResponseEntity(headers, HttpStatus.ACCEPTED);

		case DONE:
			// One entry is matched

			if (!Objects.isNull(imr.getException())) {
				// Re-raise exception as it was raised during first presentation processing
				LOGGER.trace("Raise exception from idempotent method result {}", imr);

				throw imr.getException();
			} else {
				// Return first presentation processing result
				LOGGER.trace("Returning idempotent method result {}", imr);

				headers.addAll(imr.getResponseHeaders());
				return new ResponseEntity(imr.getResponseBody(), headers, imr.getResponseStatus());
			}

		default:
			throw new IllegalArgumentException();
		}
	}

	void checkMethodReturnType(ProceedingJoinPoint joinPoint) {
		@SuppressWarnings("rawtypes")
		Class<ResponseEntity> requiredReturnType = ResponseEntity.class;

		MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
		if (!requiredReturnType.isAssignableFrom(methodSignature.getReturnType())) {
			String msg = String.format(
			        "Unable to apply idempotency to method '%s'. Cause : idempotency requires method to return a %s instance",
			        methodSignature.toLongString(), requiredReturnType.getSimpleName());
			LOGGER.error(msg);
			throw new UnsupportedOperationException(msg);
		}
	}

	boolean isExceptionRegisterable(Class<? extends Exception>[] registerableEx, Exception e) {
		Class<? extends Exception> exClazz = e.getClass();
		boolean registerable = false;
		for (Class<? extends Exception> clazz : registerableEx) {
			if (clazz.isAssignableFrom(exClazz)) {
				registerable = true;
				break;
			}
		}
		return registerable;
	}

	public IdempotentRepository getRepository() {
		return repository;
	}

	public void setRepository(IdempotentRepository repository) {
		this.repository = repository;
	}

	public InstantProviderImpl getInstantProvider() {
		return instantProvider;
	}

	public void setInstantProvider(InstantProviderImpl instantProvider) {
		this.instantProvider = instantProvider;
	}

	public IdempotentReceiverCommonConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(IdempotentReceiverCommonConfiguration configuration) {
		this.configuration = configuration;
	}

}
