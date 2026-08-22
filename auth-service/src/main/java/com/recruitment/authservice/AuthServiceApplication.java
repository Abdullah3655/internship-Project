package com.recruitment.authservice;

import com.recruitment.authservice.config.ApiVersionProperties;
import com.recruitment.authservice.config.JwtProperties;
import com.recruitment.authservice.config.LdapProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(excludeName = {
		"org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration",
		"org.springframework.boot.ldap.autoconfigure.LdapAutoConfiguration"
})
@EnableConfigurationProperties({JwtProperties.class, ApiVersionProperties.class, LdapProperties.class})
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}
}
