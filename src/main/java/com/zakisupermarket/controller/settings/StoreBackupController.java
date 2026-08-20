package com.zakisupermarket.controller.settings;

import com.zakisupermarket.dto.settings.request.BackupRequest;
import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.dto.settings.response.BackupResponse;
import com.zakisupermarket.service.settings.StoreBackupService;
import com.zakisupermarket.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Self-service export of the CALLING store's own data - always scoped to
 * SecurityUtils.getCurrentStoreId(), never a client-supplied id. This is the
 * store-facing "backup" feature; the whole-database platform backup/restore lives
 * separately at BackupController (/api/platform/backup, platform-key only). There is
 * deliberately no restore endpoint here - restoring even just one store's data
 * back into a live multi-tenant database can collide with other stores' current
 * data (shared auto-increment ids across tables), so that stays a manual,
 * platform-operator-reviewed process for now rather than a one-click API call.
 */
@RestController
@RequestMapping("/api/settings/backup")
@RequiredArgsConstructor
@Slf4j
public class StoreBackupController {

    private final StoreBackupService storeBackupService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BackupResponse>> createBackup(@Valid @RequestBody BackupRequest request) {
        Long storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("Creating store backup - storeId: {}, userId: {}", storeId, userId);

        BackupResponse backup = storeBackupService.createBackup(storeId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(backup, "Backup created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<BackupResponse>>> getBackups() {
        Long storeId = SecurityUtils.getCurrentStoreId();

        List<BackupResponse> backups = storeBackupService.getBackupsForStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(backups, "Backups retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BackupResponse>> getBackup(@PathVariable Long id) {
        Long storeId = SecurityUtils.getCurrentStoreId();

        BackupResponse backup = storeBackupService.getBackup(id, storeId);
        return ResponseEntity.ok(ApiResponse.success(backup, "Backup retrieved successfully"));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadBackup(@PathVariable Long id) {
        Long storeId = SecurityUtils.getCurrentStoreId();

        byte[] data = storeBackupService.downloadBackup(id, storeId);
        BackupResponse backup = storeBackupService.getBackup(id, storeId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", backup.getBackupName() + ".json");

        return ResponseEntity.ok().headers(headers).body(data);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBackup(@PathVariable Long id) {
        Long storeId = SecurityUtils.getCurrentStoreId();

        storeBackupService.deleteBackup(id, storeId);
        return ResponseEntity.ok(ApiResponse.success(null, "Backup deleted successfully"));
    }
}
