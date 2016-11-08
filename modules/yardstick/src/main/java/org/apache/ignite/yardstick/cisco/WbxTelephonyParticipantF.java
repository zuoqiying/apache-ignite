/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.yardstick.cisco;

import java.util.Date;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.springframework.core.annotation.Order;

/**
 *
 */
public class WbxTelephonyParticipantF {
    @Order(0)
    @QuerySqlField
    private Long sessionId;

    @Order(1)
    @QuerySqlField
    private String sessionKey;

    @Order(2)
    @QuerySqlField
    private String brmSubscriptionId;

    @Order(3)
    @QuerySqlField
    private String xaasSubscriptionId;

    @Order(4)
    @QuerySqlField
    private String blisSubscriptionId;

    @Order(5)
    @QuerySqlField
    private String siteServiceId;

    @Order(6)
    @QuerySqlField
    private Long siteId;

    @Order(7)
    @QuerySqlField
    private String siteUrl;

    @Order(8)
    @QuerySqlField
    private Long engSiteId;

    @Order(9)
    @QuerySqlField
    private Long hostId;

    @Order(10)
    @QuerySqlField
    private Long rowId;

    @Order(11)
    @QuerySqlField
    private String userId;

    @Order(12)
    @QuerySqlField
    private Date callStartDtm;

    @Order(13)
    @QuerySqlField
    private Date callEndDtm;

    @Order(14)
    @QuerySqlField
    private Double callDurationMinutes;

    @Order(15)
    @QuerySqlField
    private String callType;

    @Order(16)
    @QuerySqlField
    private String telephonyServer;

    @Order(17)
    @QuerySqlField
    private String callServiceType;

    @Order(18)
    @QuerySqlField
    private String meetingType;

    @Order(19)
    @QuerySqlField
    private String sessionType;

    @Order(20)
    @QuerySqlField
    private String majorSessionType;

    @Order(21)
    @QuerySqlField
    private Date telephonyCreationDate;

    @Order(22)
    @QuerySqlField
    private Date consumptionDate;

    @Order(23)
    @QuerySqlField
    private Long callRateId;

    @Order(24)
    @QuerySqlField
    private Double wbxPrice;

    @Order(25)
    @QuerySqlField
    private Double ratingBillingStatus;

    @Order(26)
    @QuerySqlField
    private Date ratingBillingDtm;

    @Order(27)
    @QuerySqlField
    private Date meetingStartTimeDtm;

    @Order(28)
    @QuerySqlField
    private Date meetingEndTimeDtm;

    @Order(29)
    @QuerySqlField
    private String didPhoneNumber;

    @Order(30)
    @QuerySqlField
    private String provider;

    @Order(31)
    @QuerySqlField
    private String providerCountry;

    @Order(32)
    @QuerySqlField
    private String userEmail;

    @Order(33)
    @QuerySqlField
    private String userName;

    @Order(34)
    @QuerySqlField
    private String webexid;

    @Order(35)
    @QuerySqlField
    private String didCountry;

    @Order(36)
    @QuerySqlField
    private String bridgeCountry;

    @Order(37)
    @QuerySqlField
    private String countryCode;

    @Order(38)
    @QuerySqlField
    private String tollType;

    @Order(39)
    @QuerySqlField
    private String areaCode;

    @Order(40)
    @QuerySqlField
    private String dnis;

    @Order(41)
    @QuerySqlField
    private String ani;

    @Order(42)
    @QuerySqlField
    private String billableFlag;

    @Order(43)
    @QuerySqlField
    private String usageType;

    @Order(44)
    @QuerySqlField
    private Date edwCreateDtm;

    @Order(45)
    @QuerySqlField
    private String edwCreateUser;

    @Order(46)
    @QuerySqlField
    private Date edwUpdateDtm;

    @Order(47)
    @QuerySqlField
    private String edwUpdateUser;

    public WbxTelephonyParticipantF() {
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getBrmSubscriptionId() {
        return brmSubscriptionId;
    }

    public void setBrmSubscriptionId(String brmSubscriptionId) {
        this.brmSubscriptionId = brmSubscriptionId;
    }

    public String getXaasSubscriptionId() {
        return xaasSubscriptionId;
    }

    public void setXaasSubscriptionId(String xaasSubscriptionId) {
        this.xaasSubscriptionId = xaasSubscriptionId;
    }

    public String getBlisSubscriptionId() {
        return blisSubscriptionId;
    }

    public void setBlisSubscriptionId(String blisSubscriptionId) {
        this.blisSubscriptionId = blisSubscriptionId;
    }

    public String getSiteServiceId() {
        return siteServiceId;
    }

    public void setSiteServiceId(String siteServiceId) {
        this.siteServiceId = siteServiceId;
    }

    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public Long getEngSiteId() {
        return engSiteId;
    }

    public void setEngSiteId(Long engSiteId) {
        this.engSiteId = engSiteId;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public Long getRowId() {
        return rowId;
    }

    public void setRowId(Long rowId) {
        this.rowId = rowId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getCallStartDtm() {
        return callStartDtm;
    }

    public void setCallStartDtm(Date callStartDtm) {
        this.callStartDtm = callStartDtm;
    }

    public Date getCallEndDtm() {
        return callEndDtm;
    }

    public void setCallEndDtm(Date callEndDtm) {
        this.callEndDtm = callEndDtm;
    }

    public Double getCallDurationMinutes() {
        return callDurationMinutes;
    }

    public void setCallDurationMinutes(Double callDurationMinutes) {
        this.callDurationMinutes = callDurationMinutes;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public String getTelephonyServer() {
        return telephonyServer;
    }

    public void setTelephonyServer(String telephonyServer) {
        this.telephonyServer = telephonyServer;
    }

    public String getCallServiceType() {
        return callServiceType;
    }

    public void setCallServiceType(String callServiceType) {
        this.callServiceType = callServiceType;
    }

    public String getMeetingType() {
        return meetingType;
    }

    public void setMeetingType(String meetingType) {
        this.meetingType = meetingType;
    }

    public String getSessionType() {
        return sessionType;
    }

    public void setSessionType(String sessionType) {
        this.sessionType = sessionType;
    }

    public String getMajorSessionType() {
        return majorSessionType;
    }

    public void setMajorSessionType(String majorSessionType) {
        this.majorSessionType = majorSessionType;
    }

    public Date getTelephonyCreationDate() {
        return telephonyCreationDate;
    }

    public void setTelephonyCreationDate(Date telephonyCreationDate) {
        this.telephonyCreationDate = telephonyCreationDate;
    }

    public Date getConsumptionDate() {
        return consumptionDate;
    }

    public void setConsumptionDate(Date consumptionDate) {
        this.consumptionDate = consumptionDate;
    }

    public Long getCallRateId() {
        return callRateId;
    }

    public void setCallRateId(Long callRateId) {
        this.callRateId = callRateId;
    }

    public Double getWbxPrice() {
        return wbxPrice;
    }

    public void setWbxPrice(Double wbxPrice) {
        this.wbxPrice = wbxPrice;
    }

    public Double getRatingBillingStatus() {
        return ratingBillingStatus;
    }

    public void setRatingBillingStatus(Double ratingBillingStatus) {
        this.ratingBillingStatus = ratingBillingStatus;
    }

    public Date getRatingBillingDtm() {
        return ratingBillingDtm;
    }

    public void setRatingBillingDtm(Date ratingBillingDtm) {
        this.ratingBillingDtm = ratingBillingDtm;
    }

    public Date getMeetingStartTimeDtm() {
        return meetingStartTimeDtm;
    }

    public void setMeetingStartTimeDtm(Date meetingStartTimeDtm) {
        this.meetingStartTimeDtm = meetingStartTimeDtm;
    }

    public Date getMeetingEndTimeDtm() {
        return meetingEndTimeDtm;
    }

    public void setMeetingEndTimeDtm(Date meetingEndTimeDtm) {
        this.meetingEndTimeDtm = meetingEndTimeDtm;
    }

    public String getDidPhoneNumber() {
        return didPhoneNumber;
    }

    public void setDidPhoneNumber(String didPhoneNumber) {
        this.didPhoneNumber = didPhoneNumber;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderCountry() {
        return providerCountry;
    }

    public void setProviderCountry(String providerCountry) {
        this.providerCountry = providerCountry;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getWebexid() {
        return webexid;
    }

    public void setWebexid(String webexid) {
        this.webexid = webexid;
    }

    public String getDidCountry() {
        return didCountry;
    }

    public void setDidCountry(String didCountry) {
        this.didCountry = didCountry;
    }

    public String getBridgeCountry() {
        return bridgeCountry;
    }

    public void setBridgeCountry(String bridgeCountry) {
        this.bridgeCountry = bridgeCountry;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getTollType() {
        return tollType;
    }

    public void setTollType(String tollType) {
        this.tollType = tollType;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getDnis() {
        return dnis;
    }

    public void setDnis(String dnis) {
        this.dnis = dnis;
    }

    public String getAni() {
        return ani;
    }

    public void setAni(String ani) {
        this.ani = ani;
    }

    public String getBillableFlag() {
        return billableFlag;
    }

    public void setBillableFlag(String billableFlag) {
        this.billableFlag = billableFlag;
    }

    public String getUsageType() {
        return usageType;
    }

    public void setUsageType(String usageType) {
        this.usageType = usageType;
    }

    public Date getEdwCreateDtm() {
        return edwCreateDtm;
    }

    public void setEdwCreateDtm(Date edwCreateDtm) {
        this.edwCreateDtm = edwCreateDtm;
    }

    public String getEdwCreateUser() {
        return edwCreateUser;
    }

    public void setEdwCreateUser(String edwCreateUser) {
        this.edwCreateUser = edwCreateUser;
    }

    public Date getEdwUpdateDtm() {
        return edwUpdateDtm;
    }

    public void setEdwUpdateDtm(Date edwUpdateDtm) {
        this.edwUpdateDtm = edwUpdateDtm;
    }

    public String getEdwUpdateUser() {
        return edwUpdateUser;
    }

    public void setEdwUpdateUser(String edwUpdateUser) {
        this.edwUpdateUser = edwUpdateUser;
    }
}
