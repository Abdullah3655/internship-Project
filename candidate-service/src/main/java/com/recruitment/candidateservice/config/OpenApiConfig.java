package com.recruitment.candidateservice.config;

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

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    private final ApiVersionProperties apiVersionProperties;

    public OpenApiConfig(ApiVersionProperties apiVersionProperties) {
        this.apiVersionProperties = apiVersionProperties;
    }

    @Bean
    public OpenAPI candidateServiceOpenApi() {
        String version = apiVersionProperties.getVersion();
        return new OpenAPI()
                .info(new Info()
                        .title("Candidate Service API")
                        .version(version)
                        .description("Candidate profiles and CV management. Requires JWT from auth-service."))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT from auth-service login")));
    }

    @Bean
    public OperationCustomizer candidateOperationCustomizer() {
        String acceptHeader = "application/json;version=" + apiVersionProperties.getVersion();
        return (operation, handlerMethod) -> {
            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("Accept")
                    .required(true)
                    .description("API version media type")
                    .schema(new StringSchema()._default(acceptHeader))
                    .example(acceptHeader));
            operation.setSecurity(List.of(new SecurityRequirement().addList(BEARER_AUTH)));
            return operation;
        };
    }
}
