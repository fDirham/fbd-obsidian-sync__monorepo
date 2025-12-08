package com.fbdco.fbd_obsidian_sync_dashboard_api.dto;

public class StartBackupResponse {
    private String presignedUrl;

    public String getPresignedUrl() {
        return presignedUrl;
    }

    public void setPresignedUrl(String presignedUrl) {
        this.presignedUrl = presignedUrl;
    }
}
