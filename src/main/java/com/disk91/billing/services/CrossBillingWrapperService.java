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
package com.disk91.billing.services;

import com.disk91.common.config.CommonConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Service;

@Service
public class CrossBillingWrapperService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    Object privBillingWrapperService = null;

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired(required = false)
    private AutowireCapableBeanFactory beanFactory;

    /**
     * To wrap the Non Community Edition Feature, transparently, with no compilation
     * impact, this class is a wrapper to the NCE implementation and ensure a fallback
     * response for the CE version.
     */
    @PostConstruct
    private void initBillingWrapperService() {
        log.info("[Billing] Init CrossWrapper Service");
        if ( commonConfig.isCommonNceEnable() ) {
            try {
                Class<?> clazz = Class.forName("com.disk91.billing.services.PrivBillingWrapperService");
                privBillingWrapperService = beanFactory.createBean(clazz);
                log.info("\u001B[34m[Billing] Running Non Community Edition features\u001B[0m");
                return;
            } catch (ClassNotFoundException e) {
                privBillingWrapperService = null;
            } catch (Exception e) {
                log.error("[Billing] Failed to load the PrivBillingWrapperService class : {}", e.getMessage());
            }
        }
        log.info("[Billing] Running Community Edition");
    }

    public boolean isNceEnabled() {
        return ( privBillingWrapperService != null && commonConfig.isCommonNceEnable() );
    }

    /**
     * Function to authorize a packet to be proceeded when received, based on the user billing rules
     *
     * @param deviceId - Device id receiving the packet
     *
     * @return
     */
    public boolean billingPacketReceptionAuthorized(String deviceId) {
        if ( isNceEnabled() ) {
            try {
                return (boolean) privBillingWrapperService.getClass()
                        .getMethod("billingPacketReceptionAuthorized", String.class)
                        .invoke(privBillingWrapperService, deviceId);
            } catch (Exception e) {
                log.error("[Billing] Failed to call billingPacketReceptionAuthorized : {}", e.getMessage());
            }
        }
        // default Community Edition behavior : accept all
        if ( commonConfig.isCommonNceEnable() ) {
            return true;
        }
        return true;
    }

    /**
     * Function to authorize a group creation, verify the group limitations associated to
     * billing
     *
     * @param parentId - GroupParent Id
     * @paral userId - User Id
     *
     * @return
     */
    public boolean billingGroupCreationAuthorized(String userId, String parentId) {
        if ( isNceEnabled() ) {
            try {
                return (boolean) privBillingWrapperService.getClass()
                        .getMethod("billingGroupCreationAuthorized", String.class, String.class)
                        .invoke(privBillingWrapperService, userId, parentId);
            } catch (Exception e) {
                log.error("[Billing] Failed to call billingGroupCreationAuthorized : {}", e.getMessage());
            }
        }
        // default Community Edition behavior : accept all
        if ( commonConfig.isCommonNceEnable() ) {
            return true;
        }
        return true;
    }

}
