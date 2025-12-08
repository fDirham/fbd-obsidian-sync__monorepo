package com.fbdco.fbd_obsidian_sync_dashboard_api.controller;

import com.fbdco.fbd_obsidian_sync_dashboard_api.dto.*;
import com.fbdco.fbd_obsidian_sync_dashboard_api.model.BackupId;
import com.fbdco.fbd_obsidian_sync_dashboard_api.model.UserDDBItem;
import com.fbdco.fbd_obsidian_sync_dashboard_api.repo.DDBRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import static org.mockito.ArgumentMatchers.argThat;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VaultControllerTest {

    @Mock
    private DDBRepo ddbRepo;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private S3Client s3Client;

    @Mock
    private JwtAuthenticationToken authentication;

    private VaultController vaultController;

    private Map<String, Object> tokenAttributes;
    private UserDDBItem testUser;

    @BeforeEach
    void setUp() {
        vaultController = new VaultController(ddbRepo, s3Presigner, s3Client, "test-bucket", 50, 5);

        tokenAttributes = new HashMap<>();
        tokenAttributes.put("sub", "user123");
        
        testUser = new UserDDBItem();
        testUser.setId("user123");
        testUser.setVaults(new ArrayList<>());
    }

    // MARK: getVaults
    @Test
    void getVaults_Success() {
        String userId = "user123";
        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        var vaults = new ArrayList<UserDDBItem.Vault>();
        vaults.add(new UserDDBItem.Vault("TestVault", UUID.randomUUID().toString()));
        vaults.add(new UserDDBItem.Vault("TestVault2", UUID.randomUUID().toString()));
        var vaultWithBackups = new UserDDBItem.Vault("TestVault3", UUID.randomUUID().toString());
        vaultWithBackups.setBackups(List.of(
                new UserDDBItem.Vault.Backup(new BackupId(userId, vaultWithBackups.getId(), new Date()).toString(), true),
                new UserDDBItem.Vault.Backup(new BackupId(userId, vaultWithBackups.getId(), new Date()).toString(), true),
                new UserDDBItem.Vault.Backup(new BackupId(userId, vaultWithBackups.getId(), new Date()).toString())
        ));

        vaults.add(vaultWithBackups);
        testUser.setVaults(vaults);
        when(ddbRepo.getUserById(userId)).thenReturn(testUser);

        ResponseEntity<?> response = vaultController.getVaults(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(GetVaultsResponse.class, response.getBody());
        assertEquals(3, ((GetVaultsResponse) response.getBody()).getVaults().size());
        assertEquals("TestVault", ((GetVaultsResponse) response.getBody()).getVaults().get(0).getName());
        assertEquals("TestVault2", ((GetVaultsResponse) response.getBody()).getVaults().get(1).getName());
        assertEquals("TestVault3", ((GetVaultsResponse) response.getBody()).getVaults().get(2).getName());
        assertEquals(0, ((GetVaultsResponse) response.getBody()).getVaults().get(0).getBackups().size());
        assertEquals(0, ((GetVaultsResponse) response.getBody()).getVaults().get(1).getBackups().size());
        assertEquals(2, ((GetVaultsResponse) response.getBody()).getVaults().get(2).getBackups().size());
    }

    @Test
    void getVaults_EmptyVault() {
        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);

        ResponseEntity<?> response = vaultController.getVaults(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(GetVaultsResponse.class, response.getBody());
        assertEquals(0, ((GetVaultsResponse) response.getBody()).getVaults().size());
    }

    @Test
    void getVaults_NoUser() {
        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(null);

        ResponseEntity<?> response = vaultController.getVaults(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(GetVaultsResponse.class, response.getBody());
        assertEquals(0, ((GetVaultsResponse) response.getBody()).getVaults().size());
    }

    // MARK: createVault
    @Test
    void createVault_Success() {
        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);

        ArgumentCaptor<UserDDBItem> userCaptor = ArgumentCaptor.forClass(UserDDBItem.class);

        CreateVaultRequest request = new CreateVaultRequest();
        request.setName("TestVault");
        ResponseEntity<?> response = vaultController.createVault(authentication, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(ddbRepo).putUser(userCaptor.capture());
        assertEquals(1, userCaptor.getValue().getVaults().size());
    }

    @Test
    void createVault_UserWithExistingVault() {
        UserDDBItem.Vault existingVault = new UserDDBItem.Vault("ExistingVault", UUID.randomUUID().toString());
        testUser.setVaults(new ArrayList<>(List.of(existingVault)));

        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);

        ArgumentCaptor<UserDDBItem> userCaptor = ArgumentCaptor.forClass(UserDDBItem.class);

        CreateVaultRequest request = new CreateVaultRequest();
        request.setName("NewVault");
        ResponseEntity<?> response = vaultController.createVault(authentication, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(ddbRepo).putUser(userCaptor.capture());
        assertEquals(2, userCaptor.getValue().getVaults().size());
        assertEquals("ExistingVault", userCaptor.getValue().getVaults().getFirst().getName());
        assertEquals("NewVault", userCaptor.getValue().getVaults().get(1).getName());
    }


    @Test
    void createVault_UserNotFound() {
        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(null);

        ArgumentCaptor<UserDDBItem> userCaptor = ArgumentCaptor.forClass(UserDDBItem.class);

        CreateVaultRequest request = new CreateVaultRequest();
        request.setName("TestVault");
        ResponseEntity<?> response = vaultController.createVault(authentication, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(ddbRepo).putUser(userCaptor.capture());

        UserDDBItem capturedUser = userCaptor.getValue();
        assertEquals(1, capturedUser.getVaults().size());
        assertEquals("TestVault", capturedUser.getVaults().getFirst().getName());
    }

    @Test
    void createVault_NullVaultName() {
        CreateVaultRequest request = new CreateVaultRequest();
        request.setName(null);
        ResponseEntity<?> response = vaultController.createVault(authentication, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(ddbRepo, never()).putUser(any());
    }

    @Test
    void createVault_EmptyVaultName() {
        CreateVaultRequest request = new CreateVaultRequest();
        request.setName("");
        ResponseEntity<?> response = vaultController.createVault(authentication, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(ddbRepo, never()).putUser(any());
    }

    @Test
    void createVault_BlankVaultName() {
        CreateVaultRequest request = new CreateVaultRequest();
        request.setName("   ");
        ResponseEntity<?> response = vaultController.createVault(authentication, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(ddbRepo, never()).putUser(any());
    }

    @Test
    void createVault_DynamoDbException() {
        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenThrow(DynamoDbException.builder().message("DB error").build());

        CreateVaultRequest request = new CreateVaultRequest();
        request.setName("TestVault");

        assertThrows(DynamoDbException.class, () ->
                vaultController.createVault(authentication, request)
        );
    }

    @Test
    void createVault_NullAuthentication() {
        CreateVaultRequest request = new CreateVaultRequest();
        request.setName("TestVault");
        
        assertThrows(NullPointerException.class, () -> 
            vaultController.createVault(null, request)
        );
    }

    @Test
    void createVault_VaultNameTooLong() {
        CreateVaultRequest request = new CreateVaultRequest();
        request.setName("a".repeat(51));
        ResponseEntity<?> response = vaultController.createVault(authentication, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(ddbRepo, never()).putUser(any());
    }

    @Test
    void createVault_MaxVaultsReached() {
        List<UserDDBItem.Vault> vaults = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            vaults.add(new UserDDBItem.Vault("Vault" + i, UUID.randomUUID().toString()));
        }
        testUser.setVaults(vaults);

        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);

        CreateVaultRequest request = new CreateVaultRequest();
        request.setName("NewVault");
        ResponseEntity<?> response = vaultController.createVault(authentication, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(ddbRepo, never()).putUser(any());
    }

    // MARK: deleteVault
    @Test
    void deleteVault_Success() {
        UserDDBItem.Vault vaultToDelete = new UserDDBItem.Vault("VaultToDelete", UUID.randomUUID().toString());
        testUser.setVaults(new ArrayList<>(List.of(vaultToDelete)));

        ArgumentCaptor<UserDDBItem> userCaptor = ArgumentCaptor.forClass(UserDDBItem.class);

        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);

        ResponseEntity<String> response = vaultController.deleteVault(authentication, vaultToDelete.getId());

        verify(ddbRepo).putUser(userCaptor.capture());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        UserDDBItem capturedUser = userCaptor.getValue();
        assertEquals(0, capturedUser.getVaults().size());
    }

    // MARK: updateVault
    @Test
    void updateVault_Success() {
        UserDDBItem.Vault existingVault = new UserDDBItem.Vault("ExistingVault", UUID.randomUUID().toString());
        testUser.setVaults(new ArrayList<>(List.of(existingVault)));

        ArgumentCaptor<UserDDBItem> userCaptor = ArgumentCaptor.forClass(UserDDBItem.class);

        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);

        UpdateVaultRequest request = new UpdateVaultRequest();
        request.setVaultId(existingVault.getId());
        request.setNewName("UpdatedVault");
        ResponseEntity<String> response = vaultController.updateVault(authentication, request);

        verify(ddbRepo).putUser(userCaptor.capture());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        UserDDBItem capturedUser = userCaptor.getValue();
        assertEquals(1, capturedUser.getVaults().size());
        assertEquals("UpdatedVault", capturedUser.getVaults().getFirst().getName());
    }

    // MARK: startBackup
    @Test
    void startBackup_Success() {
        UserDDBItem.Vault existingVault = new UserDDBItem.Vault("ExistingVault", UUID.randomUUID().toString());
        testUser.setVaults(new ArrayList<>(List.of(existingVault)));

        ArgumentCaptor<UserDDBItem> userCaptor = ArgumentCaptor.forClass(UserDDBItem.class);

        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);

        PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
        try {
            when(mockPresignedRequest.url()).thenReturn(java.net.URI.create("https://example.com/presigned-url").toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(mockPresignedRequest);


        StartBackupRequest request = new StartBackupRequest();
        request.setVaultId(existingVault.getId());
        ResponseEntity<?> response = vaultController.startBackup(authentication, request);

        verify(ddbRepo).putUser(userCaptor.capture());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        UserDDBItem capturedUser = userCaptor.getValue();
        assertEquals(1, capturedUser.getVaults().size());
        assertEquals("ExistingVault", capturedUser.getVaults().getFirst().getName());
        assertEquals(1, capturedUser.getVaults().getFirst().getBackups().size());
        assertNotNull(capturedUser.getVaults().getFirst().getBackups().getFirst().getId());
        assertFalse(capturedUser.getVaults().getFirst().getBackups().getFirst().getIsVerified());
        assertTrue(capturedUser.getVaults().getFirst().getBackups().getFirst().getId().startsWith("user123__" + existingVault.getId() + "__"));
    }

    // MARK: checkVaultBackups
    @Test
    void checkVaultBackups_Success() {
        UserDDBItem.Vault existingVault = new UserDDBItem.Vault("ExistingVault", UUID.randomUUID().toString());
        UserDDBItem.Vault.Backup backup1 = new UserDDBItem.Vault.Backup("backup1");
        UserDDBItem.Vault.Backup backup2 = new UserDDBItem.Vault.Backup("backup2");
        existingVault.setBackups(new ArrayList<>(List.of(backup1, backup2)));

        testUser.setVaults(new ArrayList<>(List.of(existingVault)));

        ArgumentCaptor<UserDDBItem> userCaptor = ArgumentCaptor.forClass(UserDDBItem.class);

        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);
        when(s3Client.headObject(argThat((HeadObjectRequest req) -> req != null && "backup1".equals(req.key()))))
                .thenReturn(HeadObjectResponse.builder().build());
        when(s3Client.headObject(argThat((HeadObjectRequest req) -> req != null && "backup2".equals(req.key()))))
                .thenThrow(NoSuchKeyException.builder().build());



        CheckVaultBackupsRequest req  = new CheckVaultBackupsRequest();
        req.setVaultId(existingVault.getId());
        ResponseEntity<?> response = vaultController.checkVaultBackups(authentication, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(ddbRepo).putUser(userCaptor.capture());

        UserDDBItem capturedUser = userCaptor.getValue();
        assertEquals(1, capturedUser.getVaults().size());
        var vaultBackups = capturedUser.getVaults().getFirst().getBackups();
        assertEquals(2, vaultBackups.size());

        var firstBackup = vaultBackups.getFirst();
        assertEquals(0, firstBackup.getCountCheck());
        assertTrue(firstBackup.getIsVerified());

        var secondBackup = vaultBackups.get(1);
        assertEquals(1, secondBackup.getCountCheck());
        assertFalse(secondBackup.getIsVerified());

        var resBody = (CheckVaultBackupsResponse) response.getBody();
        assertNotNull(resBody);
        var resVault = resBody.getVault();
        assertNotNull(resVault);
        assertEquals("ExistingVault", resVault.getName());
        assertEquals(existingVault.getId(), resVault.getId());
        assertEquals(1, resVault.getBackups().size());
        var resBackup1 = resVault.getBackups().getFirst();
        assertEquals("backup1", resBackup1.getId());
    }

    @Test
    void checkVaultBackups_VaultNotFound() {
        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);

        CheckVaultBackupsRequest req  = new CheckVaultBackupsRequest();
        req.setVaultId("nonexistent-vault");
        ResponseEntity<?> response = vaultController.checkVaultBackups(authentication, req);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(ddbRepo, never()).putUser(any());
    }

    @Test
    void checkVaultBackups_noChanges(){
        UserDDBItem.Vault existingVault = new UserDDBItem.Vault("ExistingVault", UUID.randomUUID().toString());
        UserDDBItem.Vault.Backup backup1 = new UserDDBItem.Vault.Backup("backup1", true);
        UserDDBItem.Vault.Backup backup2 = new UserDDBItem.Vault.Backup("backup2", true);
        existingVault.setBackups(new ArrayList<>(List.of(backup1, backup2)));

        testUser.setVaults(new ArrayList<>(List.of(existingVault)));

        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);

        CheckVaultBackupsRequest req  = new CheckVaultBackupsRequest();
        req.setVaultId(existingVault.getId());
        ResponseEntity<?> response = vaultController.checkVaultBackups(authentication, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(ddbRepo, never()).putUser(any());
    }

    @Test
    void checkVaultBackups_remove() {
        UserDDBItem.Vault existingVault = new UserDDBItem.Vault("ExistingVault", UUID.randomUUID().toString());
        UserDDBItem.Vault.Backup backup1 = new UserDDBItem.Vault.Backup("backup1");
        backup1.setCountCheck(3);
        existingVault.setBackups(new ArrayList<>(List.of(backup1)));

        testUser.setVaults(new ArrayList<>(List.of(existingVault)));

        ArgumentCaptor<UserDDBItem> userCaptor = ArgumentCaptor.forClass(UserDDBItem.class);

        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);
        when(s3Client.headObject(argThat((HeadObjectRequest req) -> req != null && "backup1".equals(req.key()))))
                .thenThrow(NoSuchKeyException.builder().build());


        CheckVaultBackupsRequest req  = new CheckVaultBackupsRequest();
        req.setVaultId(existingVault.getId());
        ResponseEntity<?> response = vaultController.checkVaultBackups(authentication, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(ddbRepo).putUser(userCaptor.capture());

        UserDDBItem capturedUser = userCaptor.getValue();
        assertEquals(1, capturedUser.getVaults().size());
        var firstVaultBackups = capturedUser.getVaults().getFirst().getBackups();
        assertEquals(0, firstVaultBackups.size());
    }

    // MARK: downloadBackup
    @Test
    void downloadBackup_success(){
        UserDDBItem.Vault existingVault = new UserDDBItem.Vault("ExistingVault", UUID.randomUUID().toString());
        BackupId backup1Id = new BackupId("user123", existingVault.getId(), new java.util.Date());
        UserDDBItem.Vault.Backup backup1 = new UserDDBItem.Vault.Backup(backup1Id.toString(), true);
        BackupId backup2Id = new BackupId("user123", existingVault.getId(), new java.util.Date());
        UserDDBItem.Vault.Backup backup2 = new UserDDBItem.Vault.Backup(backup2Id.toString(), true);
        existingVault.setBackups(new ArrayList<>(List.of(backup1, backup2)));

        testUser.setVaults(new ArrayList<>(List.of(existingVault)));

        PresignedGetObjectRequest mockPresignedRequest = mock(PresignedGetObjectRequest.class);
        String finalPresignUrl = "https://example.com/presigned-url";
        try {
            when(mockPresignedRequest.url()).thenReturn(java.net.URI.create(finalPresignUrl).toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);
        when(s3Presigner.presignGetObject(argThat((GetObjectPresignRequest req) ->
                req != null && req.getObjectRequest().key().equals(backup1Id.toString())
        ))).thenReturn(mockPresignedRequest);


        ResponseEntity<?> response = vaultController.downloadBackup(authentication, backup1Id.toString());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        var resBody = (DownloadBackupResponse) response.getBody();
        assertNotNull(resBody);
        assertEquals(finalPresignUrl, resBody.getDownloadUrl());
    }

    // MARK: downloadBackup - Error Cases
    @Test
    void downloadBackup_VaultNotPresentInUser() {
        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);

        BackupId backupId = new BackupId("user123", "nonexistent-vault", new Date());
        ResponseEntity<?> response = vaultController.downloadBackup(authentication, backupId.toString());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void downloadBackup_UserIdInBackupNotEqualToAuthenticatedUser() {
        UserDDBItem.Vault existingVault = new UserDDBItem.Vault("ExistingVault", UUID.randomUUID().toString());
        testUser.setVaults(new ArrayList<>(List.of(existingVault)));

        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);

        BackupId backupId = new BackupId("different-user", existingVault.getId(), new Date());
        ResponseEntity<?> response = vaultController.downloadBackup(authentication, backupId.toString());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void downloadBackup_BackupNotPresentInUser() {
        UserDDBItem.Vault existingVault = new UserDDBItem.Vault("ExistingVault", UUID.randomUUID().toString());
        BackupId existingBackupId = new BackupId("user123", existingVault.getId(), new Date());
        UserDDBItem.Vault.Backup existingBackup = new UserDDBItem.Vault.Backup(existingBackupId.toString());
        existingVault.setBackups(new ArrayList<>(List.of(existingBackup)));
        testUser.setVaults(new ArrayList<>(List.of(existingVault)));

        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);

        BackupId nonExistentBackupId = new BackupId("user123", existingVault.getId(), new Date());
        ResponseEntity<?> response = vaultController.downloadBackup(authentication, nonExistentBackupId.toString());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void downloadBackup_BackupNotVerifiedYet() {
        UserDDBItem.Vault existingVault = new UserDDBItem.Vault("ExistingVault", UUID.randomUUID().toString());
        BackupId backupId = new BackupId("user123", existingVault.getId(), new Date());
        UserDDBItem.Vault.Backup unverifiedBackup = new UserDDBItem.Vault.Backup(backupId.toString(), false);
        existingVault.setBackups(new ArrayList<>(List.of(unverifiedBackup)));
        testUser.setVaults(new ArrayList<>(List.of(existingVault)));

        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);

        ResponseEntity<?> response = vaultController.downloadBackup(authentication, backupId.toString());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // MARK: deleteBackup
    @Test
    void deleteBackup_success() {
        UserDDBItem.Vault existingVault = new UserDDBItem.Vault("ExistingVault", UUID.randomUUID().toString());
        BackupId backupId = new BackupId("user123", existingVault.getId(), new Date());
        UserDDBItem.Vault.Backup backup = new UserDDBItem.Vault.Backup(backupId.toString(), true);
        existingVault.setBackups(new ArrayList<>(List.of(backup)));
        testUser.setVaults(new ArrayList<>(List.of(existingVault)));

        ArgumentCaptor<UserDDBItem> userCaptor = ArgumentCaptor.forClass(UserDDBItem.class);

        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);

        ResponseEntity<?> response = vaultController.deleteBackup(authentication, backupId.toString());

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(ddbRepo).putUser(userCaptor.capture());
        UserDDBItem capturedUser = userCaptor.getValue();
        assertEquals(1, capturedUser.getVaults().size());
        assertEquals(0, capturedUser.getVaults().getFirst().getBackups().size());
        verify(s3Client).deleteObject(argThat((DeleteObjectRequest req) ->
                req != null && backupId.toString().equals(req.key())
        ));
    }

    @Test
    void deleteBackup_BackupNotFound() {
        UserDDBItem.Vault existingVault = new UserDDBItem.Vault("ExistingVault", UUID.randomUUID().toString());
        testUser.setVaults(new ArrayList<>(List.of(existingVault)));

        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);

        BackupId nonExistentBackupId = new BackupId("user123", existingVault.getId(), new Date());
        ResponseEntity<?> response = vaultController.deleteBackup(authentication, nonExistentBackupId.toString());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(ddbRepo, never()).putUser(any());
        verify(s3Client, never()).deleteObject((DeleteObjectRequest) any());
    }

    @Test
    void deleteBackup_VaultNotFound() {
        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);

        BackupId backupId = new BackupId("user123", "nonexistent-vault", new Date());
        ResponseEntity<?> response = vaultController.deleteBackup(authentication, backupId.toString());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(ddbRepo, never()).putUser(any());
        verify(s3Client, never()).deleteObject((DeleteObjectRequest) any());
    }

    @Test
    void deleteBackup_UserIdMismatch() {
        UserDDBItem.Vault existingVault = new UserDDBItem.Vault("ExistingVault", UUID.randomUUID().toString());
        testUser.setVaults(new ArrayList<>(List.of(existingVault)));

        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);

        BackupId backupId = new BackupId("different-user", existingVault.getId(), new Date());
        ResponseEntity<?> response = vaultController.deleteBackup(authentication, backupId.toString());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(ddbRepo, never()).putUser(any());
        verify(s3Client, never()).deleteObject((DeleteObjectRequest) any());
    }

    @Test
    void deleteBackup_BackupNotVerified() {
        UserDDBItem.Vault existingVault = new UserDDBItem.Vault("ExistingVault", UUID.randomUUID().toString());
        BackupId backupId = new BackupId("user123", existingVault.getId(), new Date());
        UserDDBItem.Vault.Backup unverifiedBackup = new UserDDBItem.Vault.Backup(backupId.toString(), false);
        existingVault.setBackups(new ArrayList<>(List.of(unverifiedBackup)));
        testUser.setVaults(new ArrayList<>(List.of(existingVault)));

        ArgumentCaptor<UserDDBItem> userCaptor = ArgumentCaptor.forClass(UserDDBItem.class);

        when(authentication.getTokenAttributes()).thenReturn(tokenAttributes);
        when(ddbRepo.getUserById("user123")).thenReturn(testUser);

        ResponseEntity<?> response = vaultController.deleteBackup(authentication, backupId.toString());

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(ddbRepo).putUser(userCaptor.capture());
        UserDDBItem capturedUser = userCaptor.getValue();
        assertEquals(1, capturedUser.getVaults().size());
        assertEquals(0, capturedUser.getVaults().getFirst().getBackups().size());
        verify(s3Client, never()).deleteObject((DeleteObjectRequest) any());
    }

}
