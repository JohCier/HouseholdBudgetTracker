package de.jcss.hbt.users.infrastructure.web;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Map;

/**
 * Tunes the generated OpenAPI document (see {@code packages/shared-types/openapi/users.json})
 * so that generated clients get precise types.
 */
@Configuration(proxyBeanMethods = false)
@SecurityScheme(name = OpenApiConfig.SESSION_COOKIE, type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.COOKIE,
        paramName = "HBT_SESSION", description = "Session token, set by POST /api/auth/login and /api/auth/register")
public class OpenApiConfig {

    static final String SESSION_COOKIE = "session";

    /** Responses always contain every property, so all response properties are required. */
    @Bean
    OpenApiCustomizer responsePropertiesAreRequired() {
        return openApi -> openApi.getComponents().getSchemas().forEach((name, schema) -> {
            Map<String, Schema> properties = schema.getProperties();
            if (name.endsWith("Response") && properties != null) {
                schema.setRequired(new ArrayList<>(properties.keySet()));
            }
        });
    }
}
