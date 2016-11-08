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
public class XxrptHgsMeetingUserReport {
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
    private Double conftype;

    @Order(4)
    @QuerySqlField
    private String usertype;

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
    private Double uid;

    @Order(9)
    @QuerySqlField
    private Double gid;

    @Order(10)
    @QuerySqlField
    private String webexid;

    @Order(11)
    @QuerySqlField
    private String username;

    @Order(12)
    @QuerySqlField
    private String useremail;

    @Order(13)
    @QuerySqlField
    private String ipaddress;

    @Order(14)
    @QuerySqlField
    private Double siteid;

    @Order(15)
    @QuerySqlField
    private String sitename;

    @Order(16)
    @QuerySqlField
    private Double dialin1;

    @Order(17)
    @QuerySqlField
    private Double dialin2;

    @Order(18)
    @QuerySqlField
    private Double dialout1;

    @Order(19)
    @QuerySqlField
    private Double dialout2;

    @Order(20)
    @QuerySqlField
    private Double voip;

    @Order(21)
    @QuerySqlField
    private Double hgssiteid;

    @Order(22)
    @QuerySqlField
    private String meetingtype;

    @Order(23)
    @QuerySqlField
    private Double timezone;

    @Order(24)
    @QuerySqlField
    private Double objid;

    @Order(25)
    @QuerySqlField
    private String clientagent;

    @Order(26)
    @QuerySqlField
    private String mtgStarttime;

    @Order(27)
    @QuerySqlField
    private String mtgEndtime;

    @Order(28)
    @QuerySqlField
    private Double mtgTimezone;

    @Order(29)
    @QuerySqlField
    private String registered;

    @Order(30)
    @QuerySqlField
    private String invited;

    @Order(31)
    @QuerySqlField
    private String regCompany;

    @Order(32)
    @QuerySqlField
    private String regTitle;

    @Order(33)
    @QuerySqlField
    private String regPhone;

    @Order(34)
    @QuerySqlField
    private String regAddress1;

    @Order(35)
    @QuerySqlField
    private String regAddress2;

    @Order(36)
    @QuerySqlField
    private String regCity;

    @Order(37)
    @QuerySqlField
    private String regState;

    @Order(38)
    @QuerySqlField
    private String regCountry;

    @Order(39)
    @QuerySqlField
    private String regZip;

    @Order(40)
    @QuerySqlField
    private Double nonBillable;

    @Order(41)
    @QuerySqlField
    private Double attendantid;

    @Order(42)
    @QuerySqlField
    private String attFirstname;

    @Order(43)
    @QuerySqlField
    private String attLastname;

    @Order(44)
    @QuerySqlField
    private String serviceIdentifier;

    @Order(45)
    @QuerySqlField
    private String userIdentifier;

    @Order(46)
    @QuerySqlField
    private String purchaseIdentifier;

    @Order(47)
    @QuerySqlField
    private String consumptionDate;

    @Order(48)
    @QuerySqlField
    private String subscriptionCode;

    @Order(49)
    @QuerySqlField
    private Double bossContractid;

    @Order(50)
    @QuerySqlField
    private Double rbeStatus;

    @Order(51)
    @QuerySqlField
    private String rbeTimestamp;

    @Order(52)
    @QuerySqlField
    private Double isinternal;

    @Order(53)
    @QuerySqlField
    private Double prosumer;

    @Order(54)
    @QuerySqlField
    private Double inattentiveminutes;

    @Order(55)
    @QuerySqlField
    private Double meetinglanguageid;

    @Order(56)
    @QuerySqlField
    private Double regionid;

    @Order(57)
    @QuerySqlField
    private String webexidTranslated;

    @Order(58)
    @QuerySqlField
    private String useremailTranslated;

    @Order(59)
    @QuerySqlField
    private String regAddress1Translated;

    @Order(60)
    @QuerySqlField
    private String regAddress2Translated;

    @Order(61)
    @QuerySqlField
    private String regCityTranslated;

    @Order(62)
    @QuerySqlField
    private String regCompanyTranslated;

    @Order(63)
    @QuerySqlField
    private String regCountryTranslated;

    @Order(64)
    @QuerySqlField
    private String attFirstnameTranslated;

    @Order(65)
    @QuerySqlField
    private String attLastnameTranslated;

    @Order(66)
    @QuerySqlField
    private String regStateTranslated;

    @Order(67)
    @QuerySqlField
    private String regTitleTranslated;

    @Order(68)
    @QuerySqlField
    private String usernameTranslated;

    @Order(69)
    @QuerySqlField
    private String ipaddressTranslated;

    @Order(70)
    @QuerySqlField
    private String sitenameTranslated;

    @Order(71)
    @QuerySqlField
    private String clientagentTranslated;

    @Order(72)
    @QuerySqlField
    private String createdate;

    @Order(73)
    @QuerySqlField
    private String confnameTranslated;

    @Order(74)
    @QuerySqlField
    private String privateIpaddress;

    @Order(75)
    @QuerySqlField
    private Double hostid;

    @Order(76)
    @QuerySqlField
    private Double hostlanguageid;

    @Order(77)
    @QuerySqlField
    private String accountid;

    @Order(78)
    @QuerySqlField
    private String servicecode;

    @Order(79)
    @QuerySqlField
    private String subscriptioncode;

    @Order(80)
    @QuerySqlField
    private String billingaccountid;

    @Order(81)
    @QuerySqlField
    private String meetingnodeflag;

    @Order(82)
    @QuerySqlField
    private Double attendeetag;
}
