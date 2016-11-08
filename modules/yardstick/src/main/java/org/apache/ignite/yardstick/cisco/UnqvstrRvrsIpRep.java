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
public class UnqvstrRvrsIpRep {
    @Order(0)
    @QuerySqlField
    private String uniqueVisitor;

    @Order(1)
    @QuerySqlField
    private String ip;

    @Order(2)
    @QuerySqlField
    private String countryCode;

    @Order(3)
    @QuerySqlField
    private String domain;

    @Order(4)
    @QuerySqlField
    private String dtype;

    @Order(5)
    @QuerySqlField
    private String duns;

    @Order(6)
    @QuerySqlField
    private String name;

    @Order(7)
    @QuerySqlField
    private String city;

    @Order(8)
    @QuerySqlField
    private String state;

    @Order(9)
    @QuerySqlField
    private String region;

    @Order(10)
    @QuerySqlField
    private String timezone;

    @Order(11)
    @QuerySqlField
    private String uniqueEmailUserCount;

    @Order(12)
    @QuerySqlField
    private Long timestamp;

    @Order(13)
    @QuerySqlField
    private Date viewdate;

    public UnqvstrRvrsIpRep() {
    }

    public String getUniqueVisitor() {
        return uniqueVisitor;
    }

    public void setUniqueVisitor(String uniqueVisitor) {
        this.uniqueVisitor = uniqueVisitor;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDtype() {
        return dtype;
    }

    public void setDtype(String dtype) {
        this.dtype = dtype;
    }

    public String getDuns() {
        return duns;
    }

    public void setDuns(String duns) {
        this.duns = duns;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getUniqueEmailUserCount() {
        return uniqueEmailUserCount;
    }

    public void setUniqueEmailUserCount(String uniqueEmailUserCount) {
        this.uniqueEmailUserCount = uniqueEmailUserCount;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Date getViewdate() {
        return viewdate;
    }

    public void setViewdate(Date viewdate) {
        this.viewdate = viewdate;
    }

    @Override public String toString() {
        return "UnqvstrRvrsIpRep{" +
            "uniqueVisitor=" + uniqueVisitor + '\n' +
            ", ip=" + ip + '\n' +
            ", countryCode=" + countryCode + '\n' +
            ", domain=" + domain + '\n' +
            ", dtype=" + dtype + '\n' +
            ", duns=" + duns + '\n' +
            ", name=" + name + '\n' +
            ", city=" + city + '\n' +
            ", state=" + state + '\n' +
            ", region=" + region + '\n' +
            ", timezone=" + timezone + '\n' +
            ", uniqueEmailUserCount=" + uniqueEmailUserCount + '\n' +
            ", timestamp=" + timestamp +
            ", viewdate=" + viewdate +
            '}';
    }
}
