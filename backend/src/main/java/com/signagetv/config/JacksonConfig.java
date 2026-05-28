package com.signagetv.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura Jackson para usar snake_case en la JSON pública del API,
 * tal y como aparece en el documento de arquitectura.
 *
 * Ej: defaultImageSeconds (Java) <-> default_image_seconds (JSON)
 *     mediaItemId           (Java) <-> media_item_id        (JSON)
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }
}
