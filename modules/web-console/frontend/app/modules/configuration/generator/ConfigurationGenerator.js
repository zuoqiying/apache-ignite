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
        localPort: 47500,
        localPortRange: 100,
        socketTimeout: 5000,
        ackTimeout: 5000,
        maxAckTimeout: 600000,
        networkTimeout: 5000,
        joinTimeout: 0,
        threadPriority: 10,
        heartbeatFrequency: 2000,
        maxMissedHeartbeats: 1,
        maxMissedClientHeartbeats: 5,
        topHistorySize: 1000,
        reconnectCount: 10,
        statisticsPrintFrequency: 0,
        ipFinderCleanFrequency: 60000,
        forceServerMode: false,
        clientReconnectDisabled: false,
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
            enum: false
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
    },
    communication: {
        localPort: 47100,
        localPortRange: 100,
        sharedMemoryPort: 48100,
        directBuffer: false,
        directSendBuffer: false,
        idleConnectionTimeout: 30000,
        connectTimeout: 5000,
        maxConnectTimeout: 600000,
        reconnectCount: 10,
        socketSendBuffer: 32768,
        socketReceiveBuffer: 32768,
        messageQueueLimit: 1024,
        tcpNoDelay: true,
        ackSendThreshold: 16,
        unacknowledgedMessagesBufferSize: 0,
        socketWriteTimeout: 2000
    },
    networkTimeout: 5000,
    networkSendRetryDelay: 1000,
    networkSendRetryCount: 3,
    discoveryStartupDelay: 60000,
    connector: {
        port: 11211,
        portRange: 100,
        idleTimeout: 7000,
        idleQueryCursorTimeout: 600000,
        idleQueryCursorCheckFrequency: 60000,
        receiveBufferSize: 32768,
        sendBufferSize: 32768,
        sendQueueLimit: 0,
        directBuffer: false,
        noDelay: true,
        sslEnabled: false,
        sslClientAuth: false
    },
    deploymentMode: {clsName: 'org.apache.ignite.configuration.DeploymentMode', value: 'SHARED'},
    peerClassLoadingEnabled: false,
    peerClassLoadingMissedResourcesCacheSize: 100,
    peerClassLoadingThreadPoolSize: 2,
    failoverSpi: {
        JobStealing: {
            maximumFailoverAttempts: 5
        },
        Always: {
            maximumFailoverAttempts: 5
        }
    },
    logger: {
        Log4j: {
            level: {clsName: 'org.apache.logging.log4j.Level', value: null}
        },
        Log4j2: {
            level: {clsName: 'org.apache.logging.log4j.Level', value: null}
        }
    },
    marshalLocalJobs: false,
    marshallerCacheKeepAliveTime: 10000,
    metricsHistorySize: 10000,
    metricsLogFrequency: 60000,
    metricsUpdateFrequency: 2000,
    clockSyncSamples: 8,
    clockSyncFrequency: 120000,
    timeServerPortBase: 31100,
    timeServerPortRange: 100,
    transactionConfiguration: {
        defaultTxConcurrency: {clsName: 'org.apache.ignite.transactions.TransactionConcurrency', value: 'PESSIMISTIC'},
        defaultTxIsolation: {clsName: 'org.apache.ignite.transactions.TransactionIsolation', value: 'REPEATABLE_READ'},
        defaultTxTimeout: 0,
        pessimisticTxLogLinger: 10000
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
                            .pathProperty('credentialPath')
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
                            .pathProperty('serviceAccountP12FilePath')
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

                        ipFinder.pathProperty('path');

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

                        ipFinder.pathProperty('basePath', '/services')
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
                    .property('enum');

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
            const commSpi = new Bean('org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi', 'communicationSpi',
                cluster.communication, DEFAULT.communication);

            commSpi.emptyBeanProperty('listener')
                .stringProperty('localAddress')
                .property('localPort')
                .property('localPortRange')
                .property('sharedMemoryPort')
                .property('directBuffer')
                .property('directSendBuffer')
                .property('idleConnectionTimeout')
                .property('connectTimeout')
                .property('maxConnectTimeout')
                .property('reconnectCount')
                .property('socketSendBuffer')
                .property('socketReceiveBuffer')
                .property('messageQueueLimit')
                .property('slowClientQueueLimit')
                .property('tcpNoDelay')
                .property('ackSendThreshold')
                .property('unacknowledgedMessagesBufferSize')
                .property('socketWriteTimeout')
                .property('selectorsCount')
                .emptyBeanProperty('addressResolver');

            if (commSpi.nonEmpty())
                cfg.beanProperty('communicationSpi', commSpi);

            cfg.property('networkTimeout')
                .property('networkSendRetryDelay')
                .property('networkSendRetryCount')
                .property('discoveryStartupDelay');

            return cfg;
        }

        // Generate REST access configuration.
        static clusterConnector(connector, cfg = this.igniteConfigurationBean()) {
            const connCfg = new Bean('org.apache.ignite.configuration.ConnectorConfiguration',
                'connectorConfiguration', connector, DEFAULT.connector);

            if (connCfg.valueOf('enabled')) {
                connCfg.pathProperty('jettyPath')
                    .stringProperty('host')
                    .property('port')
                    .property('portRange')
                    .property('idleTimeout')
                    .property('idleQueryCursorTimeout')
                    .property('idleQueryCursorCheckFrequency')
                    .property('receiveBufferSize')
                    .property('sendBufferSize')
                    .property('sendQueueLimit')
                    .property('directBuffer')
                    .property('noDelay')
                    .property('selectorCount')
                    .property('threadPoolSize')
                    .emptyBeanProperty('messageInterceptor')
                    .stringProperty('secretKey');

                if (connCfg.valueOf('sslEnabled')) {
                    connCfg.property('sslClientAuth')
                        .emptyBeanProperty('sslFactory');
                }

                if (connCfg.nonEmpty())
                    cfg.beanProperty('connectorConfiguration', connCfg);
            }

            return cfg;
        }

        // Generate deployment group.
        static clusterDeployment(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            cfg.enumProperty('deploymentMode')
                .property('peerClassLoadingEnabled');

            if (cfg.valueOf('peerClassLoadingEnabled')) {
                cfg.property('peerClassLoadingMissedResourcesCacheSize')
                    .property('peerClassLoadingThreadPoolSize')
                    .arrayProperty('p2pLocClsPathExcl', 'peerClassLoadingLocalClassPathExclude',
                       cluster.peerClassLoadingLocalClassPathExclude);
            }

            return cfg;
        }

        // Generate discovery group.
        static clusterDiscovery(discovery, cfg = this.igniteConfigurationBean()) {
            if (discovery) {
                // TODO IGNITE-2052 The same bean that in general section.
                const discoveryCfg = new Bean('org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi', 'discovery',
                    discovery, DEFAULT.discovery);

                discoveryCfg.stringProperty('localAddress')
                    .property('localPort')
                    .property('localPortRange')
                    .emptyBeanProperty('addressResolver')
                    .property('socketTimeout')
                    .property('ackTimeout')
                    .property('maxAckTimeout')
                    .property('networkTimeout')
                    .property('joinTimeout')
                    .property('threadPriority')
                    .property('heartbeatFrequency')
                    .property('maxMissedHeartbeats')
                    .property('maxMissedClientHeartbeats')
                    .property('topHistorySize')
                    .emptyBeanProperty('listener')
                    .emptyBeanProperty('dataExchange')
                    .emptyBeanProperty('metricsProvider')
                    .property('reconnectCount')
                    .property('statisticsPrintFrequency')
                    .property('ipFinderCleanFrequency')
                    .emptyBeanProperty('authenticator')
                    .property('forceServerMode')
                    .property('clientReconnectDisabled');

                if (discoveryCfg.nonEmpty())
                    cfg.beanProperty('discoverySpi', discoveryCfg);
            }

            return cfg;
        }

        // Generate events group.
        static clusterEvents(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            if (!_.isEmpty(cluster.includeEventTypes))
                cfg.eventTypes('events', 'includeEventTypes', cluster.includeEventTypes);

            return cfg;
        }

        // Generate failover group.
        static clusterFailover(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            const spis = [];

            _.forEach(cluster.failoverSpi, (spi) => {
                let failoverSpi;

                switch (spi.kind) {
                    case 'JobStealing':
                        failoverSpi = new Bean('org.apache.ignite.spi.failover.jobstealing.JobStealingFailoverSpi',
                            'failoverSpi', spi.JobStealing, DEFAULT.failoverSpi.JobStealing);

                        failoverSpi.property('maximumFailoverAttempts');

                        break;
                    case 'Never':
                        failoverSpi = new Bean('org.apache.ignite.spi.failover.never.NeverFailoverSpi',
                            'failoverSpi', spi.Never);

                        break;
                    case 'Always':
                        failoverSpi = new Bean('org.apache.ignite.spi.failover.always.AlwaysFailoverSpi',
                            'failoverSpi', spi.Always, DEFAULT.failoverSpi.Always);

                        failoverSpi.property('maximumFailoverAttempts');

                        break;
                    case 'Custom':
                        if (spi.Custom.class)
                            failoverSpi = new EmptyBean(spi.Custom.class);

                        break;
                    default:
                }

                if (failoverSpi)
                    spis.push(failoverSpi);
            });

            if (spis.length)
                cfg.arrayProperty('failoverSpi', 'failoverSpi', spis, 'org.apache.ignite.spi.failover.FailoverSpi');

            return cfg;
        }

        // Generate logger group.
        static clusterLogger(logger, cfg = this.igniteConfigurationBean()) {
            if (_.isNil(logger))
                return cfg;

            let loggerBean;

            switch (logger.kind) {
                case 'Log4j':
                    if (logger.Log4j && (logger.Log4j.mode === 'Default' || logger.Log4j.mode === 'Path' && !_.isEmpty(logger.Log4j.path))) {
                        loggerBean = new Bean('org.apache.ignite.logger.log4j.Log4JLogger',
                            'logger', logger.Log4j, DEFAULT.logger.Log4j);

                        if (loggerBean.valueOf('mode') === 'Path')
                            loggerBean.pathConstructorArgument('path');

                        loggerBean.enumProperty('level');
                    }

                    break;
                case 'Log4j2':
                    if (logger.Log4j2 && !_.isEmpty(logger.Log4j2.path)) {
                        loggerBean = new Bean('org.apache.ignite.logger.log4j2.Log4J2Logger',
                            'logger', logger.Log4j2, DEFAULT.logger.Log4j2);

                        loggerBean.pathConstructorArgument('path')
                            .enumProperty('level');
                    }

                    break;
                case 'Null':
                    loggerBean = new EmptyBean('org.apache.ignite.logger.NullLogger');

                    break;
                case 'Java':
                    loggerBean = new EmptyBean('org.apache.ignite.logger.java.JavaLogger');

                    break;
                case 'JCL':
                    loggerBean = new EmptyBean('org.apache.ignite.logger.jcl.JclLogger');

                    break;
                case 'SLF4J':
                    loggerBean = new EmptyBean('org.apache.ignite.logger.slf4j.Slf4jLogger');

                    break;
                case 'Custom':
                    if (logger.Custom && !_.isEmpty(logger.Custom.class))
                        loggerBean = new EmptyBean(logger.Custom.class);

                    break;
                default:
            }

            if (loggerBean)
                cfg.beanProperty('gridLogger', loggerBean);

            return cfg;
        }

        // Generate marshaller group.
        static clusterMarshaller(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            const marshaller = cluster.marshaller;

            if (marshaller && marshaller.kind) {
                let bean;

                switch (marshaller.kind) {
                    case 'OptimizedMarshaller':
                        bean = new Bean('org.apache.ignite.marshaller.optimized.OptimizedMarshaller', 'marshaller',
                            marshaller[marshaller.kind]);

                        bean.property('poolSize')
                            .property('requireSerializable');

                        break;

                    case 'JdkMarshaller':
                        bean = new Bean('org.apache.ignite.marshaller.jdk.JdkMarshaller', 'marshaller',
                            marshaller[marshaller.kind]);

                        break;

                    default:
                }

                if (bean)
                    cfg.beanProperty('marshaller', bean);
            }

            cfg.property('marshalLocalJobs')
                .property('marshallerCacheKeepAliveTime')
                .property('marshallerCacheThreadPoolSize', 'marshallerCachePoolSize');

            return cfg;
        }

        // Generate metrics group.
        static clusterMetrics(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            cfg.property('metricsExpireTime');
            cfg.property('metricsHistorySize');
            cfg.property('metricsLogFrequency');
            cfg.property('metricsUpdateFrequency');

            return cfg;
        }

        // Java code generator for cluster's SSL configuration.
        static clusterSsl = function(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            if (cluster.sslEnabled && !_.isNil(cluster.sslContextFactory)) {

                cluster.sslContextFactory.keyStorePassword =
                    $generatorCommon.isDefinedAndNotEmpty(cluster.sslContextFactory.keyStoreFilePath) ? 'ssl.key.storage.password' : null;
                cluster.sslContextFactory.trustStorePassword =
                    $generatorCommon.isDefinedAndNotEmpty(cluster.sslContextFactory.trustStoreFilePath) ? 'ssl.trust.storage.password' : null;

                const bean = new Bean('org.apache.ignite.ssl.SslContextFactory', 'sslContextFactory',
                    cluster.sslContextFactory);

                bean.property('keyAlgorithm')
                    .pathProperty('keyStoreFilePath')
                    // TODO IGNITE-2052 Should be get from secret properties
                    .property('keyStorePassword')
                    .property('keyStoreType')
                    .property('protocol');

                if ($generatorCommon.isDefinedAndNotEmpty(cluster.sslContextFactory.trustManagers))
                    bean.arrayProperty('trustManagers', 'trustManagers',
                        _.map(cluster.sslContextFactory.trustManagers, (clsName) => new EmptyBean(clsName)),
                        'javax.net.ssl.TrustManager');
                else {
                    bean.pathProperty('trustStoreFilePath')
                        // TODO IGNITE-2052 Should be get from secret properties
                        .property('trustStorePassword')
                        .property('trustStoreType');
                }

                cfg.beanProperty('sslContextFactory', bean);
            }

            return cfg;
        };

        // Generate swap group.
        static clusterSwap(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            if (cluster.swapSpaceSpi && cluster.swapSpaceSpi.kind === 'FileSwapSpaceSpi') {
                const bean = new Bean('org.apache.ignite.spi.swapspace.file.FileSwapSpaceSpi', 'swapSpaceSpi',
                    cluster.swapSpaceSpi.FileSwapSpaceSpi);

                bean.pathProperty('baseDirectory')
                    .property('readStripesNumber')
                    .property('maximumSparsity')
                    .property('maxWriteQueueSize')
                    .property('writeBufferSize');

                cfg.beanProperty('swapSpaceSpi', bean);
            }

            return cfg;
        }

        // Generate time group.
        static clusterTime(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            cfg.property('clockSyncSamples');
            cfg.property('clockSyncFrequency');
            cfg.property('timeServerPortBase');
            cfg.property('timeServerPortRange');

            return cfg;
        }

        // Generate thread pools group.
        static clusterPools(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            cfg.property('publicThreadPoolSize');
            cfg.property('systemThreadPoolSize');
            cfg.property('managementThreadPoolSize');
            cfg.property('igfsThreadPoolSize');
            cfg.property('rebalanceThreadPoolSize');

            return cfg;
        }

        // Generate transactions group.
        static clusterTransactions(transactionConfiguration, cfg = this.igniteConfigurationBean()) {
            const bean = new Bean('org.apache.ignite.configuration.TransactionConfiguration', 'transactionConfiguration',
                transactionConfiguration, DEFAULT.transactionConfiguration);

            bean.enumProperty('defaultTxConcurrency')
                .enumProperty('defaultTxIsolation')
                .property('defaultTxTimeout')
                .property('pessimisticTxLogLinger')
                .property('pessimisticTxLogSize')
                .property('txSerializableEnabled')
                .emptyBeanProperty('txManagerFactory');

            if (bean.nonEmpty())
                cfg.beanProperty('transactionConfiguration', bean);

            return cfg;
        }

        cacheConfiguration() {

        }
    }

    return ConfigurationGenerator;
}]];
