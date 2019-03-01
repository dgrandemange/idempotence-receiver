package com.github.dgrandemange.idempotencereceiver.api.service;

import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentMethodResult;

public interface IdempotentRepository {

	/**
	 * @param idempotencyKey Idempotent method result identifier (key) in repository  
	 * @param imr
	 *            Idempotent method result
	 * @return registered result
	 */
	IdempotentMethodResult register(String idempotencyKey, IdempotentMethodResult imr);

	/**
	 * @param idempotencyKey Idempotent method result identifier (key) in repository
	 * @return unregistered result
	 */
	IdempotentMethodResult unregister(String idempotencyKey);

	/**
	 * @param idempotencyKey Idempotent method result identifier (key) in repository
	 * @return result matching given idempotencyKey, null if no match
	 */
	IdempotentMethodResult find(String idempotencyKey);
	
	/**
	 * @return repository type name
	 */
	String getType();
}
