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
package com.disk91.integration.api.interfaces;

import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.tools.Now;
import org.springframework.context.annotation.Bean;

import java.util.UUID;

public class IntegrationQuery {


    public enum QueryState {
        STATE_PENDING, STATE_DONE, STATE_ERROR
    }

    public enum QueryType {
        TYPE_FIRE_AND_FORGET, TYPE_BROADCAST, TYPE_ASYNC, TYPE_SYNC
    }

    public enum QueryRoute {
        ROUTE_MQTT, ROUTE_DB, ROUTE_MEMORY
    }

    public static QueryRoute getRoutefromRouteString(String route) {
        if (route.equalsIgnoreCase("mqtt")) {
            return QueryRoute.ROUTE_MQTT;
        } else if (route.equalsIgnoreCase("db")) {
            return QueryRoute.ROUTE_DB;
        } else if (route.equalsIgnoreCase("memory")) {
            return QueryRoute.ROUTE_MEMORY;
        }
        return null;
    }

    // ----------------------------------------------
    //  Common elements for all queries
    protected UUID queryId;                       // Uid of the message to link with response
    protected ModuleCatalog.Modules serviceNameSource;       // Service name for the source related to package (users)
    protected String sourceInstanceId;            // ID of the source module to detect local processing
    protected ModuleCatalog.Modules serviceNameDest;         // Service name for the destination (users)
    protected QueryRoute route;                     // Route used for the message, mqtt, db, memory
    protected QueryType type;                       // Query type, fire & forget, broadcast, async, sync...
    protected int action;                           // Query action, value depends on services
    protected Object query;                         // Query parameters as an Object depending on action
    protected ActionResult response;                // Result of the Query, success or error like Exception
    protected Object result;                        // Query response as an Object depending on action
    protected QueryState state;                     // Query state, STATE_PENDING, STATE_DONE, STATE_ERROR
                                                    // Query access request parallelism support
    protected long query_ts;                        // Query start time ref, structure creation in ns
    protected long response_ts;                     // Response time ref, in ns
    protected long query_ms;                        // Query timestamp in ms for timeout
    protected long timeout_ms;                      // Query timeout in ms, default 10s (this is a duration)
    protected int processAttempts;                  // Number of process attempt for this query (max 1 for F&F, SYNC, ASYNC, more for Broadcast)
    protected boolean forLaterProcessing;           // Flag used by integration to queue but not process the message when sent after the app shutdown process start

    // ----------------------------------------------
    //  Message initialization
    public IntegrationQuery(ModuleCatalog.Modules serviceNameSource, String _sourceInstanceId) {
        this.query_ts = Now.NanoTime();
        this.query_ms = Now.NowUtcMs();
        this.queryId = java.util.UUID.randomUUID();
        this.serviceNameSource = serviceNameSource;
        this.sourceInstanceId = _sourceInstanceId;
        this.state = QueryState.STATE_PENDING;
        this.processAttempts = 0;
        this.forLaterProcessing = false;
    }


    // ----------------------------------------------
    //  Manage state update
    private final static Object lock = new Object();

    public QueryState getState() {
        synchronized (lock) {
            return state;
        }
    }

    public void setStateDone() {
        synchronized (lock) {
            state = QueryState.STATE_DONE;
            setResponse_ts(Now.NanoTime());
        }
    }

    public void setStateError() {
        synchronized (lock) {
            state = QueryState.STATE_ERROR;
            setResponse_ts(Now.NanoTime());
        }
    }

    // ----------------------------------------------
    // Manage rest of the structure



    public ModuleCatalog.Modules getServiceNameSource() {
        return serviceNameSource;
    }

    public void setServiceNameSource(ModuleCatalog.Modules serviceNameSource) {
        this.serviceNameSource = serviceNameSource;
    }

    public ModuleCatalog.Modules getServiceNameDest() {
        return serviceNameDest;
    }

    public void setServiceNameDest(ModuleCatalog.Modules serviceNameDest) {
        this.serviceNameDest = serviceNameDest;
    }

    public QueryType getType() {
        return type;
    }

    public void setType(QueryType type) {
        this.type = type;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public Object getQuery() {
        return query;
    }

    public void setQuery(Object query) {
        this.query = query;
    }

    public ActionResult getResponse() {
        return response;
    }

    public void setResponse(ActionResult response) {
        this.response = response;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public long getQuery_ts() {
        return query_ts;
    }

    public void setQuery_ts(long query_ts) {
        this.query_ts = query_ts;
    }

    public long getResponse_ts() {
        return response_ts;
    }

    public void setResponse_ts(long response_ts) {
        this.response_ts = response_ts;
    }

    public long getQuery_ms() {
        return query_ms;
    }

    public void setQuery_ms(long query_ms) {
        this.query_ms = query_ms;
    }

    public void setState(QueryState state) {
        this.state = state;
    }

    public long getTimeout_ms() {
        return timeout_ms;
    }

    public void setTimeout_ms(long timeout_ms) {
        this.timeout_ms = timeout_ms;
    }

    public QueryRoute getRoute() {
        return route;
    }

    public void setRoute(QueryRoute route) {
        this.route = route;
    }

    public UUID getQueryId() {
        return queryId;
    }

    public void setQueryId(UUID queryId) {
        this.queryId = queryId;
    }

    public int getProcessAttempts() {
        return processAttempts;
    }

    public void setProcessAttempts(int processAttempts) {
        this.processAttempts = processAttempts;
    }

    public boolean isForLaterProcessing() {
        return forLaterProcessing;
    }

    public void setForLaterProcessing(boolean forLaterProcessing) {
        this.forLaterProcessing = forLaterProcessing;
    }

    public String getSourceInstanceId() {
        return sourceInstanceId;
    }

    public void setSourceInstanceId(String sourceInstanceId) {
        this.sourceInstanceId = sourceInstanceId;
    }
}
