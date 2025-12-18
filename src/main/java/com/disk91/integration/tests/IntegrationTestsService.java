package com.disk91.integration.tests;

import com.disk91.audit.integration.AuditIntegration;
import com.disk91.audit.integration.AuditMessage;
import com.disk91.audit.services.AuditService;
import com.disk91.capture.api.interfaces.CaptureEndpointCreationBody;
import com.disk91.capture.mdb.entities.CaptureEndpoint;
import com.disk91.capture.mdb.repositories.CaptureEndpointRepository;
import com.disk91.capture.services.CaptureEndpointCache;
import com.disk91.capture.services.CaptureEndpointService;
import com.disk91.capture.services.CaptureIngestService;
import com.disk91.common.config.CommonConfig;
import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.tests.CommonTestsService;
import com.disk91.common.tools.CustomField;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.devices.interfaces.DeviceState;
import com.disk91.devices.mdb.entities.Device;
import com.disk91.devices.mdb.entities.sub.DevAttribute;
import com.disk91.devices.mdb.entities.sub.DeviceHistoryReason;
import com.disk91.devices.services.DeviceCache;
import com.disk91.groups.config.ActionCatalog;
import com.disk91.users.tests.UsersTestsService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class IntegrationTestsService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CommonTestsService commonTestsService;

    @Autowired
    protected CommonConfig commonConfig;


    @Autowired
    protected AuditIntegration auditIntegration;

    /**
     * This function is creating tests for the Integration module
     * It will execute some Audit message on screen and verify some message processing
     *
     * @throws ITParseException
     */
    public void runTests() throws ITParseException {

        commonTestsService.info("[integration] Create 10 audit message to check processing");

        for ( int i = 0 ; i < 10 ; i++ ) {
            auditIntegration.auditLog(
                    ModuleCatalog.Modules.CUSTOM,
                    ActionCatalog.getActionName(ActionCatalog.Actions.CREATION),
                    "test-audit-action",
                    i+" This is a test audit log with parameters: {0}, {1}, {2}",
                    new String[]{"param-1", "param-2", "param-3"}
            );
        }


    }
}

