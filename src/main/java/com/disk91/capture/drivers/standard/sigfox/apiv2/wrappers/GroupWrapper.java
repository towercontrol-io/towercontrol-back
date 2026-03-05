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

import com.disk91.capture.drivers.standard.sigfox.apiv2.models.SigfoxApiv2Group;
import com.disk91.capture.drivers.standard.sigfox.apiv2.models.SigfoxApiv2GroupListResponse;
import com.disk91.capture.drivers.standard.sigfox.apiv2.services.ITSigfoxConnection;
import com.disk91.capture.drivers.standard.sigfox.apiv2.services.ITSigfoxConnectionException;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

public class GroupWrapper {

    private static final Logger log = LoggerFactory.getLogger(GroupWrapper.class);

    /**
     * Get the list of all the Contracts actually accessible with an API account
     * @param login
     * @param password
     * @return
     * @throws ITSigfoxConnectionException
     */
    public static  List<SigfoxApiv2Group> getGroupDetailAndSub(
            String login,
            String password,
            String groupId
    ) throws ITSigfoxConnectionException, ITNotFoundException {

        ArrayList<SigfoxApiv2Group> groupsList = new ArrayList<>();
        try {

            ITSigfoxConnection<String, SigfoxApiv2Group> groupRequest = new ITSigfoxConnection<>(
                    login,
                    password
            );
            SigfoxApiv2Group group = groupRequest.execute(
                    "GET",
                    "/api/v2/groups/"+groupId+"/",
                    null,
                    null,
                    null,
                    SigfoxApiv2Group.class
            );
            if ( group == null ) throw new ITNotFoundException("sigfox-group-not-found");
            log.debug("[capture][sigfox] found valid : {}", group.getName());
            groupsList.add(group);

            ITSigfoxConnection<String, SigfoxApiv2GroupListResponse> groupListRequest = new ITSigfoxConnection<>(
                    login,
                    password
            );
            SigfoxApiv2GroupListResponse groups = groupListRequest.execute(
                    "GET",
                    "/api/v2/groups/",
                    "parentIds="+groupId,
                    null,
                    null,
                    SigfoxApiv2GroupListResponse.class
            );

            if ( groups != null ) {
                groupsList.addAll(groups.getData());
            }

        } catch (ITSigfoxConnectionException x) {
            throw new ITSigfoxConnectionException(HttpStatus.BAD_REQUEST,"Sigfox communication error");
        }
        return groupsList;
    }


}
