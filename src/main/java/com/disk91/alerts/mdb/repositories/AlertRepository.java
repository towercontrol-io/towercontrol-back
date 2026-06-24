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
package com.disk91.alerts.mdb.repositories;

import com.disk91.alerts.mdb.entities.Alert;
import com.disk91.alerts.mdb.entities.sub.AlertState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends MongoRepository<Alert, String> {

    /**
     * Find the most recent alert instance for a given stable business identifier.
     * @param alertId - stable business identifier (already instantiated)
     * @return the Alert or null
     */
    Alert findOneAlertByAlertId(String alertId);

    /**
     * Find all alert instances in the given lifecycle state.
     * Used by the async processor to pick up PENDING and ENDING alerts.
     * @param state - the AlertState to filter on
     * @return list of matching Alert instances
     */
    List<Alert> findAlertsByState(AlertState state);

    /**
     * Find all alert instances whose state is in the given list, ordered by requestMs ascending (oldest first).
     * Used at startup to re-enqueue PENDING and ENDING alerts together in submission order.
     * @param states - list of AlertState values to include
     * @return list of matching Alert instances sorted oldest-first
     */
    List<Alert> findAlertsByStateInOrderByRequestMsAsc(List<AlertState> states);

    /**
     * Find RUNNING alerts whose expiration time has passed and whose expiration is set (> 0).
     * Used by the async processor to auto-close expired alerts.
     * @param state         - expected to be AlertState.RUNNING
     * @param nowMs         - current time in milliseconds; alerts with expirationMs <= nowMs are returned
     * @return list of expired RUNNING alerts
     */
    @Query("{ 'state': ?0, 'expirationMs': { $gt: 0, $lte: ?1 } }")
    List<Alert> findExpiredRunningAlerts(AlertState state, long nowMs);

    /**
     * Find RUNNING or PENDING alert instances for a given alertId.
     * Used to detect duplicate active alerts before creating a new one.
     * @param alertId - stable business identifier
     * @param states  - list of states to check (typically [PENDING, RUNNING])
     * @return list of active Alert instances for this alertId
     */
    @Query("{ 'alertId': ?0, 'state': { $in: ?1 } }")
    List<Alert> findActiveAlertsByAlertId(String alertId, List<AlertState> states);

    /**
     * Delete all alerts in ENDED state whose requestMs is before the given cutoff.
     * Used by the scheduled purge to reclaim storage.
     * @param state     - expected to be AlertState.ENDED
     * @param cutoffMs  - any alert with requestMs before this value is deleted
     */
    void deleteByStateAndRequestMsBefore(AlertState state, long cutoffMs);

    /**
     * Return a paginated list of alerts where the given user appears in the sent array.
     * Results are ordered by requestMs descending via the Pageable sort.
     * @param userLogin - user login to match in sent.userLogin
     * @param pageable  - pagination and sort descriptor
     * @return page of matching Alert instances
     */
    @Query("{ 'sent.userLogin': ?0 }")
    Page<Alert> findByUserInSent(String userLogin, Pageable pageable);

    /**
     * Return a paginated list of alerts where the given user appears in the sent array,
     * filtered to a specific set of alertTemplateIds.
     * @param userLogin   - user login to match in sent.userLogin
     * @param templateIds - list of alertTemplateId values to filter on
     * @param pageable    - pagination and sort descriptor
     * @return page of matching Alert instances
     */
    @Query("{ 'sent.userLogin': ?0, 'alertTemplateId': { $in: ?1 } }")
    Page<Alert> findByUserInSentAndTemplateIdIn(String userLogin, List<String> templateIds, Pageable pageable);
}
