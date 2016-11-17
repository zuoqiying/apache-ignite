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

package org.apache.ignite.console.agent;

import java.io.IOException;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.util.Map;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.log4j.Logger;

/**
 *
 */
public class RestExecutor {
    /** */
    private static final Logger log = Logger.getLogger(RestExecutor.class);

    /** */
    private final OkHttpClient httpClient;

    /** Demo node URL. */
    private String demoUrl;

    /** Node URL. */
    private String nodeUrl;

    /**
     * Default constructor.
     */
    RestExecutor(String nodeUrl) {
        this.nodeUrl = nodeUrl;

        httpClient = new OkHttpClient.Builder().build();
    }

    /**
     * Stop HTTP client.
     */
    void stop() {
        if (httpClient != null)
            httpClient.dispatcher().executorService().shutdown();
    }

    /**
     * @param demo Is demo node request.
     * @param path Path segment.
     * @param params Params.
     * @param mtd Method.
     * @param headers Headers.
     * @param body Body.
     */
    public RestResult execute(boolean demo, String path, Map<String, Object> params,
        String mtd, Map<String, Object> headers, String body) throws IOException {
        if (log.isDebugEnabled())
            log.debug("Start execute REST command [method=" + mtd + ", uri=/" + (path == null ? "" : path) +
                ", parameters=" + params + "]");

        final Request.Builder reqBuilder = new Request.Builder();

        if (demo && this.demoUrl == null)
            return RestResult.fail(404, "Demo node is not started yet.");

        HttpUrl.Builder urlBuilder = HttpUrl.parse(demo ? this.demoUrl : this.nodeUrl)
            .newBuilder();

        if (path != null)
            urlBuilder.addPathSegment(path);

        if (headers != null) {
            for (Map.Entry<String, Object> entry : headers.entrySet())
                if (entry.getValue() != null)
                    reqBuilder.addHeader(entry.getKey(), entry.getValue().toString());
        }

        if ("GET".equalsIgnoreCase(mtd)) {
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    if (entry.getValue() != null)
                        urlBuilder.addQueryParameter(entry.getKey(), entry.getValue().toString());
                }
            }
        }
        else if ("POST".equalsIgnoreCase(mtd)) {
            if (body != null) {
                MediaType contentType = MediaType.parse("text/plain");

                reqBuilder.post(RequestBody.create(contentType, body));
            }
            else {
                FormBody.Builder formBody = new FormBody.Builder();

                if (params != null) {
                    for (Map.Entry<String, Object> entry : params.entrySet()) {
                        if (entry.getValue() != null)
                            formBody.add(entry.getKey(), entry.getValue().toString());
                    }
                }

                reqBuilder.post(formBody.build());
            }
        }
        else
            throw new IOException("Unknown HTTP-method: " + mtd);

        HttpUrl url = urlBuilder.build();

        reqBuilder.url(url);

        try (Response resp = httpClient.newCall(reqBuilder.build()).execute()) {
            return RestResult.success(resp.code(), resp.body().string());
        }
        catch (ConnectException e) {
            log.info("Failed connect to node and execute REST command [url=" + url + "]");

            return RestResult.fail(404, "Failed connect to node and execute REST command.");
        }
    }

    /**
     * @param demo Is demo node request.
     */
    public RestResult topology(boolean demo) throws IOException {
        Map<String, Object> params = U.newLinkedHashMap(3);

        params.put("cmd", "top");
        params.put("attr", false);
        params.put("mtr", false);

        return execute(demo, "ignite", params, "GET", null, null);
    }
}
