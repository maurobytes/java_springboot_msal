package com.example.msalpoc.config;

import com.example.msalpoc.service.MsalService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MsalConfig {

    @Bean
    public MsalService msalService() {
        return new MsalService();
    }
}
