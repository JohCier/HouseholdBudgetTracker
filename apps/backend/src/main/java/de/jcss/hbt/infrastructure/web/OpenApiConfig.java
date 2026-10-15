package de.jcss.hbt.infrastructure.web;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Tunes the generated OpenAPI document (see {@code api/openapi.json}) so that generated
 * clients get precise types.
 */
@Configuration(proxyBeanMethods = false)
@SecurityScheme(name = OpenApiConfig.SESSION_COOKIE, type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.COOKIE,
        paramName = "HBT_SESSION", description = "Session token, set by the users app at login (POST /api/auth/login)")
public class OpenApiConfig {

    static final String SESSION_COOKIE = "session";

    /**
     * Responses always contain every property (absent values are serialized as
     * {@code null} and declared {@code nullable}), so all response properties are required.
     */
    @Bean
    OpenApiCustomizer responsePropertiesAreRequired() {
        return openApi -> openApi.getComponents().getSchemas().forEach((name, schema) -> {
            Map<String, Schema> properties = schema.getProperties();
            if (name.endsWith("Response") && properties != null) {
                schema.setRequired(new ArrayList<>(properties.keySet()));
            }
        });
    }

    /**
     * OpenAPI 3.0 ignores siblings of {@code $ref}, so {@code @Schema(nullable = true)} on a
     * property referencing e.g. an enum is lost. Wrap such references in {@code allOf}.
     */
    @Bean
    PropertyCustomizer nullableReferences() {
        return (property, type) -> {
            Annotation[] annotations = type.getCtxAnnotations() == null ? new Annotation[0] : type.getCtxAnnotations();
            Optional<io.swagger.v3.oas.annotations.media.Schema> nullable = Stream.of(annotations)
                    .filter(io.swagger.v3.oas.annotations.media.Schema.class::isInstance)
                    .map(io.swagger.v3.oas.annotations.media.Schema.class::cast)
                    .filter(io.swagger.v3.oas.annotations.media.Schema::nullable)
                    .findFirst();
            if (nullable.isEmpty() || property.get$ref() == null) {
                return property;
            }
            Schema<?> wrapper = new ComposedSchema().allOf(List.of(new Schema<>().$ref(property.get$ref())));
            wrapper.setNullable(true);
            if (!nullable.get().description().isEmpty()) {
                wrapper.setDescription(nullable.get().description());
            }
            return wrapper;
        };
    }
}
