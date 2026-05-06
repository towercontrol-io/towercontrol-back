/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2025.
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
package com.disk91.audit.pdb.repositories;

import com.disk91.audit.pdb.entities.Audit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditRepository extends JpaRepository<Audit, UUID> {

    /**
     * Search audit entries with an optional free-text filter applied as a case-insensitive
     * partial match on service, action and owner simultaneously (OR condition).
     * Date range filters and pagination are also supported. Results are ordered by actionMs descending.
     * A null search value means no text filter is applied.
     * Zero values for startMs / endMs mean no date bound.
     * @param search  - free-text filter applied on service/action/owner (OR), null for no filter
     * @param startMs - lower bound on actionMs (0 = no bound)
     * @param endMs   - upper bound on actionMs (0 = no bound)
     * @param pageable - pagination parameters
     * @return page of matching Audit entries ordered from most recent to oldest
     */
    @Query("SELECT a FROM Audit a WHERE " +
            "(:search IS NULL OR LOWER(a.service) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "                 OR LOWER(a.action)  LIKE LOWER(CONCAT('%', :search, '%')) " +
            "                 OR LOWER(a.owner)   LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:startMs = 0 OR a.actionMs >= :startMs) AND " +
            "(:endMs = 0 OR a.actionMs <= :endMs) " +
            "ORDER BY a.actionMs DESC")
    Page<Audit> searchAuditLogs(
            @Param("search") String search,
            @Param("startMs") long startMs,
            @Param("endMs") long endMs,
            Pageable pageable
    );

}
