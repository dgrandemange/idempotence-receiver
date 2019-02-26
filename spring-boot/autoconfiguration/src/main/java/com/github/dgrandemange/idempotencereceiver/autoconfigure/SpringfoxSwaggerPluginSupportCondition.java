package com.github.dgrandemange.idempotencereceiver.autoconfigure;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class SpringfoxSwaggerPluginSupportCondition implements Condition {

	@Override
	public boolean matches(ConditionContext arg0, AnnotatedTypeMetadata arg1) {
		try {
			Class.forName("springfox.documentation.swagger.common.SwaggerPluginSupport");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

}
