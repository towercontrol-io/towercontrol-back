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
package com.disk91.common;

import com.disk91.common.config.CommonConfig;
import com.disk91.common.pdb.entities.Param;
import com.disk91.common.pdb.repositories.ParamRepository;
import com.mongodb.client.MongoDatabase;
import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class Common {

    /**
     * This class init the Common specific components
     *
     */

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected ParamRepository paramRepository;

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostConstruct
    void initCommonModule() {
        // Init the Comon Module
        log.info("[common] Common is starting");

        // Init the database specific elements base on sharding setting
        Param p = paramRepository.findByParamKey("common.shard.init");
        if ( p == null ) {
            p = new Param();
            p.setParamKey("common.shard.init");
            p.setLongValue(0);
            paramRepository.save(p);
        }
        if ( p.getLongValue() == 0 && commonConfig.isMongoShardingEnabled() ) {
            try {
                log.info("[common] Init shard for Common Module");
                MongoDatabase adminDB = mongoTemplate.getMongoDatabaseFactory().getMongoDatabase("admin");
                Document batLevel = adminDB.runCommand(
                        new Document("shardCollection", commonConfig.getMongoDatabase()+".common_wifimac_location")
                                .append("key", new Document("macAddress", 1).append("_id", 1)));
                p.setLongValue(1);
                paramRepository.save(p);
                log.info("[common] Mongo shard configuration OK, version is Now 1");
            } catch (Exception e) {
                log.error("[common] Error while initializing shard for Common Module {}", e.getMessage());
            }
        }
    }


}
