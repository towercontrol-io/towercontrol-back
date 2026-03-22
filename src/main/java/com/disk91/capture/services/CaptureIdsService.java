/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2026.
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
import com.disk91.capture.Capture;
import com.disk91.capture.api.interfaces.*;
import com.disk91.capture.config.CaptureConfig;
import com.disk91.capture.mdb.entities.CaptureEndpoint;
import com.disk91.capture.mdb.entities.ProtocolIds;
import com.disk91.capture.mdb.entities.Protocols;
import com.disk91.capture.mdb.entities.sub.MandatoryField;
import com.disk91.capture.mdb.entities.sub.ProtocolId;
import com.disk91.capture.mdb.repositories.ProtocolIdsRepository;
import com.disk91.common.config.CommonConfig;
import com.disk91.common.tools.CustomField;
import com.disk91.common.tools.EncryptionHelper;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITNotFoundException;
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
import java.util.HashMap;
import java.util.List;

import static com.disk91.capture.api.interfaces.sub.InsertIDsStatus.*;
import static com.disk91.capture.mdb.entities.sub.IdStateEnum.ASSIGNED;
import static com.disk91.capture.mdb.entities.sub.IdStateEnum.IN_USE;

@Service
public class CaptureIdsService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected AuditIntegration auditIntegration;

    @Autowired
    protected UserCommon userCommon;

    @Autowired
    protected CaptureProtocolsCache captureProtocolsCache;

    @Autowired
    protected CaptureEndpointCache captureEndpointCache;

    @Autowired
    protected ProtocolIdsRepository protocolIdsRepository;

    @Autowired
    protected CaptureConfig captureConfig;

    @Autowired
    protected CommonConfig commonConfig;

    // =====================================================================================================
    // FRONT-END API / Insert New IDs
    // =====================================================================================================


    /**
     * `insertIDs` lets you add IDs to the ID database.
     *  For each ID, the system checks that it matches the expected data format, then scans the entire list to determine
     *  how many errors there may be. If there are no errors in the whole list, the IDs can be inserted into the database.
     *  If an error is found, the process stops on the line containing the first error.
     *
     * @param requestorId
     * @param body
     * @param request
     * @return
     * @throws ITRightException
     */
    public CaptureInsertIdsResponseItf insertIds(
            String requestorId,
            CaptureInsertIdsBody body,
            HttpServletRequest request
    ) throws ITRightException {

        CaptureInsertIdsResponseItf ret = new CaptureInsertIdsResponseItf();
        ret.setInserted(0);
        ret.setErrorCount(0);
        ret.setErrorCount(0);
        ret.setErrorFirstLine(0);
        if (requestorId == null || requestorId.isEmpty()) {
            throw new ITRightException("user-profile-login-invalid");
        }
        try {
            // get user considering API key
            User _requestor = userCommon.getUser(requestorId);
            try {
                CaptureEndpoint e = captureEndpointCache.getCaptureEndpoint(body.getCaptureId());
                if ( e.getOwner().compareTo(_requestor.getLogin()) == 0 || _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN) ) {
                    // authorized to insert ; create the Array list in memory to insert in DB if no error
                    List<ProtocolIds> idsToInsert = new ArrayList<>();

                    // get the related definitions
                    Protocols p = null;
                    ProtocolId pid = null;
                    try {
                        p = captureProtocolsCache.getProtocol(e.getProtocolId());
                        for ( ProtocolId _pid : p.getProtocolIds() ) {
                            if ( _pid.getName().compareTo(e.getIdTypeName()) == 0 ) {
                                pid = _pid;
                                break;
                            }
                        }
                        if ( pid == null ) {
                            ret.setStatus(INVALID_TYPE_ID);
                            return ret;
                        }
                    } catch (ITNotFoundException ex) {
                        ret.setStatus(INVALID_PROTOCOL_ID);
                        return ret;
                    }

                    // Make sure status is valid
                    switch ( body.getInitialState() ) {
                        case UNKNOWN:               // Will be dynamically set later by the driver
                        case NOT_ASSIGNED:
                        case ASSIGNED:
                        case IN_USE:
                            break;
                        case RETURNED:
                        case EXPIRED_RETURNED:
                        case EXPIRED_IN_USE:
                        case REMOVED:
                            ret.setStatus(INVALID_STATUS);
                            return ret;
                    }

                    // check the header format and all are the one expected
                    String[] headers = body.getHeaders().split("[;,]");
                    HashMap<String, MandatoryField> mandatoryFieldHashMap = new HashMap<>();
                    if ( headers.length == pid.getMandatoryFields().size() ) {
                        HashMap<String, Integer> expectedHeaders = new HashMap<>();
                        for ( MandatoryField mf : pid.getMandatoryFields() ) {
                            expectedHeaders.put(mf.getName().toLowerCase(), 0);
                            mandatoryFieldHashMap.put(mf.getName().toLowerCase(), mf);
                        }
                        for (String header : headers) {
                            if( expectedHeaders.containsKey(header.toLowerCase()) ) {
                                if (expectedHeaders.get(header) == 0 ) {
                                    expectedHeaders.put(header, expectedHeaders.get(header) + 1);
                                } else {
                                    ret.setStatus(DUPLICATED_HEADER);
                                    return ret;
                                }
                            } else {
                                ret.setStatus(INVALID_HEADER);
                                return ret;
                            }
                        }
                    }

                    // header is correct, process the lines
                    int errorCount = 0;
                    int inserted = 0;
                    int cLine = 0;
                    for ( String line : body.getIds() ) {
                        cLine++;
                        String[] values = line.split("[;,]");
                        if ( values.length == pid.getMandatoryFields().size() ) {
                            ProtocolIds _id = new ProtocolIds();
                            _id.setProtocolId(p.getId());
                            _id.setConfigTypeId(e.getIdTypeName());
                            _id.setCaptureId(e.getRef());
                            _id.setState(body.getInitialState());
                            _id.setCreationBy(requestorId);
                            _id.setCreationMs(Now.NowUtcMs());
                            _id.setUpdateMs(0);
                            _id.setLastScanMs(0);
                            if ( body.getInitialState() == IN_USE ||  body.getInitialState() == ASSIGNED ) {
                                _id.setAssignedMs(Now.NowUtcMs());
                            } else _id.setAssignedMs(0);
                            _id.setReleasedMs(0);
                            _id.setSubscriptionStartMs(0);
                            _id.setSubscriptionEndMs(0);
                            _id.setRemovalMs(0);
                            _id.setCustomConfig(new ArrayList<>());
                            // Process fields
                            for ( int i = 0 ; i < headers.length ; i++ ) {
                                MandatoryField mf = mandatoryFieldHashMap.get(headers[i].toLowerCase());
                                if ( mf != null ) {
                                    if ( mf.isValueValid( values[i] ) ) {
                                        if ( mf.isEncrypted() ) {
                                            String encryptedValue = "enc_"+ EncryptionHelper.encrypt(values[i], Capture.__iv, commonConfig.getEncryptionKey());
                                            values[i] = encryptedValue;
                                        }
                                        _id.getCustomConfig().add(CustomField.of(mf.getName(), values[i]));

                                        if ( mf.isUnique() ) {
                                            // We need to make sure we don't have an existing value for these fields and endpoint
                                            if ( protocolIdsRepository.findByCaptureIdAndCustomConfigNameAndValue(
                                                    e.getRef(),
                                                    mf.getName(),
                                                    values[i]
                                            ).isPresent() ) {
                                                if ( ret.getErrorFirstLine() == 0 ) { ret.setStatus(NOT_UNIQUE); ret.setErrorFirstLine(cLine); }
                                                errorCount++;
                                                break;
                                            }
                                        }

                                    } else {
                                        if ( ret.getErrorFirstLine() == 0 ) { ret.setStatus(MALFORMED_DATA); ret.setErrorFirstLine(cLine); }
                                        errorCount++;
                                        break;
                                    }
                                } else {
                                    // inconsistent as header should have been checked before, but just in case, we check again
                                    if ( ret.getErrorFirstLine() == 0 ) { ret.setStatus(INVALID_HEADER); ret.setErrorFirstLine(cLine); }
                                    errorCount++;
                                    break;
                                }
                            }
                            idsToInsert.add(_id);
                        } else {
                            if ( ret.getErrorFirstLine() == 0 ) { ret.setStatus(MISSING_DATA); ret.setErrorFirstLine(cLine); }
                            errorCount++;
                        }
                    }
                    if ( errorCount == 0 ) {
                        // no error, we can insert the list in DB
                        for (ProtocolIds _id : idsToInsert) {
                            protocolIdsRepository.save(_id);
                            inserted++;
                        }
                        ret.setInserted(inserted);
                        ret.setStatus(INSERTED);
                    }
                } else {
                    throw new ITRightException("capture-endpoint-user-not-authorized");
                }
            } catch ( ITNotFoundException x ) {
                throw new ITRightException("capture-endpoint-user-not-authorized");
            }
        } catch (ITNotFoundException x) {
            throw new ITRightException("user-profile-user-not-found");
        }
        return ret;
    }


}
