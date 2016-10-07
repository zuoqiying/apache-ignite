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

package org.apache.ignite.internal.trace.atomic;

import org.apache.ignite.internal.trace.TraceData;
import org.apache.ignite.internal.trace.TraceThreadResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Final trace result.
 */
public class AtomicTraceResult {
    /** Client send. */
    public AtomicTraceClientSend cliSend;

    /** Client send IO. */
    public AtomicTraceClientSendIo cliSendIo;

    /**
     * @return Client start duration.
     */
    public long clientStart() {
        return cliSend.started;
    }

    /**
     * @return Client map duration.
     */
    public long clientMapDuration() {
        return cliSend.mapped - cliSend.started;
    }

    /**
     * @return Client offer duration.
     */
    public long clientOfferDuration() {
        return cliSend.offered - cliSend.mapped;
    }

    /**
     * @return Client IO poll duration.
     */
    public long clientIoPollDuration() {
        return cliSendIo.started - cliSend.offered;
    }

    /**
     * @return Client IO marshal duration.
     */
    public long clientIoMarshalDuration() {
        return cliSendIo.marshalled - cliSendIo.started;
    }

    /**
     * @return Client IO send duration.
     */
    public long clientIoSendDuration() {
        return cliSendIo.sent - cliSendIo.marshalled;
    }

    /**
     * Parse trace node results and produce final results.
     *
     * @param data Trace data.
     * @return Final result.
     */
    @SuppressWarnings("unchecked")
    public static Collection<AtomicTraceResult> parse(TraceData data) {
        Collection<AtomicTraceResult> res = new ArrayList<>();

        List<TraceThreadResult> threadSndIos = data.groupData(AtomicTrace.GRP_CLIENT_REQ_SND_IO);

        for (TraceThreadResult threadSnd : data.groupData(AtomicTrace.GRP_CLIENT_REQ_SND)) {
            List<AtomicTraceClientSend> snds = threadSnd.data();

            for (AtomicTraceClientSend snd : snds) {
                AtomicTraceClientSendIo sndIo = findSendIo(threadSndIos, threadSnd, snd);

                if (sndIo != null)
                    res.add(new AtomicTraceResult(snd, sndIo));
            }
        }

        return res;
    }

    /**
     * Find send IO.
     *
     * @param threadSndIos All thread send IOs.
     * @param threadSnd Thread send.
     * @param snd Send.
     * @return Send IO.
     */
    private static AtomicTraceClientSendIo findSendIo(List<TraceThreadResult> threadSndIos, TraceThreadResult threadSnd,
        AtomicTraceClientSend snd) {
        for (TraceThreadResult threadSndIo : threadSndIos) {
            if (threadSnd.sameNode(threadSndIo)) {
                List<Map<Integer, AtomicTraceClientSendIo>> datas = threadSndIo.data();

                for (Map<Integer, AtomicTraceClientSendIo> data : datas) {
                    AtomicTraceClientSendIo res = data.get(snd.reqHash);

                    if (res != null && res.started >= snd.offered)
                        return res;
                }
            }
        }

        return null;
    }

    /**
     * Constructor.
     *
     * @param cliSend Client send.
     * @param cliSendIo Client send IO.
     */
    public AtomicTraceResult(AtomicTraceClientSend cliSend, AtomicTraceClientSendIo cliSendIo) {
        this.cliSend = cliSend;
        this.cliSendIo = cliSendIo;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return String.format(AtomicTraceResult.class.getSimpleName() +
            "[start=%d, map=%8d, offer=%8d, poll=%8d, marsh=%8d, send=%8d]",
            clientStart(),
            clientMapDuration(),
            clientOfferDuration(),
            clientIoPollDuration(),
            clientIoMarshalDuration(),
            clientIoSendDuration()
        );
    }
}
