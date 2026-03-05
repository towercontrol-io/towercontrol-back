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
@Tag(name = "contractInfo", description = "Defines the contract info properties when reading.")
public class SigfoxApiv2ContractInfo extends SigfoxApiv2ContractInfoCommon {

    @Schema(
            description = "The contract info ID.",
            example = "47fe5b1e9e92a126ed708cd9",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String id;


    @Schema(
            description = "The contract info external ID. It’s used to identify the contract info in EDRs.",
            example = "07dd",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String contractId;

    @Schema(
            description = "The bss Order  ID. Not yet documented...",
            example = "07de",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String bssOrderId;


    @Schema(
            description = "The  Order  ID. Not yet documented...",
            example = "55f",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected SigfoxApiv2ContractInfoMin bssParentOrder;


    @Schema(
            description = "The ID of the user who created the contract in BSS.",
            example = "59799fd7500474298ba15c4c",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String userId;

    @Schema(
            description = "Defines a group entity",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected SigfoxApiv2GroupMinimal group;

    @Schema(
            description = "The contract info ID.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected SigfoxApiv2ContractInfoMin order;

    @Schema(
            description = "The pricing model used by this contract info.<br/>" +
                    "<ul>" +
                    "<li>1 -> Pricing model version 1.</li>" +
                    "<li>2 -> Pricing model version 2.</li>" +
                    "</ul>",
            example = "Not yet returned",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int pricingModel;

    @Schema(
            description = "The id of contract info’s creator user",
            example = "47a9ce229e03a139b4c95959",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String createdBy;

    @Schema(
            description = "Date of the creation of this contract info (timestamp in milliseconds since Unix Epoch).",
            example = "1527725044259",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected long creationTime;

    @Schema(
            description = "Date of the modification of this contract info (timestamp in milliseconds since Unix Epoch).",
            example = "1527725044259",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected long lastEditionTime;

    @Schema(
            description = "The id of contract info’s last editor user",
            example = "47a9ce229e03a139b4c95959",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String lastEditedBy;

    @Schema(
            description = "The start time (in milliseconds) of the contract.",
            example = "1476223200000",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected long startTime;

    @Schema(
            description = "The contract timezone name as a Java TimeZone ID (“full name” version only, like “America/Costa_Rica”).",
            example = "UTC",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String timezone;

    @Schema(
            description = "The contract info subscription plan.<br/>" +
                    "<ul>" +
                    "<li>0 -> Free order</li>" +
                    "<li>1 -> Pay As You Grow (PAYG)</li>" +
                    "<li>2 -> Committed Volume Plan (CVP)</li>" +
                    "<li>3 -> Flexible Committed Volume Plan (CVP Flex)</li>" +
                    "</ul>",
            example = "Not yet returned",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int subscriptionPlan;

    @Schema(
            description = "The token duration in months. Must be >= 0",
            example = "12",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int tokenDuration;


    @Schema(
            description = "The list of forbidden NIPs, as an array of NIP groups.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<SigfoxApiv2GroupMinimal> forbiddenNIPs;

    @Schema(
            description = "True to allow new NIPs.",
            example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean allowNewNIPs;


    @Schema(
            description = "The list of “blacklisted” territories, as an array of NIP groups.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<SigfoxApiv2GroupMinimal> blacklistedTerritories;

    @Schema(
            description = "The number of tokens in use.",
            example = "9",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int tokensInUse;

    @Schema(
            description = "The number of tokens used (expired or revoked).",
            example = "0",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int tokensUsed;

    @Schema(
            description = "The next contract in the contract chain.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected SigfoxApiv2ContractInfoMin nextContractInfo;


    // ============================================================
    // Generated Getters & Setters
    // ============================================================


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public SigfoxApiv2GroupMinimal getGroup() {
        return group;
    }

    public void setGroup(SigfoxApiv2GroupMinimal group) {
        this.group = group;
    }

    public SigfoxApiv2ContractInfoMin getOrder() {
        return order;
    }

    public void setOrder(SigfoxApiv2ContractInfoMin order) {
        this.order = order;
    }

    public int getPricingModel() {
        return pricingModel;
    }

    public void setPricingModel(int pricingModel) {
        this.pricingModel = pricingModel;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public long getLastEditionTime() {
        return lastEditionTime;
    }

    public void setLastEditionTime(long lastEditionTime) {
        this.lastEditionTime = lastEditionTime;
    }

    public String getLastEditedBy() {
        return lastEditedBy;
    }

    public void setLastEditedBy(String lastEditedBy) {
        this.lastEditedBy = lastEditedBy;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public int getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public void setSubscriptionPlan(int subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }

    public int getTokenDuration() {
        return tokenDuration;
    }

    public void setTokenDuration(int tokenDuration) {
        this.tokenDuration = tokenDuration;
    }

    public List<SigfoxApiv2GroupMinimal> getBlacklistedTerritories() {
        return blacklistedTerritories;
    }

    public void setBlacklistedTerritories(List<SigfoxApiv2GroupMinimal> blacklistedTerritories) {
        this.blacklistedTerritories = blacklistedTerritories;
    }

    public int getTokensInUse() {
        return tokensInUse;
    }

    public void setTokensInUse(int tokensInUse) {
        this.tokensInUse = tokensInUse;
    }

    public int getTokensUsed() {
        return tokensUsed;
    }

    public void setTokensUsed(int tokensUsed) {
        this.tokensUsed = tokensUsed;
    }

    public SigfoxApiv2ContractInfoMin getNextContractInfo() {
        return nextContractInfo;
    }

    public void setNextContractInfo(SigfoxApiv2ContractInfoMin nextContractInfo) {
        this.nextContractInfo = nextContractInfo;
    }

    public List<SigfoxApiv2GroupMinimal> getForbiddenNIPs() {
        return forbiddenNIPs;
    }

    public void setForbiddenNIPs(List<SigfoxApiv2GroupMinimal> forbiddenNIPs) {
        this.forbiddenNIPs = forbiddenNIPs;
    }

    public boolean isAllowNewNIPs() {
        return allowNewNIPs;
    }

    public void setAllowNewNIPs(boolean allowNewNIPs) {
        this.allowNewNIPs = allowNewNIPs;
    }

    public String getBssOrderId() {
        return bssOrderId;
    }

    public void setBssOrderId(String bssOrderId) {
        this.bssOrderId = bssOrderId;
    }

    public SigfoxApiv2ContractInfoMin getBssParentOrder() {
        return bssParentOrder;
    }

    public void setBssParentOrder(SigfoxApiv2ContractInfoMin bssParentOrder) {
        this.bssParentOrder = bssParentOrder;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }
}
