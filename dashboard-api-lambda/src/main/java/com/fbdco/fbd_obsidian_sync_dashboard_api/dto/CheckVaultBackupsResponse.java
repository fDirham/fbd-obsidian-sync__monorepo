package com.fbdco.fbd_obsidian_sync_dashboard_api.dto;

import com.fbdco.fbd_obsidian_sync_dashboard_api.dto.model.Vault;

public class CheckVaultBackupsResponse {
    private Vault vault;

    public CheckVaultBackupsResponse() {
        vault = new Vault();
    }

    public Vault getVault() {
        return vault;
    }
    public void setVault(Vault vault) {
        this.vault = vault;
    }
}
