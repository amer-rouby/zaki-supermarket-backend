package com.zakisupermarket.service.settings;

import com.zakisupermarket.dto.settings.request.BackupRequest;
import com.zakisupermarket.dto.settings.response.BackupResponse;

import java.util.List;

public interface BackupService {

    BackupResponse createBackup(BackupRequest request, Long userId);

    List<BackupResponse> getAllBackups();

    BackupResponse getBackupById(Long id);

    void deleteBackup(Long id);

    void restoreBackup(Long id);

    byte[] downloadBackup(Long id);
}