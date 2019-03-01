package com.github.dgrandemange.idempotencereceiver.api.service.support;

import java.io.IOException;
import java.nio.charset.Charset;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.StringHttpMessageConverter;

public class MyStringHttpMessageConverter extends StringHttpMessageConverter {

	private static final String MY_PREFIX = "My_";

	public MyStringHttpMessageConverter() {
		super();
	}

	public MyStringHttpMessageConverter(Charset defaultCharset) {
		super(defaultCharset);
	}

	@Override
	protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage) throws IOException {
		return MY_PREFIX + super.readInternal(clazz, inputMessage);
	}

	@Override
	protected void writeInternal(String str, HttpOutputMessage outputMessage) throws IOException {
		super.writeInternal(MY_PREFIX + str, outputMessage);
	}

}