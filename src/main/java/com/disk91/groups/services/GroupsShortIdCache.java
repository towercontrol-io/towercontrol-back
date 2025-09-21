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
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.groups.config.GroupsConfig;
import com.disk91.groups.mdb.entities.Group;
import com.disk91.groups.mdb.repositories.GroupRepository;
import com.disk91.groups.tools.GroupsList;
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
import java.util.List;

@Service
public class GroupsShortIdCache {

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

    private ObjectCache<String, GroupsList> groupCache;

    protected boolean serviceEnable = false;

    @PostConstruct
    private void initGroupsCache() {
        log.info("[groups] initGroupsShortIdCache");
        if ( groupsConfig.getGroupsCacheMaxSize() > 0 ) {
            this.groupCache = new ObjectCache<String, GroupsList>(
                    "GroupsROCache",
                    groupsConfig.getGroupsCacheMaxSize(),
                    groupsConfig.getGroupsCacheExpiration()*1000
            ) {
                @Override
                synchronized public void onCacheRemoval(String key, GroupsList obj, boolean batch, boolean last) {
                    // read only cache, do nothing
                }
                @Override
                public void bulkCacheUpdate(List<GroupsList> objects) {
                    // read only cache, do nothing
                }
            };
        }

        this.serviceEnable = true;

        Gauge.builder("groups_service_short_cache_sum_time", this.groupCache.getTotalCacheTime())
                .description("[Groups short] total time cache execution")
                .register(meterRegistry);
        Gauge.builder("groups_service_short_cache_sum", this.groupCache.getTotalCacheTry())
                .description("[Groups short] total cache try")
                .register(meterRegistry);
        Gauge.builder("groups_service_short_cache_miss", this.groupCache.getCacheMissStat())
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

    protected GroupsList buildGroupListFromDb(String shortId) throws ITNotFoundException {
        // direct access from database
        List<Group> gs = groupRepository.findByShortIdOrReferringGroupsContains(shortId);
        if (gs == null|| gs.isEmpty()) throw new ITNotFoundException("group-list-not-found");
        // find head
        Group head = null;
        for ( Group g : gs ) {
            if ( g.getShortId().compareTo(shortId) == 0 ) {
                head = g;
                break;
            }
        }
        if ( head == null ) throw new ITNotFoundException("group-list-head-less");
        GroupsList ret = new GroupsList(head,groupsConfig.getGroupsMaxDepth());
        for ( Group g : gs ) {
            if ( g.getShortId().compareTo(shortId) != 0 ) {
                try {
                    ret.addElement(g);
                } catch (ITParseException e) {
                    log.warn("[groups] group {} retrieved for hierarchy {} but not in hierarchy",g.getShortId(),shortId);
                }
            }
        }
        return ret;
    }

    /**
     * Get a Group object by a short ID this allows to have the whole hierarchy for this group
     * @param shortId - shortId to be retrieved
     * @return the group object
     * @throws ITNotFoundException if not found
     */
    protected GroupsList getGroupList(String shortId) throws ITNotFoundException {
        // virtual group management
        if ( shortId.startsWith("user_") ) {
            String userId = shortId.substring(5);
            try {
                User u = userCache.getUser(userId);
                Group g = new Group();
                g.init("groups-default-group", "groups-default-group-description", shortId, u.getLanguage());
                g.setId(shortId);
                g.setActive(u.isActive());
                g.setVirtual(true);
                g.setCreationDateMs(u.getRegistrationDate());
                g.setCreationBy(u.getLogin());
                g.setModificationDateMs(u.getRegistrationDate());
                return new GroupsList(g,groupsConfig.getGroupsMaxDepth());
            } catch (ITNotFoundException x) {
                // Group does not exist
                throw new ITNotFoundException("groups-get-not-found");
            }
        }

        // get from cache
        if (!this.serviceEnable || groupsConfig.getGroupsCacheMaxSize() == 0) {
            return buildGroupListFromDb(shortId);
        } else {
            GroupsList ret = this.groupCache.get(shortId);
            if (ret != null) return ret.clone();
            // not in cache, build it
            ret = buildGroupListFromDb(shortId);
            this.groupCache.put(ret, shortId);
            return ret.clone();
        }
    }

    /**
     * Remove a groupList from the local cache if exists (this is when the user has been updated somewhere else
     * We also need to flush all the groups that are referring this one as they will be also modified
     * @param g - group to be removed, flush all the referring groups as well
     */
    protected void flushGroup(Group g) {
        if ( g.getShortId().startsWith("user_") ) return; // virtual group, do nothing
        if ( this.serviceEnable && groupsConfig.getGroupsCacheMaxSize() > 0 ) {
            for ( String shortId : g.getReferringGroups() ) {
                this.groupCache.remove(shortId,false);
            }
            this.groupCache.remove(g.getShortId(),false);
        }
    }

}
