package com.recruitment.authservice.service;

import com.recruitment.authservice.domain.AccountStatus;
import com.recruitment.authservice.domain.IdentityProvider;
import com.recruitment.authservice.domain.User;
import com.recruitment.authservice.domain.UserRole;
import com.recruitment.authservice.dto.AuthResponse;
import com.recruitment.authservice.dto.LoginRequest;
import com.recruitment.authservice.dto.RegisterRequest;
import com.recruitment.authservice.dto.UserResponse;
import com.recruitment.authservice.exception.EmailAlreadyExistsException;
import com.recruitment.authservice.exception.InvalidCredentialsException;
import com.recruitment.authservice.exception.UserNotFoundException;
import com.recruitment.authservice.repository.UserRepository;
import com.recruitment.authservice.security.JwtService;
import com.recruitment.authservice.security.LdapAuthenticator;
import com.recruitment.authservice.security.LdapUserProvisioner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LdapAuthenticator ldapAuthenticator;
    private final LdapUserProvisioner ldapUserProvisioner;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LdapAuthenticator ldapAuthenticator,
            LdapUserProvisioner ldapUserProvisioner
    ) {
        this.userRepository = userRepository;
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

    @Transactional(readOnly = true)
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

        return toAuthResponse(user);
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

    private AuthResponse toAuthResponse(User user) {
        return AuthResponse.of(jwtService.createToken(user));
    }
}

