package com.disk91.capture.mdb.entities;

import com.disk91.capture.interfaces.CaptureDataPivot;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Sharded;
import org.springframework.data.mongodb.core.mapping.ShardingStrategy;

@Document(collection = "capture_pivot_raw")
@CompoundIndexes({
        @CompoundIndex(name = "capture_rx_uuid", def = "{'rxUuid': 'hashed'}"),
        @CompoundIndex(name = "capture_rx_ts", def = "{'rxTimestamp': -1}"),
        @CompoundIndex(name = "capture_meta_nwkDev", def = "{'metadata.nwkDeviceId': 'hashed'}"),
        @CompoundIndex(name = "capture_meta_dev", def = "{'metadata.deviceId': 'hashed'}"),
})
@Sharded(shardKey = { "metadata.deviceId", "id" }, shardingStrategy = ShardingStrategy.RANGE)
public class CapturePivotRaw extends CaptureDataPivot {

    @Id
    private String id;

    // --------------------------------

    @Override
    public CapturePivotRaw clone() {
       CapturePivotRaw p = (CapturePivotRaw) super.clone();
       p.setId(this.id);
       return p;
    }

    // --------------------------------


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
