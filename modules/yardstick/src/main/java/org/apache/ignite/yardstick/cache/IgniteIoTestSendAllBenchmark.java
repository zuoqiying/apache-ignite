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

package org.apache.ignite.yardstick.cache;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ignite.internal.managers.communication.IgniteIoTestMessage;
import org.apache.ignite.internal.util.typedef.F;
import org.yardstickframework.BenchmarkConfiguration;
import org.yardstickframework.BenchmarkUtils;

/**
 *
 */
public class IgniteIoTestSendAllBenchmark extends IgniteIoTestAbstractBenchmark {
    private final AtomicLong cntr = new AtomicLong();
    private final AtomicLong sndWrite = new AtomicLong();
    private final AtomicLong reqWriteRead = new AtomicLong();
    private final AtomicLong reqReadResSnd = new AtomicLong();
    private final AtomicLong resSndWrite = new AtomicLong();
    private final AtomicLong resWriteRead = new AtomicLong();
    private final AtomicLong resReadProc = new AtomicLong();

    @Override public void setUp(final BenchmarkConfiguration cfg) throws Exception {
        super.setUp(cfg);

        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                while (true) {
                    try {
                        Thread.sleep(5000);

                        double cntr0 = cntr.getAndSet(0);

                        double sndWriteSum = sndWrite.getAndSet(0) / cntr0;
                        double reqWriteReadSum = reqWriteRead.getAndSet(0) / cntr0;
                        double reqReadResSndSum = reqReadResSnd.getAndSet(0) / cntr0;
                        double resSndWriteSum = resSndWrite.getAndSet(0) / cntr0;
                        double resWriteReadSum = resWriteRead.getAndSet(0) / cntr0;
                        double resReadProcSum = resReadProc.getAndSet(0) / cntr0;

                        BenchmarkUtils.println(cfg,
                            String.format("Time [sndWrite=%f, reqWriteRead=%f, reqReadResSnd=%f, resSndWrite=%f, resWriteRead=%f, resReadProc=%f]",
                            sndWriteSum, reqWriteReadSum, reqReadResSndSum, resSndWriteSum, resWriteReadSum, resReadProcSum));
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> ctx) throws Exception {
        IgniteIoTestMessage msg0 = (IgniteIoTestMessage)ignite.sendIoTest(targetNodes, null, false).get();

        long sndWrite = msg0.reqWriteTime - msg0.reqSndTime;
        long reqWriteRead = msg0.reqReadTime - msg0.reqWriteTime;
        long reqReadResSnd = msg0.resSndTime - msg0.reqReadTime;
        long resSndWrite = msg0.resWriteTime - msg0.resSndTime;
        long resWriteRead = msg0.resReadTime - msg0.resWriteTime;
        long resReadProc = msg0.resTime - msg0.resReadTime;

        long sndWriteSum = this.sndWrite.addAndGet(sndWrite);
        long reqWriteReadSum = this.reqWriteRead.addAndGet(reqWriteRead);
        long reqReadResSndSum = this.reqReadResSnd.addAndGet(reqReadResSnd);
        long resSndWriteSum = this.resSndWrite.addAndGet(resSndWrite);
        long resWriteReadSum = this.resWriteRead.addAndGet(resWriteRead);
        long resReadProcSum = this.resReadProc.addAndGet(resReadProc);

        long cntr = this.cntr.incrementAndGet();

        return true;
    }
}
