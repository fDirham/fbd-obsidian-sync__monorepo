package com.fbdco.fbd_obsidian_sync_dashboard_api.dto.model;

import java.util.List;

public class Vault {
    private String id;
    private String name;
    private List<Backup> backups;

    public Vault() {
        id = "";
        name = "";
        backups = List.of();
    }

    public Vault(String id, String name) {
        this.id = id;
        this.name = name;
        this.backups = List.of();
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public List<Backup> getBackups() {
        return backups;
    }
    public void setBackups(List<Backup> backups) {
        this.backups = backups;
    }




}
