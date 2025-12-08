package com.fbdco.fbd_obsidian_sync_dashboard_api.controller;

import com.fbdco.fbd_obsidian_sync_dashboard_api.dto.*;
import com.fbdco.fbd_obsidian_sync_dashboard_api.dto.model.Backup;
import com.fbdco.fbd_obsidian_sync_dashboard_api.dto.model.Vault;
import com.fbdco.fbd_obsidian_sync_dashboard_api.model.BackupId;
import com.fbdco.fbd_obsidian_sync_dashboard_api.model.UserDDBItem;
import com.fbdco.fbd_obsidian_sync_dashboard_api.repo.DDBRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/vault")
public class VaultController {
    private final DDBRepo ddbRepo;
    private final int maxVaultNameLength;
    private final int maxVaultsPerUser;
    private final S3Presigner s3Presigner;
    private final String s3Bucket;
    private final S3Client s3Client;

    @SuppressWarnings("unused")
    public VaultController(
            DDBRepo ddbRepo,
            S3Presigner s3Presigner,
            S3Client s3Client,
            @Value("${s3.bucket}") String s3Bucket,
            @Value("${config.max.vault.name.length}") int maxVaultNameLength,
            @Value("${config.max.vaults.per.user}") int maxVaultsPerUser
    ) {
        this.ddbRepo = ddbRepo;
        this.maxVaultNameLength = maxVaultNameLength;
        this.maxVaultsPerUser = maxVaultsPerUser;
        this.s3Presigner = s3Presigner;
        this.s3Bucket = s3Bucket;
        this.s3Client = s3Client;
    }

    @GetMapping
    public ResponseEntity<?> getVaults(JwtAuthenticationToken authentication) {
        UserDDBItem userItem = getUserDDBItemFromJwt(authentication);

        if (userItem == null) {
            var claims = authentication.getTokenAttributes();
            String userId = (String) claims.get("sub");
            userItem = new UserDDBItem(userId);
            ddbRepo.putUser(userItem);
            var toReturn = new GetVaultsResponse();
            return ResponseEntity.status(HttpStatus.OK).body(toReturn);
        }

        var vaults = userItem.getVaults();
        if(vaults == null || vaults.isEmpty()){
            var toReturn = new GetVaultsResponse();
            return ResponseEntity.status(HttpStatus.OK).body(toReturn);
        }

        var resVaults = new ArrayList<Vault>();
        for (UserDDBItem.Vault vault : vaults) {
            resVaults.add(convertDDBVaultToResVault(vault));
        }

        var toReturn = new GetVaultsResponse();
        toReturn.setVaults(resVaults);

        return ResponseEntity.status(HttpStatus.OK).body(toReturn);
    }

    @DeleteMapping("/{vaultId}")
    public ResponseEntity<String> deleteVault(JwtAuthenticationToken authentication, @PathVariable String vaultId) {
        // Sanitize inputs
        if(vaultId == null || vaultId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Vault name cannot be null");
        }

        UserDDBItem  userItem = getUserDDBItemFromJwt(authentication);

        if (userItem == null) {
            return ResponseEntity.status(HttpStatus.OK).body("Vault not found");
        }

        var vaults = userItem.getVaults();
        if(vaults == null || vaults.isEmpty()){
            return ResponseEntity.status(HttpStatus.OK).body("Vault not found");
        }

        for (UserDDBItem.Vault vault : vaults) {
            if (vault.getId().equals(vaultId)) {
                vaults.remove(vault);
                userItem.setVaults(vaults);
                ddbRepo.putUser(userItem);
                return ResponseEntity.status(HttpStatus.OK).body("Vault deleted");
            }
        }

        return ResponseEntity.status(HttpStatus.OK).body("Vault not found");
    }

    @PostMapping
    public ResponseEntity<?> createVault(JwtAuthenticationToken authentication, @RequestBody CreateVaultRequest request) {
        // Sanitize inputs
        String vaultName;
        try {
            vaultName = checkAndSanitizeVaultName(request.getName());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        UserDDBItem userItem = getUserDDBItemFromJwt(authentication);

        if (userItem == null) {
            var claims = authentication.getTokenAttributes();
            String userId = (String) claims.get("sub");
            userItem = new UserDDBItem(userId);
        }

        List<UserDDBItem.Vault> vaults = userItem.getVaults();
        String newVaultId = UUID.randomUUID().toString();
        UserDDBItem.Vault newVault = new UserDDBItem.Vault(vaultName, newVaultId);
        if (vaults == null) {
            vaults = List.of(newVault);
        }
        else {
            if(vaults.size() >= this.maxVaultsPerUser) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User has reached the maximum number of vaults (5)");
            }
            for (UserDDBItem.Vault vault : vaults) {
                if (vault.getName().equals(vaultName)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("Vault with name " + vaultName + " already exists");
                }
            }
            vaults.add(newVault);
        }
        userItem.setVaults(vaults);

        ddbRepo.putUser(userItem);

        var toReturn = new CreateVaultResponse();
        toReturn.setVaultId(newVaultId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toReturn);
    }

    @PutMapping
    public ResponseEntity<String> updateVault(JwtAuthenticationToken authentication, @RequestBody UpdateVaultRequest request) {
        // Sanitize inputs
        String vaultName;
        try {
            vaultName = checkAndSanitizeVaultName(request.getNewName());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        UserDDBItem userItem = getUserDDBItemFromJwt(authentication);

        if (userItem == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Vault not found");
        }

        List<UserDDBItem.Vault> vaults = userItem.getVaults();
        if (vaults == null || vaults.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Vault not found");
        }

        // Check for name conflicts
        for (UserDDBItem.Vault vault : vaults) {
            if (vault.getName().equals(vaultName)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Vault with name " + vaultName + " already exists");
            }
        }

        // Then we update if we find
        for (UserDDBItem.Vault vault : vaults) {
            if (vault.getId().equals(request.getVaultId())) {
                vault.setName(vaultName);
                userItem.setVaults(vaults);
                ddbRepo.putUser(userItem);
                return ResponseEntity.status(HttpStatus.OK).body("Vault updated with name: " + vaultName);
            }
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Vault not found");
    }

    @PostMapping("/backup")
    public ResponseEntity<?> startBackup(JwtAuthenticationToken authentication, @RequestBody StartBackupRequest request){
        UserDDBItem userItem = getUserDDBItemFromJwt(authentication);

        if (userItem == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Vault not found");
        }

        List<UserDDBItem.Vault> vaults = userItem.getVaults();
        if (vaults == null || vaults.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Vault not found");
        }

        UserDDBItem.Vault selectedVault = null;
        for (UserDDBItem.Vault vault : vaults) {
            if (vault.getId().equals(request.getVaultId())) {
                selectedVault = vault;
                break;
            }
        }
        if (selectedVault == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Vault not found");
        }

        String newBackupId = new BackupId(userItem.getId(), selectedVault.getId(), new java.util.Date()).toString();
        UserDDBItem.Vault.Backup newBackup = new UserDDBItem.Vault.Backup(newBackupId);
        List<UserDDBItem.Vault.Backup> backups = selectedVault.getBackups();
        if (backups == null) {
            backups = List.of(newBackup);
        }
        else {
            backups.add(newBackup);
        }
        selectedVault.setBackups(backups);
        userItem.setVaults(vaults);
        ddbRepo.putUser(userItem);

        // Get presigned url
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(newBackupId)
                .contentType("application/octet-stream") // optional
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))
                        .putObjectRequest(objectRequest)
                        .build()
        );

        String presignedUrl = presignedRequest.url().toString();

        StartBackupResponse res = new StartBackupResponse();
        res.setPresignedUrl(presignedUrl);

        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @PostMapping("/check")
    public ResponseEntity<?> checkVaultBackups(JwtAuthenticationToken authentication, @RequestBody CheckVaultBackupsRequest request) {
        UserDDBItem userItem = getUserDDBItemFromJwt(authentication);

        if (userItem == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Vault not found");
        }

        List<UserDDBItem.Vault> vaults = userItem.getVaults();
        if (vaults == null || vaults.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Vault not found");
        }

        UserDDBItem.Vault selectedVault = null;
        for (UserDDBItem.Vault vault : vaults) {
            if (vault.getId().equals(request.getVaultId())) {
                selectedVault = vault;
                break;
            }
        }
        if (selectedVault == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Vault not found");
        }

        List<UserDDBItem.Vault.Backup> backups = selectedVault.getBackups();
        boolean changeMade = false;

        for(int i = backups.size() - 1; i >= 0; i--){
            UserDDBItem.Vault.Backup backup = backups.get(i);
            if(backup.getIsVerified()) continue;

            boolean backupExists = false;
            try {
                s3Client.headObject(
                        HeadObjectRequest.builder()
                                .bucket(s3Bucket)
                                .key(backup.getId())
                                .build()
                );
                backupExists = true;
            } catch (NoSuchKeyException ignored) {
            }

            // If it is, change
            if(backupExists) {
                backup.setIsVerified(true);
                changeMade = true;
                continue;
            }

            // If not, we add count check and delete if count check is greater than 3
            int countCheck = backup.getCountCheck() + 1;
            if(countCheck > 3) {
                backups.remove(backup);
                changeMade = true;
                continue;
            }
            backup.setCountCheck(countCheck);
        }

        if(changeMade) {
            selectedVault.setBackups(backups);
            userItem.setVaults(vaults);
            ddbRepo.putUser(userItem);
        }

        CheckVaultBackupsResponse res = new CheckVaultBackupsResponse();
        res.setVault(convertDDBVaultToResVault(selectedVault));

        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @GetMapping("/backup")
    public ResponseEntity<?> downloadBackup(JwtAuthenticationToken authentication, @RequestParam("b") String backupIdStr) {
        UserDDBItem userItem = getUserDDBItemFromJwt(authentication);

        if (userItem == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<UserDDBItem.Vault> vaults = userItem.getVaults();
        if (vaults == null || vaults.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        BackupId backupId = BackupId.fromString(backupIdStr);
        if(!backupId.userId().equals(userItem.getId())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDDBItem.Vault selectedVault = null;
        for (UserDDBItem.Vault vault : vaults) {
            if (vault.getId().equals(backupId.vaultId())) {
                selectedVault = vault;
                break;
            }
        }

        if (selectedVault == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<UserDDBItem.Vault.Backup> backups = selectedVault.getBackups();
        boolean isFound = false;
        for(var backup: backups) {
            if(backup.getId().equals(backupIdStr)) {
                if(!backup.getIsVerified()) break;

                isFound = true;
                break;
            }
        }
        if(!isFound) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }


        DownloadBackupResponse res = new DownloadBackupResponse();

        // Get presigned url
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(s3Bucket)
                .key(backupIdStr)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(objectRequest)
                .build();
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        res.setDownloadUrl(presignedRequest.url().toString());

        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @DeleteMapping("/backup")
    public ResponseEntity<?> deleteBackup(JwtAuthenticationToken authentication, @RequestParam("b") String backupIdStr){
        UserDDBItem userItem = getUserDDBItemFromJwt(authentication);

        if (userItem == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<UserDDBItem.Vault> vaults = userItem.getVaults();
        if (vaults == null || vaults.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        BackupId backupId = BackupId.fromString(backupIdStr);
        if(!backupId.userId().equals(userItem.getId())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDDBItem.Vault selectedVault = null;
        for (UserDDBItem.Vault vault : vaults) {
            if (vault.getId().equals(backupId.vaultId())) {
                selectedVault = vault;
                break;
            }
        }

        if (selectedVault == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<UserDDBItem.Vault.Backup> backups = selectedVault.getBackups();
        boolean isFound = false;
        boolean isVerified = false;
        for(var backup: backups) {
            if(backup.getId().equals(backupIdStr)) {
                isVerified = backup.getIsVerified();
                isFound = true;
                break;
            }
        }
        if(!isFound) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if(isVerified) {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(s3Bucket)
                            .key(backupIdStr)
                            .build()
            );
        }

        backups.removeIf(backup -> backup.getId().equals(backupIdStr));
        selectedVault.setBackups(backups);
        userItem.setVaults(vaults);
        ddbRepo.putUser(userItem);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // MARK: Helpers
    private String checkAndSanitizeVaultName(String vaultName) throws Exception {
        if (vaultName == null) {
            throw new Exception("Vault name cannot be null");
        }
        vaultName = vaultName.trim();
        if (vaultName.isEmpty()) {
            throw new Exception("Vault name cannot be empty");
        }
        if (vaultName.length() > this.maxVaultNameLength) {
            throw new Exception("Vault name cannot be longer than " + this.maxVaultNameLength + " characters.");
        }

        return vaultName;
    }

    private UserDDBItem getUserDDBItemFromJwt(JwtAuthenticationToken token) {
        var claims = token.getTokenAttributes();
        String userId = (String) claims.get("sub");

        return ddbRepo.getUserById(userId);
    }

    private Vault convertDDBVaultToResVault(UserDDBItem.Vault vault){
        var resVault = new Vault(vault.getId(), vault.getName());

        var resBackups = new ArrayList<Backup>();
        var backups = vault.getBackups();
        if(backups != null && !backups.isEmpty()){
            for (var backup : backups) {
                if(!backup.getIsVerified()) continue;

                var resBackup = new Backup();
                resBackup.setId(backup.getId());
                long createdAt = 0;
                try {
                    BackupId backupId = BackupId.fromString(backup.getId());
                    createdAt = backupId.createdAt().getTime();
                } catch (Exception ignored) {
                }

                resBackup.setCreatedAt(createdAt);
                resBackups.add(resBackup);
            }
        }
        resVault.setBackups(resBackups);
        return resVault;
    }
}
