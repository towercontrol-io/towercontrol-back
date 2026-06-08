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
package com.disk91.alerts.mdb.entities.sub;

import com.disk91.common.tools.CloneableObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;

/**
 * AlertLocaleMessage - A locale-scoped collection of medium-specific messages inside an AlertTemplate.
 * Each locale entry targets one language (e.g. "fr", "en") and holds one message variant per medium.
 */
@Tag(name = "Alert Locale Message", description = "Locale-scoped set of medium-specific message variants for an alert template")
public class AlertLocaleMessage implements CloneableObject<AlertLocaleMessage> {

    // IETF language tag for this locale (e.g. "fr", "en", "de")
    @Schema(
            description = "IETF language tag for this locale",
            example = "fr",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String locale;

    // Per-medium message variants for this locale
    @Schema(
            description = "List of medium-specific message variants for this locale",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected ArrayList<AlertMediumMessage> mediums;

    // === CREATE ===

    /**
     * Factory - create an empty AlertLocaleMessage for the given locale.
     * @param locale - IETF language tag (e.g. "en", "fr")
     * @return new AlertLocaleMessage instance with an empty mediums list
     */
    public static AlertLocaleMessage of(String locale) {
        AlertLocaleMessage l = new AlertLocaleMessage();
        l.setLocale(locale);
        l.setMediums(new ArrayList<>());
        return l;
    }

    // === CLONE ===

    public AlertLocaleMessage clone() {
        AlertLocaleMessage u = new AlertLocaleMessage();
        u.setLocale(locale);
        u.setMediums(new ArrayList<>());
        if (mediums != null) {
            for (AlertMediumMessage m : mediums) {
                u.getMediums().add(m.clone());
            }
        }
        return u;
    }

    // === GETTER / SETTER ===

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public ArrayList<AlertMediumMessage> getMediums() {
        return mediums;
    }

    public void setMediums(ArrayList<AlertMediumMessage> mediums) {
        this.mediums = mediums;
    }
}

