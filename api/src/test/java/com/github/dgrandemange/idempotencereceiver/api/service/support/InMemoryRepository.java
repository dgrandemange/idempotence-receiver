package com.github.dgrandemange.idempotencereceiver.api.service.support;

import java.util.HashMap;
import java.util.Map;

import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentMethodResult;
import com.github.dgrandemange.idempotencereceiver.api.service.IdempotentRepository;

/**
 * A basic repository using an in memory hash map.<br>
 * No automatic eviction of entries.<br>
 * <b>For test purpose only !</b>
 */
public class InMemoryRepository implements IdempotentRepository {

	Map<String, IdempotentMethodResult> map = new HashMap<>();

	public IdempotentMethodResult register(String idempotencyKey, IdempotentMethodResult imr) {
		map.put(idempotencyKey, imr);
		return imr;
	}

	public IdempotentMethodResult unregister(String idempotencyKey) {
		return map.remove(idempotencyKey);
	}

	public IdempotentMethodResult find(String idempotencyKey) {
		return map.get(idempotencyKey);
	}

	public void clear() {
		map.clear();
	}

}
