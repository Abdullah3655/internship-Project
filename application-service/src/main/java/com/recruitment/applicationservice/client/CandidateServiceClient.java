package com.recruitment.applicationservice.client;

import com.recruitment.applicationservice.config.ServiceClientProperties;
import com.recruitment.applicationservice.exception.BadRequestException;
import com.recruitment.applicationservice.exception.ConflictException;
import com.recruitment.applicationservice.exception.ServiceUnavailableException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
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
        } catch (HttpClientErrorException.BadRequest ex) {
            throw new BadRequestException(clientMessage(ex, "Invalid candidate request"));
        } catch (HttpClientErrorException.Conflict ex) {
            throw new ConflictException(clientMessage(ex, "Candidate conflict"));
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new ServiceUnavailableException("Candidate service rejected authentication");
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
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
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(JSON_V1)
                    .body("{\"talentStatus\":\"HIRED\"}")
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new BadRequestException("Candidate not found: " + candidateId);
        } catch (HttpClientErrorException.BadRequest ex) {
            throw new BadRequestException(clientMessage(ex, "Invalid hire update"));
        } catch (HttpClientErrorException.Conflict ex) {
            throw new ConflictException(clientMessage(ex, "Candidate conflict"));
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new ServiceUnavailableException("Candidate service rejected authentication");
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new ServiceUnavailableException("Candidate service rejected the hire update");
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("Candidate service is unavailable");
        }
    }

    private static String clientMessage(HttpClientErrorException ex, String fallback) {
        String body = ex.getResponseBodyAsString();
        if (body != null && body.contains("\"message\"")) {
            int start = body.indexOf("\"message\"");
            int colon = body.indexOf(':', start);
            int firstQuote = body.indexOf('"', colon + 1);
            int secondQuote = body.indexOf('"', firstQuote + 1);
            if (firstQuote >= 0 && secondQuote > firstQuote) {
                return body.substring(firstQuote + 1, secondQuote);
            }
        }
        return fallback;
    }
}
