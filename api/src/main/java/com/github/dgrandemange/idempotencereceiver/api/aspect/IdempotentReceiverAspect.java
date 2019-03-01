package com.github.dgrandemange.idempotencereceiver.api.aspect;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.github.dgrandemange.idempotencereceiver.api.annot.Idempotent;
import com.github.dgrandemange.idempotencereceiver.api.exception.MissingIdempotencyKeyHeaderException;
import com.github.dgrandemange.idempotencereceiver.api.exception.SubsequentPresentationException;
import com.github.dgrandemange.idempotencereceiver.api.exception.UnmarshallException;
import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentMethodResult;
import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentReceiverCommonConfiguration;
import com.github.dgrandemange.idempotencereceiver.api.service.IdempotentRepository;
import com.github.dgrandemange.idempotencereceiver.api.service.support.InstantProviderImpl;
import com.github.dgrandemange.idempotencereceiver.api.web.http.ByteArrayHttpInputMessage;

/**
 * <p>
 * Idempotence manual management aspect that will advise HTTP request handlers
 * methods annotated with the {@link Idempotent} annotation.
 * </p>
 */
@Aspect
public class IdempotentReceiverAspect implements Ordered {
	private static final Logger LOGGER = LoggerFactory.getLogger(IdempotentReceiverAspect.class);

	public static final String HTTP_HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";
	public static final String HTTP_HEADER_PROCESSING_DURATION = "Processing-Duration";
	public static final String REQUEST_ATTR_IDEMPOTENCE_METHOD_RESULT = "idempotenceMethodResult";

	private InstantProviderImpl instantProvider = new InstantProviderImpl();

	@Autowired
	private IdempotentReceiverCommonConfiguration configuration;

	@Autowired
	private IdempotentRepository repository;

	@Autowired
	private RequestMappingHandlerAdapter handlerAdapter;

	@Override
	public int getOrder() {
		return configuration.getOrder();
	}

	@Pointcut(value = "execution(* *(..))")
	public void anyMethod() {
		// Method used to declare aspect pointcut
	}

	@Around("anyMethod() && @annotation(annot)")
	public Object core(ProceedingJoinPoint joinpoint, Idempotent annot) throws Throwable {
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

	    String ip = request.getHeader("X-FORWARDED-FOR");
	    String ipAddr = (ip == null) ? request.getRemoteAddr() : ip;
		builder.add("ipAddr", ipAddr);

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

		LOGGER.trace("Computed hash for request {} = {}", res, hash);
		
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
			String format = "%0" + (bytes.length << 1) + "x";
			return String.format(format, bigInteger);
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

		if (Objects.isNull(imr)) {
			// No entry matched : deal with request's first presentation
			LOGGER.trace("No entry found matching hash {} : handling request as a first presentation", requestHash);
			return handleRequestFirstPresentation(joinpoint, annot, requestHash);
		} else {
			LOGGER.trace("One entry found matching hash {} : handling request as a subsequent presentation {}",
			        requestHash, imr);
			try {
				ResponseEntity<Object> initialResponse = handleRequestSubsequentPresentation(imr);
				throw new SubsequentPresentationException(initialResponse);
			} catch (UnmarshallException e) {
				LOGGER.trace(
				        "Unable to unmarshall registered idempotent method result body {} : handling request as a first presentation. Cause : {}",
				        imr, e.getMessage());
				return handleRequestFirstPresentation(joinpoint, annot, requestHash);
			}
		}
	}

	Object handleRequestFirstPresentation(ProceedingJoinPoint joinpoint, Idempotent annot, String requestHash)
	        throws Throwable {
		Object result;
		IdempotentMethodResult imr = IdempotentMethodResult.builder().startedAt(instantProvider.provide())
		        .withIdempotencyKey(requestHash).build();

		try {
			LOGGER.trace("Before delegating to handler method, init and register idempotent method result {}", imr);
			registerIdempotentImageResult(imr);

			// Proceed with handler method
			result = joinpoint.proceed();

			// Method processing has returned smoothly
			LOGGER.trace("Handler method has returned : flag idempotent method result for further registration {}",
			        imr);
			addIdempotentImageResultToRequestAttributes(imr);
		} catch (Exception e) {
			// Method processing has raised an exception

			String exceptionTypeName = e.getClass().getName();
			LOGGER.trace("Handler method has raised exception {}. Cause : {}", exceptionTypeName, e.getMessage());

			if (isExceptionRegisterable(annot.registerableEx(), e)) {
				LOGGER.trace(
				        "Exception type {} is configured registerable, update idempotent method result and flag idempotent method result for further registration {}",
				        exceptionTypeName, imr);
				addIdempotentImageResultToRequestAttributes(imr);
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

	void addIdempotentImageResultToRequestAttributes(IdempotentMethodResult imr) {
		HttpServletRequest httpRequest = retrieveCurrentHttpRequest();
		httpRequest.setAttribute(REQUEST_ATTR_IDEMPOTENCE_METHOD_RESULT, imr);
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
	ResponseEntity<Object> handleRequestSubsequentPresentation(IdempotentMethodResult imr) throws UnmarshallException {
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
			// One entry matched : return first presentation processing result
			LOGGER.trace("Returning idempotent method result {}", imr);

			headers.addAll(imr.getResponseHeaders());
			if (Objects.isNull(imr.getBody()) || (imr.getBody().length == 0)) {
				return new ResponseEntity(headers, imr.getResponseStatus());
			} else {
				return new ResponseEntity(unmarshallBody(imr), headers, imr.getResponseStatus());
			}

		default:
			throw new IllegalArgumentException();
		}
	}

	@SuppressWarnings("unchecked")
	Object unmarshallBody(IdempotentMethodResult imr) throws UnmarshallException {
		Class<? extends HttpMessageConverter<?>> selectedConverterType;
		Class<?> returnType;
		try {
			selectedConverterType = (Class<? extends HttpMessageConverter<?>>) Class
			        .forName(imr.getSelectedConverterTypeName());
			returnType = Class.forName(imr.getReturnTypeName());
		} catch (ClassNotFoundException e) {
			throw new UnmarshallException(imr, e);
		}

		Object body = null;
		boolean converterFound = false;
		List<HttpMessageConverter<?>> messageConverters = handlerAdapter.getMessageConverters();
		for (Iterator<HttpMessageConverter<?>> it = messageConverters.iterator(); it.hasNext() && !converterFound;) {
			HttpMessageConverter<?> httpMessageConverter = it.next();
			Class<?> converterClass = httpMessageConverter.getClass();
			if (converterClass.equals(selectedConverterType)) {
				try {
					converterFound = true;
					body = ((HttpMessageConverter<Object>) httpMessageConverter).read(returnType,
					        new ByteArrayHttpInputMessage(imr.getBody()));
				} catch (HttpMessageNotReadableException | IOException e) {
					// Shouldn't occur :
					// * converter itself has been selected by Spring in the first place
					// * HttpInputMessage implementation relies on a byte array input stream
				}
			}
		}

		if (converterFound) {
			return body;
		} else {
			throw new UnmarshallException(imr,
			        String.format("no http message converter '%s' found available in list of registered converters",
			                selectedConverterType.getName()));
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
