package com.fbdco.fbd_obsidian_sync_dashboard_api.dto;

import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;

public class LoginResponse {
    private String token;
    private String refreshToken;
    private String idToken;
    private int expiresIn;
    private boolean isVerified = false;

    public LoginResponse(AdminInitiateAuthResponse cognitoAuthRes, boolean isVerified) {
        this.token = cognitoAuthRes.authenticationResult().accessToken();
        this.refreshToken = cognitoAuthRes.authenticationResult().refreshToken();
        this.expiresIn = cognitoAuthRes.authenticationResult().expiresIn();
        this.idToken = cognitoAuthRes.authenticationResult().idToken();
        this.isVerified = isVerified;
    }

    public LoginResponse(boolean isVerified) {
        this.token = "";
        this.refreshToken = "";
        this.expiresIn = 0;
        this.idToken = "";
        this.isVerified = isVerified;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }
}
