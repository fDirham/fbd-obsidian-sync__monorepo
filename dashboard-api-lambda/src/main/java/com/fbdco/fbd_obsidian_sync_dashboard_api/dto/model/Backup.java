package com.fbdco.fbd_obsidian_sync_dashboard_api.dto.model;

public class Backup {
    private long createdAt;
    private String id;

    public long getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
}
