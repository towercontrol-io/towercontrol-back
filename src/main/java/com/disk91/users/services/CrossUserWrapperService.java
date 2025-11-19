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

import com.disk91.common.config.CommonConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CrossUserWrapperService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    Object privUserWrapperService = null;

    @Autowired
    protected CommonConfig commonConfig;

    /**
     * To wrap the Non Community Edition Feature, transparently, with no compilation
     * impact, this class is a wrapper to the NCE implementation and ensure a fallback
     * response for the CE version.
     */
    @PostConstruct
    private void initLogService() {
        log.info("[users] Init CrossWrapper Service");
        if ( commonConfig.isCommonNceEnable() ) {
            try {
                Class<?> clazz = Class.forName("com.disk91.users.services.PrivUserWrapperService");
                privUserWrapperService = clazz.getDeclaredConstructor().newInstance();
                log.info("\u001B[34m[Users] Running Non Community Edition features\u001B[0m");
                return;
            } catch (ClassNotFoundException e) {
                privUserWrapperService = null;
            } catch (Exception e) {
                log.error("[Users] Failed to load the PrivUserWrapperService class : {}", e.getMessage());
            }
        }
        log.info("[Users] Running Community Edition");
    }

    public boolean isNceEnabled() {
        return ( privUserWrapperService != null && commonConfig.isCommonNceEnable() );
    }

    /**
     * Function to verify captcha during user registration, return true if captcha is valid
     * false if not correctly completed. This function calls the NCE implementation if available,
     * otherwise returns true (no captcha in CE)
     *
     * @param secret
     * @return
     */
    public boolean userRegistrationVerifyCaptcha(String secret) {
        if ( isNceEnabled() ) {
            try {
                return (boolean) privUserWrapperService.getClass()
                        .getMethod("userRegistrationVerifyCaptcha", String.class)
                        .invoke(privUserWrapperService, secret);
            } catch (Exception e) {
                log.error("[Users] Failed to call userRegistrationVerifyCaptcha : {}", e.getMessage());
            }
        }
        // default Community Edition behavior : no captcha
        if ( commonConfig.isCommonNceEnable() ) {
            log.warn("[Users] Captcha verification failed to call NCE code with NCE enabled, refusing captcha");
            return false;
        }
        return true;
    }


}
