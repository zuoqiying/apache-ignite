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

import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.springframework.core.annotation.Order;

/**
 *
 */
public class XxrptHgsMeetingReport {
    @Order(0)
    @QuerySqlField
    private Double confid;

    @Order(1)
    @QuerySqlField
    private String confkey;

    @Order(2)
    @QuerySqlField
    private String confname;

    @Order(3)
    @QuerySqlField
    private String code;

    @Order(4)
    @QuerySqlField
    private Double conftype;

    @Order(5)
    @QuerySqlField
    private String starttime;

    @Order(6)
    @QuerySqlField
    private String endtime;

    @Order(7)
    @QuerySqlField
    private Double duration;

    @Order(8)
    @QuerySqlField
    private Double hostid;

    @Order(9)
    @QuerySqlField
    private String webexid;

    @Order(10)
    @QuerySqlField
    private String hostname;

    @Order(11)
    @QuerySqlField
    private String hostemail;

    @Order(12)
    @QuerySqlField
    private Double totalattendee;

    @Order(13)
    @QuerySqlField
    private Double dialinUser;

    @Order(14)
    @QuerySqlField
    private Double dialin1;

    @Order(15)
    @QuerySqlField
    private Double dialin2;

    @Order(16)
    @QuerySqlField
    private Double dialoutUser;

    @Order(17)
    @QuerySqlField
    private Double dialout1;

    @Order(18)
    @QuerySqlField
    private Double dialout2;

    @Order(19)
    @QuerySqlField
    private Double voipUser;

    @Order(20)
    @QuerySqlField
    private Double voip;

    @Order(21)
    @QuerySqlField
    private Double siteid;

    @Order(22)
    @QuerySqlField
    private String sitename;

    @Order(23)
    @QuerySqlField
    private Double invoiceid;

    @Order(24)
    @QuerySqlField
    private Double paymentid;

    @Order(25)
    @QuerySqlField
    private Double amount;

    @Order(26)
    @QuerySqlField
    private Double chargeflag;

    @Order(27)
    @QuerySqlField
    private Double peoplemin;

    @Order(28)
    @QuerySqlField
    private String division;

    @Order(29)
    @QuerySqlField
    private String department;

    @Order(30)
    @QuerySqlField
    private String project;

    @Order(31)
    @QuerySqlField
    private String other;

    @Order(32)
    @QuerySqlField
    private String charge;

    @Order(33)
    @QuerySqlField
    private Double creditcardid;

    @Order(34)
    @QuerySqlField
    private Double customerid;

    @Order(35)
    @QuerySqlField
    private String creditcardholder;

    @Order(36)
    @QuerySqlField
    private Double hgssiteid;

    @Order(37)
    @QuerySqlField
    private String meetingtype;

    @Order(38)
    @QuerySqlField
    private Double timezone;

    @Order(39)
    @QuerySqlField
    private Double platform;

    @Order(40)
    @QuerySqlField
    private Double ssofficeid;

    @Order(41)
    @QuerySqlField
    private String city;

    @Order(42)
    @QuerySqlField
    private String state;

    @Order(43)
    @QuerySqlField
    private String zipcode;

    @Order(44)
    @QuerySqlField
    private String country;

    @Order(45)
    @QuerySqlField
    private String projectnumber;

    @Order(46)
    @QuerySqlField
    private String trackingnumber;

    @Order(47)
    @QuerySqlField
    private String custom7;

    @Order(48)
    @QuerySqlField
    private String custom8;

    @Order(49)
    @QuerySqlField
    private String custom9;

    @Order(50)
    @QuerySqlField
    private String custom10;

    @Order(51)
    @QuerySqlField
    private String hostPhone;

    @Order(52)
    @QuerySqlField
    private String win;

    @Order(53)
    @QuerySqlField
    private Double registered;

    @Order(54)
    @QuerySqlField
    private Double invited;

    @Order(55)
    @QuerySqlField
    private String submeetingtype;

    @Order(56)
    @QuerySqlField
    private Double notAttended;

    @Order(57)
    @QuerySqlField
    private String serviceIdentifier;

    @Order(58)
    @QuerySqlField
    private String userIdentifier;

    @Order(59)
    @QuerySqlField
    private String purchaseIdentifier;

    @Order(60)
    @QuerySqlField
    private String consumptionDate;

    @Order(61)
    @QuerySqlField
    private String subscriptionCode;

    @Order(62)
    @QuerySqlField
    private Double bossContractid;

    @Order(63)
    @QuerySqlField
    private Double callinTollUsers;

    @Order(64)
    @QuerySqlField
    private Double callinTollfreeUsers;

    @Order(65)
    @QuerySqlField
    private Double callbackDomUsers;

    @Order(66)
    @QuerySqlField
    private Double callbackIntlUsers;

    @Order(67)
    @QuerySqlField
    private Double didMins;

    @Order(68)
    @QuerySqlField
    private Double didUsers;

    @Order(69)
    @QuerySqlField
    private Double rbeStatus;

    @Order(70)
    @QuerySqlField
    private String rbeTimestamp;

    @Order(71)
    @QuerySqlField
    private Double spvMins;

    @Order(72)
    @QuerySqlField
    private Double spvUsers;

    @Order(73)
    @QuerySqlField
    private Double mpvMins;

    @Order(74)
    @QuerySqlField
    private Double mpvUsers;

    @Order(75)
    @QuerySqlField
    private Double saUsers;

    @Order(76)
    @QuerySqlField
    private Double saMins;

    @Order(77)
    @QuerySqlField
    private Double internalUsers;

    @Order(78)
    @QuerySqlField
    private Double internalMins;

    @Order(79)
    @QuerySqlField
    private Double companyid;

    @Order(80)
    @QuerySqlField
    private Double prosumer;

    @Order(81)
    @QuerySqlField
    private Double peakattendee;

    @Order(82)
    @QuerySqlField
    private Double btBasicToll;

    @Order(83)
    @QuerySqlField
    private Double btBasicTollfree;

    @Order(84)
    @QuerySqlField
    private Double btBasicCallback;

    @Order(85)
    @QuerySqlField
    private Double btBasicIntlCallback;

    @Order(86)
    @QuerySqlField
    private Double btBasicIntlTollfree;

    @Order(87)
    @QuerySqlField
    private Double btPremiumToll;

    @Order(88)
    @QuerySqlField
    private Double btPremiumTollfree;

    @Order(89)
    @QuerySqlField
    private Double btPremiumCallback;

    @Order(90)
    @QuerySqlField
    private Double btPremiumIntlCallback;

    @Order(91)
    @QuerySqlField
    private Double btPremiumIntlTollfree;

    @Order(92)
    @QuerySqlField
    private Double recurringseq;

    @Order(93)
    @QuerySqlField
    private Double integrationtype;

    @Order(94)
    @QuerySqlField
    private Double scheduledfrom;

    @Order(95)
    @QuerySqlField
    private Double startedfrom;

    @Order(96)
    @QuerySqlField
    private String modifiedmtgtype;

    @Order(97)
    @QuerySqlField
    private Double pureattendee;

    @Order(98)
    @QuerySqlField
    private String custom10Translated;

    @Order(99)
    @QuerySqlField
    private String custom7Translated;

    @Order(100)
    @QuerySqlField
    private String custom8Translated;

    @Order(101)
    @QuerySqlField
    private String custom9Translated;

    @Order(102)
    @QuerySqlField
    private String departmentTranslated;

    @Order(103)
    @QuerySqlField
    private String divisionTranslated;

    @Order(104)
    @QuerySqlField
    private String otherTranslated;

    @Order(105)
    @QuerySqlField
    private String projectTranslated;

    @Order(106)
    @QuerySqlField
    private String projectnumberTranslated;

    @Order(107)
    @QuerySqlField
    private String trackingnumberTranslated;

    @Order(108)
    @QuerySqlField
    private String webexidTranslated;

    @Order(109)
    @QuerySqlField
    private String hostemailTranslated;

    @Order(110)
    @QuerySqlField
    private String cityTranslated;

    @Order(111)
    @QuerySqlField
    private String countryTranslated;

    @Order(112)
    @QuerySqlField
    private String stateTranslated;

    @Order(113)
    @QuerySqlField
    private String codeTranslated;

    @Order(114)
    @QuerySqlField
    private String hostnameTranslated;

    @Order(115)
    @QuerySqlField
    private String sitenameTranslated;

    @Order(116)
    @QuerySqlField
    private String zipcodeTranslated;

    @Order(117)
    @QuerySqlField
    private String modifiedmtgtypeTranslated;

    @Order(118)
    @QuerySqlField
    private String creditcardholderTranslated;

    @Order(119)
    @QuerySqlField
    private String confnameTranslated;

    @Order(120)
    @QuerySqlField
    private String createdate;

    @Order(121)
    @QuerySqlField
    private Double btPremiumReplay;

    @Order(122)
    @QuerySqlField
    private Double internalmeeting;

    @Order(123)
    @QuerySqlField
    private Double hostlanguageid;

    @Order(124)
    @QuerySqlField
    private String accountid;

    @Order(125)
    @QuerySqlField
    private String subscriptioncode;

    @Order(126)
    @QuerySqlField
    private String billingaccountid;

    @Order(127)
    @QuerySqlField
    private String servicecode;

    @Order(128)
    @QuerySqlField
    private Double didTollMins;

    @Order(129)
    @QuerySqlField
    private Double didTollfreeMins;

    @Order(130)
    @QuerySqlField
    private String meetinguuid;

    @Order(131)
    @QuerySqlField
    private String meetinginstanceuuid;

    @Order(132)
    @QuerySqlField
    private Double prerateStatus;

    @Order(133)
    @QuerySqlField
    private String prerateDate;

    @Order(134)
    @QuerySqlField
    private Double objid;

    @Order(135)
    @QuerySqlField
    private Double peakattendeeAud;

    @Order(136)
    @QuerySqlField
    private String feature;

    @Order(137)
    @QuerySqlField
    private String serviceType;

    public XxrptHgsMeetingReport() {
    }

    public Double getConfid() {
        return confid;
    }

    public void setConfid(Double confid) {
        this.confid = confid;
    }

    public String getConfkey() {
        return confkey;
    }

    public void setConfkey(String confkey) {
        this.confkey = confkey;
    }

    public String getConfname() {
        return confname;
    }

    public void setConfname(String confname) {
        this.confname = confname;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Double getConftype() {
        return conftype;
    }

    public void setConftype(Double conftype) {
        this.conftype = conftype;
    }

    public String getStarttime() {
        return starttime;
    }

    public void setStarttime(String starttime) {
        this.starttime = starttime;
    }

    public String getEndtime() {
        return endtime;
    }

    public void setEndtime(String endtime) {
        this.endtime = endtime;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public Double getHostid() {
        return hostid;
    }

    public void setHostid(Double hostid) {
        this.hostid = hostid;
    }

    public String getWebexid() {
        return webexid;
    }

    public void setWebexid(String webexid) {
        this.webexid = webexid;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getHostemail() {
        return hostemail;
    }

    public void setHostemail(String hostemail) {
        this.hostemail = hostemail;
    }

    public Double getTotalattendee() {
        return totalattendee;
    }

    public void setTotalattendee(Double totalattendee) {
        this.totalattendee = totalattendee;
    }

    public Double getDialinUser() {
        return dialinUser;
    }

    public void setDialinUser(Double dialinUser) {
        this.dialinUser = dialinUser;
    }

    public Double getDialin1() {
        return dialin1;
    }

    public void setDialin1(Double dialin1) {
        this.dialin1 = dialin1;
    }

    public Double getDialin2() {
        return dialin2;
    }

    public void setDialin2(Double dialin2) {
        this.dialin2 = dialin2;
    }

    public Double getDialoutUser() {
        return dialoutUser;
    }

    public void setDialoutUser(Double dialoutUser) {
        this.dialoutUser = dialoutUser;
    }

    public Double getDialout1() {
        return dialout1;
    }

    public void setDialout1(Double dialout1) {
        this.dialout1 = dialout1;
    }

    public Double getDialout2() {
        return dialout2;
    }

    public void setDialout2(Double dialout2) {
        this.dialout2 = dialout2;
    }

    public Double getVoipUser() {
        return voipUser;
    }

    public void setVoipUser(Double voipUser) {
        this.voipUser = voipUser;
    }

    public Double getVoip() {
        return voip;
    }

    public void setVoip(Double voip) {
        this.voip = voip;
    }

    public Double getSiteid() {
        return siteid;
    }

    public void setSiteid(Double siteid) {
        this.siteid = siteid;
    }

    public String getSitename() {
        return sitename;
    }

    public void setSitename(String sitename) {
        this.sitename = sitename;
    }

    public Double getInvoiceid() {
        return invoiceid;
    }

    public void setInvoiceid(Double invoiceid) {
        this.invoiceid = invoiceid;
    }

    public Double getPaymentid() {
        return paymentid;
    }

    public void setPaymentid(Double paymentid) {
        this.paymentid = paymentid;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getChargeflag() {
        return chargeflag;
    }

    public void setChargeflag(Double chargeflag) {
        this.chargeflag = chargeflag;
    }

    public Double getPeoplemin() {
        return peoplemin;
    }

    public void setPeoplemin(Double peoplemin) {
        this.peoplemin = peoplemin;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getOther() {
        return other;
    }

    public void setOther(String other) {
        this.other = other;
    }

    public String getCharge() {
        return charge;
    }

    public void setCharge(String charge) {
        this.charge = charge;
    }

    public Double getCreditcardid() {
        return creditcardid;
    }

    public void setCreditcardid(Double creditcardid) {
        this.creditcardid = creditcardid;
    }

    public Double getCustomerid() {
        return customerid;
    }

    public void setCustomerid(Double customerid) {
        this.customerid = customerid;
    }

    public String getCreditcardholder() {
        return creditcardholder;
    }

    public void setCreditcardholder(String creditcardholder) {
        this.creditcardholder = creditcardholder;
    }

    public Double getHgssiteid() {
        return hgssiteid;
    }

    public void setHgssiteid(Double hgssiteid) {
        this.hgssiteid = hgssiteid;
    }

    public String getMeetingtype() {
        return meetingtype;
    }

    public void setMeetingtype(String meetingtype) {
        this.meetingtype = meetingtype;
    }

    public Double getTimezone() {
        return timezone;
    }

    public void setTimezone(Double timezone) {
        this.timezone = timezone;
    }

    public Double getPlatform() {
        return platform;
    }

    public void setPlatform(Double platform) {
        this.platform = platform;
    }

    public Double getSsofficeid() {
        return ssofficeid;
    }

    public void setSsofficeid(Double ssofficeid) {
        this.ssofficeid = ssofficeid;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProjectnumber() {
        return projectnumber;
    }

    public void setProjectnumber(String projectnumber) {
        this.projectnumber = projectnumber;
    }

    public String getTrackingnumber() {
        return trackingnumber;
    }

    public void setTrackingnumber(String trackingnumber) {
        this.trackingnumber = trackingnumber;
    }

    public String getCustom7() {
        return custom7;
    }

    public void setCustom7(String custom7) {
        this.custom7 = custom7;
    }

    public String getCustom8() {
        return custom8;
    }

    public void setCustom8(String custom8) {
        this.custom8 = custom8;
    }

    public String getCustom9() {
        return custom9;
    }

    public void setCustom9(String custom9) {
        this.custom9 = custom9;
    }

    public String getCustom10() {
        return custom10;
    }

    public void setCustom10(String custom10) {
        this.custom10 = custom10;
    }

    public String getHostPhone() {
        return hostPhone;
    }

    public void setHostPhone(String hostPhone) {
        this.hostPhone = hostPhone;
    }

    public String getWin() {
        return win;
    }

    public void setWin(String win) {
        this.win = win;
    }

    public Double getRegistered() {
        return registered;
    }

    public void setRegistered(Double registered) {
        this.registered = registered;
    }

    public Double getInvited() {
        return invited;
    }

    public void setInvited(Double invited) {
        this.invited = invited;
    }

    public String getSubmeetingtype() {
        return submeetingtype;
    }

    public void setSubmeetingtype(String submeetingtype) {
        this.submeetingtype = submeetingtype;
    }

    public Double getNotAttended() {
        return notAttended;
    }

    public void setNotAttended(Double notAttended) {
        this.notAttended = notAttended;
    }

    public String getServiceIdentifier() {
        return serviceIdentifier;
    }

    public void setServiceIdentifier(String serviceIdentifier) {
        this.serviceIdentifier = serviceIdentifier;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    public String getPurchaseIdentifier() {
        return purchaseIdentifier;
    }

    public void setPurchaseIdentifier(String purchaseIdentifier) {
        this.purchaseIdentifier = purchaseIdentifier;
    }

    public String getConsumptionDate() {
        return consumptionDate;
    }

    public void setConsumptionDate(String consumptionDate) {
        this.consumptionDate = consumptionDate;
    }

    public String getSubscriptionCode() {
        return subscriptionCode;
    }

    public void setSubscriptionCode(String subscriptionCode) {
        this.subscriptionCode = subscriptionCode;
    }

    public Double getBossContractid() {
        return bossContractid;
    }

    public void setBossContractid(Double bossContractid) {
        this.bossContractid = bossContractid;
    }

    public Double getCallinTollUsers() {
        return callinTollUsers;
    }

    public void setCallinTollUsers(Double callinTollUsers) {
        this.callinTollUsers = callinTollUsers;
    }

    public Double getCallinTollfreeUsers() {
        return callinTollfreeUsers;
    }

    public void setCallinTollfreeUsers(Double callinTollfreeUsers) {
        this.callinTollfreeUsers = callinTollfreeUsers;
    }

    public Double getCallbackDomUsers() {
        return callbackDomUsers;
    }

    public void setCallbackDomUsers(Double callbackDomUsers) {
        this.callbackDomUsers = callbackDomUsers;
    }

    public Double getCallbackIntlUsers() {
        return callbackIntlUsers;
    }

    public void setCallbackIntlUsers(Double callbackIntlUsers) {
        this.callbackIntlUsers = callbackIntlUsers;
    }

    public Double getDidMins() {
        return didMins;
    }

    public void setDidMins(Double didMins) {
        this.didMins = didMins;
    }

    public Double getDidUsers() {
        return didUsers;
    }

    public void setDidUsers(Double didUsers) {
        this.didUsers = didUsers;
    }

    public Double getRbeStatus() {
        return rbeStatus;
    }

    public void setRbeStatus(Double rbeStatus) {
        this.rbeStatus = rbeStatus;
    }

    public String getRbeTimestamp() {
        return rbeTimestamp;
    }

    public void setRbeTimestamp(String rbeTimestamp) {
        this.rbeTimestamp = rbeTimestamp;
    }

    public Double getSpvMins() {
        return spvMins;
    }

    public void setSpvMins(Double spvMins) {
        this.spvMins = spvMins;
    }

    public Double getSpvUsers() {
        return spvUsers;
    }

    public void setSpvUsers(Double spvUsers) {
        this.spvUsers = spvUsers;
    }

    public Double getMpvMins() {
        return mpvMins;
    }

    public void setMpvMins(Double mpvMins) {
        this.mpvMins = mpvMins;
    }

    public Double getMpvUsers() {
        return mpvUsers;
    }

    public void setMpvUsers(Double mpvUsers) {
        this.mpvUsers = mpvUsers;
    }

    public Double getSaUsers() {
        return saUsers;
    }

    public void setSaUsers(Double saUsers) {
        this.saUsers = saUsers;
    }

    public Double getSaMins() {
        return saMins;
    }

    public void setSaMins(Double saMins) {
        this.saMins = saMins;
    }

    public Double getInternalUsers() {
        return internalUsers;
    }

    public void setInternalUsers(Double internalUsers) {
        this.internalUsers = internalUsers;
    }

    public Double getInternalMins() {
        return internalMins;
    }

    public void setInternalMins(Double internalMins) {
        this.internalMins = internalMins;
    }

    public Double getCompanyid() {
        return companyid;
    }

    public void setCompanyid(Double companyid) {
        this.companyid = companyid;
    }

    public Double getProsumer() {
        return prosumer;
    }

    public void setProsumer(Double prosumer) {
        this.prosumer = prosumer;
    }

    public Double getPeakattendee() {
        return peakattendee;
    }

    public void setPeakattendee(Double peakattendee) {
        this.peakattendee = peakattendee;
    }

    public Double getBtBasicToll() {
        return btBasicToll;
    }

    public void setBtBasicToll(Double btBasicToll) {
        this.btBasicToll = btBasicToll;
    }

    public Double getBtBasicTollfree() {
        return btBasicTollfree;
    }

    public void setBtBasicTollfree(Double btBasicTollfree) {
        this.btBasicTollfree = btBasicTollfree;
    }

    public Double getBtBasicCallback() {
        return btBasicCallback;
    }

    public void setBtBasicCallback(Double btBasicCallback) {
        this.btBasicCallback = btBasicCallback;
    }

    public Double getBtBasicIntlCallback() {
        return btBasicIntlCallback;
    }

    public void setBtBasicIntlCallback(Double btBasicIntlCallback) {
        this.btBasicIntlCallback = btBasicIntlCallback;
    }

    public Double getBtBasicIntlTollfree() {
        return btBasicIntlTollfree;
    }

    public void setBtBasicIntlTollfree(Double btBasicIntlTollfree) {
        this.btBasicIntlTollfree = btBasicIntlTollfree;
    }

    public Double getBtPremiumToll() {
        return btPremiumToll;
    }

    public void setBtPremiumToll(Double btPremiumToll) {
        this.btPremiumToll = btPremiumToll;
    }

    public Double getBtPremiumTollfree() {
        return btPremiumTollfree;
    }

    public void setBtPremiumTollfree(Double btPremiumTollfree) {
        this.btPremiumTollfree = btPremiumTollfree;
    }

    public Double getBtPremiumCallback() {
        return btPremiumCallback;
    }

    public void setBtPremiumCallback(Double btPremiumCallback) {
        this.btPremiumCallback = btPremiumCallback;
    }

    public Double getBtPremiumIntlCallback() {
        return btPremiumIntlCallback;
    }

    public void setBtPremiumIntlCallback(Double btPremiumIntlCallback) {
        this.btPremiumIntlCallback = btPremiumIntlCallback;
    }

    public Double getBtPremiumIntlTollfree() {
        return btPremiumIntlTollfree;
    }

    public void setBtPremiumIntlTollfree(Double btPremiumIntlTollfree) {
        this.btPremiumIntlTollfree = btPremiumIntlTollfree;
    }

    public Double getRecurringseq() {
        return recurringseq;
    }

    public void setRecurringseq(Double recurringseq) {
        this.recurringseq = recurringseq;
    }

    public Double getIntegrationtype() {
        return integrationtype;
    }

    public void setIntegrationtype(Double integrationtype) {
        this.integrationtype = integrationtype;
    }

    public Double getScheduledfrom() {
        return scheduledfrom;
    }

    public void setScheduledfrom(Double scheduledfrom) {
        this.scheduledfrom = scheduledfrom;
    }

    public Double getStartedfrom() {
        return startedfrom;
    }

    public void setStartedfrom(Double startedfrom) {
        this.startedfrom = startedfrom;
    }

    public String getModifiedmtgtype() {
        return modifiedmtgtype;
    }

    public void setModifiedmtgtype(String modifiedmtgtype) {
        this.modifiedmtgtype = modifiedmtgtype;
    }

    public Double getPureattendee() {
        return pureattendee;
    }

    public void setPureattendee(Double pureattendee) {
        this.pureattendee = pureattendee;
    }

    public String getCustom10Translated() {
        return custom10Translated;
    }

    public void setCustom10Translated(String custom10Translated) {
        this.custom10Translated = custom10Translated;
    }

    public String getCustom7Translated() {
        return custom7Translated;
    }

    public void setCustom7Translated(String custom7Translated) {
        this.custom7Translated = custom7Translated;
    }

    public String getCustom8Translated() {
        return custom8Translated;
    }

    public void setCustom8Translated(String custom8Translated) {
        this.custom8Translated = custom8Translated;
    }

    public String getCustom9Translated() {
        return custom9Translated;
    }

    public void setCustom9Translated(String custom9Translated) {
        this.custom9Translated = custom9Translated;
    }

    public String getDepartmentTranslated() {
        return departmentTranslated;
    }

    public void setDepartmentTranslated(String departmentTranslated) {
        this.departmentTranslated = departmentTranslated;
    }

    public String getDivisionTranslated() {
        return divisionTranslated;
    }

    public void setDivisionTranslated(String divisionTranslated) {
        this.divisionTranslated = divisionTranslated;
    }

    public String getOtherTranslated() {
        return otherTranslated;
    }

    public void setOtherTranslated(String otherTranslated) {
        this.otherTranslated = otherTranslated;
    }

    public String getProjectTranslated() {
        return projectTranslated;
    }

    public void setProjectTranslated(String projectTranslated) {
        this.projectTranslated = projectTranslated;
    }

    public String getProjectnumberTranslated() {
        return projectnumberTranslated;
    }

    public void setProjectnumberTranslated(String projectnumberTranslated) {
        this.projectnumberTranslated = projectnumberTranslated;
    }

    public String getTrackingnumberTranslated() {
        return trackingnumberTranslated;
    }

    public void setTrackingnumberTranslated(String trackingnumberTranslated) {
        this.trackingnumberTranslated = trackingnumberTranslated;
    }

    public String getWebexidTranslated() {
        return webexidTranslated;
    }

    public void setWebexidTranslated(String webexidTranslated) {
        this.webexidTranslated = webexidTranslated;
    }

    public String getHostemailTranslated() {
        return hostemailTranslated;
    }

    public void setHostemailTranslated(String hostemailTranslated) {
        this.hostemailTranslated = hostemailTranslated;
    }

    public String getCityTranslated() {
        return cityTranslated;
    }

    public void setCityTranslated(String cityTranslated) {
        this.cityTranslated = cityTranslated;
    }

    public String getCountryTranslated() {
        return countryTranslated;
    }

    public void setCountryTranslated(String countryTranslated) {
        this.countryTranslated = countryTranslated;
    }

    public String getStateTranslated() {
        return stateTranslated;
    }

    public void setStateTranslated(String stateTranslated) {
        this.stateTranslated = stateTranslated;
    }

    public String getCodeTranslated() {
        return codeTranslated;
    }

    public void setCodeTranslated(String codeTranslated) {
        this.codeTranslated = codeTranslated;
    }

    public String getHostnameTranslated() {
        return hostnameTranslated;
    }

    public void setHostnameTranslated(String hostnameTranslated) {
        this.hostnameTranslated = hostnameTranslated;
    }

    public String getSitenameTranslated() {
        return sitenameTranslated;
    }

    public void setSitenameTranslated(String sitenameTranslated) {
        this.sitenameTranslated = sitenameTranslated;
    }

    public String getZipcodeTranslated() {
        return zipcodeTranslated;
    }

    public void setZipcodeTranslated(String zipcodeTranslated) {
        this.zipcodeTranslated = zipcodeTranslated;
    }

    public String getModifiedmtgtypeTranslated() {
        return modifiedmtgtypeTranslated;
    }

    public void setModifiedmtgtypeTranslated(String modifiedmtgtypeTranslated) {
        this.modifiedmtgtypeTranslated = modifiedmtgtypeTranslated;
    }

    public String getCreditcardholderTranslated() {
        return creditcardholderTranslated;
    }

    public void setCreditcardholderTranslated(String creditcardholderTranslated) {
        this.creditcardholderTranslated = creditcardholderTranslated;
    }

    public String getConfnameTranslated() {
        return confnameTranslated;
    }

    public void setConfnameTranslated(String confnameTranslated) {
        this.confnameTranslated = confnameTranslated;
    }

    public String getCreatedate() {
        return createdate;
    }

    public void setCreatedate(String createdate) {
        this.createdate = createdate;
    }

    public Double getBtPremiumReplay() {
        return btPremiumReplay;
    }

    public void setBtPremiumReplay(Double btPremiumReplay) {
        this.btPremiumReplay = btPremiumReplay;
    }

    public Double getInternalmeeting() {
        return internalmeeting;
    }

    public void setInternalmeeting(Double internalmeeting) {
        this.internalmeeting = internalmeeting;
    }

    public Double getHostlanguageid() {
        return hostlanguageid;
    }

    public void setHostlanguageid(Double hostlanguageid) {
        this.hostlanguageid = hostlanguageid;
    }

    public String getAccountid() {
        return accountid;
    }

    public void setAccountid(String accountid) {
        this.accountid = accountid;
    }

    public String getSubscriptioncode() {
        return subscriptioncode;
    }

    public void setSubscriptioncode(String subscriptioncode) {
        this.subscriptioncode = subscriptioncode;
    }

    public String getBillingaccountid() {
        return billingaccountid;
    }

    public void setBillingaccountid(String billingaccountid) {
        this.billingaccountid = billingaccountid;
    }

    public String getServicecode() {
        return servicecode;
    }

    public void setServicecode(String servicecode) {
        this.servicecode = servicecode;
    }

    public Double getDidTollMins() {
        return didTollMins;
    }

    public void setDidTollMins(Double didTollMins) {
        this.didTollMins = didTollMins;
    }

    public Double getDidTollfreeMins() {
        return didTollfreeMins;
    }

    public void setDidTollfreeMins(Double didTollfreeMins) {
        this.didTollfreeMins = didTollfreeMins;
    }

    public String getMeetinguuid() {
        return meetinguuid;
    }

    public void setMeetinguuid(String meetinguuid) {
        this.meetinguuid = meetinguuid;
    }

    public String getMeetinginstanceuuid() {
        return meetinginstanceuuid;
    }

    public void setMeetinginstanceuuid(String meetinginstanceuuid) {
        this.meetinginstanceuuid = meetinginstanceuuid;
    }

    public Double getPrerateStatus() {
        return prerateStatus;
    }

    public void setPrerateStatus(Double prerateStatus) {
        this.prerateStatus = prerateStatus;
    }

    public String getPrerateDate() {
        return prerateDate;
    }

    public void setPrerateDate(String prerateDate) {
        this.prerateDate = prerateDate;
    }

    public Double getObjid() {
        return objid;
    }

    public void setObjid(Double objid) {
        this.objid = objid;
    }

    public Double getPeakattendeeAud() {
        return peakattendeeAud;
    }

    public void setPeakattendeeAud(Double peakattendeeAud) {
        this.peakattendeeAud = peakattendeeAud;
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }
}
