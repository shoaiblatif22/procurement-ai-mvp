package com.procurementai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.textract.TextractClient;

import java.net.URI;

/**
 * Infrastructure configuration.
 * All beans wired here — override via environment variables for different deployments.
 */
@Configuration
public class AppConfig {

    // ── Claude API WebClient ───────────────────────────────────

    @Bean
    public WebClient claudeWebClient(
            @Value("${app.claude.api-key}") String apiKey,
            @Value("${app.claude.base-url}") String baseUrl) {

        return WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("x-api-key", apiKey)
            .defaultHeader("anthropic-version", "2025-01-01")
            .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    }

    // ── AWS S3 ─────────────────────────────────────────────────

    @Bean
    public S3Client s3Client(
            @Value("${app.aws.access-key}") String accessKey,
            @Value("${app.aws.secret-key}") String secretKey,
            @Value("${app.aws.region}") String region,
            @Value("${app.aws.s3.endpoint:}") String endpoint) {

        var builder = S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            ));

        // Use LocalStack endpoint for local development
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                   .forcePathStyle(true);
        }

        return builder.build();
    }

    // ── AWS Textract ───────────────────────────────────────────

    @Bean
    public TextractClient textractClient(
            @Value("${app.aws.access-key}") String accessKey,
            @Value("${app.aws.secret-key}") String secretKey,
            @Value("${app.aws.region}") String region,
            @Value("${app.aws.textract.endpoint:}") String endpoint) {

        var builder = TextractClient.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            ));

        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }

    // ── S3 bucket name ─────────────────────────────────────────

    @Bean
    public String s3Bucket(@Value("${app.aws.s3.bucket}") String bucket) {
        return bucket;
    }
}

