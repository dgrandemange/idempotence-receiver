package com.github.dgrandemange.idempotencereceiver.api.model;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.github.dgrandemange.idempotencereceiver.api.annot.Idempotent;
import com.github.dgrandemange.idempotencereceiver.api.aspect.IdempotentReceiverAspect;
import com.github.dgrandemange.idempotencereceiver.api.web.filter.CacheRequestContentFilter;

public class IdempotentReceiverCommonConfiguration {

	private Boolean idempotencyKeyHeaderMandatory = true;

	private String namespace;

	private boolean registerCacheRequestContentFilter;

	private Integer order = Ordered.LOWEST_PRECEDENCE;

	@NestedConfigurationProperty
	private RepositoryCommonConfiguration repository = new RepositoryCommonConfiguration();

	/**
	 * <p>
	 * Indicates if an idempotence key header (see
	 * {@link IdempotentReceiverAspect#HTTP_HEADER_IDEMPOTENCY_KEY}) is required to
	 * be provided by clients when serving {@link Idempotent} methods.
	 * </p>
	 * 
	 * <p>
	 * When set to <code>true</code>, if client does not provide an idempotent key
	 * in {@link IdempotentReceiverAspect#HTTP_HEADER_IDEMPOTENCY_KEY} header, a
	 * {@link HttpStatus#BAD_REQUEST} is returned.
	 * </p>
	 * 
	 * @return true if idempotency key header is required in request, false
	 *         otherwise
	 */
	public Boolean getIdempotencyKeyHeaderMandatory() {
		return idempotencyKeyHeaderMandatory;
	}

	/**
	 * @param idempotencyKeyHeaderMandatory
	 *            See {@link #getIdempotencyKeyHeaderMandatory()}
	 */
	public void setIdempotencyKeyHeaderMandatory(Boolean idempotencyKeyHeaderMandatory) {
		this.idempotencyKeyHeaderMandatory = idempotencyKeyHeaderMandatory;
	}

	/**
	 * @return Name of logical space where idempotent method results should be
	 *         attached to
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * @param namespace
	 *            {@link #getNamespace()}
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * <p>
	 * Indicates if a {@link CacheRequestContentFilter} should be inserted into
	 * servlet filter chain or not.
	 * </p>
	 * 
	 * <p>
	 * Explanations : for the idempotence receiver to work, it needs the request
	 * body to be read multiple times.<br>
	 * Inserting {@link CacheRequestContentFilter} into the servlet filter chain
	 * provides a way do this by wrapping the {@link HttpServletRequest} into a
	 * {@link ContentCachingRequestWrapper} instance that caches the body contents
	 * on first {@link HttpServletRequest#getInputStream()} read.<br>
	 * The request body contents can then be retrieved at any time via the
	 * {@link ContentCachingRequestWrapper#getContentAsByteArray()} method.
	 * </p>
	 * 
	 * <p>
	 * Can be set to <code>false</code> when another servlet filter is already
	 * registered that allows {@link HttpServletRequest#getInputStream()} to be read
	 * multiple times.
	 * </p>
	 * 
	 * @return true if {@link CacheRequestContentFilter} should be inserted into
	 *         servlet filter chain, false otherwise
	 */
	public boolean isRegisterCacheRequestContentFilter() {
		return registerCacheRequestContentFilter;
	}

	/**
	 * @param registerCacheRequestContentFilter
	 *            {@link #isRegisterCacheRequestContentFilter()}
	 */
	public void setRegisterCacheRequestContentFilter(boolean registerCacheRequestContentFilter) {
		this.registerCacheRequestContentFilter = registerCacheRequestContentFilter;
	}

	/**
	 * <p>
	 * Idempotence management aspect precedence.
	 * </p>
	 * 
	 * <p>
	 * Must be very low priority so that other top priority aspects (i.e. spring mvc
	 * aspects, spring security aspects) keep precedence over it. <br>
	 * Set to {@link Ordered#LOWEST_PRECEDENCE} by default.
	 * </p>
	 * 
	 * @return Idempotence management aspect order
	 */
	public Integer getOrder() {
		return order;
	}

	/**
	 * @param order
	 *            {@link #getOrder()}
	 */
	public void setOrder(Integer order) {
		this.order = order;
	}

	/**
	 * @return Nested repository common configuration
	 */
	public RepositoryCommonConfiguration getRepository() {
		return repository;
	}

	/**
	 * @param repository
	 *            {@link #getRepository()}
	 */
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
