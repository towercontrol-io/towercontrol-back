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
package com.disk91.files.services;

import com.disk91.audit.integration.AuditIntegration;
import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.tools.HexCodingTools;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.RandomString;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITOverQuotaException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.files.api.interfaces.FileAdminListResponseItf;
import com.disk91.files.api.interfaces.FileUpdateBody;
import com.disk91.files.api.interfaces.FileUploadResponseItf;
import com.disk91.files.config.ActionCatalog;
import com.disk91.files.config.FilesConfig;
import com.disk91.files.pdb.entities.FileStored;
import com.disk91.files.pdb.entities.sub.FileAccessType;
import com.disk91.files.pdb.entities.sub.FileMimeCategory;
import com.disk91.files.pdb.repositories.FileStoredRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.security.SecureRandom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
public class FileService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // Hardcoded IV prepended to file content before HMAC-SHA256 computation
    private static final String SIGNATURE_IV = "f3a7b2c9e1d4f8a0b5c2d7e3f1a9b6c4";

    private static final int SHORT_NAME_LENGTH = 6;
    private static final int SHORT_NAME_MAX_ATTEMPTS = 20;

    // Access key: 16-character random [a-z0-9] token granting unauthenticated access to CONNECTED/PRIVATE files
    private static final int ACCESS_KEY_LENGTH = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    protected FilesConfig filesConfig;

    @Autowired
    protected FileStoredRepository fileStoredRepository;

    @Autowired
    protected FileCache fileCache;

    @Autowired
    protected AuditIntegration auditIntegration;

    // ================================================================================================================
    // UPLOAD
    // ================================================================================================================

    /**
     * Upload a file: enforce quotas, detect MIME type, resize/thumbnail images,
     * compute HMAC-SHA256 signature, write to disk and persist the record in DB.
     * When withShortName is true, a unique 6-character short name is generated and attached.
     * When withAccessKey is true, a 16-character access key is generated, enabling unauthenticated access.
     * @param requestorLogin - login hash of the authenticated user performing the upload
     * @param file           - multipart file received from the API
     * @param accessTypeStr  - requested access type (PUBLIC, CONNECTED, PRIVATE)
     * @param description    - optional human-readable description
     * @param filename       - optional override for the original filename
     * @param withShortName  - when true, generate and assign a unique 6-character short name
     * @param withAccessKey  - when true, generate and assign a 16-character access key
     * @param req            - HTTP request (for role checking and IP tracing)
     * @return the persisted FileStored entity
     * @throws ITParseException     - invalid input (empty file, bad accessType, unsupported format, key generation failed)
     * @throws ITRightException     - insufficient role to create public/connected files
     * @throws ITOverQuotaException - user has reached file count or storage quota
     */
    public FileStored uploadFile(
            String requestorLogin,
            MultipartFile file,
            String accessTypeStr,
            String description,
            String filename,
            boolean withShortName,
            boolean withAccessKey,
            HttpServletRequest req
    ) throws ITParseException, ITRightException, ITOverQuotaException {

        // Validate the file is not empty
        if (file == null || file.isEmpty()) throw new ITParseException("files-upload-empty-file");

        // Parse and validate the requested access type
        FileAccessType accessType;
        try {
            accessType = FileAccessType.valueOf(accessTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.debug("[files] Invalid accessType '{}' from user {}", accessTypeStr, requestorLogin);
            throw new ITParseException("files-upload-invalid-access-type");
        }

        // Verify role rights: PUBLIC and CONNECTED require ROLE_FILES_WRITE or ROLE_FILES_ADMIN
        if (accessType != FileAccessType.PRIVATE) {
            boolean hasWriteRole = req.isUserInRole("ROLE_FILES_WRITE") || req.isUserInRole("ROLE_FILES_ADMIN");
            if (!hasWriteRole) {
                log.warn("[files] User {} lacks ROLE_FILES_WRITE to create {} file", requestorLogin, accessType);
                throw new ITRightException("files-upload-missing-write-role");
            }
        }

        // Check single-file size quota before reading the bytes
        long fileSize = file.getSize();
        if (filesConfig.getQuotaMaxFileBytes() > 0 && fileSize > filesConfig.getQuotaMaxFileBytes()) {
            log.debug("[files] File too large ({} bytes) from user {}", fileSize, requestorLogin);
            throw new ITOverQuotaException("files-upload-file-too-large");
        }

        // Read raw bytes
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            log.warn("[files] Failed to read uploaded file bytes for user {}", requestorLogin, e);
            throw new ITParseException("files-upload-read-error");
        }

        // Detect MIME type from content (not just extension)
        String mimeType = detectMimeType(fileBytes, file.getOriginalFilename());
        FileMimeCategory mimeCategory = categorizeMime(mimeType);
        log.info("[files] Detected MIME type '{}' category '{}' for upload from {}", mimeType, mimeCategory, requestorLogin);

        // Check per-user file count quota
        if (filesConfig.getQuotaMaxFilesPerUser() > 0) {
            long userFileCount = fileStoredRepository.countByOwnerId(requestorLogin);
            if (userFileCount >= filesConfig.getQuotaMaxFilesPerUser()) {
                log.debug("[files] User {} reached file count quota ({}/{})", requestorLogin, userFileCount, filesConfig.getQuotaMaxFilesPerUser());
                throw new ITOverQuotaException("files-upload-quota-file-count");
            }
        }

        // Check per-user total storage quota
        if (filesConfig.getQuotaMaxTotalBytesPerUser() > 0) {
            long userTotalBytes = fileStoredRepository.sumSizeByOwnerId(requestorLogin);
            if (userTotalBytes + fileSize > filesConfig.getQuotaMaxTotalBytesPerUser()) {
                log.debug("[files] User {} reached total storage quota ({} + {} > {})",
                        requestorLogin, userTotalBytes, fileSize, filesConfig.getQuotaMaxTotalBytesPerUser());
                throw new ITOverQuotaException("files-upload-quota-storage");
            }
        }

        // Generate identifiers and unique filenames
        String id = UUID.randomUUID().toString();
        long uploadMs = Now.NowUtcMs();
        String ext = extractExtension(file.getOriginalFilename());
        String uniqueName = id + "-" + uploadMs + "." + ext;

        // Image-specific processing: resize if too large and generate thumbnail
        byte[] thumbnailBytes = null;
        String thumbnailUniqueName = null;
        String thumbnailSignature = null;

        if (mimeCategory == FileMimeCategory.IMAGE) {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(fileBytes));
                if (img != null) {
                    String imageFormat = mimeType.equals("image/jpeg") ? "jpeg" : "png";

                    // Resize original if any dimension exceeds the configured max
                    if (filesConfig.getImageMaxPixels() > 0) {
                        int maxDim = Math.max(img.getWidth(), img.getHeight());
                        if (maxDim > filesConfig.getImageMaxPixels()) {
                            img = scaleImage(img, filesConfig.getImageMaxPixels());
                            fileBytes = imageToBytes(img, imageFormat);
                            log.debug("[files] Resized image to max {}px for user {}", filesConfig.getImageMaxPixels(), requestorLogin);
                        }
                    }

                    // Generate thumbnail unconditionally
                    BufferedImage thumbImg = scaleImage(img, filesConfig.getImageThumbnailPixels());
                    thumbnailBytes = imageToBytes(thumbImg, imageFormat);
                    thumbnailUniqueName = id + "-" + uploadMs + "-thumb." + ext;
                    thumbnailSignature = computeHmacSignature(thumbnailBytes);
                    log.debug("[files] Thumbnail generated for user {}", requestorLogin);
                } else {
                    log.debug("[files] ImageIO could not decode image '{}' for user {} - stored as-is",
                            file.getOriginalFilename(), requestorLogin);
                    throw new ITParseException("files-upload-image-read-error");
                }
            } catch (Exception e) {
                log.debug("[files] Image processing failed for '{}', user {} - stored as-is: {}",
                        file.getOriginalFilename(), requestorLogin, e.getMessage());
                throw new ITParseException("files-upload-image-process-error");
            }
        }

        // Compute HMAC-SHA256 integrity signature of the final file content
        String signature;
        try {
            signature = computeHmacSignature(fileBytes);
        } catch (Exception e) {
            log.error("[files] Signature computation failed for user {}", requestorLogin, e);
            throw new ITParseException("files-upload-signature-error");
        }

        // Build storage directory: {root}/{id[0]}/{id[1]}/
        Path storageDir = Paths.get(filesConfig.getStorageRootPath())
                .resolve(String.valueOf(id.charAt(0)))
                .resolve(String.valueOf(id.charAt(1)));
        try {
            Files.createDirectories(storageDir);
        } catch (Exception e) {
            log.error("[files] Cannot create storage directory {} for user {}", storageDir, requestorLogin, e);
            throw new ITParseException("files-upload-server-side-storage-error");
        }

        // Write file bytes to disk
        try {
            Files.write(storageDir.resolve(uniqueName), fileBytes);
        } catch (Exception e) {
            log.error("[files] Failed to write file {} for user {}", uniqueName, requestorLogin, e);
            throw new ITParseException("files-upload-server-side-write-error");
        }

        // Write thumbnail bytes to disk (images only)
        if (thumbnailBytes != null) {
            try {
                Files.write(storageDir.resolve(thumbnailUniqueName), thumbnailBytes);
            } catch (Exception e) {
                log.error("[files] Failed to write thumbnail {} for user {}", thumbnailUniqueName, requestorLogin, e);
                throw new ITParseException("files-upload-server-side-write-error");
            }
        }

        // Build and persist the FileStored entity
        long finalSize = fileBytes.length;
        FileStored stored = new FileStored();
        stored.setId(id);
        stored.setUniqueName(uniqueName);
        stored.setOriginalName(file.getOriginalFilename() != null ? file.getOriginalFilename() : filename);
        stored.setDescription(description);
        stored.setMimeCategory(mimeCategory);
        stored.setMimeType(mimeType);
        stored.setSize(finalSize);
        stored.setOwnerId(requestorLogin);
        stored.setAccessType(accessType);
        stored.setAccessCount(0L);
        stored.setCreatedAt(uploadMs);
        stored.setUpdatedAt(uploadMs);
        stored.setSignature(signature);
        stored.setNoSignatureCheck(false);
        stored.setThumbnailUniqueName(thumbnailUniqueName);
        stored.setThumbnailSignature(thumbnailSignature);

        // Generate a unique short name when explicitly requested
        if (withShortName) {
            String shortName = generateUniqueShortName();
            stored.setShortName(shortName);
            log.debug("[files] Short name '{}' assigned to file id={}", shortName, uniqueName);
        }

        // Generate a unique access key when explicitly requested
        if (withAccessKey) {
            String generatedKey = generateUniqueAccessKey();
            stored.setAccessKey(generatedKey);
            log.debug("[files] Access key assigned to file id={}", uniqueName);
        }

        fileCache.saveFile(stored);

        // Audit log
        auditIntegration.auditLog(
                ModuleCatalog.Modules.FILES,
                ActionCatalog.getActionName(ActionCatalog.Actions.FILES_UPLOAD),
                requestorLogin, "File uploaded: id={0} size={1} accessType={2}",
                new String[]{uniqueName, String.valueOf(finalSize), accessType.name()});

        log.debug("[files] File uploaded: id={} size={} type={} access={} owner={}",
                uniqueName, finalSize, mimeType, accessType, requestorLogin);

        return stored;
    }

    // ================================================================================================================
    // DOWNLOAD
    // ================================================================================================================

    /**
     * Retrieve a file's bytes after verifying access rights and signature integrity.
     * Increments the access counter and updates the lastSignatureCheck timestamp in cache.
     * When fileIdOrShortName is exactly 6 characters it is treated as a short name alias.
     * When accessKey is provided and matches the file's key, authentication is bypassed.
     * @param fileIdOrShortName - uniqueName or 6-character short name of the file to download
     * @param req       - HTTP request for authentication state
     * @param accessKey - optional access key passed as query parameter (may be null)
     * @return FileDownloadData wrapping the FileStored metadata and raw content
     * @throws ITNotFoundException - file does not exist or physical file missing
     * @throws ITRightException    - caller is not allowed to access this file
     * @throws ITParseException    - file integrity check failed
     */
    public FileDownloadData downloadFile(String fileIdOrShortName, HttpServletRequest req, String accessKey)
            throws ITNotFoundException, ITRightException, ITParseException {
        FileStored file = resolveFile(fileIdOrShortName);
        checkFileAccess(file, req, accessKey);

        byte[] content = readAndVerify(file);

        // Increment access count atomically in DB (best-effort, non-blocking)
        try {
            fileStoredRepository.incrementAccessCount(file.getId());
        } catch (Exception e) {
            log.warn("[files] Could not increment access count for file {}", file.getUniqueName());
        }
        return new FileDownloadData(file, content);
    }

    /**
     * Retrieve a thumbnail's bytes after verifying access rights.
     * The original file's access counter is NOT incremented.
     * When fileIdOrShortName is exactly 6 characters it is treated as a short name alias.
     * When accessKey is provided and matches the file's key, authentication is bypassed.
     * @param fileIdOrShortName - uniqueName or 6-character short name of the parent file
     * @param req       - HTTP request for authentication state
     * @param accessKey - optional access key passed as query parameter (may be null)
     * @return FileDownloadData wrapping the FileStored metadata and thumbnail bytes
     * @throws ITNotFoundException - file has no thumbnail or does not exist
     * @throws ITRightException    - caller is not allowed to access this file
     * @throws ITParseException    - thumbnail integrity check failed
     */
    public FileDownloadData downloadThumbnail(String fileIdOrShortName, HttpServletRequest req, String accessKey)
            throws ITNotFoundException, ITRightException, ITParseException {
        FileStored file = resolveFile(fileIdOrShortName);

        // Thumbnails only exist for images
        if (file.getThumbnailUniqueName() == null) throw new ITNotFoundException("file-no-thumbnail");
        checkFileAccess(file, req, accessKey);

        Path thumbPath = buildFilePath(file.getThumbnailUniqueName());
        if (!Files.exists(thumbPath)) {
            log.error("[files] Physical thumbnail not found: {} for file {}", file.getThumbnailUniqueName(), file.getUniqueName());
            throw new ITNotFoundException("file-thumbnail-physical-missing");
        }

        byte[] content;
        try {
            content = Files.readAllBytes(thumbPath);
        } catch (Exception e) {
            log.error("[files] Failed to read thumbnail for file {}", file.getUniqueName(), e);
            throw new ITNotFoundException("file-thumbnail-read-error");
        }

        // Verify thumbnail signature if needed
        if (!file.isNoSignatureCheck() && file.getThumbnailSignature() != null) {
            String computed = computeHmacSignatureSilent(content);
            if (!computed.equals(file.getThumbnailSignature())) {
                log.error("[files] Thumbnail integrity failure for file {} - expected {} got {}",
                        file.getUniqueName(), file.getThumbnailSignature(), computed);
                auditIntegration.auditLog(
                        ModuleCatalog.Modules.FILES,
                        ActionCatalog.getActionName(ActionCatalog.Actions.FILES_INTEGRITY),
                        "system", "Thumbnail integrity check failed for file {0}",
                        new String[]{file.getUniqueName()});
                // Track signature error in metrics counter
                com.disk91.files.Files.incSignatureErrors();
                throw new ITParseException("file-thumbnail-integrity-check-failed");
            }
        }
        return new FileDownloadData(file, content);
    }

    // ================================================================================================================
    // INFO
    // ================================================================================================================

    /**
     * Return the metadata of a file after verifying access rights. No binary content is served.
     * When fileIdOrShortName is exactly 6 characters it is treated as a short name alias.
     * @param fileIdOrShortName - uniqueName or 6-character short name of the file
     * @param req       - HTTP request for authentication state
     * @return the FileStored entity (clone)
     * @throws ITNotFoundException - file does not exist
     * @throws ITRightException    - caller is not allowed to access this file
     */
    public FileStored getFileInfo(String fileIdOrShortName, HttpServletRequest req)
            throws ITNotFoundException, ITRightException {
        FileStored file = resolveFile(fileIdOrShortName);
        checkFileAccess(file, req, null);
        return file;
    }

    // ================================================================================================================
    // UPDATE
    // ================================================================================================================

    /**
     * Update the description, access type and/or short name of a file.
     * Only the owner or a ROLE_FILES_ADMIN can perform this operation.
     * Upgrading to PUBLIC/CONNECTED requires ROLE_FILES_WRITE.
     * When body.withShortName is true and no short name exists, a new one is generated.
     * When body.withShortName is false and a short name exists, it is removed.
     * When fileIdOrShortName is exactly 6 characters it is treated as a short name alias.
     * @param requestorLogin    - login hash of the user requesting the change
     * @param fileIdOrShortName - uniqueName or 6-character short name of the file to update
     * @param body              - new values for description, accessType and withShortName
     * @param req               - HTTP request for role checking
     * @return the updated FileStored entity
     * @throws ITNotFoundException - file does not exist
     * @throws ITRightException    - caller is not allowed to modify this file
     * @throws ITParseException    - invalid accessType value or short name generation exhausted
     */
    public FileStored updateFileMeta(
            String requestorLogin,
            String fileIdOrShortName,
            FileUpdateBody body,
            HttpServletRequest req
    ) throws ITNotFoundException, ITRightException, ITParseException {

        FileStored file = resolveFile(fileIdOrShortName);

        // Only owner or admin can update
        boolean isAdmin = req.isUserInRole("ROLE_FILES_ADMIN");
        if (!file.getOwnerId().equals(requestorLogin) && !isAdmin) {
            log.warn("[files] User {} attempted to update file {} owned by {}", requestorLogin, fileIdOrShortName, file.getOwnerId());
            throw new ITRightException("file-update-not-owner");
        }

        // Parse new access type
        FileAccessType newAccessType;
        try {
            newAccessType = FileAccessType.valueOf(body.getAccessType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ITParseException("file-update-invalid-access-type");
        }

        // Upgrading visibility requires ROLE_FILES_WRITE or ROLE_FILES_ADMIN
        if (newAccessType != FileAccessType.PRIVATE && !req.isUserInRole("ROLE_FILES_WRITE") && !isAdmin) {
            log.warn("[files] User {} lacks role to set file {} to {}", requestorLogin, fileIdOrShortName, newAccessType);
            throw new ITRightException("file-update-missing-write-role");
        }

        // Track changed fields for audit
        StringBuilder changedFields = new StringBuilder();
        if (!file.getAccessType().equals(newAccessType)) changedFields.append("accessType ");
        if (descriptionChanged(file.getDescription(), body.getDescription())) changedFields.append("description ");

        file.setAccessType(newAccessType);
        file.setDescription(body.getDescription());

        // Handle short name creation or removal when explicitly requested
        boolean withShortName = body.getWithShortName();
        if (withShortName && file.getShortName() == null) {
            // Generate a new short name only when none is already assigned
            String shortName = generateUniqueShortName();
            file.setShortName(shortName);
            changedFields.append("shortName(added) ");
            log.debug("[files] Short name '{}' assigned to file {} by {}", shortName, file.getUniqueName(), requestorLogin);
        } else if (!withShortName && file.getShortName() != null) {
            // Remove existing short name
            log.debug("[files] Short name '{}' removed from file {} by {}", file.getShortName(), file.getUniqueName(), requestorLogin);
            file.setShortName(null);
            changedFields.append("shortName(removed) ");
        }

        // Handle access key creation/regeneration or removal when explicitly requested
        if (body.getWithAccessKey() && file.getAccessKey() == null ) {
            // Generate or regenerate the access key (always creates a new one)
            String generatedKey = generateUniqueAccessKey();
            changedFields.append("accessKey(added) ");
            file.setAccessKey(generatedKey);
            log.debug("[files] Access key generated/regenerated for file {} by {}", file.getUniqueName(), requestorLogin);
        } else if ( !body.getWithAccessKey() && file.getAccessKey() != null) {
            // Remove existing access key
            log.debug("[files] Access key removed from file {} by {}", file.getUniqueName(), requestorLogin);
            file.setAccessKey(null);
            changedFields.append("accessKey(removed) ");
        }

        file.setUpdatedAt(Now.NowUtcMs());

        fileCache.saveFile(file);

        auditIntegration.auditLog(
                ModuleCatalog.Modules.FILES,
                ActionCatalog.getActionName(ActionCatalog.Actions.FILES_UPDATE),
                requestorLogin, "File metadata updated: id={0} fields={1}",
                new String[]{file.getUniqueName(), changedFields.toString().trim()});

        log.info("[files] File {} updated by {}", file.getUniqueName(), requestorLogin);
        return file;
    }

    // ================================================================================================================
    // DELETE
    // ================================================================================================================

    /**
     * Delete a file: removes the database record, the physical file and its thumbnail.
     * Only the owner or a ROLE_FILES_ADMIN can delete a file.
     * When fileIdOrShortName is exactly 6 characters it is treated as a short name alias.
     * @param requestorLogin    - login hash of the user requesting deletion
     * @param fileIdOrShortName - uniqueName or 6-character short name of the file to delete
     * @param req               - HTTP request for role checking
     * @throws ITNotFoundException - file does not exist
     * @throws ITRightException    - caller is not allowed to delete this file
     */
    public void deleteFile(String requestorLogin, String fileIdOrShortName, HttpServletRequest req)
            throws ITNotFoundException, ITRightException {

        FileStored file = resolveFile(fileIdOrShortName);

        // Only owner or admin can delete
        boolean isAdmin = req.isUserInRole("ROLE_FILES_ADMIN");
        if (!file.getOwnerId().equals(requestorLogin) && !isAdmin) {
            log.warn("[files] User {} attempted to delete file {} owned by {}", requestorLogin, fileIdOrShortName, file.getOwnerId());
            throw new ITRightException("file-delete-not-owner");
        }

        // Delete physical file from disk
        Path storageDir = Paths.get(filesConfig.getStorageRootPath())
                .resolve(String.valueOf(file.getUniqueName().charAt(0)))
                .resolve(String.valueOf(file.getUniqueName().charAt(1)));
        try {
            Files.deleteIfExists(storageDir.resolve(file.getUniqueName()));
        } catch (Exception e) {
            log.warn("[files] Could not delete physical file {} for id {}", file.getUniqueName(), file.getUniqueName(), e);
        }

        // Delete thumbnail from disk if present
        if (file.getThumbnailUniqueName() != null) {
            try {
                Files.deleteIfExists(storageDir.resolve(file.getThumbnailUniqueName()));
            } catch (Exception e) {
                log.warn("[files] Could not delete thumbnail {} for id {}", file.getThumbnailUniqueName(), file.getUniqueName(), e);
            }
        }

        // Remove from DB and cache
        fileStoredRepository.deleteById(file.getId());
        fileCache.flushFile(file.getUniqueName());

        auditIntegration.auditLog(
                ModuleCatalog.Modules.FILES,
                ActionCatalog.getActionName(ActionCatalog.Actions.FILES_DELETE),
                requestorLogin, "File deleted: id={0}",
                new String[]{file.getUniqueName()});

        log.info("[files] File {} deleted by {}", file.getUniqueName(), requestorLogin);
    }

    // ================================================================================================================
    // ADMIN LIST
    // ================================================================================================================

    /**
     * Return a paginated, searchable and sortable list of all files in the system (admin view).
     * The optional search string performs a case-insensitive LIKE on ownerId, originalName and description.
     * Sort order: CREATED (default, newest first) or ACCESS (most accessed first).
     * Page size is clamped to [1, 250]; default is 50.
     * @param requestorLogin - login hash of the admin
     * @param page           - 0-based page index
     * @param size           - requested page size (clamped to 1-250)
     * @param sort           - sort mode: CREATED or ACCESS
     * @param search         - optional LIKE search string (null means no filter)
     * @return paginated response wrapping matched FileStored records
     * @throws ITParseException - when the sort value is not recognised
     */
    public FileAdminListResponseItf adminListFiles(
            String requestorLogin,
            int page,
            int size,
            String sort,
            String search
    ) throws ITParseException {

        // Clamp page size to allowed range
        if (size <= 0) size = 50;
        if (size > 250) {
            log.debug("[files] Admin {} requested page size {} > 250, clamping", requestorLogin, size);
            size = 250;
        }
        if (page < 0) page = 0;

        // Resolve sort order
        Sort sortOrder;
        String sortUpper = (sort != null) ? sort.trim().toUpperCase() : "CREATED";
        switch (sortUpper) {
            case "ACCESS":
                sortOrder = Sort.by(Sort.Direction.DESC, "accessCount");
                break;
            case "CREATED":
                sortOrder = Sort.by(Sort.Direction.DESC, "createdAt");
                break;
            default:
                log.warn("[files] Admin {} provided unknown sort value '{}', rejecting", requestorLogin, sort);
                throw new ITParseException("files-admin-list-invalid-sort");
        }

        // Normalise search: treat blank as null to skip the LIKE filter
        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<FileStored> resultPage = fileStoredRepository.findAllBySearchCriteria(searchParam, pageable);

        // Map each FileStored to the response format
        List<FileUploadResponseItf> files = resultPage.getContent().parallelStream().map(f -> {
            FileUploadResponseItf r = new FileUploadResponseItf();
            r.buildFrom(f);
            return r;
        }).toList();

        FileAdminListResponseItf response = new FileAdminListResponseItf();
        response.setTotal(resultPage.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        response.setFiles(files);

        log.debug("[files] Admin list by {} - page={} size={} sort={} search='{}' total={}",
                requestorLogin, page, size, sortUpper, searchParam, resultPage.getTotalElements());

        return response;
    }

    // ================================================================================================================
    // LIST
    // ================================================================================================================

    /**
     * Return the list of files owned by the authenticated user, sorted by creation date descending.
     * @param requestorLogin - login hash of the user
     * @return list of FileStored records
     */
    public List<FileStored> listUserFiles(String requestorLogin) {
        return fileStoredRepository.findByOwnerIdOrderByCreatedAtDesc(requestorLogin);
    }

    // ================================================================================================================
    // Private helpers
    // ================================================================================================================

    /**
     * Resolve a file from either its uniqueName or its 6-character short name alias.
     * When the input is exactly 6 characters long it is treated as a short name.
     * @param fileIdOrShortName - uniqueName or 6-character short name
     * @return the matching FileStored (clone)
     * @throws ITNotFoundException when no file matches the given identifier
     */
    private FileStored resolveFile(String fileIdOrShortName) throws ITNotFoundException {
        // Short names are exactly 6 characters; uniqueNames are always longer
        if (fileIdOrShortName != null && fileIdOrShortName.length() == SHORT_NAME_LENGTH) {
            return fileCache.getFileByShortName(fileIdOrShortName);
        }
        return fileCache.getFile(fileIdOrShortName);
    }

    /**
     * Generate a cryptographically random 6-character short name that does not yet exist in DB.
     * Iterates up to SHORT_NAME_MAX_ATTEMPTS times before giving up.
     * @return a unique 6-character string composed of [a-zA-Z0-9]
     * @throws ITParseException when all attempts produce already-taken names
     */
    private String generateUniqueShortName() throws ITParseException {
        for (int attempt = 0; attempt < SHORT_NAME_MAX_ATTEMPTS; attempt++) {
            String candidate = RandomString.getRandomString(SHORT_NAME_LENGTH);
            // Verify uniqueness in DB before accepting the candidate
            if (!fileStoredRepository.existsByShortName(candidate)) {
                return candidate;
            }
            log.debug("[files] Short name candidate '{}' already taken, retrying ({}/{})",
                    candidate, attempt + 1, SHORT_NAME_MAX_ATTEMPTS);
        }
        log.error("[files] Failed to generate a unique short name after {} attempts", SHORT_NAME_MAX_ATTEMPTS);
        throw new ITParseException("files-short-name-generation-failed");
    }

    /**
     * Verify that the caller is allowed to access (READ) the given file.
     * If a valid access key is provided, it bypasses authentication for CONNECTED and PRIVATE files.
     * PUBLIC: everyone; CONNECTED: authenticated or valid key; PRIVATE: owner/admin or valid key.
     * @param file      - file metadata
     * @param req       - HTTP request
     * @param accessKey - optional key from query parameter (may be null)
     * @throws ITRightException when access is denied
     */
    private void checkFileAccess(FileStored file, HttpServletRequest req, String accessKey) throws ITRightException {
        if (isValidAccessKey(file, accessKey)) return;
        switch (file.getAccessType()) {
            case PUBLIC:
                // No restriction
                break;
            case CONNECTED:
                if (req.getUserPrincipal() == null) {
                    log.warn("[files] Unauthenticated access to CONNECTED file {}", file.getUniqueName());
                    throw new ITRightException("files-access-login-required");
                }
                break;
            case PRIVATE:
                if (req.getUserPrincipal() == null) {
                    log.warn("[files] Unauthenticated try to access to PRIVATE file {}", file.getUniqueName());
                    throw new ITRightException("files-access-login-required");
                }
                String callerLogin = req.getUserPrincipal().getName();
                boolean isAdmin = req.isUserInRole("ROLE_FILES_ADMIN");
                if (!file.getOwnerId().equals(callerLogin) && !isAdmin) {
                    log.warn("[files] User {} denied access to PRIVATE file {} owned by {}",
                            callerLogin, file.getUniqueName(), file.getOwnerId());
                    throw new ITRightException("files-access-private");
                }
                break;
        }
    }

    /**
     * Return true when the provided access key is non-null, non-blank and matches the file's stored key.
     * @param file      - file metadata holding the stored key (may be null)
     * @param accessKey - key provided by the caller (may be null)
     * @return true when access should be granted via the key
     */
    private boolean isValidAccessKey(FileStored file, String accessKey) {
        return accessKey != null && !accessKey.isBlank()
                && file.getAccessKey() != null
                && file.getAccessKey().equals(accessKey);
    }

    /**
     * Generate a cryptographically random 16-character access key that does not yet exist in DB.
     * Iterates up to ACCESS_KEY_MAX_ATTEMPTS times before giving up.
     * @return a unique 16-character string composed of [a-z0-9]
     * @throws ITParseException when all attempts produce already-taken keys
     */
    private String generateUniqueAccessKey() throws ITParseException {
        return RandomString.getRandomString(ACCESS_KEY_LENGTH);
    }

    /**
     * Read the physical bytes of a file and verify its HMAC-SHA256 signature when due.
     * Updates the lastSignatureCheck timestamp in cache after a successful verification.
     * @param file - file metadata
     * @return raw file bytes
     * @throws ITNotFoundException when the physical file is missing on disk
     * @throws ITParseException    when signature verification fails
     */
    private byte[] readAndVerify(FileStored file) throws ITNotFoundException, ITParseException {
        Path filePath = buildFilePath(file.getUniqueName());
        if (!Files.exists(filePath)) {
            log.debug("[files] Physical file missing: {} id={}", file.getUniqueName(), file.getId());
            throw new ITNotFoundException("files-physical-missing");
        }

        byte[] content;
        try {
            content = Files.readAllBytes(filePath);
        } catch (Exception e) {
            log.error("[files] Failed to read file {} id={}", file.getUniqueName(), file.getId(), e);
            throw new ITNotFoundException("files-server-side-read-error");
        }

        // Perform signature check if not disabled and interval has elapsed
        if (!file.isNoSignatureCheck()) {
            long now = Now.NowUtcMs();
            boolean checkDue = filesConfig.getSignatureCheckIntervalMs() == 0
                    || (now - file.getLastSignatureCheck()) > filesConfig.getSignatureCheckIntervalMs();
            if (checkDue) {
                String computed = computeHmacSignatureSilent(content);
                if (!computed.equals(file.getSignature())) {
                    log.error("[files] Integrity failure for file {} - expected={} computed={}",
                            file.getUniqueName(), file.getSignature(), computed);
                    auditIntegration.auditLog(ModuleCatalog.Modules.FILES, ActionCatalog.getActionName(ActionCatalog.Actions.FILES_INTEGRITY),
                            "system", "File integrity check failed: id={0} expected={1} got={2}",
                            new String[]{file.getUniqueName(), file.getSignature(), computed});
                    // Track signature error in metrics counter
                    com.disk91.files.Files.incSignatureErrors();
                    throw new ITParseException("file-integrity-check-failed");
                }
                // Record the successful check timestamp in the cache (in-memory only)
                fileCache.updateLastSignatureCheck(file.getUniqueName());
            }
        }
        return content;
    }

    /**
     * Build the absolute Path for a file stored on disk.
     * Structure: {root}/{id[0]}/{id[1]}/{uniqueName}
     * @param uniqueName - physical filename
     * @return absolute Path
     */
    private Path buildFilePath(String uniqueName) {
        return Paths.get(filesConfig.getStorageRootPath())
                .resolve(String.valueOf(uniqueName.charAt(0)))
                .resolve(String.valueOf(uniqueName.charAt(1)))
                .resolve(uniqueName);
    }

    /**
     * Detect the MIME type from raw bytes using URLConnection content sniffing.
     * Falls back to extension-based heuristics when sniffing is inconclusive.
     * @param bytes    - file content
     * @param filename - original filename (for extension fallback)
     * @return MIME type string
     */
    private String detectMimeType(byte[] bytes, String filename) {
        try {
            String sniffed = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(bytes));
            if (sniffed != null) return sniffed;
        } catch (Exception ignored) {}
        return guessMimeFromExtension(filename);
    }

    /**
     * Guess MIME type from filename extension as a fallback.
     * @param filename - original filename
     * @return best-effort MIME type string
     */
    private String guessMimeFromExtension(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".gif"))  return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg"))  return "image/svg+xml";
        if (lower.endsWith(".pdf"))  return "application/pdf";
        if (lower.endsWith(".txt"))  return "text/plain";
        if (lower.endsWith(".md"))  return "text/plain";
        if (lower.endsWith(".log"))  return "text/plain";
        if (lower.endsWith(".yaml"))  return "text/plain";
        if (lower.endsWith(".csv"))  return "text/csv";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".xml"))  return "text/xml";
        if (lower.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }

    /**
     * Map a MIME type string to a FileMimeCategory enum value.
     * @param mimeType - detected MIME type
     * @return corresponding FileMimeCategory
     */
    private FileMimeCategory categorizeMime(String mimeType) {
        if (mimeType == null) return FileMimeCategory.GENERIC;
        if (mimeType.startsWith("image/")) return FileMimeCategory.IMAGE;
        if (mimeType.equals("application/pdf")) return FileMimeCategory.PDF;
        if (mimeType.startsWith("text/") || mimeType.equals("application/json")) return FileMimeCategory.TEXT;
        return FileMimeCategory.GENERIC;
    }

    /**
     * Extract the lowercase file extension from an original filename.
     * Returns "bin" when no extension is found.
     * @param filename - original filename
     * @return extension without dot
     */
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * Proportionally scale a BufferedImage so its largest dimension equals maxDim.
     * @param src    - source image
     * @param maxDim - maximum allowed dimension in pixels
     * @return scaled BufferedImage
     */
    private BufferedImage scaleImage(BufferedImage src, int maxDim) {
        int w = src.getWidth();
        int h = src.getHeight();
        int largestDim = Math.max(w, h);
        if (largestDim <= maxDim) return src;

        double ratio = (double) maxDim / largestDim;
        int newW = (int) Math.round(w * ratio);
        int newH = (int) Math.round(h * ratio);

        // Use high-quality bicubic scaling
        BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, newW, newH, null);
        g.dispose();
        return scaled;
    }

    /**
     * Encode a BufferedImage to a byte array in the requested format.
     * Falls back to PNG if the requested format is not supported by ImageIO.
     * @param img    - source image
     * @param format - ImageIO format name (e.g. "jpeg", "png")
     * @return encoded bytes
     */
    private byte[] imageToBytes(BufferedImage img, String format) throws Exception {
        // For JPEG, convert ARGB to RGB to avoid alpha-channel encoding issues
        if ("jpeg".equalsIgnoreCase(format) && img.getType() == BufferedImage.TYPE_INT_ARGB) {
            BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
            img = rgb;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean written = ImageIO.write(img, format, baos);
        if (!written) {
            // Fallback to PNG when the requested format writer is unavailable
            baos.reset();
            ImageIO.write(img, "png", baos);
        }
        return baos.toByteArray();
    }

    /**
     * Compute the HMAC-SHA256 integrity signature of a byte array.
     * The static SIGNATURE_IV is prepended to the content before the HMAC computation
     * to ensure domain separation from other potential HMAC uses of the same key.
     * Key material: filesConfig.getSignatureSecret() (UTF-8 bytes).
     * @param content - file (or thumbnail) bytes to sign
     * @return lowercase hex-encoded HMAC-SHA256 digest
     * @throws Exception on cryptographic error
     */
    private String computeHmacSignature(byte[] content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
                filesConfig.getSignatureSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        // Prepend the static IV for domain separation
        mac.update(SIGNATURE_IV.getBytes(StandardCharsets.UTF_8));
        mac.update(content);
        return HexCodingTools.bytesToHex(mac.doFinal());
    }

    /**
     * Silent variant of computeHmacSignature that returns an empty string on error.
     * Used internally during signature verification where an exception would be escalated separately.
     * @param content - bytes to sign
     * @return hex signature or empty string on failure
     */
    private String computeHmacSignatureSilent(byte[] content) {
        try {
            return computeHmacSignature(content);
        } catch (Exception e) {
            log.error("[files] HMAC computation error", e);
            return "";
        }
    }

    /**
     * Return true when the description has changed between the stored and new values.
     * Treats null and empty string as equivalent.
     * @param stored - current stored description
     * @param updated - new description from the update request
     * @return true when the value has effectively changed
     */
    private boolean descriptionChanged(String stored, String updated) {
        String s = (stored == null) ? "" : stored;
        String u = (updated == null) ? "" : updated;
        return !s.equals(u);
    }

    // ================================================================================================================
    // Inner class: download result wrapper
    // ================================================================================================================

    /**
     * Lightweight wrapper returned by download service methods,
     * holding the file metadata and raw binary content together.
     */
    public static class FileDownloadData {
        private final FileStored metadata;
        private final byte[] content;

        public FileDownloadData(FileStored metadata, byte[] content) {
            this.metadata = metadata;
            this.content = content;
        }

        public FileStored getMetadata() { return metadata; }
        public byte[] getContent()      { return content; }
    }

}

