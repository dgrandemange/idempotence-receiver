package com.github.dgrandemange.idempotencereceiver.api.aspect;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.github.dgrandemange.idempotencereceiver.api.annot.Idempotent;
import com.github.dgrandemange.idempotencereceiver.api.aspect.IdempotentReceiverAspect;
import com.github.dgrandemange.idempotencereceiver.api.exception.MissingIdempotencyKeyHeaderException;
import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentMethodResult;
import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentReceiverCommonConfiguration;
import com.github.dgrandemange.idempotencereceiver.api.service.IdempotentRepository;
import com.github.dgrandemange.idempotencereceiver.api.service.support.InMemoryRepository;

@RunWith(MockitoJUnitRunner.class)
public class IdempotentReceiverAspectTest {

	@Spy
	@InjectMocks
	IdempotentReceiverAspect cut;

	@Mock
	IdempotentRepository repository;

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

			return new ResponseEntity<String>("dummy body" + (new Date()).getTime(), new HttpHeaders(), HttpStatus.OK);
		}

		/**
		 * Note : method NOT marked @Idempotent
		 */
		public ResponseEntity<String> get() {
			waitForProcessingTime();

			return new ResponseEntity<String>("dummy body" + (new Date()).getTime(), new HttpHeaders(), HttpStatus.OK);
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

		Mockito.doNothing().when(cut).checkMethodReturnType(joinPoint);
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

		Mockito.doNothing().when(cut).checkMethodReturnType(joinPoint);
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

		Mockito.doNothing().when(cut).checkMethodReturnType(joinPoint);

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

		String expectedHash = "e2a25fbc87a58e80b5651b06fcf634c1a97954bfddcc00170deff08a8ab5aa60";

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

		String expectedHash = "42b19f612f4b6fa840ad1c9df27d918506341139ca4f87a33e438805f1e31f8b";

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
		        .withIdempotencyKey(idempotencyKey).hasReturned("dummy body", (new HttpHeaders()), HttpStatus.OK)
		        .build();
		Mockito.doReturn(imr).when(repository).find(idempotencyKey);

		Object expectedResult = new Object();
		Mockito.doReturn(expectedResult).when(cut).handleRequestSubsequentPresentation(imr);

		Object result = cut.handleIdempotency(joinPoint, annot, idempotencyKey);

		Assertions.assertThat(result).isEqualTo(expectedResult);
		Mockito.verify(cut, Mockito.times(1)).handleRequestSubsequentPresentation(imr);
	}

	@Test
	@Idempotent
	public void testHandleRequestFirstPresentation_shouldRegisterResult_whenRequestProcessedWithoutError()
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
		        .withIdempotencyKey(idempotencyKey).hasReturned(responseBody, responseHeaders, responseStatus).build();

		Object result = cut.handleRequestFirstPresentation(joinPoint, annot, idempotencyKey);
		Assertions.assertThat(result).isEqualTo(re);

		Mockito.verify(repository, Mockito.times(1)).register(Mockito.eq(idempotencyKey),
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
		        .withIdempotencyKey(idempotencyKey).hasRaised(ex).build();

		ArgumentCaptor<IdempotentMethodResult> imrArgCaptor = ArgumentCaptor.forClass(IdempotentMethodResult.class);

		try {
			cut.handleRequestFirstPresentation(joinPoint, annot, idempotencyKey);

			Fail.fail(String.format("an exception of type %s was expected here", ex.getClass().getSimpleName()));
		} catch (ArithmeticException e) {
			Mockito.verify(repository, Mockito.times(2)).register(Mockito.eq(idempotencyKey), imrArgCaptor.capture());
			IdempotentMethodResult capturedImr = imrArgCaptor.getValue();
			Assertions.assertThat(SamePropertyValuesAs.samePropertyValuesAs(expectedImr).matches(capturedImr)).isTrue();
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

		Object result = cut.handleRequestSubsequentPresentation(
		        IdempotentMethodResult.builder().startedAt(startedAt).withIdempotencyKey(genRequestUniqueIdentifier())
		                .hasReturned(responseBody, responseHeaders, responseStatus).build());

		Assertions.assertThat(result).isInstanceOf(ResponseEntity.class);
		Assertions.assertThat(SamePropertyValuesAs.samePropertyValuesAs(expectedRe).matches((ResponseEntity<?>) result))
		        .isTrue();
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
	public void testHandleRequestSubsequentPresentation_shouldRaiseException_whenIdempotentMethodResultWasAnException()
	        throws Exception {
		Exception expectedEx = new ArithmeticException();

		try {
			cut.handleRequestSubsequentPresentation(IdempotentMethodResult.builder().startedAt(Instant.now())
			        .withIdempotencyKey(genRequestUniqueIdentifier()).hasRaised(expectedEx).build());
			Fail.fail(
			        String.format("An exception of type %s was expected here", expectedEx.getClass().getSimpleName()));
		} catch (ArithmeticException e) {
			Assertions.assertThat(e).isEqualTo(expectedEx);
		}
	}

	@Test
	public void testCheckMethodReturnType_shouldReturnWithoutError_whenOfTypeResponseEntity() throws Exception {
		Mockito.when(signature.getReturnType()).thenReturn(ResponseEntity.class);
		Mockito.when(signature.toLongString()).thenReturn("myMockedMethod()");
		cut.checkMethodReturnType(joinPoint);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testCheckMethodReturnType_shouldRaiseException_whenNotOfTypeResponseEntity() throws Exception {
		Mockito.when(signature.getReturnType()).thenReturn(String.class);
		Mockito.when(signature.toLongString()).thenReturn("myMockedMethod()");
		cut.checkMethodReturnType(joinPoint);
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
	public void testAspectWeaving_shouldHandleIdempotency_whenWeavedOnCompatibleMethod() throws InterruptedException {
		MyDummyRestWebService proxy = commonPrepareTestAspectWeaving();

		// 1st presentation
		ResponseEntity<String> resp1 = proxy.post();

		// Wait some time before representation of request
		Thread.sleep(10);

		// 2nd presentation (same idempotency key)
		ResponseEntity<String> resp2 = proxy.post();

		// Same responses content expected
		Assertions.assertThat(resp1.getBody()).isEqualTo(resp2.getBody());
	}

	@Test
	public void testAspectWeaving_shouldHandleIdempotency_whenWeavedOnCompatibleMethod_firstRequestStillProcessingCase()
	        throws InterruptedException, ExecutionException {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {

			final MyDummyRestWebService proxy = commonPrepareTestAspectWeaving();
			proxy.setDummyProcessingTimeMs(1000);

			// 1st presentation (launched asynchronously in another thread)
			Future<ResponseEntity<String>> futureResp1 = executor.submit(() -> proxy.post());

			// Wait some time before representation of request (should be short enough so
			// that first request processing is still running)
			Thread.sleep(100);

			// 2nd presentation (same idempotency key)
			ResponseEntity<String> resp2 = proxy.post();

			// Check 2nd presentation response
			Assertions.assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
			Assertions
			        .assertThat(
			                resp2.getHeaders().containsKey(IdempotentReceiverAspect.HTTP_HEADER_PROCESSING_DURATION))
			        .isTrue();

			// Wait 1st presentation response
			futureResp1.get();
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	public void testAspectWeaving_shouldNotBeWeaved_onUnannotedMethods() throws InterruptedException {
		MyDummyRestWebService proxy = commonPrepareTestAspectWeaving();

		// 1st presentation
		ResponseEntity<String> resp1 = proxy.get();

		// Wait some time before representation of request
		Thread.sleep(10);

		// 2nd presentation (same idempotency key)
		ResponseEntity<String> resp2 = proxy.get();

		// Different responses content expected
		Assertions.assertThat(resp1.getBody()).isNotEqualTo(resp2.getBody());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testAspectWeaving_shouldRaisException_whenWeavedOnIncompatibleMethod() throws InterruptedException {
		MyDummyRestWebService proxy = commonPrepareTestAspectWeaving();

		proxy.delete();
	}

	private MyDummyRestWebService commonPrepareTestAspectWeaving() {
		InMemoryRepository inMemRepository = new InMemoryRepository();
		cut.setRepository(inMemRepository);

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
