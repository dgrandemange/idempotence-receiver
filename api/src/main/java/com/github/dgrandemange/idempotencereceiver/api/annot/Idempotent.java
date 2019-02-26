package com.github.dgrandemange.idempotencereceiver.api.annot;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.github.dgrandemange.idempotencereceiver.api.aspect.IdempotentReceiverAspect;

/**
 * <p>
 * This annotation is intended to be used on any HTTP request handler methods
 * (declared within a rest controller) that requires manual idempotency. It is
 * useful for methods that are not idempotent by nature.
 * </p>
 *
 * @see IdempotentReceiverAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

	/**
	 * Defines zero or more exception classes, which must be subclasses of
	 * {@link Exception}, indicating which exception types should be considered
	 * eligible to registration in the idempotency repository.<br>
	 * 
	 * Impact is : as soon as the exception has been registered in the idempotency
	 * repository, any subsequent invocation (presenting the same idempotent key) of
	 * the idempotent method will immediately raise this registered exception,
	 * therefore skipping further method processing.
	 * 
	 * @return array of exception classes that are registerable as idempotent method
	 *         result
	 */
	Class<? extends Exception>[] registerableEx() default {};
}
