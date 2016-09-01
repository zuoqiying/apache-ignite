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

import { EmptyBean, Bean, MethodBean } from './Beans';

const DEFAULT = {
    localHost: '0.0.0.0',
    discovery: {
        Multicast: {
            multicastGroup: '228.1.2.4',
            multicastPort: 47400,
            responseWaitTime: 500,
            addressRequestAttempts: 2,
            localAddress: '0.0.0.0'
        },
        Vm: {

        },
        Jdbc: {
            initSchema: false
        },
        SharedFs: {
            path: 'disco/tcp'
        },
        ZooKeeper: {
            basePath: '/services',
            serviceName: 'ignite',
            allowDuplicateRegistrations: false,
            ExponentialBackoff: {
                baseSleepTimeMs: 1000,
                maxRetries: 10
            },
            BoundedExponentialBackoffRetry: {
                baseSleepTimeMs: 1000,
                maxSleepTimeMs: 2147483647,
                maxRetries: 10
            },
            UntilElapsed: {
                maxElapsedTimeMs: 60000,
                sleepMsBetweenRetries: 1000
            },
            RetryNTimes: {
                n: 10,
                sleepMsBetweenRetries: 1000
            },
            OneTime: {
                sleepMsBetweenRetry: 1000
            },
            Forever: {
                retryIntervalMs: 1000
            }
        }
    },
    atomics: {
        atomicSequenceReserveSize: 1000,
        backups: 0,
        cacheMode: {clsName: 'org.apache.ignite.cache.CacheMode', value: 'PARTITIONED'}
    },
    binary: {
        compactFooter: true,
        typeConfigurations: {
            enum: true
        }
    },
    collision: {
        kind: null,
        JobStealing: {
            activeJobsThreshold: 95,
            waitJobsThreshold: 0,
            messageExpireTime: 1000,
            maximumStealingAttempts: 5,
            stealingEnabled: true,
            stealingAttributes: {keyClsName: 'java.util.String', valClsName: 'java.io.Serializable'}
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

export default ['ConfigurationGenerator', ['JavaTypes', (JavaTypes) => {
    class ConfigurationGenerator {
        static igniteConfigurationBean(cluster) {
            return new Bean('org.apache.ignite.configuration.IgniteConfiguration', 'cfg', cluster, DEFAULT);
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

        // Generate general section.
        static clusterGeneral(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            cfg.stringProperty('name', 'gridName')
                .stringProperty('localHost');

            if (cluster.discovery) {
                const discovery = new Bean('org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi', 'discovery', cluster.discovery, DEFAULT.discovery);

                let ipFinder;

                switch (discovery.valueOf('kind')) {
                    case 'Vm':
                        ipFinder = new Bean('org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder',
                            'ipFinder', cluster.discovery.Vm, DEFAULT.discovery.Vm);

                        ipFinder.collectionProperty('addrs', 'addresses', cluster.discovery.Vm.addresses);

                        break;
                    case 'Multicast':
                        ipFinder = new Bean('org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder',
                            'ipFinder', cluster.discovery.Multicast, DEFAULT.discovery.Multicast);

                        ipFinder.stringProperty('multicastGroup')
                            .property('multicastPort')
                            .property('responseWaitTime')
                            .property('addressRequestAttempts')
                            .stringProperty('localAddress')
                            .collectionProperty('addrs', 'addresses', cluster.discovery.Multicast.addresses);

                        break;
                    case 'S3':
                        ipFinder = new Bean('org.apache.ignite.spi.discovery.tcp.ipfinder.s3.TcpDiscoveryS3IpFinder',
                            'ipFinder', cluster.discovery.S3, DEFAULT.discovery.S3);

                        ipFinder.stringProperty('bucketName');

                        break;
                    case 'Cloud':
                        ipFinder = new Bean('org.apache.ignite.spi.discovery.tcp.ipfinder.cloud.TcpDiscoveryCloudIpFinder',
                            'ipFinder', cluster.discovery.Cloud, DEFAULT.discovery.Cloud);

                        ipFinder.stringProperty('credential')
                            .stringProperty('credentialPath')
                            .stringProperty('identity')
                            .stringProperty('provider')
                            .collectionProperty('regions', 'regions', cluster.discovery.Cloud.regions)
                            .collectionProperty('zones', 'zones', cluster.discovery.Cloud.zones);

                        break;
                    case 'GoogleStorage':
                        ipFinder = new Bean('org.apache.ignite.spi.discovery.tcp.ipfinder.gce.TcpDiscoveryGoogleStorageIpFinder',
                            'ipFinder', cluster.discovery.GoogleStorage, DEFAULT.discovery.GoogleStorage);

                        ipFinder.stringProperty('projectName')
                            .stringProperty('bucketName')
                            .stringProperty('serviceAccountP12FilePath')
                            .stringProperty('serviceAccountId');

                        break;
                    case 'Jdbc':
                        ipFinder = new Bean('org.apache.ignite.spi.discovery.tcp.ipfinder.jdbc.TcpDiscoveryJdbcIpFinder',
                            'ipFinder', cluster.discovery.Jdbc, DEFAULT.discovery.Jdbc);

                        ipFinder.property('initSchema');

                        if (ipFinder.includes('dataSourceBean', 'dialect'))
                            ipFinder.dataSource(ipFinder.valueOf('dataSourceBean'), 'dataSource', ipFinder.valueOf('dialect'));

                        break;
                    case 'SharedFs':
                        ipFinder = new Bean('org.apache.ignite.spi.discovery.tcp.ipfinder.sharedfs.TcpDiscoverySharedFsIpFinder',
                            'ipFinder', cluster.discovery.SharedFs, DEFAULT.discovery.SharedFs);

                        ipFinder.stringProperty('path');

                        break;
                    case 'ZooKeeper':
                        const src = cluster.discovery.ZooKeeper;
                        const dflt = DEFAULT.discovery.ZooKeeper;

                        ipFinder = new Bean('org.apache.ignite.spi.discovery.tcp.ipfinder.zk.TcpDiscoveryZookeeperIpFinder',
                            'ipFinder', src, dflt);

                        ipFinder.emptyBeanProperty('curator')
                            .stringProperty('zkConnectionString');

                        if (src && src.retryPolicy && src.retryPolicy.kind) {
                            const policy = src.retryPolicy;

                            let retryPolicyBean;

                            switch (policy.kind) {
                                case 'ExponentialBackoff':
                                    retryPolicyBean = new Bean('org.apache.curator.retry.ExponentialBackoffRetry', null,
                                        policy.ExponentialBackoff, dflt.ExponentialBackoff)
                                        .constructorArgument('baseSleepTimeMs')
                                        .constructorArgument('maxRetries')
                                        .constructorArgument('maxSleepMs');

                                    break;
                                case 'BoundedExponentialBackoff':
                                    retryPolicyBean = new Bean('org.apache.curator.retry.BoundedExponentialBackoffRetry', null,
                                        policy.BoundedExponentialBackoffRetry, dflt.BoundedExponentialBackoffRetry)
                                        .constructorArgument('baseSleepTimeMs')
                                        .constructorArgument('maxSleepTimeMs')
                                        .constructorArgument('maxRetries');

                                    break;
                                case 'UntilElapsed':
                                    retryPolicyBean = new Bean('org.apache.curator.retry.RetryUntilElapsed', null,
                                        policy.UntilElapsed, dflt.UntilElapsed)
                                        .constructorArgument('maxElapsedTimeMs')
                                        .constructorArgument('sleepMsBetweenRetries');

                                    break;

                                case 'NTimes':
                                    retryPolicyBean = new Bean('org.apache.curator.retry.RetryNTimes', null,
                                        policy.NTimes, dflt.NTimes)
                                        .constructorArgument('n')
                                        .constructorArgument('sleepMsBetweenRetries');

                                    break;
                                case 'OneTime':
                                    retryPolicyBean = new Bean('org.apache.curator.retry.RetryOneTime', null,
                                        policy.OneTime, dflt.OneTime)
                                        .constructorArgument('sleepMsBetweenRetry');

                                    break;
                                case 'Forever':
                                    retryPolicyBean = new Bean('org.apache.curator.retry.RetryForever', null,
                                        policy.Forever, dflt.Forever)
                                        .constructorArgument('retryIntervalMs');

                                    break;
                                case 'Custom':
                                    if (!_.isEmpty(policy.Custom.className))
                                        retryPolicyBean = new EmptyBean(policy.Custom.className);

                                    break;
                                default:
                            }

                            if (retryPolicyBean)
                                ipFinder.beanProperty('retryPolicy', retryPolicyBean);
                        }

                        ipFinder.stringProperty('basePath', '/services')
                            .stringProperty('serviceName')
                            .stringProperty('allowDuplicateRegistrations');

                        break;
                    default:
                }

                if (ipFinder)
                    discovery.beanProperty('ipFinder', ipFinder);

                cfg.beanProperty('discoverySpi', discovery);
            }

            return cfg;
        }

        // Generate atomics group.
        static clusterAtomics(atomics, cfg = this.igniteConfigurationBean()) {
            const acfg = new Bean('org.apache.ignite.configuration.AtomicConfiguration', 'atomicCfg',
                atomics, DEFAULT.atomics);

            acfg.enumProperty('cacheMode')
                .property('atomicSequenceReserveSize');

            if (acfg.valueOf('cacheMode') === 'PARTITIONED')
                acfg.property('backups');

            if (acfg.isEmpty())
                return cfg;

            cfg.beanProperty('atomicConfiguration', acfg);

            return cfg;
        }

        // Generate binary group.
        static clusterBinary(binary, cfg = this.igniteConfigurationBean()) {
            const binaryCfg = new Bean('org.apache.ignite.configuration.BinaryConfiguration', 'binaryCfg',
                binary, DEFAULT.binary);

            binaryCfg.emptyBeanProperty('idMapper')
                .emptyBeanProperty('nameMapper')
                .emptyBeanProperty('serializer');

            const typeCfgs = [];

            _.forEach(binary.typeConfigurations, (type) => {
                const typeCfg = new MethodBean('org.apache.ignite.binary.BinaryTypeConfiguration',
                    JavaTypes.toJavaName('binaryType', type.typeName), type, DEFAULT.binary.typeConfigurations);

                typeCfg.stringProperty('typeName')
                    .emptyBeanProperty('idMapper')
                    .emptyBeanProperty('nameMapper')
                    .emptyBeanProperty('serializer')
                    .enumProperty('enum');

                if (typeCfg.nonEmpty())
                    typeCfgs.push(typeCfg);
            });

            binaryCfg.collectionProperty('types', 'typeConfigurations', typeCfgs, 'java.util.Collection', 'org.apache.ignite.binary.BinaryTypeConfiguration');

            binaryCfg.property('compactFooter');

            if (binaryCfg.isEmpty())
                return cfg;

            cfg.beanProperty('binaryConfiguration', binaryCfg);

            return cfg;
        }

        // Generate cache key configurations.
        static clusterCacheKeyConfiguration(keyCfgs, cfg = this.igniteConfigurationBean()) {
            const items = _.reduce(keyCfgs, (acc, keyCfg) => {
                if (keyCfg.typeName && keyCfg.affinityKeyFieldName) {
                    acc.push(new Bean('org.apache.ignite.cache.CacheKeyConfiguration', null, keyCfg)
                        .stringConstructorArgument('typeName')
                        .stringConstructorArgument('affinityKeyFieldName'));
                }

                return acc;
            }, []);

            if (_.isEmpty(items))
                return cfg;

            cfg.arrayProperty('cacheKeyConfiguration', 'keyConfigurations', items, 'org.apache.ignite.cache.CacheKeyConfiguration');

            return cfg;
        }

        // Generate collision group.
        static clusterCollision(collision, cfg = this.igniteConfigurationBean()) {
            let colSpi;

            switch (collision.kind) {
                case 'JobStealing':
                    colSpi = new Bean('org.apache.ignite.spi.collision.jobstealing.JobStealingCollisionSpi',
                        'colSpi', collision.JobStealing, DEFAULT.collision.JobStealing);

                    colSpi.property('activeJobsThreshold')
                        .property('waitJobsThreshold')
                        .property('messageExpireTime')
                        .property('maximumStealingAttempts')
                        .property('stealingEnabled')
                        .emptyBeanProperty('externalCollisionListener')
                        .mapProperty('stealingAttributes', 'stealingAttrs');

                    break;

                case 'FifoQueue':
                    colSpi = new Bean('org.apache.ignite.spi.collision.fifoqueue.FifoQueueCollisionSpi',
                        'colSpi', collision.FifoQueue, DEFAULT.collision.FifoQueue);

                    colSpi.property('parallelJobsNumber')
                        .property('waitingJobsNumber');

                    break;

                case 'PriorityQueue':
                    colSpi = new Bean('org.apache.ignite.spi.collision.priorityqueue.PriorityQueueCollisionSpi',
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
                    colSpi = new Bean(collision.Custom.class,
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

        // Generate communication group.
        static clusterCommunication(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            return cfg;
        }

        // Generate REST access configuration.
        static clusterConnector(connector, cfg = this.igniteConfigurationBean()) {
            return cfg;
        }

        // Generate deployment group.
        static clusterDeployment(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            return cfg;
        }

        // Generate discovery group.
        static clusterDiscovery(disco, cfg = this.igniteConfigurationBean()) {
            return cfg;
        }

        // Generate events group.
        static clusterEvents(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            if (!_.isEmpty(cluster.includeEventTypes))
                cfg.eventTypes('events', 'includeEventTypes', cluster.includeEventTypes);

            return cfg;
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
}]];
