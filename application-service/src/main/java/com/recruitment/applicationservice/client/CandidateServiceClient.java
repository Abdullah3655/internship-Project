package com.recruitment.applicationservice.client;

import com.recruitment.applicationservice.config.ServiceClientProperties;
import com.recruitment.applicationservice.exception.BadRequestException;
import com.recruitment.applicationservice.exception.ServiceUnavailableException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class CandidateServiceClient {

    private static final MediaType JSON_V1 = MediaType.parseMediaType("application/json;version=1.0");

    private final RestClient restClient;
    private final ServiceClientProperties serviceClientProperties;

    public CandidateServiceClient(RestClient restClient, ServiceClientProperties serviceClientProperties) {
        this.restClient = restClient;
        this.serviceClientProperties = serviceClientProperties;
    }

    public void requireCandidateExists(UUID candidateId, String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new BadRequestException("Authorization header is required");
        }
        try {
            restClient.get()
                    .uri(serviceClientProperties.getCandidateServiceUrl() + "/api/candidates/{id}", candidateId)
                    .header("Authorization", authorization)
                    .accept(JSON_V1)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new BadRequestException("Candidate not found: " + candidateId);
        } catch (HttpClientErrorException ex) {
            throw new ServiceUnavailableException("Candidate service rejected the request");
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("Candidate service is unavailable");
        }
    }

    public void markHired(UUID candidateId, String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new BadRequestException("Authorization header is required");
        }
        try {
            restClient.patch()
                    .uri(serviceClientProperties.getCandidateServiceUrl() + "/api/candidates/{id}", candidateId)
                    .header("Authorization", authorization)
                    .contentType(JSON_V1)
                    .accept(JSON_V1)
                    .body("{\"talentStatus\":\"HIRED\"}")
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new BadRequestException("Candidate not found: " + candidateId);
        } catch (HttpClientErrorException ex) {
            throw new ServiceUnavailableException("Candidate service rejected the hire update");
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("Candidate service is unavailable");
        }
    }
}
