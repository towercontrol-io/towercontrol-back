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
package com.disk91.files.api;

import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITOverQuotaException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.files.api.interfaces.FileUpdateBody;
import com.disk91.files.api.interfaces.FileUploadResponseItf;
import com.disk91.files.pdb.entities.FileStored;
import com.disk91.files.services.FileService;
import com.disk91.files.services.FileService.FileDownloadData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "Files management", description = "File upload, download and metadata management")
@CrossOrigin
@RequestMapping(value = "/files/1.0")
@RestController
public class ApiFiles {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected FileService fileService;

    // ================================================================================================================
    // UPLOAD
    // ================================================================================================================

    /**
     * Upload a new file with multipart/form-data.
     * Enforces quotas, resizes images, generates thumbnails and persists the record.
     * PRIVATE files require only ROLE_LOGIN_COMPLETE.
     * PUBLIC and CONNECTED files additionally require ROLE_FILE_WRITE.
     * An optional 6-character short name can be requested via withShortName=true.
     */
    @Operation(
            summary = "Upload a file",
            description = "Upload a file using multipart/form-data. " +
                    "The 'file' part carries the binary content, 'accessType' (PUBLIC|CONNECTED|PRIVATE) is mandatory. " +
                    "'filename', 'description' and 'withShortName' are optional. " +
                    "When withShortName=true, a unique 6-character short name is generated and returned. " +
                    "Images are automatically resized and a thumbnail is generated. " +
                    "Creating PUBLIC or CONNECTED files requires ROLE_FILE_WRITE.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "File uploaded",
                            content = @Content(schema = @Schema(implementation = FileUploadResponseItf.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input or format error",
                            content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Insufficient role",
                            content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "429", description = "Quota exceeded",
                            content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.POST)
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> postUploadFile(
            HttpServletRequest request,
            @RequestPart("file") MultipartFile file,
            @RequestParam("accessType") String accessType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "filename", required = false) String filename,
            @RequestParam(value = "withShortName", required = false, defaultValue = "false") boolean withShortName
    ) {
        try {
            FileStored stored = fileService.uploadFile(
                    request.getUserPrincipal().getName(),
                    file,
                    accessType,
                    description,
                    filename,
                    withShortName,
                    request
            );
            FileUploadResponseItf response = new FileUploadResponseItf();
            response.buildFrom(stored);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ITParseException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ITRightException e) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        } catch (ITOverQuotaException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    // ================================================================================================================
    // DOWNLOAD
    // ================================================================================================================

    /**
     * Download the binary content of a file.
     * Access is controlled by the file's accessType.
     * PUBLIC files are accessible without authentication.
     * The accessCount counter is incremented on each successful download.
     * The fileId path variable accepts either the uniqueName or a 6-character short name.
     */
    @Operation(
            summary = "Download a file",
            description = "Return the raw binary content of a file with appropriate Content-Type and Content-Disposition headers. " +
                    "PUBLIC files are accessible without authentication. " +
                    "CONNECTED files require authentication. " +
                    "PRIVATE files are accessible only by the owner or ROLE_FILE_ADMIN. " +
                    "The accessCount counter is incremented on success. " +
                    "The fileId path variable accepts either the uniqueName or a 6-character short name.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "File content",
                            content = @Content(schema = @Schema(type = "string", format = "binary"))),
                    @ApiResponse(responseCode = "403", description = "Access denied or file not found",
                            content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "Integrity check failed",
                            content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(value = "/{fileId}/full",
            produces = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE },
            method = RequestMethod.GET)
    // No @PreAuthorize - access control is enforced in the service based on accessType
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getDownloadFile(
            HttpServletRequest request,
            @Parameter(description = "uniqueName or 6-character short name of the file to download")
            @PathVariable String fileId
    ) {
        try {
            FileDownloadData data = fileService.downloadFile(fileId, request);
            HttpHeaders headers = buildDownloadHeaders(data.getMetadata().getMimeType(),
                    data.getMetadata().getOriginalName());
            return new ResponseEntity<>(data.getContent(), headers, HttpStatus.OK);
        } catch (ITNotFoundException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.FORBIDDEN);
        } catch (ITRightException e) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        } catch (ITParseException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    // ================================================================================================================
    // THUMBNAIL
    // ================================================================================================================

    /**
     * Download the thumbnail of an image file.
     * Same access rules as the original file. accessCount is NOT incremented.
     */
    @Operation(
            summary = "Download a file thumbnail",
            description = "Return the thumbnail image for the given file. " +
                    "Returns 403 when the file has no thumbnail (non-image) or does not exist. " +
                    "Same access rules as the original file. The accessCount counter is NOT incremented.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Thumbnail image",
                            content = @Content(schema = @Schema(type = "string", format = "binary"))),
                    @ApiResponse(responseCode = "403", description = "Access denied or no thumbnail",
                            content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(value = "/{fileId}/thumbnail",
            produces = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE },
            method = RequestMethod.GET)
    // No @PreAuthorize - service enforces access control
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getDownloadThumbnail(
            HttpServletRequest request,
            @Parameter(description = "UUID of the file whose thumbnail to download")
            @PathVariable String fileId
    ) {
        try {
            FileDownloadData data = fileService.downloadThumbnail(fileId, request);
            HttpHeaders headers = buildDownloadHeaders(data.getMetadata().getMimeType(),
                    "thumb-" + data.getMetadata().getOriginalName());
            return new ResponseEntity<>(data.getContent(), headers, HttpStatus.OK);
        } catch (ITNotFoundException | ITRightException e) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        } catch (ITParseException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    // ================================================================================================================
    // INFO
    // ================================================================================================================

    /**
     * Return the metadata of a file without serving the binary content.
     * Same access rules as the download endpoint.
     */
    @Operation(
            summary = "Get file metadata",
            description = "Return the metadata record of a file without its binary content. " +
                    "Access rules are the same as for the download endpoint.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "File metadata",
                            content = @Content(schema = @Schema(implementation = FileUploadResponseItf.class))),
                    @ApiResponse(responseCode = "403", description = "Access denied or not found",
                            content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(value = "/{fileId}/info", produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.GET)
    // No @PreAuthorize - service enforces access control
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getFileInfo(
            HttpServletRequest request,
            @Parameter(description = "UUID of the file")
            @PathVariable String fileId
    ) {
        try {
            FileStored stored = fileService.getFileInfo(fileId, request);
            FileUploadResponseItf response = new FileUploadResponseItf();
            response.buildFrom(stored);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ITNotFoundException | ITRightException e) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        }
    }

    // ================================================================================================================
    // UPDATE
    // ================================================================================================================

    /**
     * Update the description, access type and/or short name of a file.
     * Only the owner or a ROLE_FILE_ADMIN can perform this operation.
     * Set withShortName=true to generate a short name, false to remove it, omit to leave unchanged.
     * The fileId path variable can be either a uniqueName or a 6-character short name.
     */
    @Operation(
            summary = "Update file metadata",
            description = "Update the description, accessType and/or shortName of an existing file. " +
                    "Only the owner or ROLE_FILE_ADMIN can perform this operation. " +
                    "Upgrading to PUBLIC or CONNECTED additionally requires ROLE_FILE_WRITE. " +
                    "Set withShortName=true to generate a short name (if not already present), " +
                    "false to remove the current short name, omit the field to leave it unchanged. " +
                    "The fileId path variable accepts either the uniqueName or a 6-character short name.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Updated file metadata",
                            content = @Content(schema = @Schema(implementation = FileUploadResponseItf.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input",
                            content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Access denied",
                            content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(value = "/{fileId}", produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.PUT)
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> putUpdateFile(
            HttpServletRequest request,
            @Parameter(description = "uniqueName or 6-character short name of the file to update")
            @PathVariable String fileId,
            @RequestBody(required = true) FileUpdateBody body
    ) {
        try {
            FileStored updated = fileService.updateFileMeta(
                    request.getUserPrincipal().getName(), fileId, body, request);
            FileUploadResponseItf response = new FileUploadResponseItf();
            response.buildFrom(updated);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ITNotFoundException | ITRightException e) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        } catch (ITParseException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    // ================================================================================================================
    // DELETE
    // ================================================================================================================

    /**
     * Delete a file and its physical content from disk.
     * Only the owner or ROLE_FILE_ADMIN can delete a file.
     */
    @Operation(
            summary = "Delete a file",
            description = "Delete the database record and the physical file (and its thumbnail) from disk. " +
                    "Only the owner or ROLE_FILE_ADMIN can delete a file.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "File deleted"),
                    @ApiResponse(responseCode = "403", description = "Access denied or not found",
                            content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(value = "/{fileId}", produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.DELETE)
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> deleteFile(
            HttpServletRequest request,
            @Parameter(description = "UUID of the file to delete")
            @PathVariable String fileId
    ) {
        try {
            fileService.deleteFile(request.getUserPrincipal().getName(), fileId, request);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (ITNotFoundException | ITRightException e) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        }
    }

    // ================================================================================================================
    // LIST
    // ================================================================================================================

    /**
     * Return the list of files owned by the authenticated user, sorted by creation date descending.
     */
    @Operation(
            summary = "List owned files",
            description = "Return the list of files owned by the authenticated user, sorted by creation date descending.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of files",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = FileUploadResponseItf.class))))
            }
    )
    @RequestMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.GET)
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getListFiles(HttpServletRequest request) {
        List<FileStored> files = fileService.listUserFiles(request.getUserPrincipal().getName());
        if (files.isEmpty()) return new ResponseEntity<>(new ArrayList<>(),HttpStatus.OK);

        List<FileUploadResponseItf> response = new ArrayList<>();
        for (FileStored f : files) {
            FileUploadResponseItf r = new FileUploadResponseItf();
            r.buildFrom(f);
            response.add(r);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // ================================================================================================================
    // Private helpers
    // ================================================================================================================

    /**
     * Build HTTP headers for a file download response.
     * Sets Content-Type and Content-Disposition (attachment with original filename).
     * @param mimeType     - MIME type for Content-Type header
     * @param originalName - original filename for Content-Disposition
     * @return populated HttpHeaders
     */
    private HttpHeaders buildDownloadHeaders(String mimeType, String originalName) {
        HttpHeaders headers = new HttpHeaders();
        try {
            headers.setContentType(MediaType.parseMediaType(mimeType));
        } catch (Exception e) {
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(originalName != null ? originalName : "file").build()
        );
        return headers;
    }
}

