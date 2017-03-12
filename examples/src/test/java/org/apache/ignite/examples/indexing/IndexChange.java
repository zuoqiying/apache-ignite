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

package org.apache.ignite.examples.indexing;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class IndexChange<K> {
    /** */
    private final K id;

    /** */
    private final String name;

    /** */
    private Map<String, String> changes = new HashMap<>();

    /**
     * @param name Name.
     * @param id   Id.
     */
    public IndexChange(String name, K id) {
        this.id = id;
        this.name = name;
    }

    /** */
    public K id() {
        return id;
    }

    /** */
    public String name() {
        return name;
    }

    /** */
    public void addChange(String name, String val) {
        changes.put(name, val);
    }

    /** */
    public Map<String, String> changes() {
        return changes;
    }
}
