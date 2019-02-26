package com.github.dgrandemange.idempotencereceiver.api.service;

import java.time.Instant;

public interface InstantProvider {
	Instant provide();
}
