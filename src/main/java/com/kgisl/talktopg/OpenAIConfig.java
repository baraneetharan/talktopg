package com.kgisl.talktopg;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAI client.
 */
@Configuration
public class OpenAIConfig {

    @Value("${openai.api-key}")
    private String apiKey;

    @Bean
public OpenAIClient openAIClient() {
    return OpenAIOkHttpClient.builder()
            .baseUrl("https://models.inference.ai.azure.com")
            .apiKey(apiKey)
            .build();
}
}
