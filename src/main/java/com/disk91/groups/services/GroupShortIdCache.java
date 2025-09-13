/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2024.
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
package com.disk91.groups.services;

import com.disk91.common.tools.Now;
import com.disk91.common.tools.ObjectCache;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.groups.config.GroupsConfig;
import com.disk91.groups.mdb.entities.Group;
import com.disk91.groups.mdb.repositories.GroupRepository;
import com.disk91.groups.tools.GroupList;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.services.UserCache;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class GroupShortIdCache {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Group Cache Service is caching the Group Information. It may be instantiated in all the instances
     * and multiple cache should collaborate in a cluster
     */

    @Autowired
    protected GroupsConfig groupsConfig;

    @Autowired
    protected GroupRepository groupRepository;

    @Autowired
    protected MeterRegistry meterRegistry;

    @Autowired
    protected UserCache userCache;
    

    // ================================================================================================================
    // CACHE SERVICE
    // ================================================================================================================



    private ObjectCache<String, GroupList> groupCache;

    protected boolean serviceEnable = false;

    @PostConstruct
    private void initGroupsCache() {
        log.info("[groups] initGroupsShortIdCache");
        if ( groupsConfig.getGroupsCacheMaxSize() > 0 ) {
            this.groupCache = new ObjectCache<String,GroupList>(
                    "GroupsROCache",
                    groupsConfig.getGroupsCacheMaxSize(),
                    groupsConfig.getGroupsCacheExpiration()*1000
            ) {
                @Override
                synchronized public void onCacheRemoval(String key, GroupList obj, boolean batch, boolean last) {
                    // read only cache, do nothing
                }
                @Override
                public void bulkCacheUpdate(List<GroupList> objects) {
                    // read only cache, do nothing
                }
            };
        }

        this.serviceEnable = true;

        Gauge.builder("common.service.groupsshort.cache_total_time", this.groupCache.getTotalCacheTime())
                .description("[Groups short] total time cache execution")
                .register(meterRegistry);
        Gauge.builder("common.service.groupsshort.cache_total", this.groupCache.getTotalCacheTry())
                .description("[Groups short] total cache try")
                .register(meterRegistry);
        Gauge.builder("common.service.groupsshort.cache_miss", this.groupCache.getCacheMissStat())
                .description("[Groups short] total cache miss")
                .register(meterRegistry);
    }

    @PreDestroy
    public void destroy() {
        log.info("[groups] GroupShortIdCache stopping");
        this.serviceEnable = false;
        if ( groupsConfig.getGroupsCacheMaxSize() > 0 ) {
            groupCache.deleteCache();
        }
        log.info("[groups] GroupShortIdCache stopped");
    }

    @Scheduled(fixedRateString = "${groups.cache.log.period:PT24H}", initialDelay = 3601_000)
    protected void groupCacheStatus() {
        try {
            Duration duration = Duration.parse(groupsConfig.getGroupsCacheLogPeriod());
            if (duration.toMillis() >= Now.ONE_FULL_DAY ) return;
        } catch (Exception ignored) {}
        if ( ! this.serviceEnable || groupsConfig.getGroupsCacheMaxSize() == 0 ) return;
        this.groupCache.log();
    }

    // ================================================================================================================
    // Cache access
    // ================================================================================================================

    /**
     * Get the group from the cache or from the database if not in cache
     * Build the generic group from the user Objet when a User vitual group is requested
     * User Virtual group start with the prefix "user_"
     * @param groupId - groupId to be retrieved
     * @return the group object
     * @throws ITNotFoundException if not found
     */
    public GroupList getGroup(String groupId) throws ITNotFoundException {
        throw new ITNotFoundException();
    }

    /**
     * Remove a group from the local cache if exists (this is when the user has been updated somewhere else
     * @param groupId - groupId to be removed
     * @return
     */
    public void flushGroup(String groupId) {
    }

    /**
     * Save the Group structure after an update. The cache is flushed for this user
     * This is not protected against concurrent access on multiple cache service instance
     * @param u Group to be saved
     */
    public void saveGroup(Group u) {
    }


}
