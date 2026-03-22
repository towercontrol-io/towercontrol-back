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

import com.disk91.capture.api.interfaces.sub.InsertIDsStatus;
import com.disk91.capture.mdb.entities.Protocols;
import com.disk91.capture.mdb.entities.sub.MandatoryField;
import com.disk91.capture.mdb.entities.sub.ProtocolId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "Capture Ids", description = "Insert new IDs in DB")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaptureInsertIdsResponseItf {

    // Status of the insertion
    @Schema(
            description = "Result of the insertion, in case of error, nothing is inserted",
            example = "",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected InsertIDsStatus status;

    @Schema(
            description = "Number of IDs inserted in database",
            example = "120",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long inserted;

    @Schema(
            description = "Line number with the error in case of error, otherwise 0",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long errorFirstLine;

    @Schema(
            description = "Number of lines with errors",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long errorCount;


    // --------------------------------

    // --------------------------------


    public InsertIDsStatus getStatus() {
        return status;
    }

    public void setStatus(InsertIDsStatus status) {
        this.status = status;
    }

    public long getInserted() {
        return inserted;
    }

    public void setInserted(long inserted) {
        this.inserted = inserted;
    }

    public long getErrorFirstLine() {
        return errorFirstLine;
    }

    public void setErrorFirstLine(long errorFirstLine) {
        this.errorFirstLine = errorFirstLine;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }
}
