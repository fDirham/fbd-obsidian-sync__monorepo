package com.fbdco.fbd_obsidian_sync_dashboard_api.controller;

import com.fbdco.fbd_obsidian_sync_dashboard_api.dto.*;
import com.fbdco.fbd_obsidian_sync_dashboard_api.model.UserDDBItem;
import com.fbdco.fbd_obsidian_sync_dashboard_api.repo.DDBRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import software.amazon.awssdk.services.s3.S3Client;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;


@RestController
@RequestMapping("/user")
public class UserController {

    private final CognitoIdentityProviderClient cognitoClient;
    private final DDBRepo ddbRepo;
    private final S3Client s3Client;
    private final String s3Bucket;
    private final String cognitoClientId;
    private final String cognitoClientSecret;
    private final String cognitoUserPoolId;

    public UserController(
            CognitoIdentityProviderClient cognitoClient,
            DDBRepo ddbRepo,
            S3Client s3Client,
            @Value("${s3.bucket}") String s3Bucket,
            @Value("${cognito.client.id}") String cognitoClientId,
            @Value("${cognito.client.secret}") String cognitoClientSecret,
            @Value("${cognito.userpool.id}") String cognitoUserPoolId
            ) {
        this.cognitoClient = cognitoClient;
        this.cognitoClientId = cognitoClientId;
        this.cognitoClientSecret = cognitoClientSecret;
        this.cognitoUserPoolId = cognitoUserPoolId;
        this.ddbRepo = ddbRepo;
        this.s3Client = s3Client;
        this.s3Bucket = s3Bucket;
    }

    @PostMapping
    public ResponseEntity<String> createUser(@RequestBody CreateUserRequest request) {
        SignUpRequest cognitoRequest = SignUpRequest.builder()
                .clientId(this.cognitoClientId)
                .username(request.getEmail())
                .password(request.getPassword())
                .userAttributes(
                        AttributeType.builder()
                                .name("email")
                                .value(request.getEmail())
                                .build()
                )
                .secretHash(calculateSecretHash(request.getEmail()))
                .build();


        SignUpResponse cognitoResponse = cognitoClient.signUp(cognitoRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(cognitoResponse.userSub());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        boolean isVerified = isEmailVerified(loginRequest.getEmail());
        if (!isVerified) {
            return ResponseEntity.ok(new LoginResponse(false));
        }

        AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                .userPoolId(this.cognitoUserPoolId)
                .clientId(this.cognitoClientId)
                .authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                .authParameters(Map.of(
                        "USERNAME", loginRequest.getEmail(),
                        "PASSWORD", loginRequest.getPassword(),
                        "SECRET_HASH", calculateSecretHash(loginRequest.getEmail())
                ))
                .build();

        AdminInitiateAuthResponse authResponse = cognitoClient.adminInitiateAuth(authRequest);

        return ResponseEntity.ok(new LoginResponse(authResponse, true));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                .userPoolId(this.cognitoUserPoolId)
                .clientId(this.cognitoClientId)
                .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                .authParameters(Map.of(
                        "REFRESH_TOKEN", request.getRefreshToken(),
                        "SECRET_HASH", calculateSecretHash(request.getUid())
                ))
                .build();

        AdminInitiateAuthResponse authResponse = cognitoClient.adminInitiateAuth(authRequest);
        boolean isVerified = isEmailVerified(request.getEmail());

        return ResponseEntity.ok(new LoginResponse(authResponse, isVerified));
    }

    @PostMapping("/send-verification")
    public ResponseEntity<?> sendVerificationEmail(@RequestBody SendVerificationEmailRequest request) {
        ResendConfirmationCodeRequest cognitoRequest = ResendConfirmationCodeRequest.builder()
                .clientId(this.cognitoClientId)
                .username(request.getEmail())
                .secretHash(calculateSecretHash(request.getEmail()))
                .build();

        cognitoClient.resendConfirmationCode(cognitoRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm-verify")
    public ResponseEntity<?> confirmVerify(@RequestBody ConfirmVerifyUserRequest request){
        ConfirmSignUpRequest cognitoRequest = ConfirmSignUpRequest.builder()
                .clientId(this.cognitoClientId)
                .username(request.getEmail())
                .confirmationCode(request.getCode())
                .secretHash(calculateSecretHash(request.getEmail()))
                .build();


        cognitoClient.confirmSignUp(cognitoRequest);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/send-reset-password")
    public ResponseEntity<?> sendResetPasswordEmail(@RequestBody SendResetPasswordEmailRequest request) {
        ForgotPasswordRequest cognitoRequest = ForgotPasswordRequest.builder()
                .clientId(this.cognitoClientId)
                .username(request.getEmail())
                .secretHash(calculateSecretHash(request.getEmail()))
                .build();

        cognitoClient.forgotPassword(cognitoRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm-reset-password")
    public ResponseEntity<?> confirmResetPassword(@RequestBody ConfirmResetPasswordRequest request) {
        ConfirmForgotPasswordRequest cognitoRequest = ConfirmForgotPasswordRequest.builder()
                .clientId(this.cognitoClientId)
                .username(request.getEmail())
                .confirmationCode(request.getCode())
                .password(request.getNewPassword())
                .secretHash(calculateSecretHash(request.getEmail()))
                .build();

        cognitoClient.confirmForgotPassword(cognitoRequest);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<?> deleteUser(JwtAuthenticationToken authentication) {
        var claims = authentication.getTokenAttributes();
        String userId = (String) claims.get("sub");

        // Get all backups
        var userItem = ddbRepo.getUserById(userId);
        if(userItem == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        var backupIds = userItem.getVaults().stream()
                .flatMap(vault -> vault.getBackups().stream())
                .map(UserDDBItem.Vault.Backup::getId)
                .toList();

        // Delete backups in s3
        for(String backupId : backupIds){
            s3Client.deleteObject(builder -> builder.bucket(s3Bucket).key(backupId).build());
        }

        // Delete user in ddb
        ddbRepo.deleteUser(userItem);

        // Delete from cognito
        AdminDeleteUserRequest cognitoRequest = AdminDeleteUserRequest.builder()
                .userPoolId(this.cognitoUserPoolId)
                .username(userId)
                .build();

        cognitoClient.adminDeleteUser(cognitoRequest);

        return ResponseEntity.ok().build();
    }

    // MARK: Helpers
    private String calculateSecretHash(String userName){
        try {
            String message = userName + cognitoClientId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey =
                    new SecretKeySpec(cognitoClientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e){
            throw new RuntimeException("Error calculating secret hash", e);
        }
    }

    private boolean isEmailVerified(String email) {
        AdminGetUserRequest getUserRequest = AdminGetUserRequest.builder()
                .userPoolId(this.cognitoUserPoolId)
                .username(email)
                .build();

        AdminGetUserResponse getUserResponse = cognitoClient.adminGetUser(getUserRequest);
        return getUserResponse.userAttributes().stream()
                .anyMatch(attr -> "email_verified".equals(attr.name()) && "true".equals(attr.value()));
    }
}
