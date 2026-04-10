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
package com.disk91.files.api.interfaces;

import com.disk91.files.pdb.entities.FileStored;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * File metadata response returned by upload, info and list endpoints.
 * The signature field is intentionally omitted for security reasons.
 */
@Tag(name = "File metadata response", description = "Metadata of a stored file")
public class FileUploadResponseItf {

    @Schema(description = "Generated unique filename used for physical storage",
            example = "550e8400-e29b-41d4-a716-446655440000-1712345678901.jpg",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected String uniqueName;

    @Schema(description = "Original filename provided at upload time", example = "photo.jpg",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected String originalName;

    @Schema(description = "Optional human-readable description", example = "Profile picture",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected String description;

    @Schema(description = "Detected MIME category: IMAGE, PDF, TEXT, GENERIC", example = "IMAGE",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected String mimeCategory;

    @Schema(description = "Full detected MIME type", example = "image/jpeg",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected String mimeType;

    @Schema(description = "File size in bytes after any resizing", example = "204800",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected long size;

    @Schema(description = "Login hash of the user who uploaded the file",
            example = "a3f2b1c9d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected String ownerId;

    @Schema(description = "Access control type: PUBLIC, CONNECTED, PRIVATE", example = "PUBLIC",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected String accessType;

    @Schema(description = "Number of times the file has been successfully downloaded", example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected long accessCount;

    @Schema(description = "Upload timestamp in milliseconds since epoch", example = "1712345678901",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected long createdAt;

    @Schema(description = "Last metadata update timestamp in milliseconds since epoch", example = "1712345678901",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected long updatedAt;

    @Schema(description = "Unique filename of the generated thumbnail (images only, null otherwise)",
            example = "550e8400-e29b-41d4-a716-446655440000-1712345678901-thumb.jpg",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected String thumbnailUniqueName;

    @Schema(description = "6-character short name alias for this file (null when not assigned)",
            example = "aB3xYz",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected String shortName;

    // ==========================
    // Builder

    /**
     * Populate this response object from a FileStored entity.
     * @param f - source entity
     */
    public void buildFrom(FileStored f) {
        this.uniqueName = f.getUniqueName();
        this.originalName = f.getOriginalName();
        this.description = f.getDescription();
        this.mimeCategory = f.getMimeCategory() != null ? f.getMimeCategory().name() : null;
        this.mimeType = f.getMimeType();
        this.size = f.getSize();
        this.ownerId = f.getOwnerId();
        this.accessType = f.getAccessType() != null ? f.getAccessType().name() : null;
        this.accessCount = f.getAccessCount();
        this.createdAt = f.getCreatedAt();
        this.updatedAt = f.getUpdatedAt();
        this.thumbnailUniqueName = f.getThumbnailUniqueName();
        this.shortName = f.getShortName();
    }

    // ==========================
    // Getters & Setters

    public String getUniqueName() { return uniqueName; }
    public void setUniqueName(String uniqueName) { this.uniqueName = uniqueName; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMimeCategory() { return mimeCategory; }
    public void setMimeCategory(String mimeCategory) { this.mimeCategory = mimeCategory; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getAccessType() { return accessType; }
    public void setAccessType(String accessType) { this.accessType = accessType; }

    public long getAccessCount() { return accessCount; }
    public void setAccessCount(long accessCount) { this.accessCount = accessCount; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getThumbnailUniqueName() { return thumbnailUniqueName; }
    public void setThumbnailUniqueName(String thumbnailUniqueName) { this.thumbnailUniqueName = thumbnailUniqueName; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }
}

