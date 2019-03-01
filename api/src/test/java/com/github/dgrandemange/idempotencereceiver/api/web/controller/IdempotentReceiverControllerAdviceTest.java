package com.github.dgrandemange.idempotencereceiver.api.web.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.fest.assertions.Assertions;
import org.hamcrest.beans.SamePropertyValuesAs;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import com.github.dgrandemange.idempotencereceiver.api.aspect.IdempotentReceiverAspect;
import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentMethodResult;
import com.github.dgrandemange.idempotencereceiver.api.service.IdempotentRepository;
import com.github.dgrandemange.idempotencereceiver.api.service.support.MyStringHttpMessageConverter;

@RunWith(MockitoJUnitRunner.class)
public class IdempotentReceiverControllerAdviceTest {

	@InjectMocks
	IdempotentReceiverControllerAdvice cut;

	@Mock
	RequestMappingHandlerAdapter handlerAdapter;

	List<HttpMessageConverter<?>> messageConverters;

	@Mock
	IdempotentRepository repository;

	MockHttpServletRequest mockedHttpRequest;
	MockHttpServletResponse mockedHttpResponse;

	ServerHttpRequest serverHttpRequest;
	ServerHttpResponse serverHttpResponse;

	@Before
	public void setUp() {
		mockedHttpRequest = new MockHttpServletRequest();
		mockedHttpResponse = new MockHttpServletResponse();
		serverHttpRequest = new ServletServerHttpRequest(mockedHttpRequest);
		serverHttpResponse = new ServletServerHttpResponse(mockedHttpResponse);

		messageConverters = new ArrayList<>();
		Mockito.doReturn(messageConverters).when(handlerAdapter).getMessageConverters();
	}

	@Test
	public void testBeforeBodyWrite_shouldDirectlyReturnBody_whenImrAttributeIsNotSetInRequest() {
		mockedHttpRequest.removeAttribute(IdempotentReceiverAspect.REQUEST_ATTR_IDEMPOTENCE_METHOD_RESULT);

		String body = "dummy body";
		MediaType selectedContentType = MediaType.TEXT_PLAIN;
		Class<? extends HttpMessageConverter<?>> selectedConverterType = MyStringHttpMessageConverter.class;

		Object res = cut.beforeBodyWrite(body, null, selectedContentType, selectedConverterType, serverHttpRequest,
		        serverHttpResponse);

		Assertions.assertThat(res).isEqualTo(body);
		Mockito.verify(repository, Mockito.times(0)).register(Mockito.anyString(),
		        Mockito.any(IdempotentMethodResult.class));
		Mockito.verify(repository, Mockito.times(0)).unregister(Mockito.anyString());
	}

	@Test
	public void testBeforeBodyWrite_shouldUpdateAndRegisterIdempotentMethodResultWithMarshalledBody__whenImrAttributeIsSetInRequest_andBodyConverterIsFound() {
		IdempotentMethodResult imr = IdempotentMethodResult.builder().startedAt(Instant.now())
		        .withIdempotencyKey("12345").build();

		mockedHttpRequest.setAttribute(IdempotentReceiverAspect.REQUEST_ATTR_IDEMPOTENCE_METHOD_RESULT, imr);

		String body = "dummy body";
		MediaType selectedContentType = MediaType.TEXT_PLAIN;
		Class<? extends HttpMessageConverter<?>> selectedConverterType = MyStringHttpMessageConverter.class;

		messageConverters.add(new MyStringHttpMessageConverter());

		serverHttpResponse.getHeaders().add(HttpHeaders.CONTENT_TYPE, "text/plain;UTF-8");
		serverHttpResponse.getHeaders().add(HttpHeaders.LOCATION, "http://dummy/");

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.addAll(serverHttpResponse.getHeaders());

		IdempotentMethodResult expectedImr = IdempotentMethodResult.builder().from(imr)
		        .withResponse(("My_" + body).getBytes(), String.class, selectedContentType, selectedConverterType,
		                httpHeaders, HttpStatus.resolve(mockedHttpResponse.getStatus()))
		        .build();

		Object res = cut.beforeBodyWrite(body, null, selectedContentType, selectedConverterType, serverHttpRequest,
		        serverHttpResponse);

		Assertions.assertThat(res).isEqualTo(body);

		Mockito.verify(repository, Mockito.times(0)).unregister(Mockito.anyString());

		Mockito.verify(repository, Mockito.times(1)).register(Mockito.eq(expectedImr.getIdempotencyKey()),
		        Mockito.argThat(SamePropertyValuesAs.samePropertyValuesAs(expectedImr)));
	}

	@Test
	public void testBeforeBodyWrite_shouldUpdateAndRegisterIdempotentMethodResultWithMarshalledBody__whenImrAttributeIsSetInRequest_andBodyConverterIsNotFound() {
		IdempotentMethodResult imr = IdempotentMethodResult.builder().startedAt(Instant.now())
		        .withIdempotencyKey("12345").build();

		mockedHttpRequest.setAttribute(IdempotentReceiverAspect.REQUEST_ATTR_IDEMPOTENCE_METHOD_RESULT, imr);

		String body = "dummy body";
		MediaType selectedContentType = MediaType.TEXT_PLAIN;
		Class<? extends HttpMessageConverter<?>> selectedConverterType = MyStringHttpMessageConverter.class;

		// Do not register MyStringHttpMessageConverter in converters list
		messageConverters.clear();

		serverHttpResponse.getHeaders().add(HttpHeaders.CONTENT_TYPE, "text/plain;UTF-8");
		serverHttpResponse.getHeaders().add(HttpHeaders.LOCATION, "http://dummy/");

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.addAll(serverHttpResponse.getHeaders());

		Object res = cut.beforeBodyWrite(body, null, selectedContentType, selectedConverterType, serverHttpRequest,
		        serverHttpResponse);

		Assertions.assertThat(res).isEqualTo(body);

		Mockito.verify(repository, Mockito.times(0)).register(Mockito.anyString(), Mockito.any());
		Mockito.verify(repository, Mockito.times(1)).unregister(imr.getIdempotencyKey());
	}

}
