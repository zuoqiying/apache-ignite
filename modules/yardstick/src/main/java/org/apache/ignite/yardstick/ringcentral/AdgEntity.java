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

package org.apache.ignite.yardstick.ringcentral;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.ignite.cache.query.annotations.QuerySqlField;

/**
 *
 */
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class AdgEntity implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    public static final String ACC_ID = "accId_";

    @QuerySqlField(index = true, orderedGroups = {
        @QuerySqlField.Group(name = "extensionAccount", order = 0)})
    private String extensionId;

    @QuerySqlField(index = true, orderedGroups = {
        @QuerySqlField.Group(name = "accountTypeStatus", order = 1)})
    private int extensionType;

    @QuerySqlField(index = true, orderedGroups = {
        @QuerySqlField.Group(name = "accountTypeStatus", order = 2)})
    private int extensionStatus;

    @QuerySqlField(index = true, orderedGroups = {
        @QuerySqlField.Group(name = "accountPin", order = 1)})
    private String extensionNumber;

    @QuerySqlField
    private String firstName;
    @QuerySqlField
    private String lastName;
    @QuerySqlField
    private String department;
    private String businessPhone;
    private String email;
    private String extensionCountryId;
    private String localeId;

    @QuerySqlField(index = true)
    private String phoneNumberId;
    @QuerySqlField(index = true, orderedGroups = {
        @QuerySqlField.Group(name = "phoneNumberAccount", order = 0)})
    private String phoneNumber;
    @QuerySqlField
    private String city;
    @QuerySqlField
    private String stateShortName;
    @QuerySqlField
    private String usageType;
    private String paymentType;
    @QuerySqlField
    private String phoneLineType;
    @QuerySqlField
    private String areaCode;
    @QuerySqlField
    private String countryId;
    @QuerySqlField
    private String isoA2Code;
    @QuerySqlField
    private String countryName;

    @QuerySqlField(index = true)
    private String deviceId;
    @QuerySqlField
    private String deviceType;
    @QuerySqlField
    private String deviceName;
    @QuerySqlField
    private String serial;
    @QuerySqlField
    private Boolean commonPhone;
    @QuerySqlField
    private String computerName;
    @QuerySqlField
    private String modelId;
    @QuerySqlField
    private String modelName;
    private String deviceStatus;
    private String shippingStatus;

    @QuerySqlField(index = true)
    private String roleId;
    @QuerySqlField
    private String roleDisplayName;

    @QuerySqlField(index = true, orderedGroups = {
        @QuerySqlField.Group(name = "accountTypeStatus", order = 0),
        @QuerySqlField.Group(name = "extensionAccount", order = 1),
        @QuerySqlField.Group(name = "accountPin", order = 0),
        @QuerySqlField.Group(name = "phoneNumberAccount", order = 1),
    })
    private String accountId;
    private String brandId;
    private String companyNumber;

    @QuerySqlField
    private long deleteTime = Long.MAX_VALUE;

    /**
     * Custom cache key to guarantee that other caches are always collocated with its extensionId.
     *
     * @return Custom affinity key.
     */
    public AdgAffinityKey getKey(Integer key) {
        //looks like now we don't need affinity by accountId if device or phone number unassigned
        return new AdgAffinityKey(String.valueOf(key), extensionId != null ? extensionId : null);
    }

    public void setExtensionId(String extensionId) {
        this.extensionId = extensionId;
    }

    public void setExtensionType(int extensionType) {
        this.extensionType = extensionType;
    }

    public void setExtensionStatus(int extensionStatus) {
        this.extensionStatus = extensionStatus;
    }

    public void setExtensionNumber(String extensionNumber) {
        this.extensionNumber = extensionNumber;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setBusinessPhone(String businessPhone) {
        this.businessPhone = businessPhone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setExtensionCountryId(String extensionCountryId) {
        this.extensionCountryId = extensionCountryId;
    }

    public void setLocaleId(String localeId) {
        this.localeId = localeId;
    }

    public void setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setStateShortName(String stateShortName) {
        this.stateShortName = stateShortName;
    }

    public void setUsageType(String usageType) {
        this.usageType = usageType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public void setPhoneLineType(String phoneLineType) {
        this.phoneLineType = phoneLineType;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public void setCountryId(String countryId) {
        this.countryId = countryId;
    }

    public void setIsoA2Code(String isoA2Code) {
        this.isoA2Code = isoA2Code;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public void setCommonPhone(Boolean commonPhone) {
        this.commonPhone = commonPhone;
    }

    public void setComputerName(String computerName) {
        this.computerName = computerName;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public void setDeviceStatus(String deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public void setShippingStatus(String shippingStatus) {
        this.shippingStatus = shippingStatus;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public void setRoleDisplayName(String roleDisplayName) {
        this.roleDisplayName = roleDisplayName;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setBrandId(String brandId) {
        this.brandId = brandId;
    }

    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
    }

    public void setDeleteTime(long deleteTime) {
        this.deleteTime = deleteTime;
    }

    public enum ExtensionType {
        // Invalid (not initialized) value
        Unknown(0),
        // User extension
        User(1),
        // Department extension
        Department(2),
        // Announcement only extension
        Announcement(3),
        // Announcement only extension
        Voicemail(4),

        // For new User-Based Pricing services:

        DigitalUser(5),
        VirtualUser(6),
        FaxUser(7),

        PagingOnly(8),
        SharedLinesGroup(9),

        IvrMenu(10),
        ApplicationExtension(11),

        //call park orbits
        ParkLocation(12);

        private final int value;

        ExtensionType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static int randomValue() {
            int rnd = ThreadLocalRandom.current().nextInt(0, 4);

            switch (rnd) {
                case 0:
                    return SharedLinesGroup.value();

                case 1:
                    return DigitalUser.value();

                case 2:
                    return VirtualUser.value();

                case 3:
                    return User.value();

                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    public enum ExtensionState {
        Enabled(0),
        Disabled(1),
        Frozen(2),
        NotActivated(3),
        Unassigned(4),
        Unknown(Integer.MAX_VALUE);

        private final int value;

        ExtensionState(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static int randomValue() {
            int rnd = ThreadLocalRandom.current().nextInt(0, 3);

            switch (rnd) {
                case 0:
                    return Enabled.value();

                case 1:
                    return Disabled.value();

                case 2:
                    return Unassigned.value();

                default:
                    throw new IllegalArgumentException();
            }
        }
    }
}
