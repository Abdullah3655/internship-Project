package com.recruitment.applicationservice.config;

import com.recruitment.applicationservice.client.AuthApi;
import com.recruitment.applicationservice.client.CandidateApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientConfig {

    @Bean
    public CandidateApi candidateApi(ServiceClientProperties properties) {
        return createClient(properties.getCandidateServiceUrl(), CandidateApi.class);
    }

    @Bean
    public AuthApi authApi(ServiceClientProperties properties) {
        return createClient(properties.getAuthServiceUrl(), AuthApi.class);
    }

    private static <T> T createClient(String baseUrl, Class<T> type) {
        RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(type);
    }
}
