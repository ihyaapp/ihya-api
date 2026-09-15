package com.ihya.api.common.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Prefixes every {@code @RestController} route with {@code /v1}, without
 * touching the controllers themselves or the framework's own routes.
 *
 * <p>Deliberately not {@code server.servlet.context-path}: that would also
 * move Actuator's {@code /actuator/health} under {@code /v1}, and
 * infrastructure (load balancers, container health checks) expects health
 * checks at a stable, unversioned path. {@link HandlerTypePredicate#forAnnotation}
 * scopes the prefix to REST controllers only, so Actuator is unaffected.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String API_PREFIX = "/v1";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(API_PREFIX, HandlerTypePredicate.forAnnotation(RestController.class));
    }
}
