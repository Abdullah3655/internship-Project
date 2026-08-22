package com.recruitment.applicationservice;

import com.recruitment.applicationservice.config.ApiVersionProperties;
import com.recruitment.applicationservice.config.JwtProperties;
import com.recruitment.applicationservice.config.ServiceClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, ApiVersionProperties.class, ServiceClientProperties.class})
public class ApplicationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApplicationServiceApplication.class, args);
	}
}
