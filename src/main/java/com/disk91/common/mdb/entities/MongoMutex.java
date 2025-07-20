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
package com.disk91.common.mdb.entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "it_mutex")
@CompoundIndexes({
    @CompoundIndex(name = "commonIdx_mutexId", def = "{'login': 1}"),
})
public class MongoMutex {

    @Transient
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Id
    private String id;

    // Key for the mutex entry
    protected String mutexId;

    // Is the mutex taken
    protected boolean taken;

    // Date in MS since epoch when it has been taken
    protected long takenAt;

    // Who has taken it (service name)
    protected String takenBy;

    // Timeout duration in Ms -- free it after this time
    protected long timeout;

    // ===============================================================
    // Generated getters & setters
    // ===============================================================


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMutexId() {
        return mutexId;
    }

    public void setMutexId(String mutexId) {
        this.mutexId = mutexId;
    }

    public boolean isTaken() {
        return taken;
    }

    public void setTaken(boolean taken) {
        this.taken = taken;
    }

    public long getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(long takenAt) {
        this.takenAt = takenAt;
    }

    public String getTakenBy() {
        return takenBy;
    }

    public void setTakenBy(String takenBy) {
        this.takenBy = takenBy;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
