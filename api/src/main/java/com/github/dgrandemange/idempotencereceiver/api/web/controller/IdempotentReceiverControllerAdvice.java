package com.github.dgrandemange.idempotencereceiver.api.web.controller;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.github.dgrandemange.idempotencereceiver.api.aspect.IdempotentReceiverAspect;
import com.github.dgrandemange.idempotencereceiver.api.exception.IdempotentReceiverException;
import com.github.dgrandemange.idempotencereceiver.api.exception.MissingIdempotencyKeyHeaderException;
import com.github.dgrandemange.idempotencereceiver.api.exception.SubsequentPresentationException;
import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentMethodResult;
import com.github.dgrandemange.idempotencereceiver.api.service.IdempotentRepository;
import com.github.dgrandemange.idempotencereceiver.api.web.dto.HttpError;
import com.github.dgrandemange.idempotencereceiver.api.web.http.ByteArrayHttpOutputMessage;

@ControllerAdvice
public class IdempotentReceiverControllerAdvice implements ResponseBodyAdvice<Object> {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdempotentReceiverControllerAdvice.class);

	@Autowired
	private RequestMappingHandlerAdapter handlerAdapter;

	@Autowired
	private IdempotentRepository repository;

	@ExceptionHandler(SubsequentPresentationException.class)
	ResponseEntity<Object> subsequentPresentationHandler(SubsequentPresentationException ex) {
		return ex.getResponseEntity();
	}

	@ExceptionHandler(MissingIdempotencyKeyHeaderException.class)
	ResponseEntity<HttpError> missingIdempotencyKeyHeaderHandler(MissingIdempotencyKeyHeaderException ex) {
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

	@SuppressWarnings("unchecked")
	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
	        Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
	        ServerHttpResponse response) {
		if (!(request instanceof ServletServerHttpRequest)) {
			return body;
		}

		IdempotentMethodResult imr = (IdempotentMethodResult) ((ServletServerHttpRequest) request).getServletRequest()
		        .getAttribute(IdempotentReceiverAspect.REQUEST_ATTR_IDEMPOTENCE_METHOD_RESULT);
		if (Objects.isNull(imr)) {
			return body;
		}

		((ServletServerHttpRequest) request).getServletRequest()
		        .removeAttribute(IdempotentReceiverAspect.REQUEST_ATTR_IDEMPOTENCE_METHOD_RESULT);

		ByteArrayHttpOutputMessage outputMessage = null;
		List<HttpMessageConverter<?>> messageConverters = handlerAdapter.getMessageConverters();
		for (HttpMessageConverter<?> httpMessageConverter : messageConverters) {
			if (selectedConverterType.equals(httpMessageConverter.getClass())) {
				try {
					outputMessage = new ByteArrayHttpOutputMessage();
					((HttpMessageConverter<Object>) httpMessageConverter).write(body, selectedContentType,
					        outputMessage);
				} catch (HttpMessageNotWritableException | IOException e) {
					// Shouldn't occur :
					// * the converter itself has been selected by Spring
					// * HttpOutputMessage implementation relies on a byte array output stream
				}
				break;
			}
		}

		if (Objects.isNull(outputMessage)) {
			try {
				repository.unregister(imr.getIdempotencyKey());
			} catch (Exception e) {
				LOGGER.warn(
				        "Unable to unregister request identified by key '{}' from idempotent repository. Cause : {}",
				        imr.getIdempotencyKey(), e.getMessage());
			}
		} else {
			byte[] bodyAsBytes = outputMessage.getBodyAsBytes();
			IdempotentMethodResult updatedImr = IdempotentMethodResult.builder().from(imr)
			        .withResponse(bodyAsBytes, body.getClass(), selectedContentType, selectedConverterType,
			                HttpHeaders.readOnlyHttpHeaders(response.getHeaders()),
			                HttpStatus.resolve(((ServletServerHttpResponse) response).getServletResponse().getStatus()))
			        .build();

			try {
				LOGGER.trace("Registering idempotent method result into repository {}", updatedImr);
				repository.register(updatedImr.getIdempotencyKey(), updatedImr);
				LOGGER.trace("Idempotent method result has been registered into repository {}", updatedImr);
			} catch (Exception e) {
				LOGGER.warn("Unable to register idempotent method result identified by key '{}' in idempotent repository. Cause : {}",
				        updatedImr.getIdempotencyKey(), e.getMessage());
			}
		}

		return body;
	}

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return true;
	}

	public void setHandlerAdapter(RequestMappingHandlerAdapter handlerAdapter) {
		this.handlerAdapter = handlerAdapter;
	}

	public void setRepository(IdempotentRepository repository) {
		this.repository = repository;
	}

}
