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
public class DwUaUrl {
    @Order(0)
    @QuerySqlField
    private String areaLevel1;

    @Order(1)
    @QuerySqlField
    private String areaLevel2;

    @Order(2)
    @QuerySqlField
    private String areaLevel3;

    @Order(3)
    @QuerySqlField
    private String areaLevel4;

    @Order(4)
    @QuerySqlField
    private String areaLevel5;

    @Order(5)
    @QuerySqlField
    private String areaLevel6;

    @Order(6)
    @QuerySqlField
    private String urlDesc;

    @Order(7)
    @QuerySqlField
    private String url;

    @Order(8)
    @QuerySqlField
    private String updateDate;

    @Order(9)
    @QuerySqlField
    private String createDate;

    public DwUaUrl() {
    }

    public String getAreaLevel1() {
        return areaLevel1;
    }

    public void setAreaLevel1(String areaLevel1) {
        this.areaLevel1 = areaLevel1;
    }

    public String getAreaLevel2() {
        return areaLevel2;
    }

    public void setAreaLevel2(String areaLevel2) {
        this.areaLevel2 = areaLevel2;
    }

    public String getAreaLevel3() {
        return areaLevel3;
    }

    public void setAreaLevel3(String areaLevel3) {
        this.areaLevel3 = areaLevel3;
    }

    public String getAreaLevel4() {
        return areaLevel4;
    }

    public void setAreaLevel4(String areaLevel4) {
        this.areaLevel4 = areaLevel4;
    }

    public String getAreaLevel5() {
        return areaLevel5;
    }

    public void setAreaLevel5(String areaLevel5) {
        this.areaLevel5 = areaLevel5;
    }

    public String getAreaLevel6() {
        return areaLevel6;
    }

    public void setAreaLevel6(String areaLevel6) {
        this.areaLevel6 = areaLevel6;
    }

    public String getUrlDesc() {
        return urlDesc;
    }

    public void setUrlDesc(String urlDesc) {
        this.urlDesc = urlDesc;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    @Override public String toString() {
        return "DwUaUrl{" +
            "areaLevel1=" + areaLevel1 + '\n' +
            ", areaLevel2=" + areaLevel2 + '\n' +
            ", areaLevel3=" + areaLevel3 + '\n' +
            ", areaLevel4=" + areaLevel4 + '\n' +
            ", areaLevel5=" + areaLevel5 + '\n' +
            ", areaLevel6=" + areaLevel6 + '\n' +
            ", urlDesc=" + urlDesc + '\n' +
            ", url=" + url + '\n' +
            ", updateDate=" + updateDate + '\n' +
            ", createDate=" + createDate + '\n' +
            '}';
    }
}
