package com.example.msalpoc.controller;

import com.example.msalpoc.service.SecretsService;
import com.example.msalpoc.service.MsalService;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class HelloController {

    @Autowired
    private SecretsService secretsService;

    @Autowired
    private MsalService msalService;

    private final RestTemplate rest = new RestTemplate();

    @GetMapping("/api/hello/local")
    public String helloLocal(@RequestParam(defaultValue = "world") String name, Authentication auth) {
        // Simply return a hello message with the authenticated user's name or subject
        String subject = auth.getName();
        return String.format("Hello %s (requested for %s)", name, subject);
    }

    @GetMapping("/api/hello/external")
    public ResponseEntity<String> helloExternal(@RequestParam(defaultValue = "world") String name,
                                                @RequestHeader("Authorization") String authorization) throws Exception {
        // Example of using OBO to call an external API that accepts Azure AD access tokens
        // Assume front-end sent: Authorization: Bearer <user-token>
        String userToken = authorization.replaceFirst("Bearer ", "");

        SecretsService.AzureSecrets secrets = secretsService.getAzureSecrets();

        // The external API scope expected; for PoC use a placeholder scope (replace with real API scope)
        String externalApiScope = "api://external-hello-api/.default";

        IAuthenticationResult result = msalService.acquireTokenOnBehalfOf(secrets, userToken, externalApiScope);
        String accessToken = result.accessToken();

        // Call the external hello API (replace URL with your actual API)
        String externalUrl = String.format("https://example.com/hello?name=%s", name);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resp = rest.exchange(externalUrl, HttpMethod.GET, entity, String.class);
        return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
    }

    @GetMapping("/api/hello/check-group")
    public ResponseEntity<String> helloCheckGroup(Authentication auth) {
        SecretsService.AzureSecrets secrets = secretsService.getAzureSecrets();
        String expectedGroupAuthority = "GROUP_" + secrets.requiredGroupId;
        Set<String> authorities = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        
        // Check if this is a client credentials token (app-only) by looking for specific claims
        if (auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
            org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) auth.getPrincipal();
            String appIdAcr = jwt.getClaimAsString("appidacr");
            
            // If appidacr == "1", this is a client credentials token (app-only)
            if ("1".equals(appIdAcr)) {
                return ResponseEntity.ok("Application token accepted - bypassing group check. Hello " + auth.getName());
            }
        }
        
        // For user tokens, check group membership
        if (!authorities.contains(expectedGroupAuthority)) {
            return ResponseEntity.status(403).body("User is not member of required group. Available authorities: " + authorities);
        }
        return ResponseEntity.ok("User is member of required group. Hello " + auth.getName());
    }
}
