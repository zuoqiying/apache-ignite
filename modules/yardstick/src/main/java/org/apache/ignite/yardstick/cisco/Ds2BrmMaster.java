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
public class Ds2BrmMaster {
    @Order(0)
    @QuerySqlField
    private Long accountId;

    @Order(1)
    @QuerySqlField
    private String accountNumber;

    @Order(2)
    @QuerySqlField
    private String accountName;

    @Order(3)
    @QuerySqlField
    private String accountStatus;

    @Order(4)
    @QuerySqlField
    private String tenantName;

    @Order(5)
    @QuerySqlField
    private String organizationName;

    @Order(6)
    @QuerySqlField
    private String contactFirstName;

    @Order(7)
    @QuerySqlField
    private String contactLastName;

    @Order(8)
    @QuerySqlField
    private String currency;

    @Order(9)
    @QuerySqlField
    private Long billDay;

    @Order(10)
    @QuerySqlField
    private String billToAddress1;

    @Order(11)
    @QuerySqlField
    private String billToCity;

    @Order(12)
    @QuerySqlField
    private String billToStateProvince;

    @Order(13)
    @QuerySqlField
    private String billToPostalCode;

    @Order(14)
    @QuerySqlField
    private String billToCountry;

    @Order(15)
    @QuerySqlField
    private Long billToPhone;

    @Order(16)
    @QuerySqlField
    private String billToEmail;

    @Order(17)
    @QuerySqlField
    private String shipToAddress1;

    @Order(18)
    @QuerySqlField
    private String shipToCity;

    @Order(19)
    @QuerySqlField
    private String shipToStateProvince;

    @Order(20)
    @QuerySqlField
    private String shipToPostalCode;

    @Order(21)
    @QuerySqlField
    private String shipToCountry;

    @Order(22)
    @QuerySqlField
    private Long shipToPhone;

    @Order(23)
    @QuerySqlField
    private String shipToEmail;

    @Order(24)
    @QuerySqlField
    private Date createdDate;

    @Order(25)
    @QuerySqlField
    private Date createdDtm;

    @Order(26)
    @QuerySqlField
    private String paymentMethod;

    @Order(27)
    @QuerySqlField
    private String languages;

    @Order(28)
    @QuerySqlField
    private String businessType;

    @Order(29)
    @QuerySqlField
    private String country;

    @Order(30)
    @QuerySqlField
    private String stateProvince;

    @Order(31)
    @QuerySqlField
    private Long subscriptionId;

    @Order(32)
    @QuerySqlField
    private Long previousSubscriptionId;

    @Order(33)
    @QuerySqlField
    private Date subscriptionEffectiveFromDt;

    @Order(34)
    @QuerySqlField
    private Date subscriptionEffectiveToDt;

    @Order(35)
    @QuerySqlField
    private String subscriptionStatus;

    @Order(36)
    @QuerySqlField
    private Long initialTerm;

    @Order(37)
    @QuerySqlField
    private Long renewalTerm;

    @Order(38)
    @QuerySqlField
    private Date serviceRenewalDate;

    @Order(39)
    @QuerySqlField
    private Integer prepaymentTerm;

    @Order(40)
    @QuerySqlField
    private String offerCode;

    @Order(41)
    @QuerySqlField
    private String financeReasonCode;

    @Order(42)
    @QuerySqlField
    private String financeSubStatus;

    @Order(43)
    @QuerySqlField
    private Long orderId;

    @Order(44)
    @QuerySqlField
    private Long orderNumber;

    @Order(45)
    @QuerySqlField
    private String orderType;

    @Order(46)
    @QuerySqlField
    private String orderSubType;

    @Order(47)
    @QuerySqlField
    private String orderSellType;

    @Order(48)
    @QuerySqlField
    private String orderStatus;

    @Order(49)
    @QuerySqlField
    private String orderFinanceApprovedFlg;

    @Order(50)
    @QuerySqlField
    private String orderOpportunityId;

    @Order(51)
    @QuerySqlField
    private Date orderCreatedDate;

    @Order(52)
    @QuerySqlField
    private Date orderCreatedDtm;

    @Order(53)
    @QuerySqlField
    private Date orderEffectiveTo;

    @Order(54)
    @QuerySqlField
    private Date orderEffectiveToDtm;

    @Order(55)
    @QuerySqlField
    private Date orderCompletedDate;

    @Order(56)
    @QuerySqlField
    private Date orderCompletedDtm;

    @Order(57)
    @QuerySqlField
    private Date orderAcceptedDate;

    @Order(58)
    @QuerySqlField
    private Date orderAcceptedDtm;

    @Order(59)
    @QuerySqlField
    private Date orderBookedDate;

    @Order(60)
    @QuerySqlField
    private Date orderBookedDtm;

    @Order(61)
    @QuerySqlField
    private Date orderCompensationDate;

    @Order(62)
    @QuerySqlField
    private Date orderCompensationDtm;

    @Order(63)
    @QuerySqlField
    private String orderCreatedBy;

    @Order(64)
    @QuerySqlField
    private String orderModifiedBy;

    @Order(65)
    @QuerySqlField
    private String userType;

    @Order(66)
    @QuerySqlField
    private Date orderServiceStartDate;

    @Order(67)
    @QuerySqlField
    private Date orderServiceEndDate;

    @Order(68)
    @QuerySqlField
    private Date orderStartDate;

    @Order(69)
    @QuerySqlField
    private Date orderStartDtm;

    @Order(70)
    @QuerySqlField
    private Date orderEndDate;

    @Order(71)
    @QuerySqlField
    private Date orderEndDtm;

    @Order(72)
    @QuerySqlField
    private Date termStartDate;

    @Order(73)
    @QuerySqlField
    private Date termEndDate;

    @Order(74)
    @QuerySqlField
    private String skuId;

    @Order(75)
    @QuerySqlField
    private String skuLabel;

    @Order(76)
    @QuerySqlField
    private String skuType;

    @Order(77)
    @QuerySqlField
    private String chargeType;

    @Order(78)
    @QuerySqlField
    private String promoCode;

    @Order(79)
    @QuerySqlField
    private Double skuMrr;

    @Order(80)
    @QuerySqlField
    private Double skuMrrUsd;

    @Order(81)
    @QuerySqlField
    private Double sellingUnitPrice;

    @Order(82)
    @QuerySqlField
    private Long qty;

    @Order(83)
    @QuerySqlField
    private String creditor;

    @Order(84)
    @QuerySqlField
    private String recurringInd;

    @Order(85)
    @QuerySqlField
    private String serviceType;

    @Order(86)
    @QuerySqlField
    private String ratingModel;

    @Order(87)
    @QuerySqlField
    private String product;

    @Order(88)
    @QuerySqlField
    private String deploymentModel;

    @Order(89)
    @QuerySqlField
    private String coreOffers;

    @Order(90)
    @QuerySqlField
    private String initialOfferCode;

    @Order(91)
    @QuerySqlField
    private String signupType;

    @Order(92)
    @QuerySqlField
    private String fraudFlag;

    @Order(93)
    @QuerySqlField
    private Long orderTrackId;

    @Order(94)
    @QuerySqlField
    private String acquisitionSource;

    @Order(95)
    @QuerySqlField
    private String acquisitionSourceRptg;

    @Order(96)
    @QuerySqlField
    private Integer rowNumber;

    @Order(97)
    @QuerySqlField
    private String campaignName;

    @Order(98)
    @QuerySqlField
    private String promoDescription;

    @Order(99)
    @QuerySqlField
    private String deliveryChannel;

    @Order(100)
    @QuerySqlField
    private Date promoStartDate;

    @Order(101)
    @QuerySqlField
    private Date promoEndDate;

    @Order(102)
    @QuerySqlField
    private String promoDiscountType;

    @Order(103)
    @QuerySqlField
    private String discountPctAmt;

    @Order(104)
    @QuerySqlField
    private Long promoTerm;

    @Order(105)
    @QuerySqlField
    private String countryCode;

    @Order(106)
    @QuerySqlField
    private String countryName;

    @Order(107)
    @QuerySqlField
    private String geoArea;

    @Order(108)
    @QuerySqlField
    private String region;

    @Order(109)
    @QuerySqlField
    private Long orderIdPrev;

    @Order(110)
    @QuerySqlField
    private Double orderMrr;

    @Order(111)
    @QuerySqlField
    private Double orderMrrPrev;

    @Order(112)
    @QuerySqlField
    private Double orderMrrChange;

    @Order(113)
    @QuerySqlField
    private String orderChangeType;

    @Order(114)
    @QuerySqlField
    private String orderChangeTypeDetail;

    @Order(115)
    @QuerySqlField
    private Double dataMrr;

    @Order(116)
    @QuerySqlField
    private Double dataMrrPrev;

    @Order(117)
    @QuerySqlField
    private Double dataMrrChange;

    @Order(118)
    @QuerySqlField
    private Double audioMrr;

    @Order(119)
    @QuerySqlField
    private Double audioMrrPrev;

    @Order(120)
    @QuerySqlField
    private Double audioMrrChange;

    @Order(121)
    @QuerySqlField
    private Double storageMrr;

    @Order(122)
    @QuerySqlField
    private Double storageMrrPrev;

    @Order(123)
    @QuerySqlField
    private Double storageMrrChange;

    @Order(124)
    @QuerySqlField
    private String dataChangeType;

    @Order(125)
    @QuerySqlField
    private String audioChangeType;

    @Order(126)
    @QuerySqlField
    private String storageChangeType;

    @Order(127)
    @QuerySqlField
    private Integer orderFilter;

    @Order(128)
    @QuerySqlField
    private String orderComment;

    @Order(129)
    @QuerySqlField
    private Long churnReasonCode;

    @Order(130)
    @QuerySqlField
    private String churnReasonDescription;

    @Order(131)
    @QuerySqlField
    private String churnComment;

    @Order(132)
    @QuerySqlField
    private String fiscalQtrSubscrEffectFrom;

    @Order(133)
    @QuerySqlField
    private String fiscalMthSubscrEffectFrom;

    @Order(134)
    @QuerySqlField
    private String fiscalWeekSubscrEffectFrom;

    @Order(135)
    @QuerySqlField
    private String fiscalQtrOpSubscrEffectFrom;

    @Order(136)
    @QuerySqlField
    private String fiscalMthOpSubscrEffectFrom;

    @Order(137)
    @QuerySqlField
    private String fiscalQtrSubscrEffectTo;

    @Order(138)
    @QuerySqlField
    private String fiscalMthSubscrEffectTo;

    @Order(139)
    @QuerySqlField
    private String fiscalWeekSubscrEffectTo;

    @Order(140)
    @QuerySqlField
    private String fiscalQtrOpSubscrEffectTo;

    @Order(141)
    @QuerySqlField
    private String fiscalMthOpSubscrEffectTo;

    @Order(142)
    @QuerySqlField
    private Date edwCreateDtm;

    @Order(143)
    @QuerySqlField
    private String edwCreateUser;

    @Order(144)
    @QuerySqlField
    private Date edwUpdateDtm;

    @Order(145)
    @QuerySqlField
    private String edwUpdateUser;

    @Order(146)
    @QuerySqlField
    private Date orderServiceStartDtm;

    @Order(147)
    @QuerySqlField
    private Date orderServiceEndDtm;

    @Order(148)
    @QuerySqlField
    private Date subscriptionEffectiveFromDtm;

    @Order(149)
    @QuerySqlField
    private Date subscriptionEffectiveToDtm;

    @Order(150)
    @QuerySqlField
    private String subscriptionSameWeekCancellationFlag;

    @Order(151)
    @QuerySqlField
    private String firstTimeFreeToPaidFlag;

    @Order(152)
    @QuerySqlField
    private String lastRecordIdentifier;

    @Order(153)
    @QuerySqlField
    private Long churnReasonCodeCombined;

    @Order(154)
    @QuerySqlField
    private String churnReasonDescriptionCombined;

    public Ds2BrmMaster() {
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getContactFirstName() {
        return contactFirstName;
    }

    public void setContactFirstName(String contactFirstName) {
        this.contactFirstName = contactFirstName;
    }

    public String getContactLastName() {
        return contactLastName;
    }

    public void setContactLastName(String contactLastName) {
        this.contactLastName = contactLastName;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getBillDay() {
        return billDay;
    }

    public void setBillDay(Long billDay) {
        this.billDay = billDay;
    }

    public String getBillToAddress1() {
        return billToAddress1;
    }

    public void setBillToAddress1(String billToAddress1) {
        this.billToAddress1 = billToAddress1;
    }

    public String getBillToCity() {
        return billToCity;
    }

    public void setBillToCity(String billToCity) {
        this.billToCity = billToCity;
    }

    public String getBillToStateProvince() {
        return billToStateProvince;
    }

    public void setBillToStateProvince(String billToStateProvince) {
        this.billToStateProvince = billToStateProvince;
    }

    public String getBillToPostalCode() {
        return billToPostalCode;
    }

    public void setBillToPostalCode(String billToPostalCode) {
        this.billToPostalCode = billToPostalCode;
    }

    public String getBillToCountry() {
        return billToCountry;
    }

    public void setBillToCountry(String billToCountry) {
        this.billToCountry = billToCountry;
    }

    public Long getBillToPhone() {
        return billToPhone;
    }

    public void setBillToPhone(Long billToPhone) {
        this.billToPhone = billToPhone;
    }

    public String getBillToEmail() {
        return billToEmail;
    }

    public void setBillToEmail(String billToEmail) {
        this.billToEmail = billToEmail;
    }

    public String getShipToAddress1() {
        return shipToAddress1;
    }

    public void setShipToAddress1(String shipToAddress1) {
        this.shipToAddress1 = shipToAddress1;
    }

    public String getShipToCity() {
        return shipToCity;
    }

    public void setShipToCity(String shipToCity) {
        this.shipToCity = shipToCity;
    }

    public String getShipToStateProvince() {
        return shipToStateProvince;
    }

    public void setShipToStateProvince(String shipToStateProvince) {
        this.shipToStateProvince = shipToStateProvince;
    }

    public String getShipToPostalCode() {
        return shipToPostalCode;
    }

    public void setShipToPostalCode(String shipToPostalCode) {
        this.shipToPostalCode = shipToPostalCode;
    }

    public String getShipToCountry() {
        return shipToCountry;
    }

    public void setShipToCountry(String shipToCountry) {
        this.shipToCountry = shipToCountry;
    }

    public Long getShipToPhone() {
        return shipToPhone;
    }

    public void setShipToPhone(Long shipToPhone) {
        this.shipToPhone = shipToPhone;
    }

    public String getShipToEmail() {
        return shipToEmail;
    }

    public void setShipToEmail(String shipToEmail) {
        this.shipToEmail = shipToEmail;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getCreatedDtm() {
        return createdDtm;
    }

    public void setCreatedDtm(Date createdDtm) {
        this.createdDtm = createdDtm;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getLanguages() {
        return languages;
    }

    public void setLanguages(String languages) {
        this.languages = languages;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getStateProvince() {
        return stateProvince;
    }

    public void setStateProvince(String stateProvince) {
        this.stateProvince = stateProvince;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Long getPreviousSubscriptionId() {
        return previousSubscriptionId;
    }

    public void setPreviousSubscriptionId(Long previousSubscriptionId) {
        this.previousSubscriptionId = previousSubscriptionId;
    }

    public Date getSubscriptionEffectiveFromDt() {
        return subscriptionEffectiveFromDt;
    }

    public void setSubscriptionEffectiveFromDt(Date subscriptionEffectiveFromDt) {
        this.subscriptionEffectiveFromDt = subscriptionEffectiveFromDt;
    }

    public Date getSubscriptionEffectiveToDt() {
        return subscriptionEffectiveToDt;
    }

    public void setSubscriptionEffectiveToDt(Date subscriptionEffectiveToDt) {
        this.subscriptionEffectiveToDt = subscriptionEffectiveToDt;
    }

    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public Long getInitialTerm() {
        return initialTerm;
    }

    public void setInitialTerm(Long initialTerm) {
        this.initialTerm = initialTerm;
    }

    public Long getRenewalTerm() {
        return renewalTerm;
    }

    public void setRenewalTerm(Long renewalTerm) {
        this.renewalTerm = renewalTerm;
    }

    public Date getServiceRenewalDate() {
        return serviceRenewalDate;
    }

    public void setServiceRenewalDate(Date serviceRenewalDate) {
        this.serviceRenewalDate = serviceRenewalDate;
    }

    public Integer getPrepaymentTerm() {
        return prepaymentTerm;
    }

    public void setPrepaymentTerm(Integer prepaymentTerm) {
        this.prepaymentTerm = prepaymentTerm;
    }

    public String getOfferCode() {
        return offerCode;
    }

    public void setOfferCode(String offerCode) {
        this.offerCode = offerCode;
    }

    public String getFinanceReasonCode() {
        return financeReasonCode;
    }

    public void setFinanceReasonCode(String financeReasonCode) {
        this.financeReasonCode = financeReasonCode;
    }

    public String getFinanceSubStatus() {
        return financeSubStatus;
    }

    public void setFinanceSubStatus(String financeSubStatus) {
        this.financeSubStatus = financeSubStatus;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Long orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getOrderSubType() {
        return orderSubType;
    }

    public void setOrderSubType(String orderSubType) {
        this.orderSubType = orderSubType;
    }

    public String getOrderSellType() {
        return orderSellType;
    }

    public void setOrderSellType(String orderSellType) {
        this.orderSellType = orderSellType;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getOrderFinanceApprovedFlg() {
        return orderFinanceApprovedFlg;
    }

    public void setOrderFinanceApprovedFlg(String orderFinanceApprovedFlg) {
        this.orderFinanceApprovedFlg = orderFinanceApprovedFlg;
    }

    public String getOrderOpportunityId() {
        return orderOpportunityId;
    }

    public void setOrderOpportunityId(String orderOpportunityId) {
        this.orderOpportunityId = orderOpportunityId;
    }

    public Date getOrderCreatedDate() {
        return orderCreatedDate;
    }

    public void setOrderCreatedDate(Date orderCreatedDate) {
        this.orderCreatedDate = orderCreatedDate;
    }

    public Date getOrderCreatedDtm() {
        return orderCreatedDtm;
    }

    public void setOrderCreatedDtm(Date orderCreatedDtm) {
        this.orderCreatedDtm = orderCreatedDtm;
    }

    public Date getOrderEffectiveTo() {
        return orderEffectiveTo;
    }

    public void setOrderEffectiveTo(Date orderEffectiveTo) {
        this.orderEffectiveTo = orderEffectiveTo;
    }

    public Date getOrderEffectiveToDtm() {
        return orderEffectiveToDtm;
    }

    public void setOrderEffectiveToDtm(Date orderEffectiveToDtm) {
        this.orderEffectiveToDtm = orderEffectiveToDtm;
    }

    public Date getOrderCompletedDate() {
        return orderCompletedDate;
    }

    public void setOrderCompletedDate(Date orderCompletedDate) {
        this.orderCompletedDate = orderCompletedDate;
    }

    public Date getOrderCompletedDtm() {
        return orderCompletedDtm;
    }

    public void setOrderCompletedDtm(Date orderCompletedDtm) {
        this.orderCompletedDtm = orderCompletedDtm;
    }

    public Date getOrderAcceptedDate() {
        return orderAcceptedDate;
    }

    public void setOrderAcceptedDate(Date orderAcceptedDate) {
        this.orderAcceptedDate = orderAcceptedDate;
    }

    public Date getOrderAcceptedDtm() {
        return orderAcceptedDtm;
    }

    public void setOrderAcceptedDtm(Date orderAcceptedDtm) {
        this.orderAcceptedDtm = orderAcceptedDtm;
    }

    public Date getOrderBookedDate() {
        return orderBookedDate;
    }

    public void setOrderBookedDate(Date orderBookedDate) {
        this.orderBookedDate = orderBookedDate;
    }

    public Date getOrderBookedDtm() {
        return orderBookedDtm;
    }

    public void setOrderBookedDtm(Date orderBookedDtm) {
        this.orderBookedDtm = orderBookedDtm;
    }

    public Date getOrderCompensationDate() {
        return orderCompensationDate;
    }

    public void setOrderCompensationDate(Date orderCompensationDate) {
        this.orderCompensationDate = orderCompensationDate;
    }

    public Date getOrderCompensationDtm() {
        return orderCompensationDtm;
    }

    public void setOrderCompensationDtm(Date orderCompensationDtm) {
        this.orderCompensationDtm = orderCompensationDtm;
    }

    public String getOrderCreatedBy() {
        return orderCreatedBy;
    }

    public void setOrderCreatedBy(String orderCreatedBy) {
        this.orderCreatedBy = orderCreatedBy;
    }

    public String getOrderModifiedBy() {
        return orderModifiedBy;
    }

    public void setOrderModifiedBy(String orderModifiedBy) {
        this.orderModifiedBy = orderModifiedBy;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public Date getOrderServiceStartDate() {
        return orderServiceStartDate;
    }

    public void setOrderServiceStartDate(Date orderServiceStartDate) {
        this.orderServiceStartDate = orderServiceStartDate;
    }

    public Date getOrderServiceEndDate() {
        return orderServiceEndDate;
    }

    public void setOrderServiceEndDate(Date orderServiceEndDate) {
        this.orderServiceEndDate = orderServiceEndDate;
    }

    public Date getOrderStartDate() {
        return orderStartDate;
    }

    public void setOrderStartDate(Date orderStartDate) {
        this.orderStartDate = orderStartDate;
    }

    public Date getOrderStartDtm() {
        return orderStartDtm;
    }

    public void setOrderStartDtm(Date orderStartDtm) {
        this.orderStartDtm = orderStartDtm;
    }

    public Date getOrderEndDate() {
        return orderEndDate;
    }

    public void setOrderEndDate(Date orderEndDate) {
        this.orderEndDate = orderEndDate;
    }

    public Date getOrderEndDtm() {
        return orderEndDtm;
    }

    public void setOrderEndDtm(Date orderEndDtm) {
        this.orderEndDtm = orderEndDtm;
    }

    public Date getTermStartDate() {
        return termStartDate;
    }

    public void setTermStartDate(Date termStartDate) {
        this.termStartDate = termStartDate;
    }

    public Date getTermEndDate() {
        return termEndDate;
    }

    public void setTermEndDate(Date termEndDate) {
        this.termEndDate = termEndDate;
    }

    public String getSkuId() {
        return skuId;
    }

    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    public String getSkuLabel() {
        return skuLabel;
    }

    public void setSkuLabel(String skuLabel) {
        this.skuLabel = skuLabel;
    }

    public String getSkuType() {
        return skuType;
    }

    public void setSkuType(String skuType) {
        this.skuType = skuType;
    }

    public String getChargeType() {
        return chargeType;
    }

    public void setChargeType(String chargeType) {
        this.chargeType = chargeType;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public Double getSkuMrr() {
        return skuMrr;
    }

    public void setSkuMrr(Double skuMrr) {
        this.skuMrr = skuMrr;
    }

    public Double getSkuMrrUsd() {
        return skuMrrUsd;
    }

    public void setSkuMrrUsd(Double skuMrrUsd) {
        this.skuMrrUsd = skuMrrUsd;
    }

    public Double getSellingUnitPrice() {
        return sellingUnitPrice;
    }

    public void setSellingUnitPrice(Double sellingUnitPrice) {
        this.sellingUnitPrice = sellingUnitPrice;
    }

    public Long getQty() {
        return qty;
    }

    public void setQty(Long qty) {
        this.qty = qty;
    }

    public String getCreditor() {
        return creditor;
    }

    public void setCreditor(String creditor) {
        this.creditor = creditor;
    }

    public String getRecurringInd() {
        return recurringInd;
    }

    public void setRecurringInd(String recurringInd) {
        this.recurringInd = recurringInd;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getRatingModel() {
        return ratingModel;
    }

    public void setRatingModel(String ratingModel) {
        this.ratingModel = ratingModel;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getDeploymentModel() {
        return deploymentModel;
    }

    public void setDeploymentModel(String deploymentModel) {
        this.deploymentModel = deploymentModel;
    }

    public String getCoreOffers() {
        return coreOffers;
    }

    public void setCoreOffers(String coreOffers) {
        this.coreOffers = coreOffers;
    }

    public String getInitialOfferCode() {
        return initialOfferCode;
    }

    public void setInitialOfferCode(String initialOfferCode) {
        this.initialOfferCode = initialOfferCode;
    }

    public String getSignupType() {
        return signupType;
    }

    public void setSignupType(String signupType) {
        this.signupType = signupType;
    }

    public String getFraudFlag() {
        return fraudFlag;
    }

    public void setFraudFlag(String fraudFlag) {
        this.fraudFlag = fraudFlag;
    }

    public Long getOrderTrackId() {
        return orderTrackId;
    }

    public void setOrderTrackId(Long orderTrackId) {
        this.orderTrackId = orderTrackId;
    }

    public String getAcquisitionSource() {
        return acquisitionSource;
    }

    public void setAcquisitionSource(String acquisitionSource) {
        this.acquisitionSource = acquisitionSource;
    }

    public String getAcquisitionSourceRptg() {
        return acquisitionSourceRptg;
    }

    public void setAcquisitionSourceRptg(String acquisitionSourceRptg) {
        this.acquisitionSourceRptg = acquisitionSourceRptg;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public void setCampaignName(String campaignName) {
        this.campaignName = campaignName;
    }

    public String getPromoDescription() {
        return promoDescription;
    }

    public void setPromoDescription(String promoDescription) {
        this.promoDescription = promoDescription;
    }

    public String getDeliveryChannel() {
        return deliveryChannel;
    }

    public void setDeliveryChannel(String deliveryChannel) {
        this.deliveryChannel = deliveryChannel;
    }

    public Date getPromoStartDate() {
        return promoStartDate;
    }

    public void setPromoStartDate(Date promoStartDate) {
        this.promoStartDate = promoStartDate;
    }

    public Date getPromoEndDate() {
        return promoEndDate;
    }

    public void setPromoEndDate(Date promoEndDate) {
        this.promoEndDate = promoEndDate;
    }

    public String getPromoDiscountType() {
        return promoDiscountType;
    }

    public void setPromoDiscountType(String promoDiscountType) {
        this.promoDiscountType = promoDiscountType;
    }

    public String getDiscountPctAmt() {
        return discountPctAmt;
    }

    public void setDiscountPctAmt(String discountPctAmt) {
        this.discountPctAmt = discountPctAmt;
    }

    public Long getPromoTerm() {
        return promoTerm;
    }

    public void setPromoTerm(Long promoTerm) {
        this.promoTerm = promoTerm;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getGeoArea() {
        return geoArea;
    }

    public void setGeoArea(String geoArea) {
        this.geoArea = geoArea;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Long getOrderIdPrev() {
        return orderIdPrev;
    }

    public void setOrderIdPrev(Long orderIdPrev) {
        this.orderIdPrev = orderIdPrev;
    }

    public Double getOrderMrr() {
        return orderMrr;
    }

    public void setOrderMrr(Double orderMrr) {
        this.orderMrr = orderMrr;
    }

    public Double getOrderMrrPrev() {
        return orderMrrPrev;
    }

    public void setOrderMrrPrev(Double orderMrrPrev) {
        this.orderMrrPrev = orderMrrPrev;
    }

    public Double getOrderMrrChange() {
        return orderMrrChange;
    }

    public void setOrderMrrChange(Double orderMrrChange) {
        this.orderMrrChange = orderMrrChange;
    }

    public String getOrderChangeType() {
        return orderChangeType;
    }

    public void setOrderChangeType(String orderChangeType) {
        this.orderChangeType = orderChangeType;
    }

    public String getOrderChangeTypeDetail() {
        return orderChangeTypeDetail;
    }

    public void setOrderChangeTypeDetail(String orderChangeTypeDetail) {
        this.orderChangeTypeDetail = orderChangeTypeDetail;
    }

    public Double getDataMrr() {
        return dataMrr;
    }

    public void setDataMrr(Double dataMrr) {
        this.dataMrr = dataMrr;
    }

    public Double getDataMrrPrev() {
        return dataMrrPrev;
    }

    public void setDataMrrPrev(Double dataMrrPrev) {
        this.dataMrrPrev = dataMrrPrev;
    }

    public Double getDataMrrChange() {
        return dataMrrChange;
    }

    public void setDataMrrChange(Double dataMrrChange) {
        this.dataMrrChange = dataMrrChange;
    }

    public Double getAudioMrr() {
        return audioMrr;
    }

    public void setAudioMrr(Double audioMrr) {
        this.audioMrr = audioMrr;
    }

    public Double getAudioMrrPrev() {
        return audioMrrPrev;
    }

    public void setAudioMrrPrev(Double audioMrrPrev) {
        this.audioMrrPrev = audioMrrPrev;
    }

    public Double getAudioMrrChange() {
        return audioMrrChange;
    }

    public void setAudioMrrChange(Double audioMrrChange) {
        this.audioMrrChange = audioMrrChange;
    }

    public Double getStorageMrr() {
        return storageMrr;
    }

    public void setStorageMrr(Double storageMrr) {
        this.storageMrr = storageMrr;
    }

    public Double getStorageMrrPrev() {
        return storageMrrPrev;
    }

    public void setStorageMrrPrev(Double storageMrrPrev) {
        this.storageMrrPrev = storageMrrPrev;
    }

    public Double getStorageMrrChange() {
        return storageMrrChange;
    }

    public void setStorageMrrChange(Double storageMrrChange) {
        this.storageMrrChange = storageMrrChange;
    }

    public String getDataChangeType() {
        return dataChangeType;
    }

    public void setDataChangeType(String dataChangeType) {
        this.dataChangeType = dataChangeType;
    }

    public String getAudioChangeType() {
        return audioChangeType;
    }

    public void setAudioChangeType(String audioChangeType) {
        this.audioChangeType = audioChangeType;
    }

    public String getStorageChangeType() {
        return storageChangeType;
    }

    public void setStorageChangeType(String storageChangeType) {
        this.storageChangeType = storageChangeType;
    }

    public Integer getOrderFilter() {
        return orderFilter;
    }

    public void setOrderFilter(Integer orderFilter) {
        this.orderFilter = orderFilter;
    }

    public String getOrderComment() {
        return orderComment;
    }

    public void setOrderComment(String orderComment) {
        this.orderComment = orderComment;
    }

    public Long getChurnReasonCode() {
        return churnReasonCode;
    }

    public void setChurnReasonCode(Long churnReasonCode) {
        this.churnReasonCode = churnReasonCode;
    }

    public String getChurnReasonDescription() {
        return churnReasonDescription;
    }

    public void setChurnReasonDescription(String churnReasonDescription) {
        this.churnReasonDescription = churnReasonDescription;
    }

    public String getChurnComment() {
        return churnComment;
    }

    public void setChurnComment(String churnComment) {
        this.churnComment = churnComment;
    }

    public String getFiscalQtrSubscrEffectFrom() {
        return fiscalQtrSubscrEffectFrom;
    }

    public void setFiscalQtrSubscrEffectFrom(String fiscalQtrSubscrEffectFrom) {
        this.fiscalQtrSubscrEffectFrom = fiscalQtrSubscrEffectFrom;
    }

    public String getFiscalMthSubscrEffectFrom() {
        return fiscalMthSubscrEffectFrom;
    }

    public void setFiscalMthSubscrEffectFrom(String fiscalMthSubscrEffectFrom) {
        this.fiscalMthSubscrEffectFrom = fiscalMthSubscrEffectFrom;
    }

    public String getFiscalWeekSubscrEffectFrom() {
        return fiscalWeekSubscrEffectFrom;
    }

    public void setFiscalWeekSubscrEffectFrom(String fiscalWeekSubscrEffectFrom) {
        this.fiscalWeekSubscrEffectFrom = fiscalWeekSubscrEffectFrom;
    }

    public String getFiscalQtrOpSubscrEffectFrom() {
        return fiscalQtrOpSubscrEffectFrom;
    }

    public void setFiscalQtrOpSubscrEffectFrom(String fiscalQtrOpSubscrEffectFrom) {
        this.fiscalQtrOpSubscrEffectFrom = fiscalQtrOpSubscrEffectFrom;
    }

    public String getFiscalMthOpSubscrEffectFrom() {
        return fiscalMthOpSubscrEffectFrom;
    }

    public void setFiscalMthOpSubscrEffectFrom(String fiscalMthOpSubscrEffectFrom) {
        this.fiscalMthOpSubscrEffectFrom = fiscalMthOpSubscrEffectFrom;
    }

    public String getFiscalQtrSubscrEffectTo() {
        return fiscalQtrSubscrEffectTo;
    }

    public void setFiscalQtrSubscrEffectTo(String fiscalQtrSubscrEffectTo) {
        this.fiscalQtrSubscrEffectTo = fiscalQtrSubscrEffectTo;
    }

    public String getFiscalMthSubscrEffectTo() {
        return fiscalMthSubscrEffectTo;
    }

    public void setFiscalMthSubscrEffectTo(String fiscalMthSubscrEffectTo) {
        this.fiscalMthSubscrEffectTo = fiscalMthSubscrEffectTo;
    }

    public String getFiscalWeekSubscrEffectTo() {
        return fiscalWeekSubscrEffectTo;
    }

    public void setFiscalWeekSubscrEffectTo(String fiscalWeekSubscrEffectTo) {
        this.fiscalWeekSubscrEffectTo = fiscalWeekSubscrEffectTo;
    }

    public String getFiscalQtrOpSubscrEffectTo() {
        return fiscalQtrOpSubscrEffectTo;
    }

    public void setFiscalQtrOpSubscrEffectTo(String fiscalQtrOpSubscrEffectTo) {
        this.fiscalQtrOpSubscrEffectTo = fiscalQtrOpSubscrEffectTo;
    }

    public String getFiscalMthOpSubscrEffectTo() {
        return fiscalMthOpSubscrEffectTo;
    }

    public void setFiscalMthOpSubscrEffectTo(String fiscalMthOpSubscrEffectTo) {
        this.fiscalMthOpSubscrEffectTo = fiscalMthOpSubscrEffectTo;
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

    public Date getOrderServiceStartDtm() {
        return orderServiceStartDtm;
    }

    public void setOrderServiceStartDtm(Date orderServiceStartDtm) {
        this.orderServiceStartDtm = orderServiceStartDtm;
    }

    public Date getOrderServiceEndDtm() {
        return orderServiceEndDtm;
    }

    public void setOrderServiceEndDtm(Date orderServiceEndDtm) {
        this.orderServiceEndDtm = orderServiceEndDtm;
    }

    public Date getSubscriptionEffectiveFromDtm() {
        return subscriptionEffectiveFromDtm;
    }

    public void setSubscriptionEffectiveFromDtm(Date subscriptionEffectiveFromDtm) {
        this.subscriptionEffectiveFromDtm = subscriptionEffectiveFromDtm;
    }

    public Date getSubscriptionEffectiveToDtm() {
        return subscriptionEffectiveToDtm;
    }

    public void setSubscriptionEffectiveToDtm(Date subscriptionEffectiveToDtm) {
        this.subscriptionEffectiveToDtm = subscriptionEffectiveToDtm;
    }

    public String getSubscriptionSameWeekCancellationFlag() {
        return subscriptionSameWeekCancellationFlag;
    }

    public void setSubscriptionSameWeekCancellationFlag(String subscriptionSameWeekCancellationFlag) {
        this.subscriptionSameWeekCancellationFlag = subscriptionSameWeekCancellationFlag;
    }

    public String getFirstTimeFreeToPaidFlag() {
        return firstTimeFreeToPaidFlag;
    }

    public void setFirstTimeFreeToPaidFlag(String firstTimeFreeToPaidFlag) {
        this.firstTimeFreeToPaidFlag = firstTimeFreeToPaidFlag;
    }

    public String getLastRecordIdentifier() {
        return lastRecordIdentifier;
    }

    public void setLastRecordIdentifier(String lastRecordIdentifier) {
        this.lastRecordIdentifier = lastRecordIdentifier;
    }

    public Long getChurnReasonCodeCombined() {
        return churnReasonCodeCombined;
    }

    public void setChurnReasonCodeCombined(Long churnReasonCodeCombined) {
        this.churnReasonCodeCombined = churnReasonCodeCombined;
    }

    public String getChurnReasonDescriptionCombined() {
        return churnReasonDescriptionCombined;
    }

    public void setChurnReasonDescriptionCombined(String churnReasonDescriptionCombined) {
        this.churnReasonDescriptionCombined = churnReasonDescriptionCombined;
    }
}
