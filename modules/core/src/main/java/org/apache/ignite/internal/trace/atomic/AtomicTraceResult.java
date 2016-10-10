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
import org.apache.ignite.internal.trace.atomic.data.AtomicTraceDataClient;
import org.apache.ignite.internal.trace.atomic.data.AtomicTraceDataReceiveIo;
import org.apache.ignite.internal.trace.atomic.data.AtomicTraceDataServer;
import org.apache.ignite.internal.trace.atomic.data.AtomicTraceDataUser;
import org.apache.ignite.internal.trace.atomic.data.AtomicTraceDataSendIo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Final trace result.
 */
public class AtomicTraceResult {
    /** Client send. */
    public AtomicTraceDataUser usr;

    /** Client. */
    public Collection<Part> parts = new ArrayList<>(1);

    /**
     * Parse trace node results and produce final results.
     *
     * @param data Trace data.
     * @return Final result.
     */
    @SuppressWarnings("unchecked")
    public static List<AtomicTraceResult> parse(TraceData data) {
        List<AtomicTraceResult> res = new ArrayList<>();

        List<TraceThreadResult> threadSndIos = data.groupData(AtomicTrace.GRP_IO_SND);

        for (TraceThreadResult threadSnd : data.groupData(AtomicTrace.GRP_USR)) {
            List<AtomicTraceDataUser> snds = threadSnd.data();

            for (AtomicTraceDataUser snd : snds) {
                AtomicTraceDataSendIo sndIo = findSendIo(threadSndIos, threadSnd, snd);

                if (sndIo != null)
                    res.add(new AtomicTraceResult(snd, sndIo, threadSnd.threadId()));
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
    private static AtomicTraceDataSendIo findSendIo(List<TraceThreadResult> threadSndIos, TraceThreadResult threadSnd,
        AtomicTraceDataUser snd) {
        for (TraceThreadResult threadSndIo : threadSndIos) {
            if (threadSnd.sameNode(threadSndIo)) {
                List<Map<Long, AtomicTraceDataSendIo>> datas = threadSndIo.data();

                for (Map<Long, AtomicTraceDataSendIo> data : datas) {
                    AtomicTraceDataSendIo res = data.get(snd.reqId);

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
     * @param usr User part.
     */
    public AtomicTraceResult(AtomicTraceDataUser usr) {
        this.usr = usr;
    }

    /**
     * Add part.
     *
     * @param cliSnd Client send.
     * @param srvRcv Server receive.
     * @param srv Server.
     * @param srvSnd Server send.
     * @param cliRcv Client receive.
     * @param cli Client finish.
     */
    private void addPart(AtomicTraceDataSendIo cliSnd, AtomicTraceDataReceiveIo srvRcv, AtomicTraceDataServer srv,
        AtomicTraceDataSendIo srvSnd, AtomicTraceDataReceiveIo cliRcv, AtomicTraceDataClient cli) {
        parts.add(new Part(cliSnd, srvRcv, srv, srvSnd, cliRcv, cli));
    }

    /** {@inheritDoc} */
    @Override public String toString() {
//        return String.format(AtomicTraceResult.class.getSimpleName() +
//            "[start=%d, map=%8d, offer=%8d, poll=%8d, marsh=%8d, send=%8d, bufLen=%5d, msgCnt=%3d, nio=" +
//                threadId + "]",
//            clientStart(),
//            clientMapDuration(),
//            clientOfferDuration(),
//            clientIoPollDuration(),
//            clientIoMarshalDuration(),
//            clientIoSendDuration(),
//            cliSendIo.bufLen,
//            cliSendIo.msgCnt
//        );
        return "";
    }

    /**
     * Trace part.
     */
    public static class Part {
        /** Client send. */
        public AtomicTraceDataSendIo cliSnd;

        /** Server receive. */
        public AtomicTraceDataReceiveIo srvRcv;

        /** Server. */
        public AtomicTraceDataServer srv;

        /** Server send. */
        public AtomicTraceDataSendIo srvSnd;

        /** Client receive. */
        public AtomicTraceDataReceiveIo cliRcv;

        /** Client finish. */
        public AtomicTraceDataClient cli;

        /**
         * Constructor.
         *
         * @param cliSnd Client send.
         * @param srvRcv Server receive.
         * @param srv Server.
         * @param srvSnd Server send.
         * @param cliRcv Client receive.
         * @param cli Client finish.
         */
        public Part(AtomicTraceDataSendIo cliSnd, AtomicTraceDataReceiveIo srvRcv, AtomicTraceDataServer srv,
            AtomicTraceDataSendIo srvSnd, AtomicTraceDataReceiveIo cliRcv, AtomicTraceDataClient cli) {
            this.cliSnd = cliSnd;
            this.srvRcv = srvRcv;
            this.srv = srv;
            this.srvSnd = srvSnd;
            this.cliRcv = cliRcv;
            this.cli = cli;
        }
    }
}
