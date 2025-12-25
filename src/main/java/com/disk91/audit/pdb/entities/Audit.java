package com.disk91.audit.pdb.entities;


import com.disk91.audit.integration.AuditMessage;
import com.disk91.common.tools.Now;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "audit_audits",
        indexes = {
                @Index(name = "idx_audit_service_key", columnList = "service", unique = false),
                @Index(name = "idx_audit_owner_key", columnList = "owner", unique = false),
                @Index(name = "idx_audit_time_key", columnList = "action_ms", unique = false),
        }
)
public class Audit {

    @Id
    @Column(name = "audit_key", nullable = false, unique = true)
    protected UUID auditKey;

    @Column(name = "audit_timestamp_ns", nullable = false)
    protected long auditTimestampNs;

    @Column(name = "audit_signature", nullable = false)
    protected String auditSignature;

    // Service name, based on the module name ex : Users
    @Column(name = "service", nullable = false)
    protected String service;
    // Action related to the service to identify the type of audit log ex : registration
    @Column(name = "action", nullable = false)
    protected String action;
    // Action timestamp
    @Column(name = "action_ms", nullable = false)
    protected long actionMs;
    // Owner of the log - login hash for search by user
    @Column(name = "owner", nullable = false)
    protected String owner;
    // Text with parameters {x} describing the log
    @Column(name = "log_str", nullable = false)
    protected String logStr;
    // List of parameters to be used in the log, this is for encrypted parameters (sensitives information)
    @Column(name = "params", nullable = false)
    protected List<String> params;

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
    public static Audit fromAuditMessage(AuditMessage am, Audit previousAudit, String signingKey) {
        Audit a = new Audit();
        a.setAuditKey(UUID.randomUUID());
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

    public UUID getAuditKey() {
        return auditKey;
    }

    public void setAuditKey(UUID auditKey) {
        this.auditKey = auditKey;
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

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public long getActionMs() {
        return actionMs;
    }

    public void setActionMs(long actionMs) {
        this.actionMs = actionMs;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getLogStr() {
        return logStr;
    }

    public void setLogStr(String logStr) {
        this.logStr = logStr;
    }

    public List<String> getParams() {
        return params;
    }

    public void setParams(List<String> params) {
        this.params = params;
    }
}
