package com.github.dgrandemange.idempotencereceiver.api.service.support;

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentMethodResult;
import com.github.dgrandemange.idempotencereceiver.api.service.IdempotentRepository;

/**
 * A basic repository using an in memory weak hash map.<br>
 * In such map, entries are cleared each time the garbage collection is
 * triggered.<br>
 * So, below implementation is fully dependent on garbage collection policy.<br>
 * <b>For demo only! Do not use in production environment !</b>
 */
public class RepositoryInMemoryGcManagedImpl implements IdempotentRepository {

	public static final String REPOSITORY_TYPE = "internal-memory";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryInMemoryGcManagedImpl.class);

	@PostConstruct
	public void init() {
		LOGGER.error("    +     +====================================================================================+");
		LOGGER.error("   / \\    |                                                                                    |");
		LOGGER.error("  / | \\   | CAUTION : current idempotence repository implementation is for demo purpose only ! |");
		LOGGER.error(" /  !  \\  |                                                                                    |");
		LOGGER.error("+-------+ +====================================================================================+");
	}

	Map<String, IdempotentMethodResult> map = new WeakHashMap<>();

	/**
	 * @see <a href="https://stackoverflow.com/a/30004013">GC doesnt remove objects
	 *      from weakhashmap</a>
	 */
	public IdempotentMethodResult register(String idempotencyKey, IdempotentMethodResult imr) {
		map.put(new String(idempotencyKey.getBytes()), imr);
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
