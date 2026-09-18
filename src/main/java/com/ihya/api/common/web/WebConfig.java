package com.ihya.api.common.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
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
 *
 * <p><strong>Found during Phase 8:</strong> {@code forAnnotation} alone
 * matches every {@code @RestController} bean in the application context,
 * including ones from third-party libraries — springdoc's own controllers
 * are {@code @RestController}s too, so its {@code /v3/api-docs/**} config
 * endpoint was silently relocating to {@code /v1/v3/api-docs/**} and 401ing
 * (not covered by the docs-specific {@code permitAll} in
 * {@code SecurityConfig}, which reasonably assumed the un-prefixed path).
 * {@code .and(forBasePackage(...))} narrows the prefix to our own
 * controllers only.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String API_PREFIX = "/v1";
    private static final String BASE_PACKAGE = "com.ihya.api";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(API_PREFIX,
                HandlerTypePredicate.forAnnotation(RestController.class)
                        .and(HandlerTypePredicate.forBasePackage(BASE_PACKAGE)));
    }

    /**
     * Serves the hand-maintained specs under {@code src/main/resources/openapi/}
     * at {@code /openapi/*.yaml} — what {@code springdoc.swagger-ui.urls}
     * (application.yml) points Swagger UI at, instead of springdoc generating
     * its own spec from controller annotations. These are documentation the
     * team writes by hand (see CLAUDE.md), not codegen input.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/openapi/**").addResourceLocations("classpath:/openapi/");
    }
}
