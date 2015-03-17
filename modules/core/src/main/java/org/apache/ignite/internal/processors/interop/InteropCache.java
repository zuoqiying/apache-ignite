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

package org.apache.ignite.internal.processors.interop;

import org.apache.ignite.*;
import org.apache.ignite.internal.processors.cache.*;
import org.apache.ignite.interop.*;

/**
 *
 */
public class InteropCache extends InteropTargetAdapter {
    /** */
    private GridCacheProjectionEx cache;

    /**
     * @param proc Processor.
     * @param cache Cache.
     */
    public InteropCache(InteropProcessor proc, GridCacheProjectionEx cache) {
        super(proc);

        this.cache = cache;
    }

    /** {@inheritDoc} */
    @Override protected int inOp(int type, InteropInputStream in, InteropMarshaller marsh)
        throws IgniteCheckedException {
        Object key = marsh.readObject(in);

        Object val = marsh.readObject(in);

        log.info("Interop put [key=" + key + ", val=" + val + ']');

        cache.put(key, val);

        return 0;
    }

    /** {@inheritDoc} */
    @Override protected void inOutOp(int type,
        InteropInputStream in,
        InteropOutputStream out,
        InteropMarshaller marsh)
        throws IgniteCheckedException
    {
        Object key = marsh.readObject(in);

        Object val = cache.get(key);

        log.info("Interop get [key=" + key + ", val=" + val + ']');

        marsh.writeObject(val, out);
    }
}
