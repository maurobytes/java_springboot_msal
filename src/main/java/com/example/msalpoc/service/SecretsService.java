package com.example.msalpoc.service;

public interface SecretsService {
    AzureSecrets getAzureSecrets();

    class AzureSecrets {
        public final String clientId;
        public final String clientSecret;
        public final String tenantId;
        public final String requiredGroupId;

        public AzureSecrets(String clientId, String clientSecret, String tenantId, String requiredGroupId) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.tenantId = tenantId;
            this.requiredGroupId = requiredGroupId;
        }
    }
}
