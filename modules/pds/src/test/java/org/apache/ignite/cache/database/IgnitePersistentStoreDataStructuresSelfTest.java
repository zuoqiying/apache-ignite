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
 *
 */

package org.apache.ignite.cache.database;

import org.apache.ignite.IgniteAtomicSequence;
import org.apache.ignite.IgniteLock;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.processors.database.IgniteDbAbstractTest;

public class IgnitePersistentStoreDataStructuresSelfTest extends IgniteDbAbstractTest {

    @Override protected int gridCount() {
        return 4;
    }

    @Override protected boolean indexingEnabled() {
        return false;
    }

    public void testLockNotPersisted() throws Exception {
        IgniteEx ignite = grid(0);

        IgniteLock lock = ignite.reentrantLock("test", true, true, true);

        assert lock != null;

        restartGrids();

        ignite = grid(0);

        lock = ignite.reentrantLock("test", true, true, false);

        assert lock == null;
    }

    public void testAtomicSequencePersisted() throws Exception {
        IgniteEx ignite = grid(0);

        IgniteAtomicSequence seq = ignite.atomicSequence("test", 0, true);

        assert seq != null;

        long val = seq.addAndGet(100);

        assert val == 100;

        restartGrids();

        ignite = grid(0);

        seq = ignite.atomicSequence("test", 0, false);

        assert seq != null;

        assert seq.get() == Math.max(val, seq.batchSize()) : seq.get();
    }

}
