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
package com.disk91.alerts.api.interfaces;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;

/**
 * AlertTemplateListResponseItf - Response returned by the alert template list endpoint.
 * Contains the list of templates visible to the requesting user and the total count.
 */
@Tag(name = "Alert Template List Response", description = "Paginated list of alert templates visible to the user")
public class AlertTemplateListResponseItf {

    @ArraySchema(schema = @Schema(implementation = AlertTemplateResponseItf.class))
    protected ArrayList<AlertTemplateResponseItf> templates;

    @Schema(description = "Total number of templates in this response", example = "12",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected int total;

    // ==========================
    // Getters & Setters

    public ArrayList<AlertTemplateResponseItf> getTemplates() { return templates; }
    public void setTemplates(ArrayList<AlertTemplateResponseItf> templates) { this.templates = templates; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
}

