package com.github.dgrandemange.idempotencereceiver.api.service;

import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentMethodResult;

public interface IdempotentRepository {

	/**
	 * @param idempotencyKey
	 * @param imr
	 *            Idempotent method result
	 * @return registered result
	 */
	IdempotentMethodResult register(String idempotencyKey, IdempotentMethodResult imr);

	/**
	 * @param idempotencyKey
	 * @return unregistered result
	 */
	IdempotentMethodResult unregister(String idempotencyKey);

	/**
	 * @param idempotencyKey
	 * @return result matching given idempotencyKey, null if no match
	 */
	IdempotentMethodResult find(String idempotencyKey);
}
