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
package com.disk91.capture;

import com.disk91.capture.services.CaptureEndpointCache;
import com.disk91.capture.services.CaptureProtocolsCache;
import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.config.CommonConfig;
import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.pdb.entities.Param;
import com.disk91.common.pdb.repositories.ParamRepository;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.integration.api.interfaces.IntegrationCallback;
import com.disk91.integration.api.interfaces.IntegrationQuery;
import com.disk91.integration.services.IntegrationService;
import com.mongodb.client.MongoDatabase;
import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import static com.disk91.capture.integration.CaptureActions.CAPTURE_ACTION_FLUSH_CACHE_ENDPOINT;
import static com.disk91.capture.integration.CaptureActions.CAPTURE_ACTION_RELOAD_CACHE_PROTOCOL;

@Component
public class Capture {

    /**
     * This class init the Capture specific components
     *
     */

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected ParamRepository paramRepository;

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    protected IntegrationService integrationService;

    @Autowired
    protected CaptureProtocolsCache captureProtocolsCache;

    @Autowired
    protected CaptureEndpointCache captureEndpointCache;

    @PostConstruct
    void initCaptureModule() {
        // Init the Capture Module
        log.info("[capture] Capture is starting");

        // Init the database specific elements base on sharding setting
        Param p = paramRepository.findByParamKey("capture.shard.init");
        if ( p == null ) {
            p = new Param();
            p.setParamKey("capture.shard.init");
            p.setLongValue(0);
            paramRepository.save(p);
        }
        if ( p.getLongValue() == 0 && commonConfig.isMongoShardingEnabled() ) {
            try {
                log.info("[capture] Init shard for Capture Module");
                MongoDatabase adminDB = mongoTemplate.getMongoDatabaseFactory().getMongoDatabase("admin");
                adminDB.runCommand(
                        new Document("shardCollection", commonConfig.getMongoDatabase()+".capture_pivot_raw")
                                .append("key", new Document("metadata.deviceId", 1).append("_id", 1)));
                p.setLongValue(1);
                paramRepository.save(p);
                log.info("[capture] Mongo shard configuration OK, version is Now 1");
            } catch (Exception e) {
                log.error("[capture] Error while initializing shard for Common Module {}", e.getMessage());
            }
        }

        // Init the integration actions
        try {
            integrationService.registerCallback(
                    ModuleCatalog.Modules.CAPTURE,
                    new IntegrationCallback() {
                        @Override
                        public void onIntegrationEvent(IntegrationQuery q) {
                            if ( q.getAction() == CAPTURE_ACTION_RELOAD_CACHE_PROTOCOL.ordinal() ) {
                                captureProtocolsCache.initProtocolCache();
                                // terminate the action
                                q.setResponse(ActionResult.OK("Protocol cache reloaded")); // fire & forget, success on every actions
                                q.setResult(null);
                                q.setState(IntegrationQuery.QueryState.STATE_DONE);
                                q.setResponse_ts(Now.NanoTime());
                            } else if ( q.getAction() == CAPTURE_ACTION_FLUSH_CACHE_ENDPOINT.ordinal() ) {
                                String id = (String) q.getQuery();
                                captureEndpointCache.flushCaptureEndpoint(id);
                                // terminate the action
                                q.setResponse(ActionResult.OK("Endpoint cache flushed")); // fire & forget, success on every actions
                                q.setResult(null);
                                q.setState(IntegrationQuery.QueryState.STATE_DONE);
                                q.setResponse_ts(Now.NanoTime());
                            } else {
                                log.error("[capture] Receiving a unknown message from integration");
                                // terminate the action
                                q.setResponse(ActionResult.BADREQUEST("capture-integration-unknown-action"));
                                q.setResult(null);
                                q.setState(IntegrationQuery.QueryState.STATE_ERROR);
                                q.setResponse_ts(Now.NanoTime());
                            }
                        }
                    }
            );
        } catch (ITParseException | ITTooManyException x) {
            log.error("[capture] Failed to register capture integration callback: {}", x.getMessage());
        }

    }


}
