package com.vektor.dispatch_engine.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;

import com.vektor.dispatch_engine.filter.IpRateLimitFilter;
import com.vektor.dispatch_engine.filter.UserRateLimitFilter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           IpRateLimitFilter ipRateLimitFilter,
                                           UserRateLimitFilter userRateLimitFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth 
                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().denyAll() )
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakRealmRoleConverter()))
            )
            .addFilterBefore(ipRateLimitFilter, BearerTokenAuthenticationFilter.class)
            .addFilterAfter(userRateLimitFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
            
    }

    @Bean
    @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.issuer-uri", matchIfMissing = true, havingValue = "false")
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder fallbackJwtDecoder() {
        return token -> {
            throw new BadJwtException("Fallback JwtDecoder: no JWT issuer configured");
        };
    }

    /**
     * Keycloak puts realm roles in realm_access.roles, not in the standard
     * scope claim — the default converter would see no authorities at all.
     */

    private Converter<Jwt, AbstractAuthenticationToken> keycloakRealmRoleConverter() {
        return jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            Collection<GrantedAuthority> authorities = 
                realmAccess == null ? List.of() : 
                ((Collection<?>) realmAccess.getOrDefault("roles", List.of()))
                    .stream()
                    .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();

            return new JwtAuthenticationToken(jwt, authorities);
        };
    }
}
