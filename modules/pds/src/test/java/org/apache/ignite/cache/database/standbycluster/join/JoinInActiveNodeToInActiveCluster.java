/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.cache.database.standbycluster.join;

import org.apache.ignite.cache.database.standbycluster.AbstractNodeJoinTemplate;

/**
 *
 */
public class JoinInActiveNodeToInActiveCluster extends AbstractNodeJoinTemplate {
    /** {@inheritDoc} */
    @Override public JoinNodeTestBuilder withOutConfigurationTemplate() throws Exception {
        AbstractNodeJoinTemplate.JoinNodeTestBuilder b = builder();

        b.clusterConfiguration(
            cfg(name(0)).setActiveOnStart(false),
            cfg(name(1)).setActiveOnStart(false),
            cfg(name(2)).setActiveOnStart(false)
        ).afterClusterStarted(
            b.checkCacheEmpty()
        ).nodeConfiguration(
            cfg(name(3)).setActiveOnStart(false)
        ).afterNodeJoin(
            b.checkCacheEmpty()
        ).stateAfterJoin(
            false
        ).afterActivate(
            b.checkCacheNotEmpty()
        );

        return b;
    }

    /** {@inheritDoc} */
    @Override public JoinNodeTestBuilder staticCacheConfigurationOnJoinTemplate() throws Exception {
        JoinNodeTestBuilder b = builder();

        b.clusterConfiguration(
            cfg(name(0)).setActiveOnStart(false),
            cfg(name(1)).setActiveOnStart(false),
            cfg(name(2)).setActiveOnStart(false)
        ).afterClusterStarted(
            b.checkCacheEmpty()
        ).nodeConfiguration(
            cfg(name(3))
                .setActiveOnStart(false)
                .setCacheConfiguration(allCacheConfigurations())
        ).afterNodeJoin(
            b.checkCacheEmpty()
        ).stateAfterJoin(
            false
        ).afterActivate(
            b.checkCacheNotEmpty()
        );

        return b;
    }

    /** {@inheritDoc} */
    @Override public JoinNodeTestBuilder staticCacheConfigurationInClusterTemplate() throws Exception {
        JoinNodeTestBuilder b = builder();

        b.clusterConfiguration(
            cfg(name(0))
                .setActiveOnStart(false)
                .setCacheConfiguration(allCacheConfigurations()),
            cfg(name(1)).setActiveOnStart(false),
            cfg(name(2)).setActiveOnStart(false)
        ).afterClusterStarted(
            b.checkCacheEmpty()
        ).nodeConfiguration(
            cfg(name(3)).setActiveOnStart(false)
        ).afterNodeJoin(
            b.checkCacheEmpty()
        ).stateAfterJoin(
            false
        ).afterActivate(
            b.checkCacheNotEmpty()
        );

        return b;
    }

    /** {@inheritDoc} */
    @Override public JoinNodeTestBuilder staticCacheConfigurationSameOnBothTemplate() throws Exception {
        JoinNodeTestBuilder b = builder();

        b.clusterConfiguration(
            cfg(name(0))
                .setActiveOnStart(false)
                .setCacheConfiguration(allCacheConfigurations()),
            cfg(name(1)).setActiveOnStart(false),
            cfg(name(2)).setActiveOnStart(false)
        ).afterClusterStarted(
            b.checkCacheEmpty()
        ).nodeConfiguration(
            cfg(name(3))
                .setActiveOnStart(false)
                .setCacheConfiguration(allCacheConfigurations())
        ).afterNodeJoin(
            b.checkCacheEmpty()
        ).stateAfterJoin(
            false
        ).afterActivate(
            b.checkCacheNotEmpty()
        );

        return b;
    }

    /** {@inheritDoc} */
    @Override public JoinNodeTestBuilder staticCacheConfigurationDifferentOnBothTemplate() throws Exception {
        JoinNodeTestBuilder b = builder();

        b.clusterConfiguration(
            cfg(name(0))
                .setActiveOnStart(false)
                .setCacheConfiguration(transactionCfg()),
            cfg(name(1)).setActiveOnStart(false),
            cfg(name(2)).setActiveOnStart(false)
        ).afterClusterStarted(
            b.checkCacheEmpty()
        ).nodeConfiguration(
            cfg(name(3))
                .setActiveOnStart(false)
                .setCacheConfiguration(atomicCfg())
        ).afterNodeJoin(
            b.checkCacheEmpty()
        ).stateAfterJoin(
            false
        ).afterActivate(
            b.checkCacheNotEmpty()
        );

        return b;
    }

    // Server node join.

    /** {@inheritDoc} */
    @Override public void testJoinWithOutConfiguration() throws Exception {
        withOutConfigurationTemplate().build();
    }

    /** {@inheritDoc} */
    @Override public void testStaticCacheConfigurationOnJoin() throws Exception {
        staticCacheConfigurationOnJoinTemplate().build();
    }

    /** {@inheritDoc} */
    @Override public void testStaticCacheConfigurationInCluster() throws Exception {
        staticCacheConfigurationInClusterTemplate().build();
    }

    /** {@inheritDoc} */
    @Override public void testStaticCacheConfigurationSameOnBoth() throws Exception {
        staticCacheConfigurationSameOnBothTemplate().build();
    }

    /** {@inheritDoc} */
    @Override public void testStaticCacheConfigurationDifferentOnBoth() throws Exception {
        staticCacheConfigurationDifferentOnBothTemplate().build();
    }

    // Client node join.

    /** {@inheritDoc} */
    @Override public void testJoinClientWithOutConfiguration() throws Exception {
        withOutConfigurationTemplate().nodeConfiguration(setClient).build();
    }

    /** {@inheritDoc} */
    @Override public void testJoinClientStaticCacheConfigurationOnJoin() throws Exception {
        staticCacheConfigurationOnJoinTemplate().nodeConfiguration(setClient).build();
    }

    /** {@inheritDoc} */
    @Override public void testJoinClientStaticCacheConfigurationInCluster() throws Exception {
        staticCacheConfigurationInClusterTemplate().nodeConfiguration(setClient).build();
    }

    /** {@inheritDoc} */
    @Override public void testJoinClientStaticCacheConfigurationSameOnBoth() throws Exception {
        staticCacheConfigurationSameOnBothTemplate().nodeConfiguration(setClient).build();
    }

    /** {@inheritDoc} */
    @Override public void testJoinClientStaticCacheConfigurationDifferentOnBoth() throws Exception {
        staticCacheConfigurationDifferentOnBothTemplate().nodeConfiguration(setClient).build();
    }
}
