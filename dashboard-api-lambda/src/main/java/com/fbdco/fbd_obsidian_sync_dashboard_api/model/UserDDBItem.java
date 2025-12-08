package com.fbdco.fbd_obsidian_sync_dashboard_api.model;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.ArrayList;
import java.util.List;

@DynamoDbBean
public class UserDDBItem {
    private String id;
    private List<Vault> vaults;

    public UserDDBItem(){
        this.id = "";
        this.vaults = new ArrayList<>();
    }

    public UserDDBItem(String id) {
        this.id = id;
        this.vaults = new ArrayList<>();
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("user_id")
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<Vault> getVaults() { return vaults; }
    public void setVaults(List<Vault> newVaults) { this.vaults = newVaults; }

    @DynamoDbBean
    public static class Vault {
        private String name;
        private String id;
        private List<Backup> backups;

        public Vault() {}

        public Vault(String name, String id) {
            this.name = name;
            this.id = id;
            this.backups = new ArrayList<>();
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public List<Backup> getBackups() { return backups; }
        public void setBackups(List<Backup> backups) { this.backups = backups; }

        @DynamoDbBean
        public static class Backup {
            private String id;
            private boolean isVerified;
            private int countCheck;

            public Backup() {}

            public Backup(String id) {
                this.id = id;
                this.isVerified = false;
                this.countCheck = 0;
            }

            public Backup(String id, boolean isVerified) {
                this.id = id;
                this.isVerified = isVerified;
                this.countCheck = 0;
            }

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public boolean getIsVerified() { return isVerified; }
            public void setIsVerified(boolean isVerified) { this.isVerified = isVerified; }
            public int getCountCheck() { return countCheck; }
            public void setCountCheck(int countCheck) { this.countCheck = countCheck; }


        }
    }


}
