package ru.bankapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.bankapi.security.JwtAuthenticationFilter;
import ru.bankapi.security.RestAccessDeniedHandler;
import ru.bankapi.security.RestAuthenticationEntryPoint;

@Configuration
class GatewaySecurityConfig {

    @Bean
    UserDetailsService gatewayUsers() {
        return subject -> User.withUsername(subject).password("").authorities("TOKEN").build();
    }

    @Bean
    SecurityFilterChain gatewaySecurity(HttpSecurity http, JwtAuthenticationFilter jwt,
            RestAuthenticationEntryPoint unauthorized, RestAccessDeniedHandler forbidden) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(config -> config.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
                .authorizeHttpRequests(config -> config
                        .requestMatchers("/internal/**").denyAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/actuator/health/**",
                                "/openapi/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/**").denyAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
