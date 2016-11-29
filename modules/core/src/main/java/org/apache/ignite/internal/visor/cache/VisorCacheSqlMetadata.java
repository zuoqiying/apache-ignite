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

package org.apache.ignite.internal.visor.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.ignite.internal.processors.cache.query.GridCacheSqlIndexMetadata;
import org.apache.ignite.internal.processors.cache.query.GridCacheSqlMetadata;

/**
 * Data transfer object for cache SQL metadata.
 */
public class VisorCacheSqlMetadata implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private String cacheName;

    /** */
    private Collection<String> types;

    /** */
    private Map<String, String> keyClasses;

    /** */
    private Map<String, String> valClasses;

    /** */
    private Map<String, Map<String, String>> fields;

    /** */
    private Map<String, Collection<VisorCacheSqlIndexMetadata>> indexes;

    /**
     * Create data transfer object.
     *
     * @param meta Cache SQL metadata.
     */
    public VisorCacheSqlMetadata(GridCacheSqlMetadata meta) {
        cacheName = meta.cacheName();
        types = meta.types();
        keyClasses = meta.keyClasses();
        valClasses = meta.valClasses();
        fields = meta.fields();
        indexes = new HashMap<>();

        Map<String, Collection<GridCacheSqlIndexMetadata>> src = meta.indexes();

        if (src != null) {
            for (Map.Entry<String, Collection<GridCacheSqlIndexMetadata>> entry: src.entrySet()) {
                Collection<GridCacheSqlIndexMetadata> idxs = entry.getValue();

                Collection<VisorCacheSqlIndexMetadata> res = new ArrayList<>(idxs.size());

                for (GridCacheSqlIndexMetadata idx : idxs)
                    res.add(new VisorCacheSqlIndexMetadata(idx));

                indexes.put(entry.getKey(), res);
            }
        }
    }

    /**
     * @return Cache name.
     */
    public String getCacheName() {
        return cacheName;
    }

    /**
     * @return Collection of available types.
     */
    public Collection<String> getTypes() {
        return types;
    }

    /**
     * @return Key classes.
     */
    public Map<String, String> getKeyClasses() {
        return keyClasses;
    }

    /**
     * @return Value classes.
     */
    public Map<String, String> getValueClasses() {
        return valClasses;
    }

    /**
     * @return Fields.
     */
    public Map<String, Map<String, String>> getFields() {
        return fields;
    }

    /**
     * @return Indexes.
     */
    public Map<String, Collection<VisorCacheSqlIndexMetadata>> getIndexes() {
        return indexes;
    }
}
