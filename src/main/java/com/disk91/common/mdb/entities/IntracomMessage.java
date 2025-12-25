package com.disk91.common.mdb.entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Sharded;
import org.springframework.data.mongodb.core.mapping.ShardingStrategy;

@Document(collection = "common_intracom_message")
@CompoundIndexes({
        @CompoundIndex(name = "comintracomMess_createdTs_Idx", def = "{'macAddress' : 'hashed' }"),
})
@Sharded(shardKey = { "macAddress", "id" }, shardingStrategy = ShardingStrategy.RANGE)
public class IntracomMessage {

    @Transient
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Id
    protected String id = null;

    // Creation Timestamp in ms ; Id is used as a reference as ID contains Time stamp
    protected long creationMs;

    // Name of the sender, the list is dynamic and come from the property file
    // default setup in the Config files, constant listed in service
    protected String sender;

    // Name of the action, the list is dynamic and comes from every service configuration
    // properties and Config files
    protected String action;

    // Generic object used as parameter of the action
    protected Object parameter;


    // ================================================================================================================
    // Getters / Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreationMs() {
        return creationMs;
    }

    public void setCreationMs(long creationMs) {
        this.creationMs = creationMs;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Object getParameter() {
        return parameter;
    }

    public void setParameter(Object parameter) {
        this.parameter = parameter;
    }
}

