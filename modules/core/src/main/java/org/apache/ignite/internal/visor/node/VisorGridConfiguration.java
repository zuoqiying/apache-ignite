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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.util.typedef.internal.S;

import static org.apache.ignite.internal.visor.util.VisorTaskUtils.compactArray;

/**
 * Data transfer object for node configuration data.
 */
public class VisorGridConfiguration implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Basic. */
    private VisorBasicConfiguration basic;

    /** Metrics. */
    private VisorMetricsConfiguration metrics;

    /** SPIs. */
    private VisorSpisConfiguration spis;

    /** P2P. */
    private VisorPeerToPeerConfiguration p2p;

    /** Lifecycle. */
    private VisorLifecycleConfiguration lifecycle;

    /** Executors service configuration. */
    private VisorExecutorServiceConfiguration execSvc;

    /** Segmentation. */
    private VisorSegmentationConfiguration seg;

    /** Include properties. */
    private String inclProps;

    /** Include events types. */
    private int[] inclEvtTypes;

    /** REST configuration. */
    private VisorRestConfiguration rest;

    /** User attributes. */
    private Map<String, ?> userAttrs;

    /** Igfss. */
    private Iterable<VisorIgfsConfiguration> igfss;

    /** Environment. */
    private Map<String, String> env;

    /** System properties. */
    private Properties sysProps;

    /** Configuration of atomic data structures. */
    private VisorAtomicConfiguration atomic;

    /** Transactions configuration. */
    private VisorTransactionConfiguration txCfg;

    /** Database configuration. */
    private VisorMemoryConfiguration memCfg;

    /**
     * Create data transfer object with node configuration data.
     *
     * @param ignite Grid.
     */
    public VisorGridConfiguration(IgniteEx ignite) {
        assert ignite != null;

        IgniteConfiguration c = ignite.configuration();

        basic = new VisorBasicConfiguration(ignite, c);
        metrics = VisorMetricsConfiguration.from(c);
        spis = VisorSpisConfiguration.from(c);
        p2p = VisorPeerToPeerConfiguration.from(c);
        lifecycle = VisorLifecycleConfiguration.from(c);
        execSvc = VisorExecutorServiceConfiguration.from(c);
        seg = VisorSegmentationConfiguration.from(c);
        inclProps = compactArray(c.getIncludeProperties());
        inclEvtTypes = c.getIncludeEventTypes();
        rest = VisorRestConfiguration.from(c);
        userAttrs = c.getUserAttributes();
        igfss = VisorIgfsConfiguration.list(c.getFileSystemConfiguration());
        env = new HashMap<>(System.getenv());
        sysProps = IgniteSystemProperties.snapshot();
        atomic = new VisorAtomicConfiguration(c.getAtomicConfiguration());
        txCfg = VisorTransactionConfiguration.from(c.getTransactionConfiguration());
        memCfg = new VisorMemoryConfiguration(c.getMemoryConfiguration());
    }

    /**
     * @return Basic.
     */
    public VisorBasicConfiguration getBasic() {
        return basic;
    }

    /**
     * @return Metrics.
     */
    public VisorMetricsConfiguration getMetrics() {
        return metrics;
    }

    /**
     * @return SPIs.
     */
    public VisorSpisConfiguration getSpis() {
        return spis;
    }

    /**
     * @return P2P.
     */
    public VisorPeerToPeerConfiguration getP2p() {
        return p2p;
    }

    /**
     * @return Lifecycle.
     */
    public VisorLifecycleConfiguration getLifecycle() {
        return lifecycle;
    }

    /**
     * @return Executors service configuration.
     */
    public VisorExecutorServiceConfiguration getExecutorService() {
        return execSvc;
    }

    /**
     * @return Segmentation.
     */
    public VisorSegmentationConfiguration getSegmentation() {
        return seg;
    }

    /**
     * @return Include properties.
     */
    public String getIncludeProperties() {
        return inclProps;
    }

    /**
     * @return Include events types.
     */
    public int[] getIncludeEventTypes() {
        return inclEvtTypes;
    }

    /**
     * @return Rest.
     */
    public VisorRestConfiguration getRest() {
        return rest;
    }

    /**
     * @return User attributes.
     */
    public Map<String, ?> getUserAttributes() {
        return userAttrs;
    }

    /**
     * @return Igfss.
     */
    public Iterable<VisorIgfsConfiguration> getIgfss() {
        return igfss;
    }

    /**
     * @return Environment.
     */
    public Map<String, String> getEnv() {
        return env;
    }

    /**
     * @return System properties.
     */
    public Properties getSystemProperties() {
        return sysProps;
    }

    /**
     * @return Configuration of atomic data structures.
     */
    public VisorAtomicConfiguration getAtomic() {
        return atomic;
    }

    /**
     * @return Transactions configuration.
     */
    public VisorTransactionConfiguration getTransaction() {
        return txCfg;
    }

    /**
     * @return Memory configuration.
     */
    public VisorMemoryConfiguration getMemoryConfiguration() {
        return memCfg;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorGridConfiguration.class, this);
    }
}
