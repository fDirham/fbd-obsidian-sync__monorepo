package com.fbdco.fbd_obsidian_sync_dashboard_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CognitoIdentityProviderException.class)
    public ResponseEntity<String> handleCognitoException(CognitoIdentityProviderException e) {
        if (e.statusCode() != 500) {
            return ResponseEntity.status(e.statusCode()).body(e.getMessage());
        }
        e.printStackTrace();
        return ResponseEntity.status(e.statusCode()).body("Something went wrong.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong");
    }
}
