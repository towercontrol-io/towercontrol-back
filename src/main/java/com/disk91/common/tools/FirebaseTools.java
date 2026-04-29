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
package com.disk91.common.tools;

import com.disk91.common.config.CommonConfig;
import com.disk91.common.tools.exceptions.ITParseException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;

@Component
public class FirebaseTools {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CommonConfig commonConfig;

    private boolean firebaseServiceInitialized = false;

    @PostConstruct
    public void init() {}
    @PostConstruct
    public void initFirebase() {
        if ( commonConfig.isCommonFirebaseEnabled() ) {
            if (!commonConfig.getFirebaseServiceAccountPath().isEmpty() ) {
                try {
                    log.info("[common] Initializing Firebase with service account");
                    FileInputStream serviceAccount = new FileInputStream(commonConfig.getFirebaseServiceAccountPath());
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();
                    if (FirebaseApp.getApps().isEmpty()) {
                        FirebaseApp.initializeApp(options);
                    }
                    firebaseServiceInitialized = true;
                } catch (IOException e) {
                    log.error("[common] Failed to initialize Firebase: {}", e.getMessage());
                }
            } else {
                log.error("[common] Firebase is enabled but service account path is not set. Firebase will not be initialized.");
            }
        }
    }

    public void sendPush(String deviceToken, String title, String body) throws ITParseException {
        if (firebaseServiceInitialized) {
            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setToken(deviceToken)
                    .build();
            try {
                String response = FirebaseMessaging.getInstance().send(message);
                log.info("[common] Firebase push notification sent: {}", response);
            } catch (FirebaseMessagingException e) {
                log.error("[common] Failed to send push notification to {}: {}", deviceToken, e.getMessage());
                throw new ITParseException("common-firebase-push-notification-failed");
            }
        } else {
            log.warn("[common] Firebase service is not initialized. Cannot send push notification.");
        }
    }

}
