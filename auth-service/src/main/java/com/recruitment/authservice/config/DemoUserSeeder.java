package com.recruitment.authservice.config;

import com.recruitment.authservice.domain.AccountStatus;
import com.recruitment.authservice.domain.IdentityProvider;
import com.recruitment.authservice.domain.User;
import com.recruitment.authservice.domain.UserRole;
import com.recruitment.authservice.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("!test")
public class DemoUserSeeder implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "password123";
    private static final String LDAP_HR_EMAIL = "ldap.hr@company.com";
    private static final String LDAP_HR_DN = "cn=ldap.hr,ou=users,dc=company,dc=com";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedLocal(DemoIds.ADMIN, "admin@company.com", "Ada", "Admin", UserRole.ADMIN);
        seedLocal(DemoIds.HR, "hr@company.com", "Jane", "HR", UserRole.HR);
        seedLocal(DemoIds.INTERVIEWER, "interviewer@company.com", "John", "Interviewer", UserRole.INTERVIEWER);
        seedLdapHr();
    }

    private void seedLocal(UUID id, String email, String firstName, String lastName, UserRole role) {
        if (userRepository.existsById(id)
                || userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
            return;
        }
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setIdentityProvider(IdentityProvider.LOCAL);
        userRepository.save(user);
    }

    private void seedLdapHr() {
        if (userRepository.existsById(DemoIds.LDAP_HR)
                || userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(LDAP_HR_EMAIL)) {
            return;
        }
        User user = new User();
        user.setId(DemoIds.LDAP_HR);
        user.setEmail(LDAP_HR_EMAIL);
        user.setPasswordHash(null);
        user.setFirstName("LDAP");
        user.setLastName("HR");
        user.setRole(UserRole.HR);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setIdentityProvider(IdentityProvider.LDAP);
        user.setLdapDn(LDAP_HR_DN);
        userRepository.save(user);
    }
}
