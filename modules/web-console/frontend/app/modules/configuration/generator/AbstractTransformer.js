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

import StringBuilder from './StringBuilder';

export default class AbstractTransformer {
    // Generate general section.
    static clusterGeneral(cluster, sb = new StringBuilder()) {
        const cfg = this.generator.clusterGeneral(cluster);

        this._setProperties(sb, cfg);

        return sb;
    }

    // Generate atomics group.
    static clusterAtomics(atomics, sb = new StringBuilder()) {
        const cfg = this.generator.clusterAtomics(atomics);

        this._setProperties(sb, cfg);

        return sb;
    }

    // Generate binary group.
    static clusterBinary(binary, sb = new StringBuilder()) {
        const cfg = this.generator.clusterBinary(binary);

        this._setProperties(sb, cfg);

        return sb;
    }
    // Generate cache key configurations.
    static clusterCacheKeyConfiguration(keyCfgs, sb = new StringBuilder()) {
        const cfg = this.generator.clusterCacheKeyConfiguration(keyCfgs);

        this._setProperties(sb, cfg);

        return sb;
    }

    static clusterCommunication(cluster, sb = new StringBuilder()) {
        const cfg = this.generator.clusterCommunication(cluster);

        this._setProperties(sb, cfg);

        return sb;
    }

    static clusterConnector(connector, sb = new StringBuilder()) {
        const cfg = this.generator.clusterConnector(connector);

        this._setProperties(sb, cfg);

        return sb;
    }

    static clusterDeployment(cluster, sb = new StringBuilder()) {
        const cfg = this.generator.clusterDeployment(cluster);

        this._setProperties(sb, cfg);

        return sb;
    }

    static clusterDiscovery(disco, sb = new StringBuilder()) {
        const cfg = this.generator.clusterDiscovery(disco);

        this._setProperties(sb, cfg);

        return sb;
    }

    static clusterEvents(cluster, sb = new StringBuilder()) {
        const cfg = this.generator.clusterEvents(cluster);

        this._setProperties(sb, cfg);

        return sb;
    }

    static clusterFailover(cluster, sb = new StringBuilder()) {
        const cfg = this.generator.clusterFailover(cluster);

        this._setProperties(sb, cfg);

        return sb;
    }

    // Generate logger group.
    static clusterLogger(cluster, sb = new StringBuilder()) {
        const cfg = this.generator.clusterLogger(cluster);

        this._setProperties(sb, cfg);

        return sb;
    }

    // Generate marshaller group.
    static clusterMarshaller(cluster, sb = new StringBuilder()) {
        const cfg = this.generator.clusterMarshaller(cluster);

        this._setProperties(sb, cfg);

        return sb;
    }

    // Generate metrics group.
    static clusterMetrics(cluster, sb = new StringBuilder()) {
        const cfg = this.generator.clusterMetrics(cluster);

        this._setProperties(sb, cfg);

        return sb;
    }

    // Generate swap group.
    static clusterSsl(cluster, sb = new StringBuilder()) {
        const cfg = this.generator.clusterSsl(cluster);

        this._setProperties(sb, cfg);

        return sb;
    }

    // Generate swap group.
    static clusterSwap(cluster, sb = new StringBuilder()) {
        const cfg = this.generator.clusterSwap(cluster);

        this._setProperties(sb, cfg);

        return sb;
    }

    // Generate time group.
    static clusterTime(cluster, sb = new StringBuilder()) {
        const cfg = this.generator.clusterTime(cluster);

        this._setProperties(sb, cfg);

        return sb;
    }

    // Generate thread pools group.
    static clusterPools(cluster, sb = new StringBuilder()) {
        const cfg = this.generator.clusterPools(cluster);

        this._setProperties(sb, cfg);

        return sb;
    }

    // Generate transactions group.
    static clusterTransactions(transactionConfiguration, sb = new StringBuilder()) {
        const cfg = this.generator.clusterTransactions(transactionConfiguration);

        this._setProperties(sb, cfg);

        return sb;
    }
}
