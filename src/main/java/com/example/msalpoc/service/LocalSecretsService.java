package com.example.msalpoc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class LocalSecretsService implements SecretsService {

    @Value("${azure.client-id:}")
    private String clientId;

    @Value("${azure.client-secret:}")
    private String clientSecret;

    @Value("${azure.tenant-id:}")
    private String tenantId;

    @Value("${azure.required-group-id:}")
    private String requiredGroupId;

    @Override
    public AzureSecrets getAzureSecrets() {
        return new AzureSecrets(clientId, clientSecret, tenantId, requiredGroupId);
    }
}
