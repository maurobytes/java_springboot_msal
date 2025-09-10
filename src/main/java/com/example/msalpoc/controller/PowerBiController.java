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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@RestController
@RequestMapping("/api/powerbi")
public class PowerBiController {

    @Autowired
    private SecretsService secretsService;

    @Autowired
    private MsalService msalService;

    private final RestTemplate rest = new RestTemplate();

    /**
     * Minimal PoC that calls Power BI REST API to list reports using OBO (on-behalf-of) flow
     */
    @GetMapping("/reports")
    public ResponseEntity<String> listReports(@RequestHeader("Authorization") String authorization, Authentication auth) throws Exception {
        String userToken = authorization.replaceFirst("Bearer ", "");
        SecretsService.AzureSecrets secrets = secretsService.getAzureSecrets();

        // Power BI scope
        String powerBiScope = "https://analysis.windows.net/powerbi/api/.default";

        IAuthenticationResult result = msalService.acquireTokenOnBehalfOf(secrets, userToken, powerBiScope);
        String accessToken = result.accessToken();

        String url = "https://api.powerbi.com/v1.0/myorg/reports";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resp = rest.exchange(url, HttpMethod.GET, entity, String.class);
        return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
    }
}
