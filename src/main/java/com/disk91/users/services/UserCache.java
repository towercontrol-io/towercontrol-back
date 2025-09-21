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
package com.disk91.users.services;

import com.disk91.common.tools.Now;
import com.disk91.common.tools.ObjectCache;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.users.config.UsersConfig;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.repositories.UserRepository;
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
public class UserCache {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * User Cache Service is caching the User Information. It may be instantiated in all the instances
     * and multiple cache should collaborate in a cluster
     */

    @Autowired
    protected UsersConfig usersConfig;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected MeterRegistry meterRegistry;


    // ================================================================================================================
    // CACHE SERVICE
    // ================================================================================================================

    private ObjectCache<String, User> userCache;

    protected boolean serviceEnable = false;

    @PostConstruct
    private void initUsersCache() {
        log.info("[users] initUsersCache");
        if ( usersConfig.getUsersCacheMaxSize() > 0 ) {
            this.userCache = new ObjectCache<String, User>(
                    "UserROCache",
                    usersConfig.getUsersCacheMaxSize(),
                    usersConfig.getUsersCacheExpiration()*1000
            ) {
                @Override
                synchronized public void onCacheRemoval(String key, User obj, boolean batch, boolean last) {
                    // read only cache, do nothing
                }
                @Override
                public void bulkCacheUpdate(List<User> objects) {
                    // read only cache, do nothing
                }
            };
        }

        this.serviceEnable = true;

        Gauge.builder("users_service_cache_sum_time", this.userCache.getTotalCacheTime())
                .description("[Users] total time cache execution")
                .register(meterRegistry);
        Gauge.builder("users_service_cache_sum", this.userCache.getTotalCacheTry())
                .description("[Users] total cache try")
                .register(meterRegistry);
        Gauge.builder("users_service_cache_miss", this.userCache.getCacheMissStat())
                .description("[Users] total cache miss")
                .register(meterRegistry);
    }

    @PreDestroy
    public void destroy() {
        log.info("[users] UserCache stopping");
        this.serviceEnable = false;
        if ( usersConfig.getUsersCacheMaxSize() > 0 ) {
            userCache.deleteCache();
        }
        log.info("[users] UserCache stopped");
    }

    @Scheduled(fixedRateString = "${users.cache.log.period:PT24H}", initialDelay = 3600_000)
    protected void userCacheStatus() {
        try {
            Duration duration = Duration.parse(usersConfig.getUsersCacheLogPeriod());
            if (duration.toMillis() >= Now.ONE_FULL_DAY ) return;
        } catch (Exception ignored) {}
        if ( ! this.serviceEnable || usersConfig.getUsersCacheMaxSize() == 0 ) return;
        this.userCache.log();
    }

    // ================================================================================================================
    // Cache access
    // ================================================================================================================

    public User getUser(String userLogin) throws ITNotFoundException {
        if ( ! this.serviceEnable || usersConfig.getUsersCacheMaxSize() == 0 ) {
            // direct acces from database
            User u = userRepository.findOneUserByLogin(userLogin);
            if ( u == null ) throw new ITNotFoundException("User not found");
            return u.clone();
        } else {
            User u = this.userCache.get(userLogin);
            if ( u == null ) {
                // not in cache, get it from the database
                u = userRepository.findOneUserByLogin(userLogin);
                if ( u == null ) throw new ITNotFoundException("User not found");
                this.userCache.put(u, u.getLogin());
            }
            return u.clone();
        }
    }

    /**
     * Remove a user from the local cache if exists (this is when the user has been updated somewhere else
     * @param userLogin - user login to be removed
     * @return
     */
    public void flushUser(String userLogin) {
        if ( this.serviceEnable && usersConfig.getUsersCacheMaxSize() > 0 ) {
            this.userCache.remove(userLogin,false);
        }
    }

    /**
     * Save the User structure after an update. The cache is flushed for this user
     * This is not protected against concurrent access on multiple cache service instance
     * @param u
     */
    public void saveUser(User u) {
         userRepository.save(u);
         this.flushUser(u.getLogin());
    }

    // @TODO - manage the broadcast request for user flush and scan for flush trigger on each of the instances

}
