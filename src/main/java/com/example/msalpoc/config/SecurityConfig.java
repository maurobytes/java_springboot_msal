package com.example.msalpoc.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.Jwt;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${azure.required-group-id:}")
    private String requiredGroupId;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        // Default converter which maps scope/roles claims
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("SCOPE_");

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Start with scopes/roles
            Collection<GrantedAuthority> authorities = grantedAuthoritiesConverter.convert(jwt);

            // Map 'groups' claim from Azure AD into authorities prefixed with GROUP_
            List<String> groups = jwt.getClaimAsStringList("groups");
            if (groups != null) {
                authorities = authorities == null ? Collections.emptyList() : authorities;
                authorities = authorities.stream().collect(Collectors.toList());
                for (String g : groups) {
                    ((List<GrantedAuthority>) authorities).add(new SimpleGrantedAuthority("GROUP_" + g));
                }
            }
            return authorities;
        });

        return converter;
    }
}
