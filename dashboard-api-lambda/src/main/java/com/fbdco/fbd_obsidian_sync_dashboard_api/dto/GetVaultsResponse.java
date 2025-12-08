package com.fbdco.fbd_obsidian_sync_dashboard_api.dto;

import com.fbdco.fbd_obsidian_sync_dashboard_api.dto.model.Vault;

import java.util.List;

public class GetVaultsResponse {
    private List<Vault> vaults;

    public GetVaultsResponse() {
        vaults = List.of();
    }

    public List<Vault> getVaults() {
        return vaults;
    }
    public void setVaults(List<Vault> vaults) {
        this.vaults = vaults;
    }
}
