package com.recruitment.applicationservice.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.UUID;

@HttpExchange(url = "/api/auth", accept = "application/json;version=1.0")
public interface AuthApi {

    @GetExchange("/users/{id}")
    AuthUserView getUser(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authorization
    );
}
