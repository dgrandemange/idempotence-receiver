package com.github.dgrandemange.idempotencereceiver.api.web.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.ContentCachingRequestWrapper;

public class CacheRequestContentFilter extends GenericFilterBean {
	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
	        throws IOException, ServletException {
		if (servletRequest instanceof HttpServletRequest) {
			HttpServletRequest requestCacheWrapperObject = new ContentCachingRequestWrapper(
			        (HttpServletRequest) servletRequest);
			requestCacheWrapperObject.getParameterMap();

			chain.doFilter(requestCacheWrapperObject, servletResponse);
		} else {
			chain.doFilter(servletRequest, servletResponse);
		}
	}
}
