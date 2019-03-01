package com.github.dgrandemange.idempotencereceiver.api.web.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.util.Assert;

public class ByteArrayHttpInputMessage implements HttpInputMessage {

	private final HttpHeaders headers = new HttpHeaders();

	private final InputStream body;

	public ByteArrayHttpInputMessage(byte[] content) {
		Assert.notNull(content, "Byte array must not be null");
		this.body = new ByteArrayInputStream(content);
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		return this.body;
	}

}