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

package org.apache.ignite.internal.processors.cache.integration;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import static org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL;
import org.apache.ignite.cache.store.CacheStore;
import org.apache.ignite.cache.store.CacheStoreSession;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteBiInClosure;
import org.apache.ignite.resources.CacheStoreSessionResource;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.jetbrains.annotations.Nullable;

import javax.cache.Cache;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriterException;
import java.util.Collection;
import java.util.Map;

/**
 * Integration test write behind cache store with {@link CacheConfiguration#getWriteBehindCoalescing()}={@code False}
 * parameter.
 */
public class IgniteCacheTxStoreSessionWriteBehindCoalescingTest extends IgniteCacheStoreSessionWriteBehindAbstractTest {
    /** {@inheritDoc} */
    @Override protected CacheAtomicityMode atomicityMode() {
        return TRANSACTIONAL;
    }


    /**
     * @param igniteInstanceName Ignite instance name.
     * @return Cache configuration.
     * @throws Exception In case of error.
     */
    @SuppressWarnings("unchecked")
    protected CacheConfiguration cacheConfiguration(String igniteInstanceName) throws Exception {
        CacheConfiguration ccfg = super.cacheConfiguration(igniteInstanceName);

        ccfg.setWriteBehindCoalescing(false);

        ccfg.setCacheStoreFactory(singletonFactory(new TestNonCoalescingStore()));

        return ccfg;
    }

    /**
     *
     */
    private class TestNonCoalescingStore extends TestStore {

        /** {@inheritDoc} */
        @Override public void writeAll(Collection<Cache.Entry<?, ?>> entries) throws CacheWriterException {
            log.info("writeAll: " + entries);

            assertTrue("Unexpected entries: " + entries, entries.size() <= 10);

            checkSession("writeAll");

            for (int i = 0; i < entries.size(); i++)
                entLatch.countDown();
        }

        /** {@inheritDoc} */
        @Override public void delete(Object key) throws CacheWriterException {
            fail();
        }

        /** {@inheritDoc} */
        @Override public void deleteAll(Collection<?> keys) throws CacheWriterException {
            log.info("deleteAll: " + keys);

            assertTrue("Unexpected keys: " + keys, keys.size() <= 10);

            checkSession("deleteAll");

            for (int i = 0; i < keys.size(); i++)
                entLatch.countDown();
        }
    }
}