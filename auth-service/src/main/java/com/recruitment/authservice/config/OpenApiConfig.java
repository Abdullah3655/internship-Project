package com.recruitment.authservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.method.HandlerMethod;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    private final ApiVersionProperties apiVersionProperties;

    public OpenApiConfig(ApiVersionProperties apiVersionProperties) {
        this.apiVersionProperties = apiVersionProperties;
    }

    @Bean
    public OpenAPI authServiceOpenApi() {
        String version = apiVersionProperties.getVersion();
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .version(version)
                        .description("""
                                Auth service for the recruitment platform.
                                Demo users: hr@company.com, admin@company.com, interviewer@company.com, ldap.hr@company.com (password123).
                                """))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT from /api/auth/login")));
    }

    @Bean
    public OperationCustomizer authOperationCustomizer() {
        String acceptHeader = "application/json;version=" + apiVersionProperties.getVersion();
        return (operation, handlerMethod) -> {
            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("Accept")
                    .required(true)
                    .description("API version media type")
                    .schema(new StringSchema()._default(acceptHeader))
                    .example(acceptHeader));
            if (isLoginOperation(handlerMethod)) {
                operation.setSecurity(List.of());
            } else {
                operation.setSecurity(List.of(new SecurityRequirement().addList(BEARER_AUTH)));
            }
            return operation;
        };
    }

    private static boolean isLoginOperation(HandlerMethod handlerMethod) {
        PostMapping post = handlerMethod.getMethodAnnotation(PostMapping.class);
        if (post == null) {
            return false;
        }
        for (String path : post.path()) {
            if ("login".equals(path) || path.endsWith("/login")) {
                return true;
            }
        }
        return false;
    }
}
