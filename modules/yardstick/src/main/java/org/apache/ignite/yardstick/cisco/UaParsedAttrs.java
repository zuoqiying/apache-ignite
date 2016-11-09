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
public class UaParsedAttrs {
    @Order(0)
    @QuerySqlField(index = true)
    private String useragent;

    @Order(1)
    @QuerySqlField(index = true)
    private String browsername;

    @Order(2)
    @QuerySqlField
    private String browserversion;

    @Order(3)
    @QuerySqlField
    private Integer id;

    @Order(4)
    @QuerySqlField
    private String vendor;

    @Order(5)
    @QuerySqlField
    private String model;

    @Order(6)
    @QuerySqlField(index = true)
    private String osname;

    @Order(7)
    @QuerySqlField
    private String fullname;

    @Order(8)
    @QuerySqlField
    private String marketingname;

    @Order(9)
    @QuerySqlField
    private String manufacturer;

    @Order(10)
    @QuerySqlField
    private Integer yearreleased;

    @Order(11)
    @QuerySqlField
    private String primaryhardwaretype;

    @Order(12)
    @QuerySqlField
    private Integer displaywidth;

    @Order(13)
    @QuerySqlField
    private Integer displayheight;

    @Order(14)
    @QuerySqlField
    private String diagonalscreensize;

    @Order(15)
    @QuerySqlField
    private Integer displayppi;

    @Order(16)
    @QuerySqlField
    private String devicepixelratio;

    @Order(17)
    @QuerySqlField
    private Integer displaycolordepth;

    @Order(18)
    @QuerySqlField
    private String isrobot;

    @Order(19)
    @QuerySqlField
    private String botname;

    @Order(20)
    @QuerySqlField
    private String processedDate;

    public UaParsedAttrs() {
    }

    public String getUseragent() {
        return useragent;
    }

    public void setUseragent(String useragent) {
        this.useragent = useragent;
    }

    public String getBrowsername() {
        return browsername;
    }

    public void setBrowsername(String browsername) {
        this.browsername = browsername;
    }

    public String getBrowserversion() {
        return browserversion;
    }

    public void setBrowserversion(String browserversion) {
        this.browserversion = browserversion;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getOsname() {
        return osname;
    }

    public void setOsname(String osname) {
        this.osname = osname;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getMarketingname() {
        return marketingname;
    }

    public void setMarketingname(String marketingname) {
        this.marketingname = marketingname;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public Integer getYearreleased() {
        return yearreleased;
    }

    public void setYearreleased(Integer yearreleased) {
        this.yearreleased = yearreleased;
    }

    public String getPrimaryhardwaretype() {
        return primaryhardwaretype;
    }

    public void setPrimaryhardwaretype(String primaryhardwaretype) {
        this.primaryhardwaretype = primaryhardwaretype;
    }

    public Integer getDisplaywidth() {
        return displaywidth;
    }

    public void setDisplaywidth(Integer displaywidth) {
        this.displaywidth = displaywidth;
    }

    public Integer getDisplayheight() {
        return displayheight;
    }

    public void setDisplayheight(Integer displayheight) {
        this.displayheight = displayheight;
    }

    public String getDiagonalscreensize() {
        return diagonalscreensize;
    }

    public void setDiagonalscreensize(String diagonalscreensize) {
        this.diagonalscreensize = diagonalscreensize;
    }

    public Integer getDisplayppi() {
        return displayppi;
    }

    public void setDisplayppi(Integer displayppi) {
        this.displayppi = displayppi;
    }

    public String getDevicepixelratio() {
        return devicepixelratio;
    }

    public void setDevicepixelratio(String devicepixelratio) {
        this.devicepixelratio = devicepixelratio;
    }

    public Integer getDisplaycolordepth() {
        return displaycolordepth;
    }

    public void setDisplaycolordepth(Integer displaycolordepth) {
        this.displaycolordepth = displaycolordepth;
    }

    public String getIsrobot() {
        return isrobot;
    }

    public void setIsrobot(String isrobot) {
        this.isrobot = isrobot;
    }

    public String getBotname() {
        return botname;
    }

    public void setBotname(String botname) {
        this.botname = botname;
    }

    public String getProcessedDate() {
        return processedDate;
    }

    public void setProcessedDate(String processedDate) {
        this.processedDate = processedDate;
    }
}
