/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2026.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Component
public class DiscordTools {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Send a message on discord using a webhook
     *
     * @param to - dicsord webhook url
     * @param text - body of the message
     * @param subject - title of the message
     * @param from - cutsom string, don't use an email
     */
    @Async
    public void send(String to, String text, String subject, String from) {
        text = text.replace("\\n","\n");

        try {
            // Prepare JSON payload expected by Discord webhook API
            Map<String, Object> payload = new HashMap<>();
            String content = (subject + "\n\n" + text);
            payload.put("content", content.substring(0, Math.min(content.length(), 1800)) );

            // Send HTTP POST request to Discord webhook
            WebClient webClient = WebClient.builder().build();

            webClient.post()
                    .uri(to)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            // Log successful webhook call
            log.debug("[common] [discord] Message successfully sent to Discord webhook");
        } catch (Exception e) {
            log.error("[common] [discord] Error sending message to Discord webhook: {}", e.getMessage());
        }
    }

}
