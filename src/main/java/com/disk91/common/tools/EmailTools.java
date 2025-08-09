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

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.angus.mail.smtp.SMTPSenderFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.Locale;

@Component
public class EmailTools {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private JavaMailSender sender;

    @Value("${spring.mail.password:''}")
    private String mailPassword;

    @Async
    public void send(String to, String text, String subject, String from) {
        text = text.replace("\\n","\n");

        try {
            if ( mailPassword.compareToIgnoreCase("debug")==0 ) {
                log.info("[common][email] Sending email to ({}) with subject ({}) and text ({})", to, subject, text);
            } else {
                MimeMessage message = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message);
                helper.setFrom(from);
                helper.setTo(to);
                helper.setText(text);
                helper.setSubject(subject);
                sender.send(message);
            }
        } catch ( MailAuthenticationException x ) {
            log.error("[common][email] Failed to send email, bad authentication - Make sure you setup email credentials");
        } catch ( SMTPSenderFailedException x) {
            log.error("[common][email] Failed to send email, bad setting - {}", x.getMessage());
        } catch (MessagingException e) {
            log.error("[common][email] Impossible to send an email to ({})", to);
        }
    }

    public Locale extractLocale(HttpServletRequest request, Locale defaultLocale) {
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage == null || acceptLanguage.isEmpty()) {
            return defaultLocale; // Fallback to default language
        }
        return Locale.forLanguageTag(acceptLanguage.split(",")[0]); // Use the first language in the header
    }


}
