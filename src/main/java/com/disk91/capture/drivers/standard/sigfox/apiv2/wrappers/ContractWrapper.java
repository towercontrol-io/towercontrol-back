/*
 * Copyright (c) 2018.
 *
 *  This is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  this software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  -------------------------------------------------------------------------------
 *  Author : Paul Pinault aka disk91
 *  See https://www.disk91.com
 *
 *  Commercial license of this software can be obtained contacting disk91.com or ingeniousthings.fr
 *  -------------------------------------------------------------------------------
 *
 */

package com.disk91.capture.drivers.standard.sigfox.apiv2.wrappers;

import com.disk91.capture.drivers.standard.sigfox.apiv2.models.SigfoxApiv2ContractInfo;
import com.disk91.capture.drivers.standard.sigfox.apiv2.models.SigfoxApiv2ContractListResponse;
import com.disk91.capture.drivers.standard.sigfox.apiv2.services.ITSigfoxConnection;
import com.disk91.capture.drivers.standard.sigfox.apiv2.services.ITSigfoxConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class ContractWrapper {

    private static final Logger log = LoggerFactory.getLogger(ContractWrapper.class);

    /**
     * Get the list of all the Contracts actually accessible with an API account
     * @param login
     * @param password
     * @return
     * @throws ITSigfoxConnectionException
     */
    public static SigfoxApiv2ContractListResponse getAllAssociatedContracts(
            String apiBackend,
            String login,
            String password
    ) throws ITSigfoxConnectionException {

        try {
            ITSigfoxConnection<String, SigfoxApiv2ContractListResponse> contractListRequest = new ITSigfoxConnection<>(
                    apiBackend,
                    login,
                    password
            );
            return contractListRequest.execute(
                    "GET",
                    "/api/v2/contract-infos/",
                    null,
                    null,
                    null,
                    SigfoxApiv2ContractListResponse.class
            );
        } catch (ITSigfoxConnectionException x) {
            throw new ITSigfoxConnectionException(HttpStatus.BAD_REQUEST,"Sigfox communication error");
        }
    }

    /**
     * Get the list of all the Contracts actually accessible with an API account
     * Filter the valid contracts
     * @TODO : filtering not in place, function created for later use. in APIv2 state field has been removed potentially the end of communication field can be used
     *
     * @param login
     * @param password
     * @return
     * @throws ITSigfoxConnectionException
     */
    public static SigfoxApiv2ContractListResponse getAllAssociatedValidContracts(
            String apiBackend,
            String login,
            String password
    ) throws ITSigfoxConnectionException {

        return getAllAssociatedContracts(apiBackend,login,password);

    }


    /**
     * Get details about One contract from Sigfox API
     * @param login
     * @param password
     * @return
     * @throws ITSigfoxConnectionException
     */
    public static SigfoxApiv2ContractInfo getOneContract(
            String apiBackend,
            String login,
            String password,
            String contractId
    ) throws ITSigfoxConnectionException {

        try {

            ITSigfoxConnection<String, SigfoxApiv2ContractInfo> contractListRequest = new ITSigfoxConnection<>(
                    apiBackend,
                    login,
                    password
            );
            return contractListRequest.execute(
                    "GET",
                    "/api/v2/contract-infos/" + contractId,
                    null,
                    null,
                    null,
                    SigfoxApiv2ContractInfo.class
            );
        } catch (ITSigfoxConnectionException x) {
            log.error("[capture][sigfox] Problem accessing sigfox in Contract Wrapper... 1");
            return null;

        }
    }

}
