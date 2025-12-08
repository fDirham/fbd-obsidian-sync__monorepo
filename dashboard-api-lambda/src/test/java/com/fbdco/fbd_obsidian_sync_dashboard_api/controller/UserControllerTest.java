package com.fbdco.fbd_obsidian_sync_dashboard_api.controller;

import com.fbdco.fbd_obsidian_sync_dashboard_api.dto.CreateUserRequest;
import com.fbdco.fbd_obsidian_sync_dashboard_api.repo.DDBRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
* TODO:
*  - More comprehensive tests for the UserController class
*  - Test edge cases and error scenarios
*  - Test malformed input scenarios
* */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private UserController userController;

    @Mock
    private CognitoIdentityProviderClient cognitoClient;

    @Mock
    private DDBRepo ddbRepo;

    @Mock
    private S3Client s3Client;

    @BeforeEach
    void setUp() {
        userController = new UserController(cognitoClient,
                ddbRepo,
                s3Client,
                "s3-bucket",
                "clientId",
                "clientSecret",
                "userPoolId"
        );
    }

    // MARK: createUser
    @Test
    void createUser_shouldCallCognitoSignUpAndReturnUserSub() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        String testCreatedSub = "test-user-sub-123";
        SignUpResponse mockResponse = SignUpResponse.builder()
                .userSub(testCreatedSub)
                .build();
        
        when(cognitoClient.signUp(any(SignUpRequest.class))).thenReturn(mockResponse);
        
        ResponseEntity<String> response = userController.createUser(request);
        
        verify(cognitoClient).signUp(any(SignUpRequest.class));
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(testCreatedSub, response.getBody());
    }
}