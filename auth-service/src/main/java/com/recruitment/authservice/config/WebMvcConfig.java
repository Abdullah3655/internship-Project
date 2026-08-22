package com.recruitment.authservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiVersionProperties apiVersionProperties;

    public WebMvcConfig(ApiVersionProperties apiVersionProperties) {
        this.apiVersionProperties = apiVersionProperties;
    }

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
                .useMediaTypeParameter(MediaType.APPLICATION_JSON, "version")
                .addSupportedVersions(apiVersionProperties.getVersion())
                .setDefaultVersion(apiVersionProperties.getVersion());
    }
}
