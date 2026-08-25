package com.recruitment.applicationservice.config;

import com.recruitment.applicationservice.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/jobs", "/api/jobs/{id}").hasAnyRole("HR", "ADMIN", "INTERVIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/jobs", "/api/jobs/{id}/publish").hasAnyRole("HR", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/jobs/{id}").hasAnyRole("HR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/jobs/{id}").hasAnyRole("HR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/applications", "/api/applications/{id}",
                                "/api/applications/{id}/stage-changes",
                                "/api/applications/{id}/assignments",
                                "/api/applications/{id}/evaluations")
                        .hasAnyRole("HR", "ADMIN", "INTERVIEWER")
                        .requestMatchers(HttpMethod.GET, "/api/assignments")
                        .hasRole("INTERVIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/applications",
                                "/api/applications/{id}/stage-changes",
                                "/api/applications/{id}/assignments")
                        .hasAnyRole("HR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/applications/{id}",
                                "/api/applications/{id}/assignments/{assignmentId}",
                                "/api/applications/{id}/evaluations/{evaluationId}")
                        .hasAnyRole("HR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/applications/{id}/evaluations")
                        .hasRole("INTERVIEWER")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJson(response, HttpServletResponse.SC_FORBIDDEN, "Access denied")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
