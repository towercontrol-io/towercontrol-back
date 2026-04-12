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
package com.disk91.files.pdb.repositories;

import com.disk91.files.pdb.entities.FileStored;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileStoredRepository extends JpaRepository<FileStored, String> {

    /**
     * Find all files owned by a given user, sorted by creation date descending.
     * Used by the list-owned-files endpoint.
     * @param ownerId - login hash of the owner
     * @return ordered list of FileStored records
     */
    List<FileStored> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    /**
     * Count the number of files owned by a given user.
     * Used for per-user file-count quota enforcement.
     * @param ownerId - login hash of the owner
     * @return total number of files owned by the user
     */
    long countByOwnerId(String ownerId);

    /**
     * Compute the total storage size (bytes) consumed by all files owned by a given user.
     * Used for per-user cumulative-size quota enforcement.
     * Returns 0 when the user has no files yet.
     * @param ownerId - login hash of the owner
     * @return sum of file sizes in bytes
     */
    @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileStored f WHERE f.ownerId = :ownerId")
    long sumSizeByOwnerId(@Param("ownerId") String ownerId);

    /**
     * Find a file by its physical unique name on disk.
     * @param uniqueName - generated unique filename
     * @return matching FileStored record if it exists
     */
    Optional<FileStored> findByUniqueName(String uniqueName);

    /**
     * Find a file by its short name alias.
     * @param shortName - 6-character short name alias
     * @return matching FileStored record if it exists
     */
    Optional<FileStored> findByShortName(String shortName);

    /**
     * Check whether a given short name is already taken.
     * Used during short name generation to guarantee uniqueness before persisting.
     * @param shortName - candidate 6-character short name
     * @return true when a file with that short name already exists
     */
    boolean existsByShortName(String shortName);

    /**
     * Check whether a given access key is already taken.
     * Used during access key generation to guarantee uniqueness before persisting.
     * @param accessKey - candidate 16-character access key
     * @return true when a file with that access key already exists
     */
    boolean existsByAccessKey(String accessKey);

    /**
     * Called on every successful download without reloading the whole entity.
     * @param fileId - UUID of the file
     */
    @Modifying
    @Transactional
    @Query("UPDATE FileStored f SET f.accessCount = f.accessCount + 1 WHERE f.id = :fileId")
    void incrementAccessCount(@Param("fileId") String fileId);

    /**
     * Paginated search across all files with optional case-insensitive LIKE filter.
     * When search is null or blank, all files are returned.
     * The filter is applied on ownerId, originalName and description fields.
     * Sorting is controlled by the Pageable argument (CREATED or ACCESS).
     * @param search   - optional search string for LIKE filtering (may be null)
     * @param pageable - pagination and sort specification
     * @return a page of matching FileStored records
     */
    @Query("SELECT f FROM FileStored f " +
            "WHERE (:search IS NULL OR :search = '' OR " +
            "LOWER(f.ownerId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(f.originalName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(f.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))" +
            ")")
    Page<FileStored> findAllBySearchCriteria(@Param("search") String search, Pageable pageable);

}

