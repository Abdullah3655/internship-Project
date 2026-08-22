package com.recruitment.candidateservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api")
public class ApiVersionProperties {

    /**
     * Current API version, sent as Accept: application/json;version=1.0
     */
    private String version = "1.0";

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
