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
package com.disk91.billing.integration;

import com.disk91.billing.config.BillingConfig;
import com.disk91.common.config.CommonConfig;
import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.tools.CustomField;
import com.disk91.common.tools.exceptions.ITOverQuotaException;
import com.disk91.integration.api.interfaces.IntegrationQuery;
import com.disk91.integration.services.IntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class BillingIntegration {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    protected IntegrationService integrationService;

    @Autowired
    protected BillingConfig billingConfig;


    /**
     * Report a billing event to the billing service from different kind of situations
     * Billing service is part of NCE, this interface is part of NCE but only processed
     * by NCE parts.
     */
    public void billingLog(ModuleCatalog.Modules sourceService, BillingActions action, ArrayList<CustomField> param) {
        log.debug("[billing] new log: action {} from {}", action, sourceService);

        IntegrationQuery query = new IntegrationQuery(sourceService, commonConfig.getInstanceId());
        query.setServiceNameDest(ModuleCatalog.Modules.BILLING);
        query.setType(IntegrationQuery.QueryType.TYPE_FIRE_AND_FORGET);
        query.setAction(action.ordinal());
        query.setQuery(param);
        query.setRoute(IntegrationQuery.getRoutefromRouteString(billingConfig.getBillingIntegrationMedium()));
        try {
            integrationService.processQuery(query);
        } catch (ITOverQuotaException e) {
            // Skip billing log, we are closing the service
            // @TODO something we should not lose ?
        }
    }


}
