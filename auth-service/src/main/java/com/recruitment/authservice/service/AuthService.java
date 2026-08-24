package com.recruitment.authservice.service;

import com.recruitment.authservice.domain.AccountStatus;
import com.recruitment.authservice.domain.IdentityProvider;
import com.recruitment.authservice.domain.RefreshToken;
import com.recruitment.authservice.domain.User;
import com.recruitment.authservice.domain.UserRole;
import com.recruitment.authservice.dto.AuthResponse;
import com.recruitment.authservice.dto.LoginRequest;
import com.recruitment.authservice.dto.RegisterRequest;
import com.recruitment.authservice.dto.UserResponse;
import com.recruitment.authservice.exception.EmailAlreadyExistsException;
import com.recruitment.authservice.exception.InvalidCredentialsException;
import com.recruitment.authservice.exception.InvalidRefreshTokenException;
import com.recruitment.authservice.exception.UserNotFoundException;
import com.recruitment.authservice.repository.RefreshTokenRepository;
import com.recruitment.authservice.repository.UserRepository;
import com.recruitment.authservice.security.JwtService;
import com.recruitment.authservice.security.LdapAuthenticator;
import com.recruitment.authservice.security.LdapUserProvisioner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LdapAuthenticator ldapAuthenticator;
    private final LdapUserProvisioner ldapUserProvisioner;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LdapAuthenticator ldapAuthenticator,
            LdapUserProvisioner ldapUserProvisioner
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.ldapAuthenticator = ldapAuthenticator;
        this.ldapUserProvisioner = ldapUserProvisioner;
    }

    @Transactional
    public UserResponse register(RegisterRequest request, UserRole role) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setRole(role);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setIdentityProvider(IdentityProvider.LOCAL);
        user = userRepository.save(user);

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse registerLdap(RegisterRequest request, UserRole role) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        String ldapDn = ldapUserProvisioner.createUser(
                email,
                request.password(),
                request.firstName(),
                request.lastName()
        );

        try {
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(null);
            user.setFirstName(request.firstName().trim());
            user.setLastName(request.lastName().trim());
            user.setRole(role);
            user.setAccountStatus(AccountStatus.ACTIVE);
            user.setIdentityProvider(IdentityProvider.LDAP);
            user.setLdapDn(ldapDn);
            user = userRepository.save(user);
            return UserResponse.from(user);
        } catch (RuntimeException ex) {
            ldapUserProvisioner.deleteUser(ldapDn);
            throw ex;
        }
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new InvalidCredentialsException();
        }

        if (user.getIdentityProvider() == IdentityProvider.LOCAL) {
            authenticateLocal(user, request.password());
        } else if (user.getIdentityProvider() == IdentityProvider.LDAP) {
            authenticateLdap(user, request.password());
        } else {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hashToken(refreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(stored);
            throw new InvalidRefreshTokenException();
        }

        User user = stored.getUser();
        if (user.getDeletedAt() != null || user.getAccountStatus() != AccountStatus.ACTIVE) {
            refreshTokenRepository.delete(stored);
            throw new InvalidRefreshTokenException();
        }

        refreshTokenRepository.delete(stored);
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return UserResponse.from(user);
    }

    public UserResponse toUserResponse(User user) {
        return UserResponse.from(user);
    }

    private void authenticateLocal(User user, String password) {
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
    }

    private void authenticateLdap(User user, String password) {
        if (!ldapAuthenticator.authenticate(user.getLdapDn(), password)) {
            throw new InvalidCredentialsException();
        }
    }

    private AuthResponse issueTokens(User user) {
        String refreshToken = newToken();
        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setTokenHash(hashToken(refreshToken));
        stored.setExpiresAt(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()));
        refreshTokenRepository.save(stored);

        return AuthResponse.of(
                jwtService.createToken(user),
                refreshToken,
                jwtService.getExpirationMs() / 1000
        );
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}

