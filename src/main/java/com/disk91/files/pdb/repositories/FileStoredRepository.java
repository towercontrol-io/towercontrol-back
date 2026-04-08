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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileStoredRepository extends CrudRepository<FileStored, String> {

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

}

