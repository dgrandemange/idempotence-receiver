package com.github.dgrandemange.idempotencereceiver.api.web.interceptor;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.github.dgrandemange.idempotencereceiver.api.aspect.IdempotentReceiverAspect;
import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentMethodResult;
import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentMethodResult.ProcessingState;
import com.github.dgrandemange.idempotencereceiver.api.service.IdempotentRepository;

public class ImrHandlerInterceptor extends HandlerInterceptorAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImrHandlerInterceptor.class);

	@Autowired
	private IdempotentRepository repository;

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
	        throws Exception {

		IdempotentMethodResult imr = null;
		Object attribute = request.getAttribute(IdempotentReceiverAspect.REQUEST_ATTR_IDEMPOTENCE_METHOD_RESULT);
		if (Objects.isNull(attribute) || !(attribute instanceof IdempotentMethodResult)) {
			return;
		}

		imr = (IdempotentMethodResult) attribute;
		request.removeAttribute(IdempotentReceiverAspect.REQUEST_ATTR_IDEMPOTENCE_METHOD_RESULT);
		if (ProcessingState.DONE.equals(imr.getState())) {
			return;
		}

		// At this point, an imr is found and is still in RUNNING state

		// Finalize imr using current response status and http headers
		HttpHeaders httpHeaders = new HttpHeaders();
		for (String headerName : response.getHeaderNames()) {
			for (String headerValue : response.getHeaders(headerName)) {
				httpHeaders.add(headerName, headerValue);
			}
		}

		HttpStatus httpStatus = HttpStatus.resolve(response.getStatus());

		IdempotentMethodResult updatedImr = IdempotentMethodResult.builder().from(imr)
		        .withResponse(httpHeaders, httpStatus).build();

		try {
			LOGGER.trace("Registering idempotent method result into repository {}", updatedImr);
			repository.register(updatedImr.getIdempotencyKey(), updatedImr);
			LOGGER.trace("Idempotent method result has been registered into repository {}", updatedImr);
		} catch (Exception e) {
			LOGGER.warn(
			        "Unable to register idempotent method result identified by key '{}' in idempotent repository. Cause : {}",
			        updatedImr.getIdempotencyKey(), e.getMessage());
		}

	}

}
