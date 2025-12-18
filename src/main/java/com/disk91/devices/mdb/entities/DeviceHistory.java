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

/**
 * UserPending -  This class stores account creation requests awaiting email validation and handles confirmations.
 * Entries are automatically removed either upon activation or after a specified period.
 */

package com.disk91.devices.mdb.entities;


import com.disk91.common.tools.Now;
import com.disk91.devices.mdb.entities.sub.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;

@Document(collection = "devices_history")
@CompoundIndexes({
        @CompoundIndex(name = "id_date", def = "{'deviId': 'hashed', 'dateMs': 1}"),
})
public class DeviceHistory extends Device {

    // As the Id must be uniq, we need to move device Id into devId
    protected String devId;

    // The reason why the device has been saved in history ( state change... )
    protected DeviceHistoryReason reason;
    // The date of the event
    protected long dateMs;

    // ============== constructor from the parent class
    public static DeviceHistory getDeviceHistory(Device d, DeviceHistoryReason reason) {
        DeviceHistory u = new DeviceHistory();
        u.setReason(reason);
        u.setDateMs(Now.NowUtcMs());

        u.setDevId(d.getId());
        u.setVersion(d.getVersion());
        u.setInstanceOfId(d.getInstanceOfId());
        u.setPublicId(d.getPublicId());
        u.setHardwareIds(new ArrayList<>());
        for (DevHardwareId hardwareId : d.getHardwareIds()) {
            u.getHardwareIds().add(hardwareId.clone());
        }
        u.setNearFieldId(d.getNearFieldId());
        u.setCustomerId(d.getCustomerId());
        u.setDataStreamId(d.getDataStreamId());
        u.setOnboardingId(d.getOnboardingId());
        u.setFamilyId(d.getFamilyId());
        u.setName(d.getName());
        u.setDescription(d.getDescription());
        u.setDevState(d.getDevState());
        if ( u.getSubState() != null ) {
            u.setSubState(d.getSubState().clone());
        } else {
            u.setSubState(DevSubState.initNone());
        }
        u.setSubState(d.getSubState().clone());
        u.setDevStateDateMs(d.getDevStateDateMs());
        u.setLastSeenDateMs(d.getLastSeenDateMs());
        u.setLastRestartDateMs(d.getLastRestartDateMs());
        u.setFirmwareVersion(d.getFirmwareVersion());
        u.setHardwareVersion(d.getHardwareVersion());
        u.setBatteryType(d.getBatteryType());
        u.setBatteryCapacity(d.getBatteryCapacity());
        u.setBatteryLowLevel(d.getBatteryLowLevel());
        u.setBatteryLevel(d.getBatteryLevel());
        u.setCreationBy(d.getCreationBy());
        u.setCreationOnMs(d.getCreationOnMs());
        u.setUpdatedBy(d.getUpdatedBy());
        u.setUpdatedOnMs(d.getUpdatedOnMs());
        u.setBillingPeriodStartMs(d.getBillingPeriodStartMs());
        u.setBillingPeriodEndMs(d.getBillingPeriodEndMs());
        u.setBillingGroupId(d.getBillingGroupId());
        u.setDynamicLocation(d.isDynamicLocation());
        u.setLocation(d.getLocation().clone());
        u.setDataEncrypted(d.isDataEncrypted());
        u.setCommunicationIds(new ArrayList<>());
        for (DevAttribute communicationId : d.getCommunicationIds()) {
            u.getCommunicationIds().add(communicationId.clone());
        }
        u.setAttributes(new ArrayList<>());
        for (DevAttribute attribute : d.getAttributes()) {
            u.getAttributes().add(attribute.clone());
        }
        u.setAssociatedGroups(new ArrayList<>());
        for (DevGroupAssociated group : d.getAssociatedGroups()) {
            u.getAssociatedGroups().add(group.clone());
        }
        return u;
    }



    // ====== Getters & Setters ========


    public DeviceHistoryReason getReason() {
        return reason;
    }

    public void setReason(DeviceHistoryReason reason) {
        this.reason = reason;
    }

    public long getDateMs() {
        return dateMs;
    }

    public void setDateMs(long dateMs) {
        this.dateMs = dateMs;
    }

    public String getDevId() {
        return devId;
    }

    public void setDevId(String devId) {
        this.devId = devId;
    }
}
