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

import _ from 'lodash';
import Bean from './Bean';

const enumMapper = (val) => _.capitalize(val);

const DEFAULT = {
    atomics: {
        atomicSequenceReserveSize: 1000,
        backups: 0,
        cacheMode: {clsName: 'Apache.Ignite.Core.Cache.Configuration.CacheMode', value: 'PARTITIONED', mapper: enumMapper}
    }
};

export default ['PlatformGenerator', () => {
    class PlatformGenerator {
        static igniteConfigurationBean(cluster) {
            const cfg = new Bean('Apache.Ignite.Core.IgniteConfiguration', 'cfg', cluster, DEFAULT);

            return cfg;
        }

        /**
         * Function to generate ignite configuration.
         *
         * @param {Object} cluster Cluster to process.
         * @param {String} pkg Package name.
         * @param {String} className Class name for generate factory class otherwise generate code snippet.
         * @param {Boolean} clientNearCfg Optional client near cache configuration.
         * @return {String} Generated ignite configuration.
         */
        static igniteConfiguration(cluster, pkg, className, clientNearCfg) {
            const cfg = this.igniteConfigurationBean(cluster);

            this.clusterAtomics(cluster.atomics, cfg);

            return cfg;
        }

        clusterGeneral() {

        }

        static clusterAtomics(atomics, cfg = this.igniteConfigurationBean()) {
            const acfg = new Bean('Apache.Ignite.Core.DataStructures.Configuration.AtomicConfiguration', 'atomicCfg',
                atomics, DEFAULT.atomics);

            if (acfg.isEmpty())
                return acfg;

            cfg.beanProperty('AtomicConfiguration', acfg);

            acfg.enumProperty('cacheMode', 'CacheMode')
                .property('atomicSequenceReserveSize', 'AtomicSequenceReserveSize');

            if (acfg.valueOf('cacheMode') === 'PARTITIONED')
                acfg.property('backups', 'Backups');

            return cfg;
        }

        static clusterCollision(collision, cfg = this.igniteConfigurationBean()) {

        }

        clusterConnector() {

        }

        clusterCommunication() {

        }

        clusterDeployment() {

        }

        clusterEvents() {

        }

        clusterFailover() {

        }

        clusterLogger() {

        }

        clusterMarshaller() {

        }

        clusterMetrics() {

        }

        clusterSwap() {

        }

        clusterTime() {

        }

        clusterPools() {

        }

        clusterTransactions() {

        }

        cacheConfiguration() {

        }
    }

    return PlatformGenerator;
}];
