package com.fbdco.fbd_obsidian_sync_dashboard_api.dto;

public class UpdateVaultRequest {
    private String vaultId;
    private String newName;

    public String getVaultId() {
        return vaultId;
    }

    public void setVaultId(String vaultId) {
        this.vaultId = vaultId;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }
}
