package com.disk91.common.interfaces.chirpstack;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiCreateDeviceRequest {

    /**
     *
     * {
     *     "device": {
     *         "applicationId" : "{{appId}}",
     *         "description": "test",
     *         "devEui": "0123456789abcdef",
     *         "joinEui": "0123456789abcdef",
     *         "deviceProfileId": "{{profile}}",
     *         "name": "test",
     *         "referenceAltitude": 0,
     *         "skipFCntCheck": true,
     *         "isDisabled": false,
     *         "skipFcntCheck": true
     *     }
     * }
     *
     */

    @JsonProperty("device")
    private Device device;

    public static class Device {
        @JsonProperty("applicationId")
        private String applicationId;

        @JsonProperty("description")
        private String description;

        @JsonProperty("devEui")
        private String devEui;

        @JsonProperty("joinEui")
        private String joinEui;

        @JsonProperty("deviceProfileId")
        private String deviceProfileId;

        @JsonProperty("name")
        private String name;

        @JsonProperty("referenceAltitude")
        private int referenceAltitude;

        @JsonProperty("skipFCntCheck")
        private boolean skipFCntCheck;

        @JsonProperty("isDisabled")
        private boolean isDisabled;

        public String getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(String applicationId) {
            this.applicationId = applicationId;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDevEui() {
            return devEui;
        }

        public void setDevEui(String devEui) {
            this.devEui = devEui;
        }

        public String getJoinEui() {
            return joinEui;
        }

        public void setJoinEui(String joinEui) {
            this.joinEui = joinEui;
        }

        public String getDeviceProfileId() {
            return deviceProfileId;
        }

        public void setDeviceProfileId(String deviceProfileId) {
            this.deviceProfileId = deviceProfileId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getReferenceAltitude() {
            return referenceAltitude;
        }

        public void setReferenceAltitude(int referenceAltitude) {
            this.referenceAltitude = referenceAltitude;
        }

        public boolean isSkipFCntCheck() {
            return skipFCntCheck;
        }

        public void setSkipFCntCheck(boolean skipFCntCheck) {
            this.skipFCntCheck = skipFCntCheck;
        }

        public boolean isDisabled() {
            return isDisabled;
        }

        public void setDisabled(boolean disabled) {
            isDisabled = disabled;
        }
    }

    // Getters and Setters


    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }
}
