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
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.files.api.interfaces.FileAdminListResponseItf;
import com.disk91.files.api.interfaces.FileUpdateBody;
import com.disk91.files.api.interfaces.FileUploadResponseItf;
import com.disk91.files.pdb.entities.FileStored;
import com.disk91.files.services.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Files admin API", description = "Admin file management endpoints, requires ROLE_FILES_ADMIN")
@CrossOrigin
@RequestMapping(value = "/files/1.0/admin")
@RestController
public class ApiFilesAdmin {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected FileService fileService;

    // ================================================================================================================
    // ADMIN LIST
    // ================================================================================================================

    /**
     * Return a paginated, searchable and sortable list of all files in the system.
     * Accessible only to ROLE_FILES_ADMIN.
     * The optional search string performs a case-insensitive LIKE on ownerId, originalName and description.
     * Sort order is either CREATED (most recent first, default) or ACCESS (most accessed first).
     * Page size is clamped to [1, 250]; default is 50.
     */
    @Operation(
            summary = "Paginated admin file search",
            description = "Return a paginated list of all files in the system. " +
                    "The optional 'search' query parameter applies a case-insensitive LIKE filter on the " +
                    "owner login, original filename and description fields. " +
                    "The 'sort' parameter accepts CREATED (default, newest first) or ACCESS (most accessed first). " +
                    "Page size defaults to 50 and is capped at 250. " +
                    "Requires ROLE_FILES_ADMIN.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Paginated list of files",
                            content = @Content(schema = @Schema(implementation = FileAdminListResponseItf.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid parameter",
                            content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Insufficient role",
                            content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and hasRole('ROLE_FILES_ADMIN')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getAdminListFiles(
            HttpServletRequest request,
            @Parameter(description = "Page index, 0-based (default: 0)")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size, 1-250 (default: 50)")
            @RequestParam(value = "size", defaultValue = "50") int size,
            @Parameter(description = "Sort order: CREATED (newest first, default) or ACCESS (most accessed first)")
            @RequestParam(value = "sort", defaultValue = "CREATED") String sort,
            @Parameter(description = "Optional case-insensitive search string applied on owner, filename and description")
            @RequestParam(value = "search", required = false) String search
    ) {
        try {
            FileAdminListResponseItf result = fileService.adminListFiles(
                    request.getUserPrincipal().getName(),
                    page, size, sort, search
            );
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (ITParseException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    // ================================================================================================================
    // ADMIN UPDATE
    // ================================================================================================================

    /**
     * Update the metadata (description, accessType, shortName, accessKey) of any file in the system.
     * An admin is not restricted to files they own; all ownership checks are bypassed.
     * The fileId path variable accepts either the uniqueName or a 6-character short name.
     */
    @Operation(
            summary = "Admin update file metadata",
            description = "Update any file's description, accessType, shortName and/or accessKey. " +
                    "The admin bypasses ownership checks. " +
                    "Set withShortName=true to generate a short name, false to remove it, omit to leave unchanged. " +
                    "Set withAccessKey=true to generate/regenerate a 16-character access key, false to remove it. " +
                    "Requires ROLE_FILES_ADMIN.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Updated file metadata",
                            content = @Content(schema = @Schema(implementation = FileUploadResponseItf.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input",
                            content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Access denied or not found",
                            content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(value = "/{fileId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT)
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and hasRole('ROLE_FILES_ADMIN')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> putAdminUpdateFile(
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
    // ADMIN DELETE
    // ================================================================================================================

    /**
     * Delete any file in the system, regardless of ownership.
     * Removes the database record and the physical file (and its thumbnail) from disk.
     * The fileId path variable accepts either the uniqueName or a 6-character short name.
     */
    @Operation(
            summary = "Admin delete a file",
            description = "Delete any file's database record and physical content from disk, regardless of ownership. " +
                    "Requires ROLE_FILES_ADMIN.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "File deleted",
                            content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Access denied or not found",
                            content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(value = "/{fileId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE)
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and hasRole('ROLE_FILES_ADMIN')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> deleteAdminFile(
            HttpServletRequest request,
            @Parameter(description = "uniqueName or 6-character short name of the file to delete")
            @PathVariable String fileId
    ) {
        try {
            fileService.deleteFile(request.getUserPrincipal().getName(), fileId, request);
            return new ResponseEntity<>(ActionResult.OK("files-file-deleted"), HttpStatus.OK);
        } catch (ITNotFoundException | ITRightException e) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        }
    }
}

