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

package org.apache.ignite.internal.util.nio;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TODO
 */
public class DirectReaderQueue<T> {
    private final Queue<T> q = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean reserved = new AtomicBoolean();

    public void add(T t) {
        q.add(t);
    }

    public boolean reserved() {
        return reserved.get();
    }

    public void process() {
        for (;;) {
            if (reserved.get() || !reserved.compareAndSet(false, true))
                return;

            try {
                // TODO need to limit max chunks count processed at a time.
                for (T t = q.poll(); t != null; t = q.poll())
                    process0(t);
            }
            finally {
                reserved.set(false);
            }

            if (q.isEmpty())
                break;
        }
    }

    private void process0(T t) {

    }
}
