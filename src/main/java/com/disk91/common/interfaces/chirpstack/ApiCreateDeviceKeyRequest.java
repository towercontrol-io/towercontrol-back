package com.disk91.common.interfaces.chirpstack;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiCreateDeviceKeyRequest {

    @JsonProperty("deviceKeys")
    private DeviceKeys deviceKeys;

    public static class DeviceKeys {

        @JsonProperty("appKey")
        private String appKey;

        @JsonProperty("nwkKey")
        private String nwkKey;

        public String getAppKey() {
            return appKey;
        }

        public void setAppKey(String appKey) {
            this.appKey = appKey;
        }

        public String getNwkKey() {
            return nwkKey;
        }

        public void setNwkKey(String nwkKey) {
            this.nwkKey = nwkKey;
        }
    }

    public DeviceKeys getDeviceKeys() {
        return deviceKeys;
    }

    public void setDeviceKeys(DeviceKeys deviceKeys) {
        this.deviceKeys = deviceKeys;
    }
}
