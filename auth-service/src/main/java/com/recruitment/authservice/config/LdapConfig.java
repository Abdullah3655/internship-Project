package com.recruitment.authservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

@Configuration
@EnableConfigurationProperties(LdapProperties.class)
public class LdapConfig {

    @Bean
    @ConditionalOnProperty(prefix = "ldap", name = "enabled", havingValue = "true")
    public LdapContextSource ldapContextSource(LdapProperties properties) {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(properties.getUrl());
        contextSource.setBase(properties.getBase());
        contextSource.setUserDn(properties.getAdminDn());
        contextSource.setPassword(properties.getAdminPassword());
        contextSource.afterPropertiesSet();
        return contextSource;
    }

    @Bean
    @ConditionalOnProperty(prefix = "ldap", name = "enabled", havingValue = "true")
    public LdapTemplate ldapTemplate(LdapContextSource ldapContextSource) {
        return new LdapTemplate(ldapContextSource);
    }
}
