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

package com.disk91.devices.mdb.repositories;

import com.disk91.devices.interfaces.DeviceState;
import com.disk91.devices.mdb.entities.Device;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DevicesRepository extends MongoRepository<Device,String> {

    public Device findOneDeviceById(String id);

    public List<Device> findDevicesByDataStreamId(String dataStreamId);


    @Query(value = "{ 'associatedGroups.groupId': { $in: ?0 } }")
    List<Device> findDevicesByAssociatedGroups(List<String> groupIds);

    /**
     * Search a device based on one of it's communicationId (like a LoRaWAN DevEUI)
     * @param type
     * @param value
     * @return
     */
    @Query(value = "{ 'communicationIds': { $elemMatch: { 'type': ?0, 'params.key': ?1, 'params.values' : ?2 } }, 'devState': { $in: ?3 } }")
    List<Device> findDevicesByCommunicationIdTypeAndParamAndStates(String type, String key, String value, List<DeviceState> states);

    /**
     * Get a list of devices with id greater than startId, ordered by id ascending
     * Purpose is to process all the devices in batches
     * @param startId
     * @param pageable
     * @return
     */
    List<Device> findByIdGreaterThanOrderByIdAsc(String startId, Pageable pageable);


}
