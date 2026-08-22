package com.recruitment.applicationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services")
public class ServiceClientProperties {

    private String candidateServiceUrl = "http://localhost:8082";
    private String authServiceUrl = "http://localhost:8081";

    public String getCandidateServiceUrl() {
        return candidateServiceUrl;
    }

    public void setCandidateServiceUrl(String candidateServiceUrl) {
        this.candidateServiceUrl = candidateServiceUrl;
    }

    public String getAuthServiceUrl() {
        return authServiceUrl;
    }

    public void setAuthServiceUrl(String authServiceUrl) {
        this.authServiceUrl = authServiceUrl;
    }
}
