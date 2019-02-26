package com.github.dgrandemange.idempotencereceiver.api.service.support;

import java.time.Clock;
import java.time.Instant;

import com.github.dgrandemange.idempotencereceiver.api.service.InstantProvider;

public class InstantProviderImpl implements InstantProvider {

	private Clock clock = Clock.systemUTC();

	public Instant provide() {
		return Instant.now(clock);
	}

	public void setClock(Clock clock) {
		this.clock = clock;
	}

}
