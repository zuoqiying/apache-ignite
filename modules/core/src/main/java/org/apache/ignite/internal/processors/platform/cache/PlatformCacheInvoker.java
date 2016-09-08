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

package org.apache.ignite.internal.processors.platform.cache;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.internal.binary.BinaryRawReaderEx;
import org.apache.ignite.internal.processors.platform.websession.LockEntryProcessor;
import org.apache.ignite.internal.processors.platform.websession.SessionStateData;
import org.apache.ignite.internal.processors.platform.websession.SessionStateLockInfo;
import org.apache.ignite.internal.processors.platform.websession.SetAndUnlockEntryProcessor;
import org.apache.ignite.internal.processors.platform.websession.UnlockEntryProcessor;

/**
 * Custom entry processor invoker.
 */
public class PlatformCacheInvoker {
    /** */
    public static final int OP_SESSION_LOCK = 1;

    /** */
    public static final int OP_SESSION_UNLOCK = 2;

    /** */
    public static final int OP_SESSION_SET_AND_UNLOCK = 3;

    /**
     * Invokes the custom processor.
     *
     * @param reader Reader.
     * @param cache Cache.
     *
     * @return Result.
     */
    @SuppressWarnings("unchecked")
    public static Object invoke(BinaryRawReaderEx reader, IgniteCache cache) {
        int opCode = reader.readInt();

        String key = reader.readString();

        Object res = null;

        switch (opCode) {
            case OP_SESSION_LOCK: {
                SessionStateLockInfo lockInfo = reader.readObject();

                res = cache.invoke(key, new LockEntryProcessor(), lockInfo);

                break;
            }

            case OP_SESSION_UNLOCK: {
                SessionStateLockInfo lockInfo = reader.readObject();

                cache.invoke(key, new UnlockEntryProcessor(), lockInfo);

                break;
            }

            case OP_SESSION_SET_AND_UNLOCK:
                SessionStateData data = reader.readObject();

                cache.invoke(key, new SetAndUnlockEntryProcessor(), data);

                break;
        }
        return res;
    }
}
