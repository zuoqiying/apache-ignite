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
 * Created by Nikolay on 07.11.2016.
 */
public class CprUserInfoVw {
    @Order(0)
    @QuerySqlField
    private String userid;

    @Order(1)
    @QuerySqlField
    private String contractnumber;

    @Order(2)
    @QuerySqlField
    private String contractType;

    @Order(3)
    @QuerySqlField
    private String accesslevel;

    @Order(4)
    @QuerySqlField
    private String ccie;

    @Order(5)
    @QuerySqlField
    private String registrationtime;

    @Order(6)
    @QuerySqlField
    private String svopricing;

    @Order(7)
    @QuerySqlField
    private String lastlogin;

    @Order(8)
    @QuerySqlField
    private String usersbrowser;

    @Order(9)
    @QuerySqlField
    private String usershost;

    @Order(10)
    @QuerySqlField
    private String picaadminuserid;

    @Order(11)
    @QuerySqlField
    private String logincount;

    @Order(12)
    @QuerySqlField
    private String userLanguage;

    @Order(13)
    @QuerySqlField
    private String commerceaccess;

    @Order(14)
    @QuerySqlField
    private String companybillto;

    @Order(15)
    @QuerySqlField
    private String companyshipto;

    @Order(16)
    @QuerySqlField
    private String encryptswaccess;

    @Order(17)
    @QuerySqlField
    private String casemanagement;

    @Order(18)
    @QuerySqlField
    private String userproperties;

    @Order(19)
    @QuerySqlField
    private String contracttypeadditional;

    @Order(20)
    @QuerySqlField
    private String contractadditional;

    @Order(21)
    @QuerySqlField
    private String firstname;

    @Order(22)
    @QuerySqlField
    private String lastname;

    @Order(23)
    @QuerySqlField
    private String company;

    @Order(24)
    @QuerySqlField
    private String street;

    @Order(25)
    @QuerySqlField
    private String street2;

    @Order(26)
    @QuerySqlField
    private String city;

    @Order(27)
    @QuerySqlField
    private String state;

    @Order(28)
    @QuerySqlField
    private String country;

    @Order(29)
    @QuerySqlField
    private String postalcode;

    @Order(30)
    @QuerySqlField
    private String telephonenumber;

    @Order(31)
    @QuerySqlField
    private String facsimiletelephonenumber;

    @Order(32)
    @QuerySqlField
    private String mail;

    @Order(33)
    @QuerySqlField
    private String emailfromcisco;

    @Order(34)
    @QuerySqlField
    private String thirdpartyemail;

    @Order(35)
    @QuerySqlField
    private String jobtitle;

    @Order(36)
    @QuerySqlField
    private String cancelledcontracts;

    @Order(37)
    @QuerySqlField
    private String passwordchangetime;

    @Order(38)
    @QuerySqlField
    private String userpassword;

    @Order(39)
    @QuerySqlField
    private String channelqualifications;

    @Order(40)
    @QuerySqlField
    private String surveycompletetime;

    @Order(41)
    @QuerySqlField
    private String openforumflags;

    @Order(42)
    @QuerySqlField
    private String ciscouid;

    @Order(43)
    @QuerySqlField
    private String employeenumber;

    @Order(44)
    @QuerySqlField
    private String softwaredownload;

    @Order(45)
    @QuerySqlField
    private String picaadminprefix;

    @Order(46)
    @QuerySqlField
    private String userexpire;

    @Order(47)
    @QuerySqlField
    private String websurvey;

    public CprUserInfoVw() {
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getContractnumber() {
        return contractnumber;
    }

    public void setContractnumber(String contractnumber) {
        this.contractnumber = contractnumber;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public String getAccesslevel() {
        return accesslevel;
    }

    public void setAccesslevel(String accesslevel) {
        this.accesslevel = accesslevel;
    }

    public String getCcie() {
        return ccie;
    }

    public void setCcie(String ccie) {
        this.ccie = ccie;
    }

    public String getRegistrationtime() {
        return registrationtime;
    }

    public void setRegistrationtime(String registrationtime) {
        this.registrationtime = registrationtime;
    }

    public String getSvopricing() {
        return svopricing;
    }

    public void setSvopricing(String svopricing) {
        this.svopricing = svopricing;
    }

    public String getLastlogin() {
        return lastlogin;
    }

    public void setLastlogin(String lastlogin) {
        this.lastlogin = lastlogin;
    }

    public String getUsersbrowser() {
        return usersbrowser;
    }

    public void setUsersbrowser(String usersbrowser) {
        this.usersbrowser = usersbrowser;
    }

    public String getUsershost() {
        return usershost;
    }

    public void setUsershost(String usershost) {
        this.usershost = usershost;
    }

    public String getPicaadminuserid() {
        return picaadminuserid;
    }

    public void setPicaadminuserid(String picaadminuserid) {
        this.picaadminuserid = picaadminuserid;
    }

    public String getLogincount() {
        return logincount;
    }

    public void setLogincount(String logincount) {
        this.logincount = logincount;
    }

    public String getUserLanguage() {
        return userLanguage;
    }

    public void setUserLanguage(String userLanguage) {
        this.userLanguage = userLanguage;
    }

    public String getCommerceaccess() {
        return commerceaccess;
    }

    public void setCommerceaccess(String commerceaccess) {
        this.commerceaccess = commerceaccess;
    }

    public String getCompanybillto() {
        return companybillto;
    }

    public void setCompanybillto(String companybillto) {
        this.companybillto = companybillto;
    }

    public String getCompanyshipto() {
        return companyshipto;
    }

    public void setCompanyshipto(String companyshipto) {
        this.companyshipto = companyshipto;
    }

    public String getEncryptswaccess() {
        return encryptswaccess;
    }

    public void setEncryptswaccess(String encryptswaccess) {
        this.encryptswaccess = encryptswaccess;
    }

    public String getCasemanagement() {
        return casemanagement;
    }

    public void setCasemanagement(String casemanagement) {
        this.casemanagement = casemanagement;
    }

    public String getUserproperties() {
        return userproperties;
    }

    public void setUserproperties(String userproperties) {
        this.userproperties = userproperties;
    }

    public String getContracttypeadditional() {
        return contracttypeadditional;
    }

    public void setContracttypeadditional(String contracttypeadditional) {
        this.contracttypeadditional = contracttypeadditional;
    }

    public String getContractadditional() {
        return contractadditional;
    }

    public void setContractadditional(String contractadditional) {
        this.contractadditional = contractadditional;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getStreet2() {
        return street2;
    }

    public void setStreet2(String street2) {
        this.street2 = street2;
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPostalcode() {
        return postalcode;
    }

    public void setPostalcode(String postalcode) {
        this.postalcode = postalcode;
    }

    public String getTelephonenumber() {
        return telephonenumber;
    }

    public void setTelephonenumber(String telephonenumber) {
        this.telephonenumber = telephonenumber;
    }

    public String getFacsimiletelephonenumber() {
        return facsimiletelephonenumber;
    }

    public void setFacsimiletelephonenumber(String facsimiletelephonenumber) {
        this.facsimiletelephonenumber = facsimiletelephonenumber;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getEmailfromcisco() {
        return emailfromcisco;
    }

    public void setEmailfromcisco(String emailfromcisco) {
        this.emailfromcisco = emailfromcisco;
    }

    public String getThirdpartyemail() {
        return thirdpartyemail;
    }

    public void setThirdpartyemail(String thirdpartyemail) {
        this.thirdpartyemail = thirdpartyemail;
    }

    public String getJobtitle() {
        return jobtitle;
    }

    public void setJobtitle(String jobtitle) {
        this.jobtitle = jobtitle;
    }

    public String getCancelledcontracts() {
        return cancelledcontracts;
    }

    public void setCancelledcontracts(String cancelledcontracts) {
        this.cancelledcontracts = cancelledcontracts;
    }

    public String getPasswordchangetime() {
        return passwordchangetime;
    }

    public void setPasswordchangetime(String passwordchangetime) {
        this.passwordchangetime = passwordchangetime;
    }

    public String getUserpassword() {
        return userpassword;
    }

    public void setUserpassword(String userpassword) {
        this.userpassword = userpassword;
    }

    public String getChannelqualifications() {
        return channelqualifications;
    }

    public void setChannelqualifications(String channelqualifications) {
        this.channelqualifications = channelqualifications;
    }

    public String getSurveycompletetime() {
        return surveycompletetime;
    }

    public void setSurveycompletetime(String surveycompletetime) {
        this.surveycompletetime = surveycompletetime;
    }

    public String getOpenforumflags() {
        return openforumflags;
    }

    public void setOpenforumflags(String openforumflags) {
        this.openforumflags = openforumflags;
    }

    public String getCiscouid() {
        return ciscouid;
    }

    public void setCiscouid(String ciscouid) {
        this.ciscouid = ciscouid;
    }

    public String getEmployeenumber() {
        return employeenumber;
    }

    public void setEmployeenumber(String employeenumber) {
        this.employeenumber = employeenumber;
    }

    public String getSoftwaredownload() {
        return softwaredownload;
    }

    public void setSoftwaredownload(String softwaredownload) {
        this.softwaredownload = softwaredownload;
    }

    public String getPicaadminprefix() {
        return picaadminprefix;
    }

    public void setPicaadminprefix(String picaadminprefix) {
        this.picaadminprefix = picaadminprefix;
    }

    public String getUserexpire() {
        return userexpire;
    }

    public void setUserexpire(String userexpire) {
        this.userexpire = userexpire;
    }

    public String getWebsurvey() {
        return websurvey;
    }

    public void setWebsurvey(String websurvey) {
        this.websurvey = websurvey;
    }
}
