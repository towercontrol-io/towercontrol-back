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
package com.disk91.integration.services;

import com.disk91.common.tools.Now;
import com.disk91.integration.api.interfaces.InterfaceQuery;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.HashMap;

public class IntegrationService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private HashMap<String, InterfaceQuery> _queries = new HashMap<>();

    private long maxQueryDuration;

    @PostConstruct
    public void initIntegration() {
        log.debug("[integration] Init");
        // max query lue depuis le fichier de conf
    }

    public InterfaceQuery requestSync(InterfaceQuery query) {
        log.debug("[integration] Query");
        // route the query and send the query (call the response function of send to the right service)
        // mqtt or http
        // response is the service function corresponding to,  here we have a big matrix ...
        // mais en gros on doit envoyer ca dans un processeur de requetes par service pour que ce soit plus logique
        // qui va envoyer sa reponse sur response...

        this._queries.put(query.getQueryId(), query);

        // wait for the response to be updated
        while ( query.getState() == InterfaceQuery.QueryState.STATE_PENDING ) {
            Now.sleep(1);

        }
        return query;
    }

    public void response(InterfaceQuery query) {
        query.setResponse_ts(Now.NanoTime());
        query.setStateDone();
        log.debug("[integration] Response {} us", (query.getResponse_ts() - query.getQuery_ts())/1000);
        if ( this._queries.get(query.getQueryId()) == null ) {
            log.warn("[integration] Response not found (late) {} ms", Now.NowUtcMs() - query.getQuery_ms());
        }
        this._queries.remove(query.getQueryId());
    }

    @Scheduled(fixedRate = 1000)
    void garbage() {
        // remove all the queries that are too old
        long now = Now.NowUtcMs();
        ArrayList<String> toRemove = new ArrayList<>();
        this._queries.forEach((k,v) -> {
            if ( now - v.getQuery_ms() > this.maxQueryDuration ) {
                log.debug("[integration] Query {} timeout", v.getQueryId());
                v.setStateError();
                toRemove.add(k);
            }
        });
        toRemove.forEach(k -> this._queries.remove(k));
    }



}
