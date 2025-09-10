package com.example.msalpoc.service;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.example.msalpoc.service.SecretsService.AzureSecrets;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.OnBehalfOfParameters;
import com.microsoft.aad.msal4j.UserAssertion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MsalService {

    private static final Logger LOG = LoggerFactory.getLogger(MsalService.class);

    private ConfidentialClientApplication ccaClient; // kept simple; created lazily per call

    public MsalService() {
        // empty; will create per-call when secrets provided
    }

    private ConfidentialClientApplication buildConfidentialClient(AzureSecrets secrets) throws MalformedURLException {
        if (secrets == null) throw new IllegalArgumentException("Azure secrets cannot be null");
        String authority = String.format("https://login.microsoftonline.com/%s/", secrets.tenantId);
        ConfidentialClientApplication cca = ConfidentialClientApplication
                .builder(secrets.clientId, ClientCredentialFactory.createFromSecret(secrets.clientSecret))
                .authority(authority)
                .build();
        return cca;
    }

    /**
     * Acquire token on behalf of the incoming user access token (OBO flow).
     * @param userAccessToken token received from front-end
     * @param scope single scope/resource in the form "https://resource/.default" or a scope like "api://.../.default"
     */
    public IAuthenticationResult acquireTokenOnBehalfOf(AzureSecrets secrets, String userAccessToken, String scope) throws Exception {
        ConfidentialClientApplication cca = buildConfidentialClient(secrets);
        UserAssertion assertion = new UserAssertion(userAccessToken);

        OnBehalfOfParameters parameters = OnBehalfOfParameters
                .builder(Collections.singleton(scope), assertion)
                .build();

        CompletableFuture<IAuthenticationResult> future = cca.acquireToken(parameters);
        IAuthenticationResult result = future.get();
        LOG.info("Acquired token for scopes {} exp: {}", scope, result.expiresOnDate());
        return result;
    }

    /**
     * Acquire token using client credentials (app-only)
     */
    public IAuthenticationResult acquireTokenForClient(AzureSecrets secrets, Set<String> scopes) throws Exception {
        ConfidentialClientApplication cca = buildConfidentialClient(secrets);
        ClientCredentialParameters parameters = ClientCredentialParameters.builder(scopes).build();
        CompletableFuture<IAuthenticationResult> future = cca.acquireToken(parameters);
        IAuthenticationResult result = future.get();
        LOG.info("Acquired client token for scopes {} exp: {}", scopes, result.expiresOnDate());
        return result;
    }
}
