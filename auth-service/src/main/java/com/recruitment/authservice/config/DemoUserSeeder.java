package com.recruitment.authservice.config;

import com.recruitment.authservice.domain.AccountStatus;
import com.recruitment.authservice.domain.IdentityProvider;
import com.recruitment.authservice.domain.User;
import com.recruitment.authservice.domain.UserRole;
import com.recruitment.authservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DemoUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);
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
        seedLocal("admin@company.com", "Ada", "Admin", UserRole.ADMIN);
        seedLocal("hr@company.com", "Jane", "HR", UserRole.HR);
        seedLocal("interviewer@company.com", "John", "Interviewer", UserRole.INTERVIEWER);
        seedLdapHr();
        log.info("Demo LOCAL logins (password for all): {}", DEMO_PASSWORD);
        log.info("Demo LDAP login: {} / {} (DN: {})", LDAP_HR_EMAIL, DEMO_PASSWORD, LDAP_HR_DN);
    }

    private void seedLocal(String email, String firstName, String lastName, UserRole role) {
        if (userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
            return;
        }
        User user = new User();
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
        if (userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(LDAP_HR_EMAIL)) {
            return;
        }
        User user = new User();
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
