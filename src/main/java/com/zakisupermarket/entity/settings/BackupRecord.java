package com.zakisupermarket.entity.settings;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "backup_records", schema = "zaki_supermarket")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "backup_name", nullable = false)
    private String backupName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "backup_type")
    private String backupType;

    @Column(name = "status")
    private String status;

    @Column(name = "description")
    private String description;

    @Column(name = "created_by")
    private Long createdBy;

    /**
     * NULL = a whole-database (PLATFORM scope) backup, covering every store - only
     * ever created/restored via the platform-operator API key, never by a store's
     * own ADMIN. Non-null = a STORE-scope backup, a JSON export of just that
     * store's own data, self-service for that store's ADMIN.
     */
    @Column(name = "store_id")
    private Long storeId;

    /** "PLATFORM" or "STORE" - see storeId. */
    @Column(name = "scope", length = 20)
    @Builder.Default
    private String scope = "PLATFORM";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "restored_at")
    private LocalDateTime restoredAt;
}