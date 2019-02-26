package com.github.dgrandemange.idempotencereceiver.api.model;

public enum IdempotentKeyProvidingStrategyEnum {
	READ_HEADER_FROM_INCOMING_REQUEST,
	COMPUTE_INCOMING_REQUEST_BODY_HASH;
}
