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

import Bean from './Bean';

const DEFAULT = {
    atomics: {
        atomicSequenceReserveSize: 1000,
        backups: 0,
        cacheMode: {clsName: 'org.apache.ignite.cache.CacheMode', value: 'PARTITIONED'}
    },
    collision: {
        kind: null,
        JobStealing: {
            activeJobsThreshold: 95,
            waitJobsThreshold: 0,
            messageExpireTime: 1000,
            maximumStealingAttempts: 5,
            stealingEnabled: true,
            stealingAttributes: {clsName: 'java.util.HashMap', keyClsName: 'java.util.String', valClsName: 'java.io.Serializable'}
        },
        FifoQueue: {

        },
        PriorityQueue: {
            priorityAttributeKey: 'grid.task.priority',
            jobPriorityAttributeKey: 'grid.job.priority',
            defaultPriority: 0,
            starvationIncrement: 1,
            starvationPreventionEnabled: true
        },
        Custom: {
        }
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
         * @return {Bean} Generated ignite configuration.
         */
        static igniteConfiguration(cluster) {
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
                return cfg;

            cfg.beanProperty('atomicConfiguration', acfg);

            acfg.enumProperty('cacheMode')
                .property('atomicSequenceReserveSize');

            if (acfg.valueOf('cacheMode') === 'PARTITIONED')
                acfg.property('backups');

            return cfg;
        }

        static clusterCollision(collision, cfg = this.igniteConfigurationBean()) {
            let colSpi;

            switch (collision.kind) {
                case 'JobStealing':
                    colSpi = this.createBean('org.apache.ignite.spi.collision.jobstealing.JobStealingCollisionSpi',
                        'colSpi', collision.JobStealing, DEFAULT.collision.JobStealing);

                    colSpi.property('activeJobsThreshold')
                        .property('waitJobsThreshold')
                        .property('messageExpireTime')
                        .property('maximumStealingAttempts')
                        .property('stealingEnabled')
                        .emptyBeanProperty('externalCollisionListener')
                        .mapProperty('stealingAttrs', 'stealingAttributes');

                    break;

                case 'FifoQueue':
                    colSpi = this.createBean('org.apache.ignite.spi.collision.fifoqueue.FifoQueueCollisionSpi',
                        'colSpi', collision.FifoQueue, DEFAULT.collision.FifoQueue);

                    colSpi.property('parallelJobsNumber')
                        .property('waitingJobsNumber');

                    break;

                case 'PriorityQueue':
                    colSpi = this.createBean('org.apache.ignite.spi.collision.priorityqueue.PriorityQueueCollisionSpi',
                        'colSpi', collision.PriorityQueue, DEFAULT.collision.PriorityQueue);

                    colSpi.property('parallelJobsNumber')
                        .property('waitingJobsNumber')
                        .property('priorityAttributeKey')
                        .property('jobPriorityAttributeKey')
                        .property('defaultPriority')
                        .property('starvationIncrement')
                        .property('starvationPreventionEnabled');

                    break;

                case 'Custom':
                    colSpi = this.createBean(collision.Custom.class,
                        'colSpi', collision.PriorityQueue, DEFAULT.collision.PriorityQueue);

                    break;

                default:
                    return cfg;
            }

            if (colSpi.isEmpty())
                return cfg;

            cfg.beanProperty('collisionSpi', colSpi);

            return cfg;
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
