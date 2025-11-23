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
public class GroupsCache {

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

    private ObjectCache<String, Group> groupCache;

    protected boolean serviceEnable = false;

    @PostConstruct
    private void initGroupsCache() {
        log.info("[groups] initGroupsCache");
        if ( groupsConfig.getGroupsCacheMaxSize() > 0 ) {
            this.groupCache = new ObjectCache<String, Group>(
                    "GroupsROCache",
                    groupsConfig.getGroupsCacheMaxSize(),
                    groupsConfig.getGroupsCacheExpiration()*1000L
            ) {
                @Override
                synchronized public void onCacheRemoval(String key, Group obj, boolean batch, boolean last) {
                    // read only cache, do nothing
                }
                @Override
                public void bulkCacheUpdate(List<Group> objects) {
                    // read only cache, do nothing
                }
            };
        }

        this.serviceEnable = true;

        Gauge.builder("groups_service_cache_sum_time", this.groupCache.getTotalCacheTime())
                .description("[Groups] total time cache execution")
                .register(meterRegistry);
        Gauge.builder("groups_service_cache_sum", this.groupCache.getTotalCacheTry())
                .description("[Groups] total cache try")
                .register(meterRegistry);
        Gauge.builder("groups_service_cache_miss", this.groupCache.getCacheMissStat())
                .description("[Groups] total cache miss")
                .register(meterRegistry);
    }

    @PreDestroy
    public void destroy() {
        log.info("[groups] GroupCache stopping");
        this.serviceEnable = false;
        if ( groupsConfig.getGroupsCacheMaxSize() > 0 ) {
            groupCache.deleteCache();
        }
        log.info("[groups] GroupCache stopped");
    }

    @Scheduled(fixedRateString = "${groups.cache.log.period:PT24H}", initialDelay = 3600_000)
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
     * @param shortId - shortId to be retrieved
     * @return the group object
     * @throws ITNotFoundException if not found
     */
    protected Group getGroup(String shortId) throws ITNotFoundException {
        if ( shortId.startsWith("user_") ) {
            // virtual group, build it from the user object
            String userId = shortId.substring(5);
            try {
                User u = userCache.getUser(userId);
                Group g = new Group();
                g.setId(shortId);
                g.setShortId(shortId);
                g.setVersion(Group.GROUP_VERSION);
                g.setName("groups-default-group");
                g.setDescription("groups-default-group-description");
                g.setLanguage(u.getLanguage());
                g.setActive(u.isActive());
                g.setVirtual(true);
                g.setCreationDateMs(u.getRegistrationDate());
                g.setCreationBy(u.getLogin());
                g.setModificationDateMs(u.getRegistrationDate());
                g.setAttributes(new ArrayList<>());
                g.setReferringGroups(new ArrayList<>());
                return g;
            } catch (ITNotFoundException x) {
                // Group does not exist
                throw new ITNotFoundException("groups-get-not-found");
            }
        } else {
            if (!this.serviceEnable || groupsConfig.getGroupsCacheMaxSize() == 0) {
                // direct access from database
                Group u = groupRepository.findOneGroupByShortId(shortId);
                if (u == null) throw new ITNotFoundException("groups-get-not-found");
                return u.clone();
            } else {
                Group u = this.groupCache.get(shortId);
                if (u == null) {
                    // not in cache, get it from the database
                    u = groupRepository.findOneGroupByShortId(shortId);
                    if (u == null) throw new ITNotFoundException("groups-get-not-found");
                    this.groupCache.put(u, u.getShortId());
                }
                return u.clone();
            }
        }
    }

    protected boolean isInCache(String shortId) {
        if ( shortId.startsWith("user_") ) return false; // virtual group, do nothing
        if ( this.serviceEnable && groupsConfig.getGroupsCacheMaxSize() > 0 ) {
            return this.groupCache.get(shortId) != null;
        }
        return false;
    }

    /**
     * Remove a group from the local cache if exists (this is when the user has been updated somewhere else
     * @param shortId - groupId to be removed
     */
    protected void flushGroup(String shortId) {
        if ( shortId.startsWith("user_") ) return; // virtual group, do nothing
        if ( this.serviceEnable && groupsConfig.getGroupsCacheMaxSize() > 0 ) {
            this.groupCache.remove(shortId,false);
        }
    }

}
