package com.zakisupermarket.controller.settings;

import com.zakisupermarket.dto.settings.request.BackupRequest;
import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.dto.settings.response.BackupResponse;
import com.zakisupermarket.security.PlatformAdminAuth;
import com.zakisupermarket.service.settings.BackupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Whole-database backup/restore - covers EVERY store at once, so this is
 * deliberately NOT reachable through the normal store-facing JWT/role system (no
 * store ADMIN, no matter how the role checks are configured, should ever be able
 * to restore-overwrite every other store's data). Gated entirely by
 * PlatformAdminAuth instead - see SecurityConfig, this path is permitAll() at the
 * Spring Security layer and enforces its own separate secret per request. A
 * store's own self-service data export lives at StoreBackupController instead.
 */
@RestController
@RequestMapping("/api/platform/backup")
@RequiredArgsConstructor
@Slf4j
public class BackupController {

    private final BackupService backupService;
    private final PlatformAdminAuth platformAdminAuth;

    @PostMapping
    public ResponseEntity<ApiResponse<BackupResponse>> createBackup(
            @RequestHeader(PlatformAdminAuth.HEADER_NAME) String adminKey,
            @Valid @RequestBody BackupRequest request) {
        platformAdminAuth.require(adminKey);
        log.info("Platform admin creating whole-database backup");

        BackupResponse backup = backupService.createBackup(request, null);
        return ResponseEntity.ok(ApiResponse.success(backup, "Backup created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BackupResponse>>> getAllBackups(
            @RequestHeader(PlatformAdminAuth.HEADER_NAME) String adminKey) {
        platformAdminAuth.require(adminKey);

        List<BackupResponse> backups = backupService.getAllBackups();
        return ResponseEntity.ok(ApiResponse.success(backups, "Backups retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BackupResponse>> getBackupById(
            @RequestHeader(PlatformAdminAuth.HEADER_NAME) String adminKey,
            @PathVariable Long id) {
        platformAdminAuth.require(adminKey);

        BackupResponse backup = backupService.getBackupById(id);
        return ResponseEntity.ok(ApiResponse.success(backup, "Backup retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBackup(
            @RequestHeader(PlatformAdminAuth.HEADER_NAME) String adminKey,
            @PathVariable Long id) {
        platformAdminAuth.require(adminKey);
        log.info("Platform admin deleting backup id: {}", id);

        backupService.deleteBackup(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Backup deleted successfully"));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreBackup(
            @RequestHeader(PlatformAdminAuth.HEADER_NAME) String adminKey,
            @PathVariable Long id) {
        platformAdminAuth.require(adminKey);
        log.warn("Platform admin restoring whole-database backup id: {}", id);

        backupService.restoreBackup(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Backup restored successfully"));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadBackup(
            @RequestHeader(PlatformAdminAuth.HEADER_NAME) String adminKey,
            @PathVariable Long id) {
        platformAdminAuth.require(adminKey);

        byte[] backupData = backupService.downloadBackup(id);
        BackupResponse backup = backupService.getBackupById(id);

        String extension = backup.getFilePath() != null && backup.getFilePath().contains(".")
                ? backup.getFilePath().substring(backup.getFilePath().lastIndexOf('.'))
                : ".dump";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", backup.getBackupName() + extension);

        return ResponseEntity.ok()
                .headers(headers)
                .body(backupData);
    }
}
