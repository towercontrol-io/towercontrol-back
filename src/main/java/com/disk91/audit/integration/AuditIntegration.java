/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2024.
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
package com.disk91.audit.integration;

import com.disk91.audit.config.AuditConfig;
import com.disk91.common.config.CommonConfig;
import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.tools.EncryptionHelper;
import com.disk91.common.tools.Now;
import com.disk91.integration.api.interfaces.InterfaceQuery;
import com.disk91.integration.services.IntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class AuditIntegration {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    protected AuditConfig auditConfig;

    @Autowired
    protected IntegrationService integrationService;

    private static final String IV = "4f7c2e9a8b1d3f6e7a9c0d2b5e8f1a3c";


    /** Create and encrypt the audit messages
     *
     * @param service - name of the module
     * @param action - name of the corresponding action
     * @param logStr - the log message with {x} for parameters
     * @param params - the parameters to encrypt
     */
    public AuditMessage creatNewAuditMessage(ModuleCatalog.Modules service, String action, String logStr, String[] params) {
        AuditMessage a = new AuditMessage();
        a.setService(ModuleCatalog.getServiceName(service));
        a.setAction(action);
        a.setActionMs(Now.NowUtcMs());
        a.setLogStr(logStr);
        a.setParams(new ArrayList<String>());
        if ( params != null ) {
            for (String param : params) {
                a.getParams().add(EncryptionHelper.encrypt(param, IV, commonConfig.getEncryptionKey()));
            }
        }
        return a;
    }

    /**
     * Converts a audit message into a string to be printed in the logs
     * @param a
     * @return
     */
    public String toString(AuditMessage a) {
        StringBuffer l = new StringBuffer();
        l.append(Now.formatToYYYYMMDDHHMMSSUtc(a.getActionMs()));
        l.append(" [").append(a.getService()).append("] ");
        l.append("[").append(a.getAction()).append("] ");
        ArrayList<String> _params = new ArrayList<>(a.getParams());
        if ( auditConfig.isAuditLogsDecryptionEnabled() ) {
            for ( int i = 0; i < _params.size(); i++ ) {
                _params.set(i, EncryptionHelper.decrypt(_params.get(i), IV, commonConfig.getEncryptionKey()));
            }
        }
        String _log = a.getLogStr();
        for ( int i = 0; i < _params.size(); i++ ) {
            _log = _log.replace("{" + i + "}", _params.get(i));
        }
        l.append(_log);
        return l.toString();
    }


    /**
     * Generate a simple audit log from a message with the source service
     * @param service
     * @param action
     * @param logStr
     * @param params
     */
    public void auditLog(ModuleCatalog.Modules service, String action, String logStr, String[] params) {
        AuditMessage message = this.creatNewAuditMessage(service, action, logStr, params);
        this.auditLog(message, service);
    }


    /**
     * Generate a simple audit log from a message with the source service
     * @param message
     * @param service
     */
    public void auditLog(AuditMessage message, ModuleCatalog.Modules service) {
        log.debug("[audit] Audit log: {}", message);

        InterfaceQuery query = new InterfaceQuery(service);
        query.setServiceNameDest(ModuleCatalog.Modules.AUDIT);
        query.setType(InterfaceQuery.QueryType.TYPE_FIRE_AND_FORGET);
        query.setAction(AuditActions.AUDIT_ACTION_POST_LOG.ordinal());
        query.setQuery(message);
        query.setRoute(InterfaceQuery.getRoutefromRouteString(auditConfig.getAuditIntegrationMedium()));
        this.auditLogIntegration(query);
    }

    public void auditLogIntegration(InterfaceQuery query) {
        log.debug("Audit log integration: {}", (String)query.getQuery());
        integrationService.processQuery(query);
    }

}
