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
package com.disk91.capture.services;

import com.disk91.audit.integration.AuditIntegration;
import com.disk91.capture.api.interfaces.CaptureEndpointCreationBody;
import com.disk91.capture.api.interfaces.CaptureEndpointResponseItf;
import com.disk91.capture.api.interfaces.CaptureProtocolResponseItf;
import com.disk91.capture.config.ActionCatalog;
import com.disk91.capture.config.CaptureConfig;
import com.disk91.capture.mdb.entities.CaptureEndpoint;
import com.disk91.capture.mdb.entities.Protocols;
import com.disk91.capture.mdb.entities.sub.MandatoryField;
import com.disk91.capture.mdb.repositories.CaptureEndpointRepository;
import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.tools.CustomField;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.RandomString;
import com.disk91.common.tools.Tools;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.services.UserCommon;
import com.disk91.users.services.UsersRolesCache;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CaptureEndpointService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected AuditIntegration auditIntegration;

    @Autowired
    protected UserCommon userCommon;

    @Autowired
    protected CaptureProtocolsCache captureProtocolsCache;

    @Autowired
    protected CaptureEndpointRepository captureEndpointRepository;

    @Autowired
    protected CaptureConfig captureConfig;

    // =====================================================================================================
    // FRONT-END API / Protocol List
    // =====================================================================================================

    public List<CaptureProtocolResponseItf> listProtocols() {

        ArrayList<CaptureProtocolResponseItf> ret = new ArrayList<>();
        for ( Protocols p : captureProtocolsCache.getProtocols() ) {
            ret.add( CaptureProtocolResponseItf.createFromProtocol(p) );
        }

        return ret;
    }

    // =====================================================================================================
    // FRONT-END API / Endpoint List
    // =====================================================================================================

    public List<CaptureEndpointResponseItf> listEndpoints(
            String userLogin
    ) throws ITRightException {
        try {
            User u = userCommon.getUser(userLogin);
            ArrayList<CaptureEndpointResponseItf> ret = new ArrayList<>();
            if ( u.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN) ) {
                // list all
                List<CaptureEndpoint> all = captureEndpointRepository.findAllCaptureEndpointByOrderByCreationMsDesc();
                for ( CaptureEndpoint endp : all ) {
                    // convert to response
                    ret.add( CaptureEndpointResponseItf.fromCaptureEndpoint(endp) );
                }
            } else {
                // list only own
                List<CaptureEndpoint> my = captureEndpointRepository.findCaptureEndpointByOwnerOrderByCreationMsDesc(u.getLogin());
                for ( CaptureEndpoint endp : my ) {
                    // convert to response
                    ret.add( CaptureEndpointResponseItf.fromCaptureEndpoint(endp) );
                }
            }
            return ret;
        } catch (ITNotFoundException x) {
            log.warn("[capture] User not found when listing capture endpoints: {}", userLogin);
            throw new ITRightException("capture-endpoint-list-user-not-authorized");
        }
    }

    // =====================================================================================================
    // FRONT-END API / Endpoint Creation
    // =====================================================================================================

    /**
     * Create a capture endpoint referring a given protocol and the associated configuration
     * @param req - request will all information related to the user
     * @param body - body containing the endpoint creation information
     * @throws ITRightException
     * @throws ITParseException
     */
    public CaptureEndpoint createCaptureEndpoint(
            HttpServletRequest req,
            CaptureEndpointCreationBody body
    ) throws ITRightException, ITParseException, ITNotFoundException {
        try {
            User u = userCommon.getUser(req.getUserPrincipal().getName());
            return createCaptureEndpoint(
                    u.getLogin(),
                    Tools.getRemoteIp(req),
                    body
            );
        } catch (ITNotFoundException x) {
            log.warn("[capture] User not found when creating capture endpoint: {}", req.getUserPrincipal().getName());
            throw new ITRightException("capture-endpoint-user-not-authorized");
        }
    }



    /**
     * Create a capture endpoint referring a given protocol and the associated configuration
     * @param userLogin - User id
     * @param userIp - User IP
     * @param body - body containing the endpoint creation information
     * @throws ITRightException
     * @throws ITParseException
     */
    public CaptureEndpoint createCaptureEndpoint(
            String userLogin,
            String userIp,
            CaptureEndpointCreationBody body
    ) throws ITParseException, ITNotFoundException {
        CaptureEndpoint endp = new CaptureEndpoint();
        // default data from the body
        if ( body.getName() != null && !body.getName().isEmpty() ) {
            endp.setName(body.getName());
        } else {
            throw new ITParseException("capture-endpoint-name-missing");
        }
        if ( body.getDescription() == null ) {
            endp.setDescription("");
        } else {
            endp.setDescription(body.getDescription());
        }
        endp.setEncrypted(body.isEncrypted());

        // check the protocol
        if ( body.getProtocolId() == null ) throw new ITParseException("capture-endpoint-protocol-missing");
        try {
            Protocols p = captureProtocolsCache.getProtocol(body.getProtocolId());
            endp.setProtocolId(p.getId());

            // Check the mandatory fields
            if ( ! p.getMandatoryFields().isEmpty() && ( body.getCustomConfig() == null || body.getCustomConfig().isEmpty() ) ) {
                throw new ITParseException("capture-endpoint-mandatory-fields-missing");
            }
            for (MandatoryField m : p.getMandatoryFields()) {
                boolean found = false;
                for ( CustomField cf : body.getCustomConfig() ) {
                    if ( cf.getName().equals(m.getName()) && cf.getValue() != null ) {
                        found = true;
                        break;
                    }
                }
                if ( !found ) {
                    throw new ITParseException("capture-endpoint-mandatory-field-missing");
                }
            }
            endp.setCustomConfig(new ArrayList<>());
            for ( CustomField cf : body.getCustomConfig() ) {
                endp.getCustomConfig().add( cf.clone() );
            }

            // Generate a ref for the endpoint
            boolean found;
            int maxTry = 30;
            do {
                found = false;
                maxTry--;
                endp.setRef(RandomString.getRandomAZString(10));
                if (captureEndpointRepository.findOneByRef(endp.getRef()) != null) {
                    found = true;
                }
            } while (found && maxTry > 0);
            if (found) {
                throw new ITParseException("capture-endpoint-ref-generation-failed");
            }
            endp.setOwner(userLogin);
            endp.setCreationMs(Now.NowUtcMs());
            if (body.isForceWideOpen()) {
                endp.setWideOpen(true);
            } else {
                endp.setWideOpen(p.isDefaultWideOpen());
            }

            // Add the default data processor
            endp.setProcessingClassName(captureConfig.getCaptureDataProcessorClass());

            // Save the endpoint
            captureEndpointRepository.save(endp);

            // Trace with Audit
            auditIntegration.auditLog(
                    ModuleCatalog.Modules.CAPTURE,
                    ActionCatalog.getActionName(ActionCatalog.Actions.CREATE),
                    userLogin,
                    "User {0} has created a new endpoint with ref {1} from IP {2}",
                    new String[]{userLogin, endp.getRef(), userIp}
            );
            return endp;
        } catch (ITNotFoundException x) {
            throw new ITParseException("capture-endpoint-protocol-missing");
        }
    }

}
