/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2026.
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *    and associated documentation files (the "Software"), to deal in the Software without restriction,
 *    including without limitation the rights to use, copy, modify, merge, publish, distribute,
 *    sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 *    furnished to do so, subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included in all copies or
 *    substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 *    OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *    WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 *    IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.disk91.files.pdb.entities;

import com.disk91.common.tools.CloneableObject;
import com.disk91.files.pdb.entities.sub.FileAccessType;
import com.disk91.files.pdb.entities.sub.FileMimeCategory;
import jakarta.persistence.*;

/**
 * JPA entity representing a stored file record in PostgreSQL.
 * Holds all metadata for a file uploaded by a user, including access control,
 * integrity signature, thumbnail reference and quota-relevant fields.
 * The physical content is stored on disk; only metadata lives in this table.
 */
@Entity
@Table(
        name = "files_stored",
        indexes = {
                @Index(name = "idx_file_owner_id", columnList = "owner_id", unique = false),
                @Index(name = "idx_file_created_at", columnList = "created_at", unique = false),
                @Index(name = "idx_file_unique_name", columnList = "unique_name", unique = true)
        }
)
public class FileStored implements CloneableObject<FileStored> {

    // UUID assigned at upload time, used as primary key and in the physical filename
    @Id
    @Column(name = "id", nullable = false, length = 36)
    protected String id;

    // Generated unique filename used for physical storage: {id}-{epoch_ms}.{ext}
    @Column(name = "unique_name", nullable = false, unique = true, length = 128)
    protected String uniqueName;

    // Original filename provided by the uploader
    @Column(name = "original_name", nullable = false, length = 512)
    protected String originalName;

    // Optional human-readable description of the file
    @Column(name = "description", columnDefinition = "text", nullable = true)
    protected String description;

    // Detected MIME category (IMAGE, PDF, TEXT, GENERIC)
    @Column(name = "mime_category", nullable = false)
    @Enumerated(EnumType.STRING)
    protected FileMimeCategory mimeCategory;

    // Full detected MIME type (e.g. image/png, application/pdf)
    @Column(name = "mime_type", nullable = false, length = 128)
    protected String mimeType;

    // File size in bytes (after any resizing for images)
    @Column(name = "size", nullable = false)
    protected long size;

    // Login (hash) of the user who uploaded the file
    @Column(name = "owner_id", nullable = false, length = 128)
    protected String ownerId;

    // Access control type: PUBLIC, CONNECTED or PRIVATE
    @Column(name = "access_type", nullable = false)
    @Enumerated(EnumType.STRING)
    protected FileAccessType accessType;

    // Number of times the file has been successfully downloaded
    @Column(name = "access_count", nullable = false)
    protected long accessCount = 0;

    // Upload timestamp in milliseconds since epoch
    @Column(name = "created_at", nullable = false)
    protected long createdAt;

    // Last metadata update timestamp in milliseconds since epoch
    @Column(name = "updated_at", nullable = false)
    protected long updatedAt;

    // SHA-256 hex digest of the file content, computed at upload time
    @Column(name = "signature", nullable = false, length = 64)
    protected String signature;

    // When true, integrity verification is skipped on download; only admin can set this flag
    @Column(name = "no_signature_check", nullable = false)
    protected boolean noSignatureCheck = false;

    // Unique filename of the generated thumbnail (images only, null otherwise)
    @Column(name = "thumbnail_unique_name", nullable = true, length = 128)
    protected String thumbnailUniqueName;

    // SHA-256 hex digest of the thumbnail content (images only, null otherwise)
    @Column(name = "thumbnail_signature", nullable = true, length = 64)
    protected String thumbnailSignature;

    // In-memory only: timestamp (ms) of the last successful signature verification; never persisted
    @jakarta.persistence.Transient
    protected long lastSignatureCheck = 0;


    // ==========================
    // Clone

    /**
     * Deep-copy this FileStored instance.
     * The in-memory field lastSignatureCheck is preserved in the copy.
     * @return a new FileStored with all fields copied
     */
    @Override
    public FileStored clone() {
        FileStored f = new FileStored();
        f.setId(this.id);
        f.setUniqueName(this.uniqueName);
        f.setOriginalName(this.originalName);
        f.setDescription(this.description);
        f.setMimeCategory(this.mimeCategory);
        f.setMimeType(this.mimeType);
        f.setSize(this.size);
        f.setOwnerId(this.ownerId);
        f.setAccessType(this.accessType);
        f.setAccessCount(this.accessCount);
        f.setCreatedAt(this.createdAt);
        f.setUpdatedAt(this.updatedAt);
        f.setSignature(this.signature);
        f.setNoSignatureCheck(this.noSignatureCheck);
        f.setThumbnailUniqueName(this.thumbnailUniqueName);
        f.setThumbnailSignature(this.thumbnailSignature);
        // Copy in-memory-only field
        f.setLastSignatureCheck(this.lastSignatureCheck);
        return f;
    }

    // ==========================
    // Getters & Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FileMimeCategory getMimeCategory() {
        return mimeCategory;
    }

    public void setMimeCategory(FileMimeCategory mimeCategory) {
        this.mimeCategory = mimeCategory;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public FileAccessType getAccessType() {
        return accessType;
    }

    public void setAccessType(FileAccessType accessType) {
        this.accessType = accessType;
    }

    public long getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(long accessCount) {
        this.accessCount = accessCount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public boolean isNoSignatureCheck() {
        return noSignatureCheck;
    }

    public void setNoSignatureCheck(boolean noSignatureCheck) {
        this.noSignatureCheck = noSignatureCheck;
    }

    public String getThumbnailUniqueName() {
        return thumbnailUniqueName;
    }

    public void setThumbnailUniqueName(String thumbnailUniqueName) {
        this.thumbnailUniqueName = thumbnailUniqueName;
    }

    public String getThumbnailSignature() {
        return thumbnailSignature;
    }

    public void setThumbnailSignature(String thumbnailSignature) {
        this.thumbnailSignature = thumbnailSignature;
    }

    public long getLastSignatureCheck() {
        return lastSignatureCheck;
    }

    public void setLastSignatureCheck(long lastSignatureCheck) {
        this.lastSignatureCheck = lastSignatureCheck;
    }
}


