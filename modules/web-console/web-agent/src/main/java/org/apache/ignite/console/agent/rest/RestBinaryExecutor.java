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

package org.apache.ignite.console.agent.rest;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.ignite.console.demo.AgentClusterDemo;
import org.apache.ignite.internal.client.GridClient;
import org.apache.ignite.internal.client.GridClientCacheMode;
import org.apache.ignite.internal.client.GridClientConfiguration;
import org.apache.ignite.internal.client.GridClientException;
import org.apache.ignite.internal.client.GridClientFactory;
import org.apache.ignite.internal.client.GridClientNode;
import org.apache.ignite.internal.processors.rest.GridRestResponse;
import org.apache.ignite.internal.processors.rest.client.message.GridClientTaskResultBean;
import org.apache.ignite.internal.processors.rest.protocols.http.jetty.GridJettyObjectMapper;
import org.apache.log4j.Logger;

/**
 * REST executor via GridClient.
 */
public class RestBinaryExecutor implements RestExecutor {
    /** */
    private static final Logger log = Logger.getLogger(RestBinaryExecutor.class);

    /** JSON object mapper. */
    private static final ObjectMapper mapper = new GridJettyObjectMapper();

    /** */
    private static final Pattern IS_PARAM = Pattern.compile("p[0-9]+");

    /** Node URL. */
    private String nodeUrl;

    /** */
    private GridClient clientCluster;

    /** */
    private GridClient clientDemo;

    /**
     * Default constructor.
     */
    public RestBinaryExecutor(String nodeUrl) {
        this.nodeUrl = nodeUrl;
    }

    /** {@inheritDoc} */
    @Override public void stop() {
        if (clientCluster != null)
            clientCluster.close();

        if (clientDemo != null)
            clientDemo.close();
    }

    /** */
    private GridClient client(boolean demo) throws GridClientException {
        GridClient client = demo ? clientDemo : clientCluster;

        if (client == null) {
            GridClientConfiguration clnCfg = new GridClientConfiguration();

            String srv = demo ? "localhost:60700" : nodeUrl.substring("binary://".length());

            clnCfg.setServers(Collections.singletonList(srv));

            client = GridClientFactory.start(clnCfg);

            if (demo)
                clientDemo = client;
            else
                clientCluster = client;
        }

        return client;
    }

    /** */
    private RestResult sendRequest(boolean demo, Map<String, Object> params) throws IOException {
        if (demo && AgentClusterDemo.getDemoUrl() == null) {
            try {
                AgentClusterDemo.tryStart().await();
            }
            catch (InterruptedException ignore) {
                throw new IllegalStateException("Failed to execute request because of embedded node for demo mode is not started yet.");
            }
        }

        try {
            GridClient client = client(demo);

            String cmd = params.get("cmd").toString();

            switch (cmd) {
                case "top":
                    List<GridClientNode> nodes = client.compute().refreshTopology(
                            Boolean.parseBoolean(params.get("attr").toString()),
                            Boolean.parseBoolean(params.get("mtr").toString()));

                    ArrayNode jsonRes = mapper.createArrayNode();

                    for(GridClientNode node : nodes) {
                        ObjectNode jsonNode = jsonRes.addObject();

                        jsonNode.put("nodeId", node.nodeId().toString());
                        ArrayNode arr = jsonNode.putArray("tcpAddresses");
                        for (String adr : node.tcpAddresses())
                            arr.add(adr);

                        ObjectNode map = jsonNode.putObject("attributes");
                        for (Map.Entry<String, Object> entry : node.attributes().entrySet())
                            map.put(entry.getKey(), String.valueOf(entry.getValue()));

                        jsonNode.set("metrics", mapper.valueToTree(node.metrics()));

                        ArrayNode caches = jsonNode.putArray("caches");
                        for (Map.Entry<String, GridClientCacheMode> entry : node.caches().entrySet()) {
                            ObjectNode cache = caches.addObject();
                            cache.put("name", entry.getKey());
                            cache.put("mode", entry.getValue().toString());
                        }
                    }

                    return RestResult.success(jsonRes.toString());

                case "exe":
                    Map<Integer, Object> args = new TreeMap<>();

                    for (Map.Entry<String, Object> param : params.entrySet()) {
                        if (IS_PARAM.matcher(param.getKey()).matches())
                            args.put(Integer.valueOf(param.getKey().substring(1)), param.getValue().toString());
                    }

                    Object taskRes = client.compute()
                        .execute("org.apache.ignite.internal.visor.compute.VisorGatewayTask", args.values().toArray());

                    GridClientTaskResultBean bean = new GridClientTaskResultBean();
                    bean.setFinished(true);
                    bean.setResult(taskRes);

                    return RestResult.success(mapper.writeValueAsString(bean));

                case "metadata":
                    GridRestResponse res = client.compute()
                        .execute("org.apache.ignite.internal.processors.rest.handlers.cache.GridCacheCommandHandler$MetadataTask", null);

                    return RestResult.success(mapper.writeValueAsString(res.getResponse()));
            }

            return RestResult.fail(404, "Unknown command: " + cmd);
        }
        catch (GridClientException e) {
            throw new IOException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override public RestResult execute(boolean demo, String path, Map<String, Object> params, String mtd,
        Map<String, Object> headers, String body) {
        log.debug("Start execute REST command [method=" + mtd + ", uri=/" + (path == null ? "" : path) +
                ", parameters=" + params + "]");

        try {
            return sendRequest(demo, params);
        }
        catch (Exception e) {
            log.info("Failed to execute REST command [method=" + mtd + ", uri=/" + (path == null ? "" : path) +
                ", parameters=" + params + "]", e);

            return RestResult.fail(404, e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override public RestResult topology(boolean demo, boolean full) throws IOException {
        Map<String, Object> params = new HashMap<>(3);

        params.put("cmd", "top");
        params.put("attr", true);
        params.put("mtr", full);

        return sendRequest(demo, params);
    }
}
