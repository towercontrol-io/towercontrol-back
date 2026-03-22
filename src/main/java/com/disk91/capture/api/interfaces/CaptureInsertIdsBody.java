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
package com.disk91.capture.api.interfaces;

import com.disk91.capture.mdb.entities.sub.IdStateEnum;
import com.disk91.common.tools.CustomField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Capture IDs Creation", description = "Request to created new IDs in database")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaptureInsertIdsBody {

    // The endpoint refer to the protocol definition and the type of IDs to insert, this is the reference to be used.
    @Schema(
            description = "Endpoint reference",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String captureId;

    // The endpoint refer to the protocol definition and the type of IDs to insert, this is the reference to be used.
    @Schema(
            description = "Expected initial state of the IDs to insert ; not all states are possible",
            example = "NOT_ASSIGNED",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected IdStateEnum initialState;

    // The Ids are a list of string containing the IDs to insert
    @Schema(
            description = "List of fields to insert, with the field name and the order, separated by ; or , (csv)",
            example = "lorawan-dev;lorawan-join",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String headers;


    // The Ids are a list of string containing the IDs to insert
    @Schema(
            description = "List of IDs, one line per ID, credentials separated by ; or , (csv)",
            example = "12313156;1231516111\n12313156;1231516111\n12313156;1231516111",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected List<String> ids;



    // ==========================================


    // ==========================================


    public String getCaptureId() {
        return captureId;
    }

    public void setCaptureId(String captureId) {
        this.captureId = captureId;
    }

    public IdStateEnum getInitialState() {
        return initialState;
    }

    public void setInitialState(IdStateEnum initialState) {
        this.initialState = initialState;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }
}
