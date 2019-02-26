package com.github.dgrandemange.idempotencereceiver.api.model;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.github.dgrandemange.idempotencereceiver.api.annot.Idempotent;
import com.github.dgrandemange.idempotencereceiver.api.aspect.IdempotentReceiverAspect;
import com.github.dgrandemange.idempotencereceiver.api.web.filter.CacheRequestContentFilter;

public class IdempotentReceiverCommonConfiguration {

	/**
	 * <p>
	 * Indicates if an idempotence key header (see
	 * {@link IdempotentReceiverAspect#HTTP_HEADER_IDEMPOTENCY_KEY}) is required to
	 * be provided by clients when serving {@link Idempotent} methods
	 * </p>
	 * 
	 * <p>
	 * When set to <code>true</code>, if client does not provide an idempotent key
	 * in {@link IdempotentReceiverAspect#HTTP_HEADER_IDEMPOTENCY_KEY} header, a
	 * {@link HttpStatus#BAD_REQUEST} is returned
	 * </p>
	 */
	private Boolean idempotencyKeyHeaderMandatory = true;

	/**
	 * <p>
	 * Name of logical space where idempotent method results should be attached to
	 * </p>
	 */
	private String namespace;

	/**
	 * <p>
	 * Indicates if a {@link CacheRequestContentFilter} should be inserted
	 * (<code>true</code>) into servlet filter chain or not (<code>false</code>).
	 * </p>
	 * 
	 * <p>
	 * Please note that, for the idempotence receiver to work, it needs the request
	 * body to be read multiple times.<br>
	 * Inserting {@link CacheRequestContentFilter} into the servlet filter chain
	 * provides a way do this by wrapping the {@link HttpServletRequest} into a
	 * {@link ContentCachingRequestWrapper} instance that caches the body contents
	 * on first {@link HttpServletRequest#getInputStream()} read.<br>
	 * The request body contents can then be retrieved at any time via the
	 * {@link ContentCachingRequestWrapper#getContentAsByteArray()} method
	 * </p>
	 * 
	 * <p>
	 * May be set to <code>false</code> when another servlet filter is already
	 * registered that allows {@link HttpServletRequest#getInputStream()} to be read
	 * multiple times.
	 * </p>
	 * 
	 */
	private boolean registerCacheRequestContentFilter;

	/**
	 * <p>
	 * Idempotence management aspect precedence
	 * </p>
	 * 
	 * <p>
	 * Must be very low priority so that other top priority aspects (i.e. spring mvc
	 * aspects, spring security aspects) keep precedence over it
	 * </p>
	 */
	private Integer order = Ordered.LOWEST_PRECEDENCE;

	/**
	 * Nested repository common configuration
	 */
	private RepositoryCommonConfiguration repository = new RepositoryCommonConfiguration();

	public Boolean getIdempotencyKeyHeaderMandatory() {
		return idempotencyKeyHeaderMandatory;
	}

	public void setIdempotencyKeyHeaderMandatory(Boolean idempotencyKeyHeaderMandatory) {
		this.idempotencyKeyHeaderMandatory = idempotencyKeyHeaderMandatory;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public boolean isRegisterCacheRequestContentFilter() {
		return registerCacheRequestContentFilter;
	}

	public void setRegisterCacheRequestContentFilter(boolean registerCacheRequestContentFilter) {
		this.registerCacheRequestContentFilter = registerCacheRequestContentFilter;
	}

	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

	public RepositoryCommonConfiguration getRepository() {
		return repository;
	}

	public void setRepository(RepositoryCommonConfiguration repository) {
		this.repository = repository;
	}

	@Override
	public String toString() {
		return "IdempotentReceiverCommonConfiguration [idempotencyKeyHeaderMandatory=" + idempotencyKeyHeaderMandatory
		        + ", namespace=" + namespace + ", registerCacheRequestContentFilter="
		        + registerCacheRequestContentFilter + ", order=" + order + ", repository=" + repository + "]";
	}

}
