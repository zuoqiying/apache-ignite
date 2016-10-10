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
import org.apache.ignite.internal.trace.atomic.data.AtomicTraceDataMessageKey;
import org.apache.ignite.internal.trace.atomic.data.AtomicTraceDataReceiveIo;
import org.apache.ignite.internal.trace.atomic.data.AtomicTraceDataServer;
import org.apache.ignite.internal.trace.atomic.data.AtomicTraceDataUser;
import org.apache.ignite.internal.trace.atomic.data.AtomicTraceDataSendIo;
import org.apache.ignite.internal.trace.atomic.data.AtomicTraceDataUserPart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        // Prepare IO send map.
        List<TraceThreadResult> threadRess = data.groupData(AtomicTrace.GRP_IO_SND);

        Map<UUID, Map<Long, AtomicTraceDataSendIo>> sndIoMap = new HashMap<>();

        for (TraceThreadResult threadRes : threadRess) {
            Map<Long, AtomicTraceDataSendIo> nodeMap = sndIoMap.get(threadRes.nodeId());

            if (nodeMap == null) {
                nodeMap = new HashMap<>();

                sndIoMap.put(threadRes.nodeId(), nodeMap);
            }

            List<Map<Long, AtomicTraceDataSendIo>> sndIoDatas = threadRes.data();

            for (Map<Long, AtomicTraceDataSendIo> sndIoData : sndIoDatas)
                nodeMap.putAll(sndIoData);
        }

        // Prepare receive IO map.
        threadRess = data.groupData(AtomicTrace.GRP_IO_RCV);

        Map<AtomicTraceDataMessageKey, AtomicTraceDataReceiveIo> rcvIoMap = new HashMap<>();

        for (TraceThreadResult threadRes : threadRess) {
            List<Map<AtomicTraceDataMessageKey, AtomicTraceDataReceiveIo>> rcvIoDatas = threadRes.data();

            for (Map<AtomicTraceDataMessageKey, AtomicTraceDataReceiveIo> rcvIoData : rcvIoDatas)
            rcvIoMap.putAll(rcvIoData);
        }

        // Prepare server data.
        threadRess = data.groupData(AtomicTrace.GRP_SRV);

        Map<AtomicTraceDataMessageKey, AtomicTraceDataServer> srvMap = new HashMap<>();

        for (TraceThreadResult threadRes : threadRess) {
            List<AtomicTraceDataServer> srvDatas = threadRes.data();

            for (AtomicTraceDataServer srvData : srvDatas) {
                AtomicTraceDataMessageKey key =
                    new AtomicTraceDataMessageKey(srvData.fromNode, srvData.toNode, srvData.reqId);

                srvMap.put(key, srvData);
            }
        }

        // Prepare client data.
        threadRess = data.groupData(AtomicTrace.GRP_CLI);

        Map<UUID, Map<Long, AtomicTraceDataClient>> cliMap = new HashMap<>();

        for (TraceThreadResult threadRes : threadRess) {
            List<AtomicTraceDataClient> cliDatas = threadRes.data();

            for (AtomicTraceDataClient cliData : cliDatas) {
                Long reqId = cliData.reqId;

                Map<Long, AtomicTraceDataClient> cliNodeMap = cliMap.get(threadRes.nodeId());

                if (cliNodeMap == null) {
                    cliNodeMap = new HashMap<>();

                    cliMap.put(threadRes.nodeId(), cliNodeMap);
                }

                cliNodeMap.put(reqId, cliData);
            }
        }

        // Perform assembly.
        List<AtomicTraceResult> ress = new ArrayList<>();

        threadRess = data.groupData(AtomicTrace.GRP_USR);

        for (TraceThreadResult threadRes : threadRess) {
            List<AtomicTraceDataUser> usrs = threadRes.data();

            for (AtomicTraceDataUser usr : usrs) {
                AtomicTraceResult res = new AtomicTraceResult(usr);

                for (AtomicTraceDataUserPart usrPart : usr.reqs) {
                    long reqId = usrPart.key.msgId;

                    if (sndIoMap.containsKey(threadRes.nodeId())) {
                        AtomicTraceDataSendIo cliSnd = sndIoMap.get(threadRes.nodeId()).get(reqId);

                        if (cliSnd != null) {
                            AtomicTraceDataReceiveIo srvRcv = rcvIoMap.get(usrPart.key);
                            AtomicTraceDataServer srv = srvMap.get(usrPart.key);

                            if (srvRcv != null && srv != null) {
                                if (sndIoMap.containsKey(srv.toNode)) {
                                    AtomicTraceDataSendIo srvSnd = sndIoMap.get(srv.toNode).get(srv.respId);

                                    if (srvSnd != null) {
                                        AtomicTraceDataMessageKey reverseKey =
                                            new AtomicTraceDataMessageKey(srv.toNode, srv.fromNode, srv.respId);

                                        AtomicTraceDataReceiveIo cliRcv = rcvIoMap.get(reverseKey);

                                        if (cliRcv != null && cliMap.containsKey(threadRes.nodeId())) {
                                            AtomicTraceDataClient cli = cliMap.get(threadRes.nodeId()).get(reqId);

                                            if (cli != null)
                                                res.addPart(usrPart, cliSnd, srvRcv, srv, srvSnd, cliRcv, cli);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (res.parts.size() != 0)
                    ress.add(res);
            }
        }

        return ress;
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
     * @param usrPart User part.
     * @param cliSnd Client send.
     * @param srvRcv Server receive.
     * @param srv Server.
     * @param srvSnd Server send.
     * @param cliRcv Client receive.
     * @param cli Client finish.
     */
    private void addPart(AtomicTraceDataUserPart usrPart, AtomicTraceDataSendIo cliSnd, AtomicTraceDataReceiveIo srvRcv,
        AtomicTraceDataServer srv, AtomicTraceDataSendIo srvSnd, AtomicTraceDataReceiveIo cliRcv,
        AtomicTraceDataClient cli) {
        parts.add(new Part(usrPart, cliSnd, srvRcv, srv, srvSnd, cliRcv, cli));
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        StringBuilder res = new StringBuilder("usrStart=" + usr.started + "\n");

        boolean first = true;

        for (AtomicTraceResult.Part p : parts) {
            if (first)
                first = false;
            else
                res.append("\n");

            String ps = String.format(
                "\tuser=  [%8d]\n " +
                "\tcliSnd=[%8d %8d %8d]\n" +
                "\tsrvRcv=[%8d %8d %8d]\n" +
                "\tserver=[%8d %8d]\n" +
                "\tsrvSnd=[%8d %8d %8d]\n" +
                "\tcliRcv=[%8d %8d %8d]\n " +
                "\tclient=[%8d %8d %8d]",

                p.usrPart.offered - usr.started,

                p.cliSnd.polled - p.usrPart.offered,
                p.cliSnd.marshalled - p.cliSnd.polled,
                p.cliSnd.sent - p.cliSnd.marshalled,

                p.srvRcv.read - p.cliSnd.sent,
                p.srvRcv.unmarshalled - p.srvRcv.read,
                p.srvRcv.offered - p.srvRcv.unmarshalled,

                p.srv.started - p.srvRcv.offered,
                p.srv.offered - p.srv.started,

                p.srvSnd.polled - p.srv.offered,
                p.srvSnd.marshalled - p.srvSnd.polled,
                p.srvSnd.sent - p.srvSnd.marshalled,

                p.cliRcv.read - p.srvSnd.sent,
                p.cliRcv.unmarshalled - p.cliRcv.read,
                p.cliRcv.offered - p.cliRcv.unmarshalled,

                p.cli.started - p.cliRcv.offered,
                p.cli.completed - p.cli.started,
                p.cli.finished - p.cli.completed
            );

            res.append(ps);
        }

        return res.toString();
    }

    /**
     * Trace part.
     */
    public static class Part {
        /** User part. */
        public AtomicTraceDataUserPart usrPart;

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
         * @param usrPart User part.
         * @param cliSnd Client send.
         * @param srvRcv Server receive.
         * @param srv Server.
         * @param srvSnd Server send.
         * @param cliRcv Client receive.
         * @param cli Client finish.
         */
        public Part(AtomicTraceDataUserPart usrPart, AtomicTraceDataSendIo cliSnd, AtomicTraceDataReceiveIo srvRcv,
            AtomicTraceDataServer srv, AtomicTraceDataSendIo srvSnd, AtomicTraceDataReceiveIo cliRcv,
            AtomicTraceDataClient cli) {
            this.usrPart = usrPart;
            this.cliSnd = cliSnd;
            this.srvRcv = srvRcv;
            this.srv = srv;
            this.srvSnd = srvSnd;
            this.cliRcv = cliRcv;
            this.cli = cli;
        }
    }
}
