package com.recruitment.authservice.service;

import com.recruitment.authservice.domain.RefreshToken;
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
import com.recruitment.authservice.exception.InvalidRefreshTokenException;
import com.recruitment.authservice.repository.RefreshTokenRepository;
import com.recruitment.authservice.repository.UserRepository;
import com.recruitment.authservice.security.JwtService;
import com.recruitment.authservice.security.LdapAuthenticator;
import com.recruitment.authservice.security.LdapUserProvisioner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private LdapAuthenticator ldapAuthenticator;
    @Mock
    private LdapUserProvisioner ldapUserProvisioner;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("jane@company.com", "password123", "Jane", "HR");
    }

    @Test
    void registerSavesLocalHrUser() {
        when(userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("jane@company.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            setId(user, UUID.fromString("11111111-1111-1111-1111-111111111111"));
            return user;
        });

        UserResponse response = authService.register(registerRequest, UserRole.HR);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("jane@company.com");
        assertThat(saved.getRole()).isEqualTo(UserRole.HR);
        assertThat(saved.getIdentityProvider()).isEqualTo(IdentityProvider.LOCAL);
        assertThat(saved.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(response.role()).isEqualTo(UserRole.HR);
        assertThat(response.email()).isEqualTo("jane@company.com");
        verify(ldapUserProvisioner, never()).createUser(any(), any(), any(), any());
    }

    @Test
    void registerLdapCreatesDirectoryUserAndAppRow() {
        when(userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("jane@company.com")).thenReturn(false);
        when(ldapUserProvisioner.createUser(
                eq("jane@company.com"),
                eq("password123"),
                eq("Jane"),
                eq("HR")
        )).thenReturn("cn=jane,ou=users,dc=company,dc=com");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            setId(user, UUID.fromString("44444444-4444-4444-4444-444444444444"));
            return user;
        });

        UserResponse response = authService.registerLdap(registerRequest, UserRole.HR);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getIdentityProvider()).isEqualTo(IdentityProvider.LDAP);
        assertThat(saved.getPasswordHash()).isNull();
        assertThat(saved.getLdapDn()).isEqualTo("cn=jane,ou=users,dc=company,dc=com");
        assertThat(saved.getRole()).isEqualTo(UserRole.HR);
        assertThat(response.email()).isEqualTo("jane@company.com");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("jane@company.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest, UserRole.HR))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void loginReturnsTokenForValidPassword() {
        User user = hrUser();
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("jane@company.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.createToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(900_000L);
        when(jwtService.getRefreshExpirationMs()).thenReturn(604_800_000L);
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(new LoginRequest("Jane@company.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
        verify(ldapAuthenticator, never()).authenticate(any(), any());
    }

    @Test
    void loginRejectsWrongPassword() {
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("jane@company.com")).thenReturn(Optional.of(hrUser()));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@company.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginLdapUserUsesDirectoryBind() {
        User user = ldapHrUser();
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("ldap.hr@company.com")).thenReturn(Optional.of(user));
        when(ldapAuthenticator.authenticate("cn=ldap.hr,ou=users,dc=company,dc=com", "password123")).thenReturn(true);
        when(jwtService.createToken(user)).thenReturn("ldap-jwt");
        when(jwtService.getExpirationMs()).thenReturn(900_000L);
        when(jwtService.getRefreshExpirationMs()).thenReturn(604_800_000L);
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(new LoginRequest("ldap.hr@company.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("ldap-jwt");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void loginLdapUserRejectsFailedBind() {
        User user = ldapHrUser();
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("ldap.hr@company.com")).thenReturn(Optional.of(user));
        when(ldapAuthenticator.authenticate("cn=ldap.hr,ou=users,dc=company,dc=com", "wrong")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ldap.hr@company.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refreshReturnsNewTokensAndRemovesOldRefreshToken() {
        User user = hrUser();
        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setTokenHash("existing-hash");
        stored.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));
        when(jwtService.createToken(user)).thenReturn("new-access");
        when(jwtService.getExpirationMs()).thenReturn(900_000L);
        when(jwtService.getRefreshExpirationMs()).thenReturn(604_800_000L);
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.refresh("old-refresh-token");

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isNotBlank();
        verify(refreshTokenRepository).delete(stored);
    }

    @Test
    void refreshRejectsUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("missing"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    private static User hrUser() {
        User user = new User();
        setId(user, UUID.fromString("11111111-1111-1111-1111-111111111111"));
        user.setEmail("jane@company.com");
        user.setPasswordHash("hashed");
        user.setFirstName("Jane");
        user.setLastName("HR");
        user.setRole(UserRole.HR);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setIdentityProvider(IdentityProvider.LOCAL);
        return user;
    }

    private static User ldapHrUser() {
        User user = new User();
        setId(user, UUID.fromString("33333333-3333-3333-3333-333333333333"));
        user.setEmail("ldap.hr@company.com");
        user.setPasswordHash(null);
        user.setFirstName("LDAP");
        user.setLastName("HR");
        user.setRole(UserRole.HR);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setIdentityProvider(IdentityProvider.LDAP);
        user.setLdapDn("cn=ldap.hr,ou=users,dc=company,dc=com");
        return user;
    }

    private static void setId(User user, UUID id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
