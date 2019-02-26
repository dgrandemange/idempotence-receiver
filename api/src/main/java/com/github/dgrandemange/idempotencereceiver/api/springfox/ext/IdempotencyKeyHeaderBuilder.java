package com.github.dgrandemange.idempotencereceiver.api.springfox.ext;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import com.github.dgrandemange.idempotencereceiver.api.annot.Idempotent;
import com.github.dgrandemange.idempotencereceiver.api.aspect.IdempotentReceiverAspect;
import com.github.dgrandemange.idempotencereceiver.api.model.IdempotentReceiverCommonConfiguration;
import com.google.common.base.Optional;

import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.swagger.common.SwaggerPluginSupport;

@Order(SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER)
public class IdempotencyKeyHeaderBuilder implements OperationBuilderPlugin {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdempotencyKeyHeaderBuilder.class);

	@Autowired
	private IdempotentReceiverCommonConfiguration configuration;

	@Override
	public boolean supports(DocumentationType documentationType) {
		return DocumentationType.SWAGGER_2.equals(documentationType);
	}

	@Override
	public void apply(OperationContext context) {

		Optional<Idempotent> optAnnot = context.findAnnotation(Idempotent.class);

		if (optAnnot.isPresent()) {
			String requestMappingPattern = context.requestMappingPattern();
			LOGGER.debug(
			        "Operation '{}(...)' mapped on '{}' is marked idempotent and will be documented with http header '{}'",
			        context.getName(), requestMappingPattern, IdempotentReceiverAspect.HTTP_HEADER_IDEMPOTENCY_KEY);

			final List<Parameter> parameters = new LinkedList<>();

			//@formatter:off
			// Create idempotency key header parameter
	        parameters.add(new ParameterBuilder()
	    		.name(IdempotentReceiverAspect.HTTP_HEADER_IDEMPOTENCY_KEY)
	    		.description("a value uniquely identifying a request "
	    				+ "(e.g. an <a href='https://www.uuidgenerator.net/version4'>UUID version 4</a>) "
	    				+ "generated at client's discretion, "
	    				+ "and used by server to detect any subsequent retries of this request and prevent duplicated processing")
	    		.modelRef(new ModelRef("string"))
	    		.parameterType("header")
	    		.required(configuration.getIdempotencyKeyHeaderMandatory())
	    		.build()
	   		);
	        //@formatter:on

			// Add parameter to endpoint documentation
			context.operationBuilder().parameters(parameters);

		}
	}
}
