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
package com.disk91.audit.mdb.entities;

import com.disk91.audit.integration.AuditMessage;
import com.disk91.common.tools.Now;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Sharded;
import org.springframework.data.mongodb.core.mapping.ShardingStrategy;

import java.util.ArrayList;

@Document(collection = "audit_message")
@CompoundIndexes({
        @CompoundIndex(name = "auditMess_service_Idx", def = "{'service' : 'hashed' }"),
        @CompoundIndex(name = "auditMess_owner_Idx", def = "{'owner' : 'hashed' }"),
        @CompoundIndex(name = "auditMess_time_Idx", def = "{'actionMs' : 'hashed' }"),
})
@Sharded(shardKey = { "id" }, shardingStrategy = ShardingStrategy.RANGE)
public class AuditMdb extends AuditMessage {

    @Id
    protected String id;

    protected long auditTimestampNs;

    protected String auditSignature;

    // ================================================================================================================
    // from an AuditMessage

    /**
     * Create a Audit ready for database insertion from an AuditMessage
     * The signature will be created later, and it will take a previous signature into account
     * to create a chain of audits.
     * @param am - audit message to be converted
     * @param previousAudit - previous audit in the chain, can be null if first audit
     * @param signingKey - key to be used for signature, it can include a time based modification for avoiding recalculation
     * @return Audit element ready for db interaction
     */
    public static AuditMdb fromAuditMessage(AuditMessage am, AuditMdb previousAudit, String signingKey) {
        AuditMdb a = new AuditMdb();
        a.setService(am.getService());
        a.setAction(am.getAction());
        a.setActionMs(am.getActionMs());
        a.setOwner(am.getOwner());
        a.setLogStr(am.getLogStr());
        a.setParams(new ArrayList<>());
        for (String param : am.getParams()) {
            a.getParams().add(param);
        }
        a.setAuditTimestampNs(Now.NanoTime());
        a.setAuditSignature("not-yet-implemented");

        // @TODO implement signature calculation with previousAudit and signingKey

        return a;
    }


    // ================================================================================================================
    // Getters & Setters


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getAuditTimestampNs() {
        return auditTimestampNs;
    }

    public void setAuditTimestampNs(long auditTimestampNs) {
        this.auditTimestampNs = auditTimestampNs;
    }

    public String getAuditSignature() {
        return auditSignature;
    }

    public void setAuditSignature(String auditSignature) {
        this.auditSignature = auditSignature;
    }
}
