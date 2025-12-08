package com.fbdco.fbd_obsidian_sync_dashboard_api.model;
import org.jetbrains.annotations.NotNull;


import java.util.Date;

public record BackupId(String userId, String vaultId, Date createdAt) {

    @Override
    @NotNull
    public String toString() {
        return userId + "__" + vaultId + "__" + createdAt.getTime();
    }

    public static BackupId fromString(String backupName) throws IllegalArgumentException {
        String[] parts = backupName.split("__");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid backup name format");
        }
        if (parts[0].isEmpty() || parts[1].isEmpty() || parts[2].isEmpty()) {
            throw new IllegalArgumentException("Invalid backup name format");
        }
        return new BackupId(parts[0], parts[1], new Date(Long.parseLong(parts[2])));
    }
}
