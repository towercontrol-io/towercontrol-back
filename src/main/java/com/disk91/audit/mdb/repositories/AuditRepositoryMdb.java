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
package com.disk91.audit.mdb.repositories;

import com.disk91.audit.mdb.entities.AuditMdb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRepositoryMdb extends MongoRepository<AuditMdb, String> {

    /**
     * Search audit entries using a native MongoDB query with an optional free-text filter
     * applied as a case-insensitive regex on service, action and owner simultaneously (OR condition),
     * combined with a date range. Results are ordered via Pageable (sort by actionMs DESC expected).
     * Pass an empty string for search to match all entries.
     * Pass 0 for startMs to skip the lower bound, Long.MAX_VALUE for endMs to skip the upper bound.
     * @param search  - regex pattern applied on service/action/owner (OR), empty string = match all
     * @param startMs - lower bound on actionMs inclusive (0 = match all)
     * @param endMs   - upper bound on actionMs inclusive (Long.MAX_VALUE = match all)
     * @param pageable - pagination and sort parameters
     * @return Page of matching AuditMdb entries
     */
    @Query("{ '$and': [ " +
            "  { '$or': [ " +
            "    { 'service': { '$regex': ?0, '$options': 'i' } }, " +
            "    { 'action':  { '$regex': ?0, '$options': 'i' } }, " +
            "    { 'owner':   { '$regex': ?0, '$options': 'i' } }  " +
            "  ] }, " +
            "  { 'actionMs': { '$gte': ?1, '$lte': ?2 } } " +
            "] }")
    Page<AuditMdb> searchAuditLogs(String search, long startMs, long endMs, Pageable pageable);

}
