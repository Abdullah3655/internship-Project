package com.recruitment.candidateservice;

import com.recruitment.candidateservice.config.ApiVersionProperties;
import com.recruitment.candidateservice.config.FileUploadProperties;
import com.recruitment.candidateservice.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, FileUploadProperties.class, ApiVersionProperties.class})
public class CandidateServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CandidateServiceApplication.class, args);
	}
}
