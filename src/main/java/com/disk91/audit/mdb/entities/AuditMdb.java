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
