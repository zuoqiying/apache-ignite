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

package org.apache.ignite.internal.visor.node;

import java.io.Serializable;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 * Data transfer object for node metrics configuration properties.
 */
public class VisorMetricsConfiguration implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Metrics expired time. */
    private long expTime;

    /** Number of node metrics stored in memory. */
    private int histSize;

    /** Frequency of metrics log printout. */
    private long logFreq;

    /**
     * Create transfer object for node metrics configuration properties.
     *
     * @param c Grid configuration.
     */
    public VisorMetricsConfiguration(IgniteConfiguration c) {
        expTime = c.getMetricsExpireTime();
        histSize = c.getMetricsHistorySize();
        logFreq = c.getMetricsLogFrequency();
    }

    /**
     * @return Metrics expired time.
     */
    public long expireTime() {
        return expTime;
    }

    /**
     * @return Number of node metrics stored in memory.
     */
    public int historySize() {
        return histSize;
    }

    /**
     * @return Frequency of metrics log printout.
     */
    public long loggerFrequency() {
        return logFreq;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorMetricsConfiguration.class, this);
    }
}
