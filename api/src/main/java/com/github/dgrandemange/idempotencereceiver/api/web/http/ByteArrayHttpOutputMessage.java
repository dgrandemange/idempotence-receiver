package com.github.dgrandemange.idempotencereceiver.api.web.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;

public class ByteArrayHttpOutputMessage implements HttpOutputMessage {

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private final HttpHeaders headers = new HttpHeaders();

	private final ByteArrayOutputStream body = new ByteArrayOutputStream(1024);

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public OutputStream getBody() throws IOException {
		return this.body;
	}

	/**
	 * @return body content as a byte array
	 */
	public byte[] getBodyAsBytes() {
		return this.body.toByteArray();
	}

	/**
	 * @return the body content interpreted as a UTF-8 string
	 */
	public String getBodyAsString() {
		return getBodyAsString(DEFAULT_CHARSET);
	}

	/**
	 * @param charset
	 *            the charset to use to turn the body content to a String
	 * @return the body content as a string
	 */
	public String getBodyAsString(Charset charset) {
		byte[] bytes = getBodyAsBytes();
		return new String(bytes, charset);
	}

}
