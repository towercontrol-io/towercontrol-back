package com.disk91.billing.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = {"file:configuration/billing.properties"}, ignoreResourceNotFound = true)
public class BillingConfig {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // ----------------------------------------------
    // Integration setup
    // ----------------------------------------------

    // How the audit service is connected to the different service instances
    @Value("${billing.integration.medium:memory}")
    protected String billingIntegrationMedium;
    public String getBillingIntegrationMedium() {
        return billingIntegrationMedium;
    }

    @Value("${billing.integration.timeout.ms:10000}")
    protected long billingIntegrationTimeout;
    public long getBillingIntegrationTimeout() {
        return billingIntegrationTimeout;
    }


}
