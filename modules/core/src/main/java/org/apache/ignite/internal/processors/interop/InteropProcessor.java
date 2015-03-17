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
import org.apache.ignite.internal.*;
import org.apache.ignite.internal.processors.*;
import org.apache.ignite.internal.processors.cache.*;
import org.apache.ignite.interop.*;
import org.jetbrains.annotations.*;

/**
 *
 */
public class InteropProcessor extends GridProcessorAdapter {
    /**
     * @param ctx Context.
     */
    public InteropProcessor(GridKernalContext ctx) {
        super(ctx);
    }

    /** {@inheritDoc} */
    public InteropMarshaller marshaller() {
        return new TestInteropMarshaller();
    }

    /**
     * @return Context.
     */
    public GridKernalContext context() {
        return ctx;
    }

    /**
     * @param cacheName Cache name.
     * @return Interop cache.
     * @throws IgniteCheckedException If failed.
     */
    public InteropTarget cache(@Nullable String cacheName) throws IgniteCheckedException {
        GridCache cache = ctx.cache().cache(cacheName);

        if (cache == null)
            throw new IgniteCheckedException("Cache with the given name doesn't exist: " + cacheName);
        else
            return new InteropCache(ctx.interop(), (GridCacheProjectionEx)cache);
    }
}
