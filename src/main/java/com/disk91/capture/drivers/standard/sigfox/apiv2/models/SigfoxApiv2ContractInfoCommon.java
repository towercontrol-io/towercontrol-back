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

/**
 * -------------------------------------------------------------------------------
 * This file is part of IngeniousThings Sigfox-Api.
 *
 * IngeniousThings Sigfox-Api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IngeniousThings Sigfox-Api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * -------------------------------------------------------------------------------
 * Author : Paul Pinault aka disk91
 * See https://www.disk91.com
 * ----
 * More information about IngeniousThings : https://www.ingeniousthings.fr
 * ----
 * Commercial license of this software can be obtained contacting ingeniousthings
 * -------------------------------------------------------------------------------
 */
package com.disk91.capture.drivers.standard.sigfox.apiv2.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Tag(name = "commonContractInfo", description = "Defines a contract info’s common properties")
public class SigfoxApiv2ContractInfoCommon {


    @Schema(
            description = "The contract info name",
            example = "03662_SUBS PLATINUM",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String name;

    @Schema(
            description = "The activation end time (in milliseconds) of the contract info.",
            example = "1551264713282",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long activationEndTime;

    @Schema(
            description = "The end time (in milliseconds) of the communication.",
            example = "1582801013282",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long communicationEndTime;

    @Schema(
            description = "True if the contract info is bidirectional.",
            example = "true",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean bidir;

    @Schema(
            description = "True if all downlinks are high priority.",
            example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean highPriorityDownlink;

    @Schema(
            description = "The maximum number of uplink frames. Must be >=0",
            example = "140",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int maxUplinkFrames;

    @Schema(
            description = "The maximum number of downlink frames. Must be >=0",
            example = "4",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int maxDownlinkFrames;

    @Schema(
            description = "The maximum number of tokens for this contract info. Either 0 (unlimited) or a positive number.",
            example = "10",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int maxTokens;

    @Schema(
            description = "The number of test messages. Must be >= 0 and <= 25.",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int testMessages;

    @Schema(
            description = "The test message duration in months. Must be >= 0.",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int testMessagesDuration;

    @Schema(
            description = "The geolocation mode. To be defined, for instance:" +
                    "<ul>" +
                    "<li>0 -> no geoloc</li>" +
                    "<li>1 -> high service level (1km precision)</li>" +
                    "<li>2 -> low service level (10km precision)</li>" +
                    "</uL>",
            example = "1",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int geolocationMode;

    @Schema(
            description = "The maximum number of renewals.",
            example = "0",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int renewalLimit;

    @Schema(
            description = "True if automatic renewal is allowed.",
            example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean automaticRenewal;

    @Schema(
            description = "The renewal duration in months. Must be >= 0",
            example = "0",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int renewalDuration;

    @Schema(
            description = "True to allow new territories.",
            example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean allowNewTerritories;


    @Tag(name = "commonContractInfo.OptionParameter", description = "The parameters of the premium option")
    public static class OptionParameter {
        @Schema(
                description = "payloadEncryption:<br/>" +
                        "level: 0 (DEVICE_TO_SIGFOX_CLOUD, default), 1 (END_TO_END)<br/>" +
                        "geolocation:</br>" +
                        "level: 1 (ATLAS, default), 2 (ATLAS_WIFI)<br/>" +
                        "cognition:\n" +
                        "level: 0 (MONARCH, default)",
                example = "1",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        public int level;

        @Schema(
                description = "testFrames:<br/>" +
                        "nb: 1 - 25 (default=1)",
                example = "1",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        public int nb;

        @Schema(
                description = "testFrames:<br/>" +
                        "duration: 0+ (default=0)",
                example = "0",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        public int duration;

    }


    @Tag(name = "commonContractInfo.Option", description = "The activated premium options")
    public static class Option {
        @Schema(
                description = "The premium option id (payloadEncryption, geolocation, cognition, testFrames, …)",
                example = "geolocation",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        public String id;

        @Schema(
                description = "The parameters of the premium option",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        public OptionParameter parameters;

    }

    @Schema(
            description = "The activated premium options.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<Option> options;

    // ============================================================
    // Generated Getters & Setters
    // ============================================================


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getActivationEndTime() {
        return activationEndTime;
    }

    public void setActivationEndTime(long activationEndTime) {
        this.activationEndTime = activationEndTime;
    }

    public long getCommunicationEndTime() {
        return communicationEndTime;
    }

    public void setCommunicationEndTime(long communicationEndTime) {
        this.communicationEndTime = communicationEndTime;
    }

    public boolean isBidir() {
        return bidir;
    }

    public void setBidir(boolean bidir) {
        this.bidir = bidir;
    }

    public boolean isHighPriorityDownlink() {
        return highPriorityDownlink;
    }

    public void setHighPriorityDownlink(boolean highPriorityDownlink) {
        this.highPriorityDownlink = highPriorityDownlink;
    }

    public int getMaxUplinkFrames() {
        return maxUplinkFrames;
    }

    public void setMaxUplinkFrames(int maxUplinkFrames) {
        this.maxUplinkFrames = maxUplinkFrames;
    }

    public int getMaxDownlinkFrames() {
        return maxDownlinkFrames;
    }

    public void setMaxDownlinkFrames(int maxDownlinkFrames) {
        this.maxDownlinkFrames = maxDownlinkFrames;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getTestMessages() {
        return testMessages;
    }

    public void setTestMessages(int testMessages) {
        this.testMessages = testMessages;
    }

    public int getTestMessagesDuration() {
        return testMessagesDuration;
    }

    public void setTestMessagesDuration(int testMessagesDuration) {
        this.testMessagesDuration = testMessagesDuration;
    }

    public int getGeolocationMode() {
        return geolocationMode;
    }

    public void setGeolocationMode(int geolocationMode) {
        this.geolocationMode = geolocationMode;
    }

    public int getRenewalLimit() {
        return renewalLimit;
    }

    public void setRenewalLimit(int renewalLimit) {
        this.renewalLimit = renewalLimit;
    }

    public boolean isAutomaticRenewal() {
        return automaticRenewal;
    }

    public void setAutomaticRenewal(boolean automaticRenewal) {
        this.automaticRenewal = automaticRenewal;
    }

    public int getRenewalDuration() {
        return renewalDuration;
    }

    public void setRenewalDuration(int renewalDuration) {
        this.renewalDuration = renewalDuration;
    }

    public boolean isAllowNewTerritories() {
        return allowNewTerritories;
    }

    public void setAllowNewTerritories(boolean allowNewTerritories) {
        this.allowNewTerritories = allowNewTerritories;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
    }
}
