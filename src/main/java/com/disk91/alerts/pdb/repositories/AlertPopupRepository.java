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
package com.disk91.alerts.pdb.repositories;

import com.disk91.alerts.pdb.entities.AlertPopup;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * AlertPopupRepository - JPA repository for AlertPopup psql entities.
 */
public interface AlertPopupRepository extends JpaRepository<AlertPopup, UUID> {

    /**
     * Returns recent or unread popups for a user, ordered newest first.
     * Covers: all unread (viewedMs = 0) AND read popups within the given time window.
     * @param userLogin - target user
     * @param sinceMs - lower bound timestamp (exclusive)
     * @param pageable - used to cap results (max 10)
     * @return ordered list of matching popups
     */
    @Query("SELECT p FROM AlertPopup p WHERE p.userLogin = :userLogin AND (p.timeMs > :sinceMs OR p.viewedMs = 0) ORDER BY p.timeMs DESC")
    List<AlertPopup> findRecentOrUnread(
            @Param("userLogin") String userLogin,
            @Param("sinceMs") long sinceMs,
            Pageable pageable
    );

    /**
     * Counts unread (not yet viewed) popups for a given user.
     * @param userLogin - target user
     * @return number of unread popup entries
     */
    long countByUserLoginAndViewedMs(String userLogin, long viewedMs);

    /**
     * Marks all unread popups for a user as viewed at the given timestamp.
     * @param userLogin - target user
     * @param nowMs - current timestamp to record as viewed time
     * @return number of rows updated
     */
    @Modifying
    @Transactional
    @Query("UPDATE AlertPopup p SET p.viewedMs = :nowMs WHERE p.userLogin = :userLogin AND p.viewedMs = 0")
    int markAllViewedByUser(@Param("userLogin") String userLogin, @Param("nowMs") long nowMs);

    /**
     * Returns popups newer than the given timestamp for a user, ordered oldest first.
     * Used by the toaster polling mechanism to detect new arrivals since last check.
     * Does not affect the viewed/unread state.
     * @param userLogin - target user
     * @param sinceMs - lower bound timestamp (exclusive)
     * @return list of new popups ordered by timeMs ascending
     */
    List<AlertPopup> findByUserLoginAndTimeMsGreaterThanOrderByTimeMsAsc(String userLogin, long sinceMs);

    /**
     * Deletes all popup entries older than the given cutoff timestamp.
     * @param cutoffMs - entries with timeMs before this value are removed
     */
    @Modifying
    @Transactional
    void deleteByTimeMsBefore(long cutoffMs);
}
