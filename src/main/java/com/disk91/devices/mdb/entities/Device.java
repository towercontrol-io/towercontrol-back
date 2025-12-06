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


import com.disk91.common.tools.CloneableObject;
import com.disk91.common.tools.Now;
import com.disk91.devices.interfaces.DeviceBatType;
import com.disk91.devices.interfaces.DeviceState;
import com.disk91.devices.mdb.entities.sub.DevAttribute;
import com.disk91.devices.mdb.entities.sub.DevGroupAssociated;
import com.disk91.devices.mdb.entities.sub.DevHardwareId;
import com.disk91.devices.mdb.entities.sub.DevLocation;
import com.uber.h3core.H3Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.disk91.devices.interfaces.DeviceBatType.UNKNOWN_BATTERY_TYPE;
import static com.disk91.devices.interfaces.DeviceState.IDENTIFIED;

@Document(collection = "devices")
@CompoundIndexes({
        @CompoundIndex(name = "publicId", def = "{'publicId': 'hashed'}"),
        @CompoundIndex(name = "nearFieldId", def = "{'nearFieldId': 'hashed'}"),
        @CompoundIndex(name = "customerId", def = "{'customerId': 'hashed'}"),
        @CompoundIndex(name = "dataStreamId", def = "{'dataStreamId': 'hashed'}"),
        @CompoundIndex(name = "name", def = "{'name': 'hashed'}"),
        @CompoundIndex(name = "comm_type_param_idx", def = "{'communicationIds.type': 1, 'communicationIds.param': 1}")
})
public class Device implements CloneableObject<Device> {

    @Transient
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Transient
    public static final int DEVICE_VERSION = 1;

    @Id
    protected String id;

    // The device structure version
    protected int version;

    // This represents the ID of a master device to which this device is linked.
    // It is used when a reusable physical device is instantiated as a new, distinct device,
    // while maintaining a reference to the original physical device.
    // Format - MongoID ; refer to the master device ID field
    protected String instanceOfId;

    // This is a random key that can be generated to make this object publicly accessible to anyone who knows
    // this key. For example, to track a package, the user can know this key (tracking number) which allows them to access the
    // data of this specific object.
    protected String publicId;

    // This is an identification associated with the object, hardware-related, and can be used to search for
    // it. It can be a MAC address, a Sigfox ID, or something else. An object can have multiple hardware identifiers.
    protected ArrayList<DevHardwareId> hardwareIds;

    // This is an identification associated with the object that will be easily manipulated by the user,
    // for example, for onboarding, a QR code, an NFC ID, or RFID; it is randomly generated or assigned during the manufacturing process.
    protected String nearFieldId;

    // This is a business-related identification, assigned by the user, for example, an order number or an
    // ID from another system.
    protected String customerId;

    // This identifier links to a data stream, aiming to decouple the data from the physical object. This way,
    // it is possible to clean the data of an object without necessarily deleting it, or to link data even if the object has been
    // replaced. This is useful in case of failure, for example.
    protected String dataStreamId;

    // This identifier is used to associate the object during onboarding. Unlike the near field id, this
    // onboarding ID is considered a secret, a random value that only the person holding the object can know. This secures
    // the ownership of the object. Generally, after use, this code is changed and will be required to reassign ownership to another user.
    protected String onboardingId;

    // This identifier refers to a family of objects, a free field used to identify the type of object and the
    // common behaviors associated with it. It can be a string giving the name of the object or an ID corresponding to a
    // specific entity with its own attributes, usable in other modules.
    protected String familyId;

    // device name, this is the name of the device, given by device owner or manager
    protected String name;

    // device description, this is a free text description of the device, given by device owner or manager
    protected String description;

    // Current device state, see the device life cycle management section for more information
    protected DeviceState devState;
    // Current device state date in MS since epoch, this is the date of the last state change
    protected long devStateDateMs;

    // last seen date in MS since epoch, this is the last time the device was seen by the platform
    protected long lastSeenDateMs;
    // last restart date in MS since epoch, this is the last time the device was restarted
    protected long lastRestartDateMs;

    // firmware version, this is the version of the firmware running on the device
    protected String firmwareVersion;
    // hardware version, this is the version of the hardware running on the device
    protected String hardwareVersion;

    // type of battery used in the device, this allows some autonomy calculation
    protected DeviceBatType batteryType;
    // capacity of the battery used in the device, this allows some autonomy calculation, in mAh
    protected int batteryCapacity;
    // low level of the battery used in the device, when 0, a default base on type applies, in mV
    protected int batteryLowLevel;
    // level of the battery used in the device, in mV
    protected int batteryLevel;

    // user login of the creator
    protected String creationBy;
    // date of creation in MS since epoch
    protected long creationOnMs;
    // user login of the updater
    protected String updatedBy;
    // date of update in MS since epoch
    protected long updatedOnMs;

    // billing period start date in MS since epoch
    protected long billingPeriodStartMs;
    // billing period end date in MS since epoch
    protected long billingPeriodEndMs;
    // billing group id, this is the id used to identify the billing group. This is a Group
    // where the biling information are attached
    protected String billingGroupId;

    // dynamic location flag, this is the flag used to identify if the device is a dynamic location device
    // when true, the device position can be updated based on network information dynamically.
    protected boolean dynamicLocation;
    // Position of the device
    protected DevLocation location;

    // device data is encrypted flag, this is the flag used to identify if the device data is encrypted
    // The encryption information are stored as part of the attributes.
    protected boolean dataEncrypted;

    // list of communication ids, this is the list of communication ids used by the device
    protected ArrayList<DevAttribute> communicationIds;

    // list of attributes to be associated with the device
    protected ArrayList<DevAttribute> attributes;

    // list of groups to be associated with the device
    protected ArrayList<DevGroupAssociated> associatedGroups;

    // ========================================

    public static Device newDevice(String user) {
        Device device = new Device();
        device.setVersion(Device.DEVICE_VERSION);
        device.setInstanceOfId("");
        device.setPublicId("");
        device.setHardwareIds(new ArrayList<>());
        device.setNearFieldId("");
        device.setCustomerId("");
        device.setDataStreamId("");
        device.setOnboardingId("");
        device.setFamilyId("");
        device.setName("");
        device.setDescription("");
        device.setDevState(IDENTIFIED);
        device.setDevStateDateMs(Now.NowUtcMs());
        device.setLastSeenDateMs(0);
        device.setLastRestartDateMs(0);
        device.setFirmwareVersion("");
        device.setHardwareVersion("");
        device.setBatteryType(UNKNOWN_BATTERY_TYPE);
        device.setBatteryCapacity(0);
        device.setBatteryLowLevel(0);
        device.setBatteryLevel(0);
        device.setCreationBy(user);
        device.setCreationOnMs(Now.NowUtcMs());
        device.setUpdatedBy(user);
        device.setUpdatedOnMs(Now.NowUtcMs());
        device.setBillingPeriodStartMs(0);
        device.setBillingPeriodEndMs(0);
        device.setBillingGroupId("");
        device.setDynamicLocation(false);
        DevLocation loc = new DevLocation();
        loc.setLocationMs(0);
        loc.setLatitude(0);
        loc.setLongitude(0);
        loc.setAltitude(0);
        loc.setAccuracy(0);
        loc.setFloor("");
        device.setLocation(loc);
        device.setDataEncrypted(false);
        device.setCommunicationIds(new ArrayList<>());
        device.setAttributes(new ArrayList<>());
        device.setAssociatedGroups(new ArrayList<>());
        return device;
    }

    public Device clone() {
        Device u = new Device();
        u.setId(id);
        u.setVersion(version);
        u.setInstanceOfId(instanceOfId);
        u.setPublicId(publicId);
        u.setHardwareIds(new ArrayList<>());
        for (DevHardwareId hardwareId : hardwareIds) {
            u.getHardwareIds().add(hardwareId.clone());
        }
        u.setNearFieldId(nearFieldId);
        u.setCustomerId(customerId);
        u.setDataStreamId(dataStreamId);
        u.setOnboardingId(onboardingId);
        u.setFamilyId(familyId);
        u.setName(name);
        u.setDescription(description);
        u.setDevState(devState);
        u.setDevStateDateMs(devStateDateMs);
        u.setLastSeenDateMs(lastSeenDateMs);
        u.setLastRestartDateMs(lastRestartDateMs);
        u.setFirmwareVersion(firmwareVersion);
        u.setHardwareVersion(hardwareVersion);
        u.setBatteryType(batteryType);
        u.setBatteryCapacity(batteryCapacity);
        u.setBatteryLowLevel(batteryLowLevel);
        u.setBatteryLevel(batteryLevel);
        u.setCreationBy(creationBy);
        u.setCreationOnMs(creationOnMs);
        u.setUpdatedBy(updatedBy);
        u.setUpdatedOnMs(updatedOnMs);
        u.setBillingPeriodStartMs(billingPeriodStartMs);
        u.setBillingPeriodEndMs(billingPeriodEndMs);
        u.setBillingGroupId(billingGroupId);
        u.setDynamicLocation(dynamicLocation);
        u.setLocation(location.clone());
        u.setDataEncrypted(dataEncrypted);
        u.setCommunicationIds(new ArrayList<>());
        for (DevAttribute communicationId : communicationIds) {
            u.getCommunicationIds().add(communicationId.clone());
        }
        u.setAttributes(new ArrayList<>());
        for (DevAttribute attribute : attributes) {
            u.getAttributes().add(attribute.clone());
        }
        u.setAssociatedGroups(new ArrayList<>());
        for (DevGroupAssociated group : associatedGroups) {
            u.getAssociatedGroups().add(group.clone());
        }
        return u;
    }

    // ========================================

    /**
     * Update the location and the hexLocation in one single call
     * @param lat
     * @param lng
     */
    public void setLatLng(double lat, double lng) {
        if (location == null) location = new DevLocation();
        location.setLatitude(lat);
        location.setLongitude(lng);
        try {
            // get the hex corresponding in a resolution of 14 - 3m2
            H3Core h3 = H3Core.newInstance();
            location.setHexLocation(h3.latLngToCellAddress(location.getLatitude(), location.getLongitude(),14));
        } catch (IOException ioException) {
            log.error("[devices] Unable to create H3Core instance", ioException);
        }
    }


    // ========================================


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getInstanceOfId() {
        return instanceOfId;
    }

    public void setInstanceOfId(String instanceOfId) {
        this.instanceOfId = instanceOfId;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public ArrayList<DevHardwareId> getHardwareIds() {
        return hardwareIds;
    }

    public void setHardwareIds(ArrayList<DevHardwareId> hardwareIds) {
        this.hardwareIds = hardwareIds;
    }

    public String getNearFieldId() {
        return nearFieldId;
    }

    public void setNearFieldId(String nearFieldId) {
        this.nearFieldId = nearFieldId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getDataStreamId() {
        return dataStreamId;
    }

    public void setDataStreamId(String dataStreamId) {
        this.dataStreamId = dataStreamId;
    }

    public String getOnboardingId() {
        return onboardingId;
    }

    public void setOnboardingId(String onboardingId) {
        this.onboardingId = onboardingId;
    }

    public String getFamilyId() {
        return familyId;
    }

    public void setFamilyId(String familyId) {
        this.familyId = familyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DeviceState getDevState() {
        return devState;
    }

    public void setDevState(DeviceState devState) {
        this.devState = devState;
    }

    public long getDevStateDateMs() {
        return devStateDateMs;
    }

    public void setDevStateDateMs(long devStateDateMs) {
        this.devStateDateMs = devStateDateMs;
    }

    public long getLastSeenDateMs() {
        return lastSeenDateMs;
    }

    public void setLastSeenDateMs(long lastSeenDateMs) {
        this.lastSeenDateMs = lastSeenDateMs;
    }

    public long getLastRestartDateMs() {
        return lastRestartDateMs;
    }

    public void setLastRestartDateMs(long lastRestartDateMs) {
        this.lastRestartDateMs = lastRestartDateMs;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getHardwareVersion() {
        return hardwareVersion;
    }

    public void setHardwareVersion(String hardwareVersion) {
        this.hardwareVersion = hardwareVersion;
    }

    public DeviceBatType getBatteryType() {
        return batteryType;
    }

    public void setBatteryType(DeviceBatType batteryType) {
        this.batteryType = batteryType;
    }

    public int getBatteryCapacity() {
        return batteryCapacity;
    }

    public void setBatteryCapacity(int batteryCapacity) {
        this.batteryCapacity = batteryCapacity;
    }

    public int getBatteryLowLevel() {
        return batteryLowLevel;
    }

    public void setBatteryLowLevel(int batteryLowLevel) {
        this.batteryLowLevel = batteryLowLevel;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public String getCreationBy() {
        return creationBy;
    }

    public void setCreationBy(String creationBy) {
        this.creationBy = creationBy;
    }

    public long getCreationOnMs() {
        return creationOnMs;
    }

    public void setCreationOnMs(long creationOnMs) {
        this.creationOnMs = creationOnMs;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public long getUpdatedOnMs() {
        return updatedOnMs;
    }

    public void setUpdatedOnMs(long updatedOnMs) {
        this.updatedOnMs = updatedOnMs;
    }

    public long getBillingPeriodStartMs() {
        return billingPeriodStartMs;
    }

    public void setBillingPeriodStartMs(long billingPeriodStartMs) {
        this.billingPeriodStartMs = billingPeriodStartMs;
    }

    public long getBillingPeriodEndMs() {
        return billingPeriodEndMs;
    }

    public void setBillingPeriodEndMs(long billingPeriodEndMs) {
        this.billingPeriodEndMs = billingPeriodEndMs;
    }

    public String getBillingGroupId() {
        return billingGroupId;
    }

    public void setBillingGroupId(String billingGroupId) {
        this.billingGroupId = billingGroupId;
    }

    public boolean isDynamicLocation() {
        return dynamicLocation;
    }

    public void setDynamicLocation(boolean dynamicLocation) {
        this.dynamicLocation = dynamicLocation;
    }

    public DevLocation getLocation() {
        return location;
    }

    public void setLocation(DevLocation location) {
        this.location = location;
    }

    public boolean isDataEncrypted() {
        return dataEncrypted;
    }

    public void setDataEncrypted(boolean dataEncrypted) {
        this.dataEncrypted = dataEncrypted;
    }

    public ArrayList<DevAttribute> getCommunicationIds() {
        return communicationIds;
    }

    public void setCommunicationIds(ArrayList<DevAttribute> communicationIds) {
        this.communicationIds = communicationIds;
    }

    public ArrayList<DevAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(ArrayList<DevAttribute> attributes) {
        this.attributes = attributes;
    }

    public ArrayList<DevGroupAssociated> getAssociatedGroups() {
        return associatedGroups;
    }

    public void setAssociatedGroups(ArrayList<DevGroupAssociated> associatedGroups) {
        this.associatedGroups = associatedGroups;
    }
}
