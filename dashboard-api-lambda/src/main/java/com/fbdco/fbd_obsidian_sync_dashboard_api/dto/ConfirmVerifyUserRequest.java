package com.fbdco.fbd_obsidian_sync_dashboard_api.dto;

public class ConfirmVerifyUserRequest {
    private String code;
    private String email;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
