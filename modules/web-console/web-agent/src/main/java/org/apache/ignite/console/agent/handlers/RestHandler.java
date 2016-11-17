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

package org.apache.ignite.console.agent.handlers;

import java.util.Map;
import org.apache.ignite.console.agent.AgentConfiguration;
import org.apache.ignite.console.agent.RestExecutor;
import org.apache.ignite.console.agent.RestResult;
import org.apache.ignite.console.demo.AgentClusterDemo;

/**
 * API to translate REST requests to Ignite cluster.
 */
public class RestHandler extends AbstractHandler {
    /** */
    private final RestExecutor restExecutor;

    /**
     * @param restExecutor Config.
     */
    public RestHandler(RestExecutor restExecutor) {
        this.restExecutor = restExecutor;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public Object execute(Map<String, Object> args) throws Exception {
        if (log.isDebugEnabled())
            log.debug("Start parse REST command args: " + args);

        String path = null;

        if (args.containsKey("uri"))
            path = args.get("uri").toString();

        Map<String, Object> params = null;

        if (args.containsKey("params"))
            params = (Map<String, Object>)args.get("params");

        if (!args.containsKey("demo"))
            throw new IllegalArgumentException("Missing demo flag in arguments: " + args);

        boolean demo = (boolean)args.get("demo");

        if (!args.containsKey("method"))
            throw new IllegalArgumentException("Missing method in arguments: " + args);

        String mtd = args.get("method").toString();

        Map<String, Object> headers = null;

        if (args.containsKey("headers"))
            headers = (Map<String, Object>)args.get("headers");

        String body = null;

        if (args.containsKey("body"))
            body = args.get("body").toString();

        return restExecutor.execute(demo, path, params, mtd, headers, body);
    }
}
