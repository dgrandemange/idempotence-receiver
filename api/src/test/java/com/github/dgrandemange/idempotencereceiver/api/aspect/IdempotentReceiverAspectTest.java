package com.github.dgrandemange.idempotencereceiver.api.aspect;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.fest.assertions.Assertions;
import org.fest.assertions.Fail;
import org.hamcrest.beans.SamePropertyValuesAs;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import com.github.dgrandemange.idempotencereceiver.api.annot.Idempotent;
import com.github.dgrandemange.idempotencereceiver.api.exception.MissingIdempotencyKeyHeaderException;
import com.github.dgrandemange.idempotencereceiver.api.exception.SubsequentPresentationException;
import com.github.dgrandemange.idempotencereceiver.api.exception.UnmarshallException;
import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentMethodResult;
import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentReceiverCommonConfiguration;
import com.github.dgrandemange.idempotencereceiver.api.service.IdempotentRepository;
import com.github.dgrandemange.idempotencereceiver.api.service.support.InMemoryRepository;
import com.github.dgrandemange.idempotencereceiver.api.service.support.MyStringHttpMessageConverter;

@RunWith(MockitoJUnitRunner.class)
public class IdempotentReceiverAspectTest {

	@Spy
	@InjectMocks
	IdempotentReceiverAspect cut;

	@Mock
	IdempotentRepository repository;

	@Mock
	RequestMappingHandlerAdapter handlerAdapter;

	@Mock
	ProceedingJoinPoint joinPoint;

	@Mock
	MethodSignature signature;

	@Mock
	Clock clock;

	MockHttpServletRequest mockedHttpRequest = new MockHttpServletRequest();

	MessageDigest md;

	IdempotentReceiverCommonConfiguration configuration;

	public class MyDummyRestWebService {

		private long dummyProcessingTimeMs;

		/**
		 * Method marked {@link Idempotent}<br>
		 * Compatible return type {@link ResponseEntity}
		 */
		@Idempotent
		public ResponseEntity<String> post() {
			waitForProcessingTime();

			return new ResponseEntity<String>("dummy body", new HttpHeaders(), HttpStatus.OK);
		}

		/**
		 * Note : method NOT marked @Idempotent
		 */
		public ResponseEntity<String> get() {
			waitForProcessingTime();

			return new ResponseEntity<String>("dummy body", new HttpHeaders(), HttpStatus.OK);
		}

		/**
		 * Note : method marked @Idempotent, but return type is not a
		 * {@link ResponseEntity}
		 */
		@Idempotent
		public String delete() {
			waitForProcessingTime();

			return "dummy body";
		}

		private void waitForProcessingTime() {
			try {
				Thread.sleep(dummyProcessingTimeMs);
			} catch (InterruptedException e) {
			}
		}

		public long getDummyProcessingTimeMs() {
			return dummyProcessingTimeMs;
		}

		public void setDummyProcessingTimeMs(long dummyProcessingTimeMs) {
			this.dummyProcessingTimeMs = dummyProcessingTimeMs;
		}
	}

	@Before
	public void setup() throws NoSuchAlgorithmException {
		configuration = new IdempotentReceiverCommonConfiguration();
		configuration.setNamespace("IdempotentReceiverAspectTest");

		cut.setConfiguration(configuration);

		Mockito.when(joinPoint.getSignature()).thenReturn(signature);

		mockedHttpRequest.setRequestURI("/some/dummy/uri");
		Mockito.doReturn(mockedHttpRequest).when(cut).retrieveCurrentHttpRequest();

		md = MessageDigest.getInstance("SHA-256");
	}

	@Test
	@Idempotent
	public void testCore_shouldHandleIdempotency_whenAnIdempotencyKeyIsProvidedInIncomingRequest() throws Throwable {
		Idempotent annot = new Object() {
		}.getClass().getEnclosingMethod().getAnnotation(Idempotent.class);

		String idempotencyKey = genRequestUniqueIdentifier();

		mockedHttpRequest.addHeader("Idempotency-Key", idempotencyKey);

		String dummyRequestHash = "157840f0f1c1d77526a0beb9980c1170d2bc4f5fe170d8fab4309032a1640b36";
		Mockito.doReturn(dummyRequestHash).when(cut).computeRequestHash(mockedHttpRequest);
		Object expectedResult = new Object();
		Mockito.doReturn(expectedResult).when(cut).handleIdempotency(joinPoint, annot, dummyRequestHash);

		Object result = cut.core(joinPoint, annot);
		Assertions.assertThat(result).isEqualTo(expectedResult);
		Mockito.verify(cut, Mockito.times(1)).retrieveCurrentHttpRequest();
		Mockito.verify(cut, Mockito.times(1)).handleIdempotency(joinPoint, annot, dummyRequestHash);
	}

	@Test
	@Idempotent
	public void testCore_shouldHandleIdempotency_whenIdempotencyKeyIsNotMandatory() throws Throwable {
		Idempotent annot = new Object() {
		}.getClass().getEnclosingMethod().getAnnotation(Idempotent.class);

		configuration.setIdempotencyKeyHeaderMandatory(false);

		// Idempotencey key header not set
		mockedHttpRequest.removeHeader("Idempotency-Key");

		String dummyRequestHash = "157840f0f1c1d77526a0beb9980c1170d2bc4f5fe170d8fab4309032a1640b36";
		Mockito.doReturn(dummyRequestHash).when(cut).computeRequestHash(mockedHttpRequest);
		Object expectedResult = new Object();
		Mockito.doReturn(expectedResult).when(cut).handleIdempotency(joinPoint, annot, dummyRequestHash);

		Object result = cut.core(joinPoint, annot);
		Assertions.assertThat(result).isEqualTo(expectedResult);
		Mockito.verify(cut, Mockito.times(1)).retrieveCurrentHttpRequest();
		Mockito.verify(cut, Mockito.times(1)).handleIdempotency(joinPoint, annot, dummyRequestHash);
	}

	@Test
	@Idempotent
	public void testCore_shouldThrowException_whenIdempotencyKeyIsMandatoryButNotProvidedInIncomingRequest()
	        throws Throwable {
		Idempotent annot = new Object() {
		}.getClass().getEnclosingMethod().getAnnotation(Idempotent.class);

		// Note : no idempotency key header set in incoming Http request

		try {
			configuration.setIdempotencyKeyHeaderMandatory(true);
			cut.core(joinPoint, annot);

			Fail.fail("a MissingIdempotencyKeyHeaderException was expected here");
		} catch (MissingIdempotencyKeyHeaderException e) {
			// Expected
			Mockito.verify(cut, Mockito.times(1)).retrieveCurrentHttpRequest();
			Mockito.verify(cut, Mockito.times(0)).computeRequestHash(Mockito.any(HttpServletRequest.class));
			Mockito.verify(joinPoint, Mockito.times(0)).proceed();
		}
	}

	@Test
	@Idempotent
	public void testComputeRequestHash_shouldReturnRequestBodyComputedHash() throws Throwable {
		mockedHttpRequest.addHeader(IdempotentReceiverAspect.HTTP_HEADER_IDEMPOTENCY_KEY,
		        "123e4567-e89b-12d3-a456-556642440000");
		
		mockedHttpRequest.addHeader("X-FORWARDED-FOR", "192.168.10.100");
		mockedHttpRequest.setRemoteAddr("127.0.0.1");

		Principal mockPrincipal = Mockito.mock(Principal.class);
		Mockito.when(mockPrincipal.getName()).thenReturn("BCRRJPPL");
		mockedHttpRequest.setUserPrincipal(mockPrincipal);

		HttpSession session = Mockito.mock(HttpSession.class);
		Mockito.when(session.getId()).thenReturn("12345678901234567890");
		mockedHttpRequest.setSession(session);

		mockedHttpRequest.setMethod("POST");
		mockedHttpRequest.setRequestURI("/books");
		mockedHttpRequest.setQueryString("a=1&b=2");

		String requestBody = "some dummy request body contents";
		mockedHttpRequest.setContent(requestBody.getBytes("UTF-8"));

		String expectedHash = "65f4e443ad6591b8878da34f77e84381759c6a1412ebffc73c31707b08a054aa";

		String idempotentKey = cut.computeRequestHash(mockedHttpRequest);

		Assertions.assertThat(idempotentKey).isEqualTo(expectedHash);
	}

	@Test
	@Idempotent
	public void testComputeRequestHash_shouldReturnRequestBodyComputedHash_OptionalFieldsAbsent() throws Throwable {
		// Idempotency key header not set

		mockedHttpRequest.setRemoteAddr("127.0.0.1");

		// No principal
		mockedHttpRequest.setUserPrincipal(null);

		// No session
		mockedHttpRequest.setSession(null);

		mockedHttpRequest.setMethod("POST");
		mockedHttpRequest.setRequestURI("/books");

		// No query string
		mockedHttpRequest.setQueryString(null);

		// No request body
		mockedHttpRequest.setContent(null);

		String expectedHash = "54e211a44fd4a616ef6d1fb1af6be84c8aa6e967f8d228484dd7f637a0c5e305";

		String idempotentKey = cut.computeRequestHash(mockedHttpRequest);

		Assertions.assertThat(idempotentKey).isEqualTo(expectedHash);
	}

	@Test
	@Idempotent
	public void testHandleIdempotency_shouldHandleRequestAsAfirstPresentation_whenNoEntryMatchesTheProvidedIdempotencyKey()
	        throws Throwable {
		Idempotent annot = new Object() {
		}.getClass().getEnclosingMethod().getAnnotation(Idempotent.class);

		String idempotencyKey = genRequestUniqueIdentifier();
		Mockito.doReturn(null).when(repository).find(idempotencyKey);

		Object expectedResult = new Object();
		Mockito.doReturn(expectedResult).when(cut).handleRequestFirstPresentation(joinPoint, annot, idempotencyKey);

		Object result = cut.handleIdempotency(joinPoint, annot, idempotencyKey);

		Assertions.assertThat(result).isEqualTo(expectedResult);
		Mockito.verify(cut, Mockito.times(1)).handleRequestFirstPresentation(joinPoint, annot, idempotencyKey);
	}

	@Test
	public void testHandleIdempotency_shouldHandleRequestAsASubsequentPresentation_whenAnEntryActuallyMatchesTheProvidedIdempotencyKey()
	        throws Throwable {
		Idempotent annot = new Object() {
		}.getClass().getEnclosingMethod().getAnnotation(Idempotent.class);

		String idempotencyKey = genRequestUniqueIdentifier();
		IdempotentMethodResult imr = IdempotentMethodResult.builder().startedAt(Instant.now())
		        .withIdempotencyKey(idempotencyKey).withResponse("dummy body".getBytes(), String.class,
		                MediaType.TEXT_PLAIN, StringHttpMessageConverter.class, HttpHeaders.EMPTY, HttpStatus.OK)
		        .build();
		Mockito.doReturn(imr).when(repository).find(idempotencyKey);

		ResponseEntity<Object> expectedResponseEntity = new ResponseEntity<Object>(HttpStatus.OK);
		Mockito.doReturn(expectedResponseEntity).when(cut).handleRequestSubsequentPresentation(imr);

		try {
			cut.handleIdempotency(joinPoint, annot, idempotencyKey);
			Fail.fail(String.format("A %s was expected", SubsequentPresentationException.class.getSimpleName()));
		} catch (SubsequentPresentationException e) {
			Assertions.assertThat(e.getResponseEntity()).isEqualTo(expectedResponseEntity);
			Mockito.verify(cut, Mockito.times(1)).handleRequestSubsequentPresentation(imr);
		}
	}

	@Test
	@Idempotent
	public void testHandleRequestFirstPresentation_shouldAddIdempotentMethodResultToRequestAttribute_whenRequestProcessedWithoutError()
	        throws Throwable {
		Idempotent annot = new Object() {
		}.getClass().getEnclosingMethod().getAnnotation(Idempotent.class);

		String idempotencyKey = genRequestUniqueIdentifier();

		Instant processingStartsAt = Instant.ofEpochSecond(0);
		Mockito.doReturn(processingStartsAt).when(clock).instant();
		cut.getInstantProvider().setClock(clock);

		final Instant processingEndsAt = Instant.ofEpochSecond(5);

		Mockito.doReturn(processingStartsAt).when(clock).instant();

		String responseBody = "dummy contents";
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("someHeader", "someHeaderValue");
		HttpStatus responseStatus = HttpStatus.OK;
		final ResponseEntity<String> re = new ResponseEntity<String>(responseBody, responseHeaders, responseStatus);

		Mockito.doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Mockito.doReturn(processingEndsAt).when(clock).instant();
				return re;
			}
		}).when(joinPoint).proceed();

		IdempotentMethodResult expectedImr = IdempotentMethodResult.builder().startedAt(processingStartsAt)
		        .withIdempotencyKey(idempotencyKey).build();

		Mockito.doNothing().when(cut).addIdempotentImageResultToRequestAttributes(Mockito.any());

		Object result = cut.handleRequestFirstPresentation(joinPoint, annot, idempotencyKey);
		Assertions.assertThat(result).isEqualTo(re);

		Mockito.verify(cut, Mockito.times(1))
		        .registerIdempotentImageResult(Mockito.argThat(SamePropertyValuesAs.samePropertyValuesAs(expectedImr)));

		Mockito.verify(cut, Mockito.times(1)).addIdempotentImageResultToRequestAttributes(
		        Mockito.argThat(SamePropertyValuesAs.samePropertyValuesAs(expectedImr)));
	}

	@Test
	@Idempotent(registerableEx = { ArithmeticException.class, NumberFormatException.class })
	public void testHandleRequestFirstPresentation_shouldRegisterRaisedException_whenExceptionIsDeclaredRegisterable()
	        throws Throwable {
		Idempotent annot = new Object() {
		}.getClass().getEnclosingMethod().getAnnotation(Idempotent.class);

		final Exception ex = new ArithmeticException();

		String idempotencyKey = genRequestUniqueIdentifier();

		Instant processingStartsAt = Instant.ofEpochSecond(0);
		Mockito.doReturn(processingStartsAt).when(clock).instant();
		cut.getInstantProvider().setClock(clock);

		final Instant processingEndsAt = Instant.ofEpochSecond(5);

		Mockito.doReturn(processingStartsAt).when(clock).instant();

		Mockito.doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Mockito.doReturn(processingEndsAt).when(clock).instant();
				throw ex;
			}
		}).when(joinPoint).proceed();

		IdempotentMethodResult expectedImr = IdempotentMethodResult.builder().startedAt(processingStartsAt)
		        .withIdempotencyKey(idempotencyKey).build();

		ArgumentCaptor<IdempotentMethodResult> imrArgCaptor = ArgumentCaptor.forClass(IdempotentMethodResult.class);

		try {
			cut.handleRequestFirstPresentation(joinPoint, annot, idempotencyKey);

			Fail.fail(String.format("an exception of type %s was expected here", ex.getClass().getSimpleName()));
		} catch (ArithmeticException e) {
			Mockito.verify(repository, Mockito.times(1)).register(Mockito.eq(idempotencyKey), imrArgCaptor.capture());
			IdempotentMethodResult capturedImr = imrArgCaptor.getValue();
			Assertions.assertThat(SamePropertyValuesAs.samePropertyValuesAs(expectedImr).matches(capturedImr)).isTrue();
			Mockito.verify(cut, Mockito.times(1)).addIdempotentImageResultToRequestAttributes(
			        Mockito.argThat(SamePropertyValuesAs.samePropertyValuesAs(expectedImr)));
		}
	}

	@Test
	@Idempotent(registerableEx = { NumberFormatException.class })
	public void testHandleRequestFirstPresentation_shouldNotRegisterRaisedException_whenExceptionIsNotDeclaredRegisterable()
	        throws Throwable {
		Idempotent annot = new Object() {
		}.getClass().getEnclosingMethod().getAnnotation(Idempotent.class);

		Exception ex = new ArithmeticException();
		Mockito.doThrow(ex).when(joinPoint).proceed();

		String idempotencyKey = genRequestUniqueIdentifier();

		Instant processingStartsAt = Instant.ofEpochSecond(0);
		Mockito.doReturn(processingStartsAt).when(clock).instant();
		cut.getInstantProvider().setClock(clock);

		IdempotentMethodResult expectedImr = IdempotentMethodResult.builder().startedAt(processingStartsAt)
		        .withIdempotencyKey(idempotencyKey).build();

		Mockito.doReturn(processingStartsAt).when(clock).instant();

		try {
			cut.handleRequestFirstPresentation(joinPoint, annot, idempotencyKey);

			Fail.fail(String.format("an exception of type %s was expected here", ex.getClass().getSimpleName()));
		} catch (ArithmeticException e) {
			Mockito.verify(repository, Mockito.times(1)).register(Mockito.eq(idempotencyKey),
			        Mockito.argThat(SamePropertyValuesAs.samePropertyValuesAs(expectedImr)));
			Mockito.verify(cut, Mockito.times(0)).addIdempotentImageResultToRequestAttributes(Mockito.any());
		}
	}

	@Test
	public void testHandleRequestSubsequentPresentation_shouldReturnResponseEntity_whenIdempotentMethodResultWasAstandardHttpResponse()
	        throws Exception {
		Instant startedAt = Instant.now();

		String responseBody = "dummy contents";
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("someHeader", "someHeaderValue");
		HttpStatus responseStatus = HttpStatus.OK;

		HttpHeaders expectedResponseHeaders = new HttpHeaders();
		expectedResponseHeaders.addAll(responseHeaders);

		ResponseEntity<String> expectedRe = new ResponseEntity<String>(responseBody, expectedResponseHeaders,
		        responseStatus);

		IdempotentMethodResult imr = IdempotentMethodResult.builder().startedAt(startedAt)
		        .withIdempotencyKey(genRequestUniqueIdentifier())
		        .withResponse(responseBody.getBytes(), responseBody.getClass(), MediaType.TEXT_PLAIN,
		                StringHttpMessageConverter.class, responseHeaders, responseStatus)
		        .build();

		Mockito.doReturn(responseBody).when(cut).unmarshallBody(imr);

		Object result = cut.handleRequestSubsequentPresentation(imr);

		Assertions.assertThat(result).isInstanceOf(ResponseEntity.class);
		Assertions.assertThat(SamePropertyValuesAs.samePropertyValuesAs(expectedRe).matches((ResponseEntity<?>) result))
		        .isTrue();
		Mockito.verify(cut, Mockito.times(1)).unmarshallBody(imr);
	}

	@Test
	public void testHandleRequestSubsequentPresentation_shouldReturnHttpStatusACCEPTED_whenFirstRequestProcessingIsStillRunning()
	        throws Exception {

		Instant processingStartsAt = Instant.ofEpochSecond(0L);
		Instant now = Instant.ofEpochMilli(5500L);
		Mockito.doReturn(now).when(clock).instant();
		cut.getInstantProvider().setClock(clock);

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add(IdempotentReceiverAspect.HTTP_HEADER_PROCESSING_DURATION, "5500");

		HttpHeaders expectedResponseHeaders = new HttpHeaders();
		expectedResponseHeaders.addAll(responseHeaders);

		ResponseEntity<String> expectedRe = new ResponseEntity<String>(expectedResponseHeaders, HttpStatus.ACCEPTED);

		Object result = cut.handleRequestSubsequentPresentation(IdempotentMethodResult.builder()
		        .startedAt(processingStartsAt).withIdempotencyKey(genRequestUniqueIdentifier()).build());

		Assertions.assertThat(result).isInstanceOf(ResponseEntity.class);
		Assertions.assertThat(SamePropertyValuesAs.samePropertyValuesAs(expectedRe).matches((ResponseEntity<?>) result))
		        .isTrue();
	}

	@Test
	public void testUnmarshallBody_testMessageConverterSelection_shouldExactlyMatchConverterClass1()
	        throws HttpMessageNotReadableException, IOException, UnmarshallException {

		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
		messageConverters.add(new MyStringHttpMessageConverter());
		messageConverters.add(new StringHttpMessageConverter());
		Mockito.doReturn(messageConverters).when(handlerAdapter).getMessageConverters();

		IdempotentMethodResult imr = IdempotentMethodResult.builder().startedAt(Instant.now())
		        .withIdempotencyKey("12345").withResponse("dummy body".getBytes(), String.class, MediaType.TEXT_PLAIN,
		                StringHttpMessageConverter.class, HttpHeaders.EMPTY, HttpStatus.OK)
		        .build();

		Object unmarshalled = cut.unmarshallBody(imr);

		String expectedBody = "dummy body";
		Assertions.assertThat(unmarshalled).isEqualTo(expectedBody);
	}

	@Test
	public void testUnmarshallBody_testMessageConverterSelection_shouldExactlyMatchConverterClass2()
	        throws HttpMessageNotReadableException, IOException, UnmarshallException {

		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
		messageConverters.add(new StringHttpMessageConverter());
		messageConverters.add(new MyStringHttpMessageConverter());
		Mockito.doReturn(messageConverters).when(handlerAdapter).getMessageConverters();

		IdempotentMethodResult imr = IdempotentMethodResult.builder().startedAt(Instant.now())
		        .withIdempotencyKey("12345").withResponse("dummy body".getBytes(), String.class, MediaType.TEXT_PLAIN,
		                MyStringHttpMessageConverter.class, HttpHeaders.EMPTY, HttpStatus.OK)
		        .build();

		Object unmarshalled = cut.unmarshallBody(imr);

		String expectedBody = "My_dummy body";
		Assertions.assertThat(unmarshalled).isEqualTo(expectedBody);
	}

	@Test
	public void testUnmarshallBody_testMessageConverterSelection_shouldRaiseExceptionWhenNoConverterMatches()
	        throws HttpMessageNotReadableException, IOException, UnmarshallException {

		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
		messageConverters.add(new StringHttpMessageConverter());
		Mockito.doReturn(messageConverters).when(handlerAdapter).getMessageConverters();

		IdempotentMethodResult imr = IdempotentMethodResult.builder().startedAt(Instant.now())
		        .withIdempotencyKey("12345").withResponse("dummy body".getBytes(), String.class, MediaType.TEXT_PLAIN,
		                MyStringHttpMessageConverter.class, HttpHeaders.EMPTY, HttpStatus.OK)
		        .build();

		try {
			cut.unmarshallBody(imr);
		} catch (UnmarshallException e) {
			Assertions.assertThat(e.getImr()).isEqualTo(imr);
		}
	}

	@Test
	public void testUnmarshallBody_testMessageConverterSelection_shouldRaiseExceptionWhenBodyTypeIsNotFound()
	        throws HttpMessageNotReadableException, IOException, UnmarshallException {

		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
		messageConverters.add(new StringHttpMessageConverter());
		Mockito.doReturn(messageConverters).when(handlerAdapter).getMessageConverters();

		IdempotentMethodResult imr = IdempotentMethodResult.builder().startedAt(Instant.now())
		        .withIdempotencyKey("12345").withResponse("dummy body".getBytes(), String.class, MediaType.TEXT_PLAIN,
		                MyStringHttpMessageConverter.class, HttpHeaders.EMPTY, HttpStatus.OK)
		        .build();
		imr.setReturnTypeName("some.non.existing.package.1111Abcd");

		try {
			cut.unmarshallBody(imr);
		} catch (UnmarshallException e) {
			Assertions.assertThat(e.getImr()).isEqualTo(imr);
			Assertions.assertThat(e.getCause()).isInstanceOf(ClassNotFoundException.class);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testIsExceptionRegisterable_shouldReturnTrue_whenExceptionTypeFoundInEligibleTypes() throws Exception {
		Assertions
		        .assertThat(cut.isExceptionRegisterable(new Class[] { UnsupportedOperationException.class,
		                IllegalArgumentException.class, NullPointerException.class }, new IllegalArgumentException()))
		        .isTrue();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testIsExceptionRegisterable_shouldReturnFalse_whenExceptionTypeNotFoundInEligibleTypes()
	        throws Exception {
		Assertions.assertThat(cut.isExceptionRegisterable(
		        new Class[] { UnsupportedOperationException.class, NullPointerException.class },
		        new IllegalArgumentException())).isFalse();

	}

	@Test
	public void testAspectWeaving_shouldBeWeaved_OnAnnotatedMethod()
	        throws InterruptedException, NoSuchAlgorithmException, IOException {
		InMemoryRepository repo = new InMemoryRepository();
		MyDummyRestWebService proxy = commonPrepareTestAspectWeaving(repo);

		Mockito.doReturn("12345").when(cut).computeRequestHash(mockedHttpRequest);

		// Invoke a web service method explicitly declared idempotent
		proxy.post();

		IdempotentMethodResult imr = repo.find("12345");
		Assertions.assertThat(imr).isNotNull();
	}

	@Test
	public void testAspectWeaving_shouldNotBeWeaved_onUnannotedMethod()
	        throws InterruptedException, NoSuchAlgorithmException, IOException {
		InMemoryRepository repo = new InMemoryRepository();
		MyDummyRestWebService proxy = commonPrepareTestAspectWeaving(repo);

		Mockito.doReturn("12345").when(cut).computeRequestHash(mockedHttpRequest);

		// Invoke a web service method NOT declared idempotent
		proxy.get();

		IdempotentMethodResult imr = repo.find("12345");
		Assertions.assertThat(imr).isNull();
	}

	private MyDummyRestWebService commonPrepareTestAspectWeaving(IdempotentRepository repo) {
		cut.setRepository(repo);

		String idempotencyKey = genRequestUniqueIdentifier();
		mockedHttpRequest.addHeader("Idempotency-Key", idempotencyKey);

		MyDummyRestWebService target = new MyDummyRestWebService();
		AspectJProxyFactory proxyMaker = new AspectJProxyFactory(target);
		proxyMaker.addAspect(cut);
		MyDummyRestWebService proxy = proxyMaker.getProxy();
		return proxy;
	}

	private String genRequestUniqueIdentifier() {
		return UUID.randomUUID().toString();
	}

}
