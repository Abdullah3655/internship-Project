package com.recruitment.authservice.security;

import com.recruitment.authservice.config.LdapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.stereotype.Component;

import javax.naming.directory.DirContext;

@Component
public class LdapAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(LdapAuthenticator.class);

    private final LdapProperties ldapProperties;
    private final ObjectProvider<LdapContextSource> ldapContextSource;

    public LdapAuthenticator(LdapProperties ldapProperties, ObjectProvider<LdapContextSource> ldapContextSource) {
        this.ldapProperties = ldapProperties;
        this.ldapContextSource = ldapContextSource;
    }

    public boolean authenticate(String ldapDn, String password) {
        if (!ldapProperties.isEnabled()) {
            log.warn("LDAP login attempted but ldap.enabled=false");
            return false;
        }
        if (ldapDn == null || ldapDn.isBlank() || password == null || password.isBlank()) {
            return false;
        }

        LdapContextSource contextSource = ldapContextSource.getIfAvailable();
        if (contextSource == null) {
            log.warn("LDAP context source is not available");
            return false;
        }

        DirContext ctx = null;
        try {
            ctx = contextSource.getContext(ldapDn.trim(), password);
            return true;
        } catch (Exception ex) {
            log.debug("LDAP bind failed for {}: {}", ldapDn, ex.getMessage());
            return false;
        } finally {
            LdapUtils.closeContext(ctx);
        }
    }
}
