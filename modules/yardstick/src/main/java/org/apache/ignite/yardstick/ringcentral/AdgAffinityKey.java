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

/**
 *
 */
public class AdgAffinityKey implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private String key;

    /** */
    private String extId;

    /**
     * @param key Key.
     * @param extId Affinity ID.
     */
    public AdgAffinityKey(String key, String extId) {
        this.key = key;
        this.extId = extId;
    }

    /**
     * @return Key.
     */
    public String getKey() {
        return key;
    }

    /**
     * @return Ext ID. Affinity key.
     */
    public String getExtId() {
        return extId;
    }
}


//    select extensionId,
//    min(extensionStatus),
//    max(concat(firstName,' ',lastName)),
//    min(phoneNumber),
//    max(extensionNumber),
//    from \"AdgEntry\".ADGENTITY
//    where accountId = ?
//    and extensionType in (?,?)
//    and extensionStatus in (?,?)
//    and deleteTime > ?
//    and ((lcase(concat(firstName,' ',lastName)) like '%john%')
//    OR (lcase(phoneNumber) like '%john%')
//    OR (lcase(extensionNumber) like '%john%'))
//    group by extensionId  order by 2 asc, 3 desc, 4 asc, 5 desc limit 100 offset 0