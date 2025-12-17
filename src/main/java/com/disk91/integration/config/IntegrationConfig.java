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

package com.disk91.integration.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = {"file:configuration/integration.properties"}, ignoreResourceNotFound = true)
public class IntegrationConfig {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // ----------------------------------------------
    // modules config
    // ----------------------------------------------
    @Value("${integration.route.memory.enabled:true}")
    protected boolean integrationRouteMemoryEnabled;
    public boolean isIntegrationRouteMemoryEnabled() {
        return integrationRouteMemoryEnabled;
    }

    @Value("${integration.route.db.enabled:true}")
    protected boolean integrationRouteDbEnabled;
    public boolean isIntegrationRouteDbEnabled() {
        return integrationRouteDbEnabled;
    }

    @Value("${integration.route.mqtt.enabled:true}")
    protected boolean integrationRouteMqttEnabled;
    public boolean isIntegrationRouteMqttEnabled() {
        return integrationRouteMqttEnabled;
    }


    // ----------------------------------------------
    // worker setup
    // ----------------------------------------------

    @Value("${integration.workers.max.count:1}")
    protected int integrationWorkersMaxCount;
    public int getIntegrationWorkersMaxCount() {
        return integrationWorkersMaxCount;
    }

}
