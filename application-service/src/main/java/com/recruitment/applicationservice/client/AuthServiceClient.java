package com.recruitment.applicationservice.client;

import com.recruitment.applicationservice.exception.BadRequestException;
import com.recruitment.applicationservice.exception.ConflictException;
import com.recruitment.applicationservice.exception.ServiceUnavailableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class AuthServiceClient {

    private final AuthApi authApi;

    public AuthServiceClient(AuthApi authApi) {
        this.authApi = authApi;
    }

    public AuthUserView requireUserExists(UUID userId, String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new BadRequestException("Authorization header is required");
        }
        try {
            AuthUserView user = authApi.getUser(userId, authorization);
            if (user == null || user.id() == null) {
                throw new BadRequestException("User not found: " + userId);
            }
            return user;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new BadRequestException("User not found: " + userId);
        } catch (HttpClientErrorException.BadRequest ex) {
            throw new BadRequestException(clientMessage(ex, "Invalid user request"));
        } catch (HttpClientErrorException.Conflict ex) {
            throw new ConflictException(clientMessage(ex, "User conflict"));
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new ServiceUnavailableException("Auth service rejected authentication");
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new ServiceUnavailableException("Auth service rejected the request");
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("Auth service is unavailable");
        }
    }

    public void requireInterviewer(UUID userId, String authorization) {
        AuthUserView user = requireUserExists(userId, authorization);
        if (user.role() == null || !"INTERVIEWER".equalsIgnoreCase(user.role())) {
            throw new BadRequestException("Assigned user must have INTERVIEWER role");
        }
        if (user.accountStatus() != null && !"ACTIVE".equalsIgnoreCase(user.accountStatus())) {
            throw new BadRequestException("Assigned user is not active");
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
