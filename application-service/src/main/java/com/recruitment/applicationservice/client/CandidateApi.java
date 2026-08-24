package com.recruitment.applicationservice.client;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;

import java.util.Map;
import java.util.UUID;

@HttpExchange(url = "/api/candidates", accept = "application/json;version=1.0")
public interface CandidateApi {

    @GetExchange("/{id}")
    void getCandidate(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authorization
    );

    @PatchExchange(value = "/{id}", contentType = MediaType.APPLICATION_JSON_VALUE)
    void updateCandidate(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, String> body
    );
}
