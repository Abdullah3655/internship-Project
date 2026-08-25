package com.recruitment.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ldap")
public class LdapProperties {

    private boolean enabled = false;

    private String url = "ldap://localhost:1389";

    private String base = "dc=company,dc=com";

    private String adminDn = "cn=admin,dc=company,dc=com";

    private String adminPassword = "admin";

    private String userOu = "users";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getAdminDn() {
        return adminDn;
    }

    public void setAdminDn(String adminDn) {
        this.adminDn = adminDn;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getUserOu() {
        return userOu;
    }

    public void setUserOu(String userOu) {
        this.userOu = userOu;
    }
}
