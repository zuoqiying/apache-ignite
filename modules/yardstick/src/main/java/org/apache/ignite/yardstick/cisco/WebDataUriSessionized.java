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
import java.util.Map;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.springframework.core.annotation.Order;

/**
 *
 */
public class WebDataUriSessionized {
    @Order(0)
    @QuerySqlField
    private String key;

    @Order(1)
    @QuerySqlField
    private String ip;

    @Order(2)
    @QuerySqlField
    private String cookie;

    @Order(3)
    @QuerySqlField
    private String userid;

    @Order(4)
    @QuerySqlField
    private String dattime;

    @Order(5)
    @QuerySqlField
    private Long timestamp;

    @Order(6)
    @QuerySqlField
    private String referrer;

    @Order(7)
    @QuerySqlField
    private String request;

    @Order(8)
    @QuerySqlField(index = true)
    private String useragent;

    @Order(9)
    @QuerySqlField
    private String absoluteips;

    @Order(10)
    @QuerySqlField
    private String xff;

    @Order(11)
    @QuerySqlField(index = true)
    private String calltype;

    @Order(12)
    @QuerySqlField
    private String url;

    @Order(13)
    @QuerySqlField
    private String dqs;

    @Order(14)
    @QuerySqlField
    private String uniqueVisitor;

    @Order(15)
    @QuerySqlField
    private String referrerUrl;

    //@Order(16)
    @QuerySqlField
    private Map<String, String> referrerDqp;

    //@Order(17)
    @QuerySqlField
    private Map<String, String> dqp;

    //@Order(18)
    @QuerySqlField
    private Map<String, String> params;

    //@Order(19)
    @QuerySqlField
    private Map<String, String> eventsSubcategory;

    @Order(18)
    @QuerySqlField(index = true)
    private String sessionId;

    @Order(19)
    @QuerySqlField
    private String source;

    @Order(20)
    @QuerySqlField(index = true)
    private Date dwelltime;

//    @Order(23)
    @QuerySqlField
    private String traffic;

    public WebDataUriSessionized() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getDattime() {
        return dattime;
    }

    public void setDattime(String dattime) {
        this.dattime = dattime;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getUseragent() {
        return useragent;
    }

    public void setUseragent(String useragent) {
        this.useragent = useragent;
    }

    public String getAbsoluteips() {
        return absoluteips;
    }

    public void setAbsoluteips(String absoluteips) {
        this.absoluteips = absoluteips;
    }

    public String getXff() {
        return xff;
    }

    public void setXff(String xff) {
        this.xff = xff;
    }

    public String getCalltype() {
        return calltype;
    }

    public void setCalltype(String calltype) {
        this.calltype = calltype;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDqs() {
        return dqs;
    }

    public void setDqs(String dqs) {
        this.dqs = dqs;
    }

    public String getUniqueVisitor() {
        return uniqueVisitor;
    }

    public void setUniqueVisitor(String uniqueVisitor) {
        this.uniqueVisitor = uniqueVisitor;
    }

    public String getReferrerUrl() {
        return referrerUrl;
    }

    public void setReferrerUrl(String referrerUrl) {
        this.referrerUrl = referrerUrl;
    }

    public Map<String, String> getReferrerDqp() {
        return referrerDqp;
    }

    public void setReferrerDqp(Map<String, String> referrerDqp) {
        this.referrerDqp = referrerDqp;
    }

    public Map<String, String> getDqp() {
        return dqp;
    }

    public void setDqp(Map<String, String> dqp) {
        this.dqp = dqp;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public Map<String, String> getEventsSubcategory() {
        return eventsSubcategory;
    }

    public void setEventsSubcategory(Map<String, String> eventsSubcategory) {
        this.eventsSubcategory = eventsSubcategory;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Date getDwelltime() {
        return dwelltime;
    }

    public void setDwelltime(Date dwelltime) {
        this.dwelltime = dwelltime;
    }

    public String getTraffic() {
        return traffic;
    }

    public void setTraffic(String traffic) {
        this.traffic = traffic;
    }
}
