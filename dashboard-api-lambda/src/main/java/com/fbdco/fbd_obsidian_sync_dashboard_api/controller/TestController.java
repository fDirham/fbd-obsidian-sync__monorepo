package com.fbdco.fbd_obsidian_sync_dashboard_api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {
    private final S3Client s3Client;
    private final CognitoIdentityProviderClient cognitoClient;
    private final DynamoDbClient dynamoDbClient;
    private final S3Presigner s3Presigner;
    private final String userPoolId;
    private final String s3Bucket;
    private final String dynamoDbTable;

    @SuppressWarnings("unused")
    public TestController(
            S3Client s3Client,
            CognitoIdentityProviderClient cognitoClient,
            DynamoDbClient dynamoDbClient,
            S3Presigner s3Presigner,
            @Value("${cognito.userpool.id}") String userPoolId,
            @Value("${s3.bucket}") String s3Bucket,
            @Value("${dynamodb.table}") String dynamoDbTable
    ) {
        this.s3Client = s3Client;
        this.cognitoClient = cognitoClient;
        this.dynamoDbClient = dynamoDbClient;
        this.s3Presigner = s3Presigner;
        this.userPoolId = userPoolId;
        this.s3Bucket = s3Bucket;
        this.dynamoDbTable = dynamoDbTable;
    }

    @GetMapping
    public String test() {
        return "Hello World!";
    }

    @GetMapping("/s3")
    public String testS3() {
        String key = "test.txt";
        String content = "Hello from S3!";

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(key)
                .build(),
            RequestBody.fromString(content)
        );

        return "File created in S3: " + key;
    }

    @GetMapping("/s3/upload")
    public String testS3Upload(@org.springframework.web.bind.annotation.RequestBody String key) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(key)
                .contentType("application/octet-stream") // optional
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))
                        .putObjectRequest(objectRequest)
                        .build()
        );

        return  presignedRequest.url().toString();
    }

    @GetMapping("/cognito")
    public String testCognito(JwtAuthenticationToken authentication) {
        var claims = authentication.getTokenAttributes();
        String username = (String) claims.get("username");
        String userId = (String) claims.get("sub");

        AdminGetUserResponse user = cognitoClient.adminGetUser(
            AdminGetUserRequest.builder()
                .userPoolId(userPoolId)
                .username(username)
                .build()
        );

        String email = user.userAttributes().stream()
            .filter(attr -> "email".equals(attr.name()))
            .findFirst()
            .map(AttributeType::value)
            .orElse(null);

        return "username: " + username + "\nemail: " + email + "\nuid: " + userId;
    }

    @GetMapping("/dynamodb")
    public String testDynamoDb() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("type", AttributeValue.builder().s("user").build());
        item.put("id", AttributeValue.builder().s("5000").build());
        item.put("message", AttributeValue.builder().s("Hello from DynamoDB!").build());

        dynamoDbClient.putItem(
            PutItemRequest.builder()
                .tableName(dynamoDbTable)
                .item(item)
                .build()
        );

        return "Item added to DynamoDB";
    }
}
