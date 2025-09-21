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

package com.disk91.groups.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = {"file:configuration/groups.properties"}, ignoreResourceNotFound = true)
public class GroupsConfig {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // ----------------------------------------------
    // Common setup
    // ----------------------------------------------

    // Service Id used for multi-instance environment, must be different for every instances
    @Value("${common.service.id:83DZqvwbXzmtllVq}")
    protected String commonServiceId;
    public String getCommonServiceId() {
        return (commonServiceId.isEmpty())?"83DZqvwbXzmtllVq":commonServiceId;
    }

    // Select the medium to be used for the intracom service communication
    @Value("${groups.intracom.medium:db}")
    protected String groupsIntracomMedium;
    public String getGroupsIntracomMedium() {
        return groupsIntracomMedium;
    }

    // --------------------------------------------
    // Group Parameters
    // --------------------------------------------

    @Value("${groups.max.depth:16}")
    protected int groupsMaxDepth;
    public int getGroupsMaxDepth() {
        return groupsMaxDepth;
    }

    @Value("${groups.shortid.size:6}")
    protected int groupsShortidSize;
    public int getGroupsShortidSize() {
        return groupsShortidSize;
    }

    @Value("${groups.virtual.allows.sub:false}")
    protected boolean groupVituralAllowsSub;
    public boolean isGroupVituralAllowsSub() {
        return groupVituralAllowsSub;
    }

    @Value("${groups.retention.ms.before.delete:604800000}")
    protected long groupsRetentionMsBeforeDelete;
    public long getGroupsRetentionMsBeforeDelete() {
        return groupsRetentionMsBeforeDelete;
    }

    // --------------------------------------------
    // Group Cache
    // --------------------------------------------

    @Value("${groups.cache.max.size:1000}")
    protected int groupsCacheMaxSize;
    public int getGroupsCacheMaxSize() {
        return groupsCacheMaxSize;
    }

    @Value("${groups.cache.expiration_s:0}")
    protected int groupsCacheExpiration;
    public int getGroupsCacheExpiration() {
        return groupsCacheExpiration;
    }

    @Value("${groups.cache.log.period:PT24H}")
    protected String groupsCacheLogPeriod;
    public String getGroupsCacheLogPeriod() {
        return groupsCacheLogPeriod;
    }

}
