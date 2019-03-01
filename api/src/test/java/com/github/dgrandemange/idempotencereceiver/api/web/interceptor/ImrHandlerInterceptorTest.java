package com.github.dgrandemange.idempotencereceiver.api.web.interceptor;

import java.time.Instant;
import java.util.List;
import java.util.Map.Entry;

import org.fest.assertions.Assertions;
import org.hamcrest.beans.SamePropertyValuesAs;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.github.dgrandemange.idempotencereceiver.api.aspect.IdempotentReceiverAspect;
import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentMethodResult;
import com.github.dgrandemange.idempotencereceiver.api.service.IdempotentRepository;

@RunWith(MockitoJUnitRunner.class)
public class ImrHandlerInterceptorTest {

	@Spy
	@InjectMocks
	ImrHandlerInterceptor interceptor;

	@Mock
	IdempotentRepository repository;

	MockHttpServletRequest mockedHttpRequest;

	MockHttpServletResponse mockedHttpResponse;

	@Before
	public void setup() {
		mockedHttpRequest = new MockHttpServletRequest();
		mockedHttpResponse = new MockHttpServletResponse();
	}

	@Test
	public void testAfterCompletion_shouldReturn_whenImrAttributeIsNotPresent() throws Exception {
		mockedHttpRequest.removeAttribute(IdempotentReceiverAspect.REQUEST_ATTR_IDEMPOTENCE_METHOD_RESULT);

		interceptor.afterCompletion(mockedHttpRequest, mockedHttpResponse, null, null);

		Mockito.verify(repository, Mockito.times(0)).register(Mockito.anyString(), Mockito.any());
	}

	@Test
	public void testAfterCompletion_shouldReturn_whenImrAttributeIsPresent_andProcessingStateIsDONE() throws Exception {
		IdempotentMethodResult imr = IdempotentMethodResult.builder().withIdempotencyKey("12345")
		        .startedAt(Instant.now()).withResponse("dummy body".getBytes(), String.class, MediaType.TEXT_PLAIN,
		                StringHttpMessageConverter.class, HttpHeaders.EMPTY, HttpStatus.OK)
		        .build();
		mockedHttpRequest.setAttribute(IdempotentReceiverAspect.REQUEST_ATTR_IDEMPOTENCE_METHOD_RESULT, imr);
		Assertions
		        .assertThat(
		                mockedHttpRequest.getAttribute(IdempotentReceiverAspect.REQUEST_ATTR_IDEMPOTENCE_METHOD_RESULT))
		        .isEqualTo(imr);

		interceptor.afterCompletion(mockedHttpRequest, mockedHttpResponse, null, null);

		Mockito.verify(repository, Mockito.times(0)).register(Mockito.anyString(), Mockito.any());
		Assertions
		        .assertThat(
		                mockedHttpRequest.getAttribute(IdempotentReceiverAspect.REQUEST_ATTR_IDEMPOTENCE_METHOD_RESULT))
		        .isNull();
	}

	@Test
	public void testAfterCompletion_shouldUpdateImrWithResponseSatsusAndHeaders_whenImrAttributeIsPresent_andProcessingStateIsRUNNING()
	        throws Exception {
		IdempotentMethodResult imr = IdempotentMethodResult.builder().withIdempotencyKey("12345")
		        .startedAt(Instant.now()).build();
		mockedHttpRequest.setAttribute(IdempotentReceiverAspect.REQUEST_ATTR_IDEMPOTENCE_METHOD_RESULT, imr);
		Assertions
		        .assertThat(
		                mockedHttpRequest.getAttribute(IdempotentReceiverAspect.REQUEST_ATTR_IDEMPOTENCE_METHOD_RESULT))
		        .isEqualTo(imr);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add(HttpHeaders.LOCATION, "http://dummy/resource/1");
		HttpStatus httpStatus = HttpStatus.OK;

		mockedHttpResponse.setStatus(httpStatus.value());
		for (Entry<String, List<String>> entry : httpHeaders.entrySet()) {
			for (String value : entry.getValue()) {
				mockedHttpResponse.addHeader(entry.getKey(), value);
			}
		}

		IdempotentMethodResult expectedImr = IdempotentMethodResult.builder().from(imr)
		        .withResponse(httpHeaders, httpStatus).build();

		interceptor.afterCompletion(mockedHttpRequest, mockedHttpResponse, null, null);

		Mockito.verify(repository, Mockito.times(1)).register(Mockito.eq(expectedImr.getIdempotencyKey()),
		        Mockito.argThat(SamePropertyValuesAs.samePropertyValuesAs(expectedImr)));
		Assertions
		        .assertThat(
		                mockedHttpRequest.getAttribute(IdempotentReceiverAspect.REQUEST_ATTR_IDEMPOTENCE_METHOD_RESULT))
		        .isNull();
	}

}
