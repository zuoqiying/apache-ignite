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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import io.socket.client.Ack;
import io.socket.emitter.Emitter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Base class for web socket handlers.
 */
abstract class AbstractHandler implements Emitter.Listener {
    /** */
    private static final ExecutorService pool = Executors.newCachedThreadPool();

    /** JSON object mapper. */
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        JsonOrgModule module = new JsonOrgModule();

        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        mapper.registerModule(module);
    }

    /** */
    final Logger log = Logger.getLogger(this.getClass().getName());

    /** */
    private final Ack noopCb = new Ack() {
        @Override public void call(Object... args) {
            if (args != null && args.length > 0 && args[0] instanceof Throwable)
                log.error("Failed to execute request on agent.", (Throwable) args[0]);
            else
                log.info("Request on agent successfully executed " + Arrays.toString(args));
        }
    };

    /**
     * @param obj Object.
     * @return {@link JSONObject} or {@link JSONArray}.
     */
    private Object toJSON(Object obj) {
        if (obj instanceof Iterable)
            return mapper.convertValue(obj, JSONArray.class);

        return mapper.convertValue(obj, JSONObject.class);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public final void call(Object... args) {
        boolean hasCb = args != null && args.length > 0 && args[args.length - 1] instanceof Ack;

        final Ack cb = hasCb ? (Ack)args[args.length - 1] : noopCb;

        if (hasCb)
            args = Arrays.copyOf(args, args.length - 1);

        try {
            final Map<String, Object> params;

            if (args == null || args.length == 0)
                params = Collections.emptyMap();
            else if (args.length == 1)
                params = mapper.convertValue(args[0], Map.class);
            else
                throw new IllegalArgumentException("Wrong arguments count, must be <= 1: " + Arrays.toString(args));

            pool.submit(new Runnable() {
                @Override public void run() {
                    try {
                        Object res = execute(params);

                        cb.call(null, toJSON(res));
                    } catch (Exception e) {
                        cb.call(e, null);
                    }
                }
            });
        }
        catch (Exception e) {
            cb.call(e, null);
        }
    }

    /**
     * Execute command with specified arguments.
     *
     * @param args Map with method args.
     */
    public abstract Object execute(Map<String, Object> args) throws Exception;
}
