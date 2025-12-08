package com.fbdco.fbd_obsidian_sync_dashboard_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsConfig {
    private final String cognitoRegion;
    private final String s3Region;
    private final String dynamoDbRegion;

    @SuppressWarnings("unused")
    public AwsConfig(
            @Value("${cognito.region}") String cognitoRegion,
            @Value("${s3.region}") String s3Region,
            @Value("${dynamodb.region}") String dynamoDbRegion
    ){
        this.cognitoRegion = cognitoRegion;
        this.s3Region = s3Region;
        this.dynamoDbRegion = dynamoDbRegion;
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.create();
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(s3Region))
                .build();
    }

    @Bean
    public CognitoIdentityProviderClient cognitoClient() {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(cognitoRegion))
                .build();
    }

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.of(dynamoDbRegion))
                .build();
    }
}
