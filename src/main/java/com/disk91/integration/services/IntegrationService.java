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

import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.integration.api.interfaces.InterfaceQuery;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class IntegrationService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /*
     * For every service, we have a ConcurrentLinkedQueue to store all the pending
     * Queries, so a service que request all the pending messages addressed to it
     * one by one. This is used for the in-memory integration. In memory integration
     * works only for a single instance with all the services running in the same.
     */
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<InterfaceQuery>>  _queries = new ConcurrentHashMap<>();

    @PostConstruct
    public void initIntegration() {
        log.debug("[integration] Init");
    }


    /**
     * Process the query depending on the mode and the route, not sure about the right
     * way to make it currently.... let see later, fire& forget is easy, it is just async
     * without any later consideration
     * @param query
     * @return
     */
    public InterfaceQuery processQuery(InterfaceQuery query) {
        log.debug("[integration] Process query {}", query.getQueryId());

        switch ( query.getType() ) {
            case TYPE_FIRE_AND_FORGET -> {
                // process depends on route
                switch (query.getRoute()) {
                    case ROUTE_MEMORY -> {
                        // in memory integration
                        // add the query to the queue of the service
                        if (this._queries.get(ModuleCatalog.getServiceName(query.getServiceNameDest())) == null) {
                            this._queries.put(ModuleCatalog.getServiceName(query.getServiceNameDest()), new ConcurrentLinkedQueue<>());
                        }
                        this._queries.get(ModuleCatalog.getServiceName(query.getServiceNameDest())).add(query);
                    }
                    case ROUTE_DB, ROUTE_MQTT -> log.error("[integration] Unknown route {} not yet implemented", query.getRoute());
                }
            }
        }
        return query;
    }


    public InterfaceQuery getQuery(ModuleCatalog.Modules service, InterfaceQuery.QueryRoute route)
    throws ITNotFoundException, ITParseException {
        switch ( route ) {
            case ROUTE_MEMORY -> {
                // get on pending query from the given service
                ConcurrentLinkedQueue<InterfaceQuery> q = this._queries.get(ModuleCatalog.getServiceName(service));
                if (q == null || q.isEmpty()) throw new ITNotFoundException("No pending query");

                // get the first query
                return q.poll();
            }
        }
        throw new ITParseException("Unsupported route type");
    }


    /*

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
    */


    /**
     * Clean the outdated pending queries every second
     */
    @Scheduled(fixedRate = 1000)
    void garbage() {
        // remove all the queries that are too old
        long now = Now.NowUtcMs();
        this._queries.forEach((k,v) -> {
            // scan the linked list
            ArrayList<String> toRemove = new ArrayList<>();
            for (InterfaceQuery q : v) {
                if ( now - (q.getQuery_ms()+q.getTimeout_ms()) > 0 ) {
                    log.debug("[integration] Query {} timeout", q.getQueryId());
                    q.setStateError();
                    toRemove.add(k);
                }
            }
            // remove the queries that are too old
            toRemove.forEach(v::remove);
        });
    }



}
