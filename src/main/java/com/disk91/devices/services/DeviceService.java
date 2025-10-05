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
package com.disk91.devices.services;


import com.disk91.devices.config.DevicesConfig;
import com.disk91.devices.mdb.entities.Device;
import com.disk91.devices.mdb.repositories.DevicesHistoryRepository;
import com.disk91.devices.mdb.repositories.DevicesRepository;
import com.disk91.users.mdb.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeviceService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Group Cache Service is caching the Group Information. It may be instantiated in all the instances
     * and multiple cache should collaborate in a cluster
     */

    @Autowired
    protected DevicesConfig deviceConfig;

    @Autowired
    protected DevicesRepository devicesRepository;

    @Autowired
    protected DevicesHistoryRepository devicesHistoryRepository;

    @Autowired
    protected DeviceCache deviceCache;


    /**
     * Get all the devices for a given user whatever the device state is. The system will look at the group the
     * user belongs to and have read access to and will return all the devices in these groups.
     * @param user
     * @return
     */
    public List<Device> getUserDevices(User user) {
        // @TODO : here we do not check the roles & ACL, so this should be later modified with
        //         access to a list of groups where we have this minimum level.
        ArrayList<String> groups = user.getAllGroups(false,true);
        List<Device> devices = devicesRepository.findDevicesByAssociatedGroups(groups);
        return devices;
    }

}
