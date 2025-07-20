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

/**
 * UserPending -  This class stores account creation requests awaiting email validation and handles confirmations.
 * Entries are automatically removed either upon activation or after a specified period.
 */
package com.disk91.common.services;

import com.disk91.common.config.CommonConfig;
import com.disk91.common.tools.EncryptionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EncryptionService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CommonConfig commonConfig;

    private static String __iv = "4fee88822bce7d331d6db0d69d978492";

    /**
     * Encryt the given string with the configuration key store in the configuration file
     * @param tobeEncrypted
     * @return
     */
    public String encryptStringWithServerKey(String tobeEncrypted) {
        return EncryptionHelper.encrypt(tobeEncrypted, __iv, commonConfig.getEncryptionKey());
    }


    public String decryptStringWithServerKey(String tobeDecrypted) {
        return EncryptionHelper.decrypt(tobeDecrypted, __iv, commonConfig.getEncryptionKey());
    }

}
