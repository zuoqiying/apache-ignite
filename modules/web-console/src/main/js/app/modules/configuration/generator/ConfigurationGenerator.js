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

const DEFAULT = {
    atomics: {
        atomicSequenceReserveSize: 1000,
        backups: 0,
        cacheMode: {clsName: 'org.apache.ignite.cache.CacheMode', value: 'PARTITIONED'}
    }
};

export default ['ConfigurationGenerator', () => {
    class ConfigurationGenerator {
        /**
         * @param {String} clsName
         * @param {String} id
         * @param {Object} src
         * @param {Object} dflt
         * @returns {Bean}
         */
        static createBean(clsName, id, src, dflt) {
            return new Bean(clsName, id, src, dflt);
        }


        static igniteConfigurationBean(cluster) {
            return this.createBean('org.apache.ignite.configuration.IgniteConfiguration', 'cfg', cluster, DEFAULT);
        }

        /**
         * Function to generate ignite configuration.
         *
         * @param {Object} cluster Cluster to process.
         * @param {String} pkg Package name.
         * @param {String} className Class name for generate factory class otherwise generate code snippet.
         * @param {Boolean} clientNearCfg Optional client near cache configuration.
         * @return {Bean} Generated ignite configuration.
         */
        static igniteConfiguration(cluster, pkg, className, clientNearCfg) {
            const cfg = this.igniteConfigurationBean(cluster);

            this.clusterAtomics(cluster.atomics, cfg);

            return cfg;
        }

        clusterGeneral() {

        }

        static clusterAtomics(atomics, cfg = this.igniteConfigurationBean()) {
            const acfg = this.createBean('org.apache.ignite.configuration.AtomicConfiguration', 'atomicCfg',
                atomics, DEFAULT.atomics);

            if (acfg.isEmpty())
                return acfg;

            cfg.beanProperty('atomicConfiguration', acfg);

            acfg.enumProperty('cacheMode')
                .property('atomicSequenceReserveSize');

            if (acfg.valueOf('cacheMode') === 'PARTITIONED')
                acfg.property('backups');

            return cfg;
        }

        clusterCollision() {

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

    return ConfigurationGenerator;
}];
