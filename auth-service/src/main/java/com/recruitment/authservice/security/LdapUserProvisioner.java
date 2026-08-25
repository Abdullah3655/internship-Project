package com.recruitment.authservice.security;

import com.recruitment.authservice.config.LdapProperties;
import com.recruitment.authservice.exception.EmailAlreadyExistsException;
import com.recruitment.authservice.exception.LdapProvisioningException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ldap.NameAlreadyBoundException;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Component;

import javax.naming.Name;
import java.util.Locale;

@Component
public class LdapUserProvisioner {

    private static final Logger log = LoggerFactory.getLogger(LdapUserProvisioner.class);

    private final LdapProperties ldapProperties;
    private final ObjectProvider<LdapTemplate> ldapTemplate;

    public LdapUserProvisioner(LdapProperties ldapProperties, ObjectProvider<LdapTemplate> ldapTemplate) {
        this.ldapProperties = ldapProperties;
        this.ldapTemplate = ldapTemplate;
    }

    public String createUser(String email, String password, String firstName, String lastName) {
        if (!ldapProperties.isEnabled()) {
            throw new LdapProvisioningException("LDAP is not enabled");
        }
        LdapTemplate template = ldapTemplate.getIfAvailable();
        if (template == null) {
            throw new LdapProvisioningException("LDAP is not available");
        }

        String uid = uidFromEmail(email);
        String userOu = ldapProperties.getUserOu();
        Name relativeDn = LdapNameBuilder.newInstance()
                .add("ou", userOu)
                .add("cn", uid)
                .build();
        String fullDn = "cn=" + uid + ",ou=" + userOu + "," + ldapProperties.getBase();

        DirContextAdapter context = new DirContextAdapter(relativeDn);
        context.setAttributeValues("objectClass", new String[]{"top", "inetOrgPerson", "posixAccount", "shadowAccount"});
        context.setAttributeValue("cn", uid);
        context.setAttributeValue("sn", lastName.trim());
        context.setAttributeValue("givenName", firstName.trim());
        context.setAttributeValue("uid", uid);
        context.setAttributeValue("mail", email.trim().toLowerCase(Locale.ROOT));
        context.setAttributeValue("userPassword", password);
        context.setAttributeValue("uidNumber", String.valueOf(uidNumberFor(email)));
        context.setAttributeValue("gidNumber", "1000");
        context.setAttributeValue("homeDirectory", "/home/" + uid);

        try {
            template.bind(context);
            return fullDn;
        } catch (NameAlreadyBoundException ex) {
            throw new EmailAlreadyExistsException(email);
        } catch (EmailAlreadyExistsException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LdapProvisioningException("Could not create LDAP user: " + ex.getMessage(), ex);
        }
    }

    public void deleteUser(String fullDn) {
        LdapTemplate template = ldapTemplate.getIfAvailable();
        if (template == null || fullDn == null || fullDn.isBlank()) {
            return;
        }
        try {
            String base = ldapProperties.getBase();
            String relative = fullDn;
            if (fullDn.toLowerCase(Locale.ROOT).endsWith("," + base.toLowerCase(Locale.ROOT))) {
                relative = fullDn.substring(0, fullDn.length() - base.length() - 1);
            }
            template.unbind(relative);
        } catch (Exception ex) {
            log.warn("Could not roll back LDAP user {}: {}", fullDn, ex.getMessage());
        }
    }

    static String uidFromEmail(String email) {
        int at = email.indexOf('@');
        String local = at > 0 ? email.substring(0, at) : email;
        String uid = local.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "");
        if (uid.isBlank()) {
            throw new LdapProvisioningException("Email local-part cannot be used as LDAP uid");
        }
        return uid;
    }

    private static int uidNumberFor(String email) {
        int hashed = Math.floorMod(email.toLowerCase(Locale.ROOT).hashCode(), 50_000);
        return 2_000 + hashed;
    }
}
