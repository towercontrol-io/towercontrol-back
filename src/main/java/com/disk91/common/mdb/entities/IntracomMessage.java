package com.disk91.common.mdb.entities;

import com.disk91.common.tools.CloneableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
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

    // Creation Timestamp in ms
    protected long creationMs;




    // ================================================================================================================
    // Getters / Setters

}

