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

import DFLT_CLUSTER from 'app/data/cluster.json';
import DFLT_DOMAIN from 'app/data/domain.json';
import DFLT_CACHE from 'app/data/cache.json';
import DFLT_IGFS from 'app/data/igfs.json';
import DFLT_DIALECTS from 'app/data/dialects.json';

import { EmptyBean, Bean, MethodBean } from './Beans';

export default ['ConfigurationGenerator', ['JavaTypes', (JavaTypes) => {
    class ConfigurationGenerator {
        static igniteConfigurationBean(cluster) {
            return new Bean('org.apache.ignite.configuration.IgniteConfiguration', 'cfg', cluster, DFLT_CLUSTER);
        }

        static igfsConfigurationBean(igfs) {
            return new Bean('org.apache.ignite.configuration.FileSystemConfiguration', 'igfs', igfs, DFLT_IGFS);
        }

        static cacheConfigurationBean(cache) {
            return new Bean('org.apache.ignite.configuration.CacheConfiguration', 'cache', cache, DFLT_CACHE);
        }

        static domainConfigurationBean(domain) {
            return new Bean('', 'cache', domain, DFLT_DOMAIN);
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
                const discovery = new Bean('org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi', 'discovery',
                    cluster.discovery, DFLT_CLUSTER.discovery);

                let ipFinder;

                switch (discovery.valueOf('kind')) {
                    case 'Vm':
                        ipFinder = new Bean('org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder',
                            'ipFinder', cluster.discovery.Vm, DFLT_CLUSTER.discovery.Vm);

                        ipFinder.collectionProperty('addrs', 'addresses', cluster.discovery.Vm.addresses);

                        break;
                    case 'Multicast':
                        ipFinder = new Bean('org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder',
                            'ipFinder', cluster.discovery.Multicast, DFLT_CLUSTER.discovery.Multicast);

                        ipFinder.stringProperty('multicastGroup')
                            .intProperty('multicastPort')
                            .intProperty('responseWaitTime')
                            .intProperty('addressRequestAttempts')
                            .stringProperty('localAddress')
                            .collectionProperty('addrs', 'addresses', cluster.discovery.Multicast.addresses);

                        break;
                    case 'S3':
                        ipFinder = new Bean('org.apache.ignite.spi.discovery.tcp.ipfinder.s3.TcpDiscoveryS3IpFinder',
                            'ipFinder', cluster.discovery.S3, DFLT_CLUSTER.discovery.S3);

                        ipFinder.stringProperty('bucketName');

                        break;
                    case 'Cloud':
                        ipFinder = new Bean('org.apache.ignite.spi.discovery.tcp.ipfinder.cloud.TcpDiscoveryCloudIpFinder',
                            'ipFinder', cluster.discovery.Cloud, DFLT_CLUSTER.discovery.Cloud);

                        ipFinder.stringProperty('credential')
                            .pathProperty('credentialPath')
                            .stringProperty('identity')
                            .stringProperty('provider')
                            .collectionProperty('regions', 'regions', cluster.discovery.Cloud.regions)
                            .collectionProperty('zones', 'zones', cluster.discovery.Cloud.zones);

                        break;
                    case 'GoogleStorage':
                        ipFinder = new Bean('org.apache.ignite.spi.discovery.tcp.ipfinder.gce.TcpDiscoveryGoogleStorageIpFinder',
                            'ipFinder', cluster.discovery.GoogleStorage, DFLT_CLUSTER.discovery.GoogleStorage);

                        ipFinder.stringProperty('projectName')
                            .stringProperty('bucketName')
                            .pathProperty('serviceAccountP12FilePath')
                            .stringProperty('serviceAccountId');

                        break;
                    case 'Jdbc':
                        ipFinder = new Bean('org.apache.ignite.spi.discovery.tcp.ipfinder.jdbc.TcpDiscoveryJdbcIpFinder',
                            'ipFinder', cluster.discovery.Jdbc, DFLT_CLUSTER.discovery.Jdbc);

                        ipFinder.intProperty('initSchema');

                        if (ipFinder.includes('dataSourceBean', 'dialect'))
                            ipFinder.dataSource(ipFinder.valueOf('dataSourceBean'), 'dataSource', ipFinder.valueOf('dialect'));

                        break;
                    case 'SharedFs':
                        ipFinder = new Bean('org.apache.ignite.spi.discovery.tcp.ipfinder.sharedfs.TcpDiscoverySharedFsIpFinder',
                            'ipFinder', cluster.discovery.SharedFs, DFLT_CLUSTER.discovery.SharedFs);

                        ipFinder.pathProperty('path');

                        break;
                    case 'ZooKeeper':
                        const src = cluster.discovery.ZooKeeper;
                        const dflt = DFLT_CLUSTER.discovery.ZooKeeper;

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
                                        .intConstructorArgument('baseSleepTimeMs')
                                        .intConstructorArgument('maxRetries')
                                        .intConstructorArgument('maxSleepMs');

                                    break;
                                case 'BoundedExponentialBackoff':
                                    retryPolicyBean = new Bean('org.apache.curator.retry.BoundedExponentialBackoffRetry',
                                        null, policy.BoundedExponentialBackoffRetry, dflt.BoundedExponentialBackoffRetry)
                                        .intConstructorArgument('baseSleepTimeMs')
                                        .intConstructorArgument('maxSleepTimeMs')
                                        .intConstructorArgument('maxRetries');

                                    break;
                                case 'UntilElapsed':
                                    retryPolicyBean = new Bean('org.apache.curator.retry.RetryUntilElapsed', null,
                                        policy.UntilElapsed, dflt.UntilElapsed)
                                        .intConstructorArgument('maxElapsedTimeMs')
                                        .intConstructorArgument('sleepMsBetweenRetries');

                                    break;

                                case 'NTimes':
                                    retryPolicyBean = new Bean('org.apache.curator.retry.RetryNTimes', null,
                                        policy.NTimes, dflt.NTimes)
                                        .intConstructorArgument('n')
                                        .intConstructorArgument('sleepMsBetweenRetries');

                                    break;
                                case 'OneTime':
                                    retryPolicyBean = new Bean('org.apache.curator.retry.RetryOneTime', null,
                                        policy.OneTime, dflt.OneTime)
                                        .intConstructorArgument('sleepMsBetweenRetry');

                                    break;
                                case 'Forever':
                                    retryPolicyBean = new Bean('org.apache.curator.retry.RetryForever', null,
                                        policy.Forever, dflt.Forever)
                                        .intConstructorArgument('retryIntervalMs');

                                    break;
                                case 'Custom':
                                    if (_.nonEmpty(policy.Custom.className))
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
                atomics, DFLT_CLUSTER.atomics);

            acfg.enumProperty('cacheMode')
                .intProperty('atomicSequenceReserveSize');

            if (acfg.valueOf('cacheMode') === 'PARTITIONED')
                acfg.intProperty('backups');

            if (acfg.isEmpty())
                return cfg;

            cfg.beanProperty('atomicConfiguration', acfg);

            return cfg;
        }

        // Generate binary group.
        static clusterBinary(binary, cfg = this.igniteConfigurationBean()) {
            const binaryCfg = new Bean('org.apache.ignite.configuration.BinaryConfiguration', 'binaryCfg',
                binary, DFLT_CLUSTER.binary);

            binaryCfg.emptyBeanProperty('idMapper')
                .emptyBeanProperty('nameMapper')
                .emptyBeanProperty('serializer');

            const typeCfgs = [];

            _.forEach(binary.typeConfigurations, (type) => {
                const typeCfg = new MethodBean('org.apache.ignite.binary.BinaryTypeConfiguration',
                    JavaTypes.toJavaName('binaryType', type.typeName), type, DFLT_CLUSTER.binary.typeConfigurations);

                typeCfg.stringProperty('typeName')
                    .emptyBeanProperty('idMapper')
                    .emptyBeanProperty('nameMapper')
                    .emptyBeanProperty('serializer')
                    .intProperty('enum');

                if (typeCfg.nonEmpty())
                    typeCfgs.push(typeCfg);
            });

            binaryCfg.collectionProperty('types', 'typeConfigurations', typeCfgs, 'java.util.Collection',
                'org.apache.ignite.binary.BinaryTypeConfiguration');

            binaryCfg.intProperty('compactFooter');

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

            cfg.arrayProperty('cacheKeyConfiguration', 'keyConfigurations', items,
                'org.apache.ignite.cache.CacheKeyConfiguration');

            return cfg;
        }

        // Generate collision group.
        static clusterCollision(collision, cfg = this.igniteConfigurationBean()) {
            let colSpi;

            switch (collision.kind) {
                case 'JobStealing':
                    colSpi = new Bean('org.apache.ignite.spi.collision.jobstealing.JobStealingCollisionSpi',
                        'colSpi', collision.JobStealing, DFLT_CLUSTER.collision.JobStealing);

                    colSpi.intProperty('activeJobsThreshold')
                        .intProperty('waitJobsThreshold')
                        .intProperty('messageExpireTime')
                        .intProperty('maximumStealingAttempts')
                        .intProperty('stealingEnabled')
                        .emptyBeanProperty('externalCollisionListener')
                        .mapProperty('stealingAttrs', 'stealingAttributes');

                    break;
                case 'FifoQueue':
                    colSpi = new Bean('org.apache.ignite.spi.collision.fifoqueue.FifoQueueCollisionSpi',
                        'colSpi', collision.FifoQueue, DFLT_CLUSTER.collision.FifoQueue);

                    colSpi.intProperty('parallelJobsNumber')
                        .intProperty('waitingJobsNumber');

                    break;
                case 'PriorityQueue':
                    colSpi = new Bean('org.apache.ignite.spi.collision.priorityqueue.PriorityQueueCollisionSpi',
                        'colSpi', collision.PriorityQueue, DFLT_CLUSTER.collision.PriorityQueue);

                    colSpi.intProperty('parallelJobsNumber')
                        .intProperty('waitingJobsNumber')
                        .intProperty('priorityAttributeKey')
                        .intProperty('jobPriorityAttributeKey')
                        .intProperty('defaultPriority')
                        .intProperty('starvationIncrement')
                        .intProperty('starvationPreventionEnabled');

                    break;
                case 'Custom':
                    colSpi = new Bean(collision.Custom.class,
                        'colSpi', collision.PriorityQueue, DFLT_CLUSTER.collision.PriorityQueue);

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
                cluster.communication, DFLT_CLUSTER.communication);

            commSpi.emptyBeanProperty('listener')
                .stringProperty('localAddress')
                .intProperty('localPort')
                .intProperty('localPortRange')
                .intProperty('sharedMemoryPort')
                .intProperty('directBuffer')
                .intProperty('directSendBuffer')
                .intProperty('idleConnectionTimeout')
                .intProperty('connectTimeout')
                .intProperty('maxConnectTimeout')
                .intProperty('reconnectCount')
                .intProperty('socketSendBuffer')
                .intProperty('socketReceiveBuffer')
                .intProperty('messageQueueLimit')
                .intProperty('slowClientQueueLimit')
                .intProperty('tcpNoDelay')
                .intProperty('ackSendThreshold')
                .intProperty('unacknowledgedMessagesBufferSize')
                .intProperty('socketWriteTimeout')
                .intProperty('selectorsCount')
                .emptyBeanProperty('addressResolver');

            if (commSpi.nonEmpty())
                cfg.beanProperty('communicationSpi', commSpi);

            cfg.intProperty('networkTimeout')
                .intProperty('networkSendRetryDelay')
                .intProperty('networkSendRetryCount')
                .intProperty('discoveryStartupDelay');

            return cfg;
        }

        // Generate REST access configuration.
        static clusterConnector(connector, cfg = this.igniteConfigurationBean()) {
            const connCfg = new Bean('org.apache.ignite.configuration.ConnectorConfiguration',
                'connectorConfiguration', connector, DFLT_CLUSTER.connector);

            if (connCfg.valueOf('enabled')) {
                connCfg.pathProperty('jettyPath')
                    .stringProperty('host')
                    .intProperty('port')
                    .intProperty('portRange')
                    .intProperty('idleTimeout')
                    .intProperty('idleQueryCursorTimeout')
                    .intProperty('idleQueryCursorCheckFrequency')
                    .intProperty('receiveBufferSize')
                    .intProperty('sendBufferSize')
                    .intProperty('sendQueueLimit')
                    .intProperty('directBuffer')
                    .intProperty('noDelay')
                    .intProperty('selectorCount')
                    .intProperty('threadPoolSize')
                    .emptyBeanProperty('messageInterceptor')
                    .stringProperty('secretKey');

                if (connCfg.valueOf('sslEnabled')) {
                    connCfg.intProperty('sslClientAuth')
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
                .intProperty('peerClassLoadingEnabled');

            if (cfg.valueOf('peerClassLoadingEnabled')) {
                cfg.intProperty('peerClassLoadingMissedResourcesCacheSize')
                    .intProperty('peerClassLoadingThreadPoolSize')
                    .arrayProperty('p2pLocClsPathExcl', 'peerClassLoadingLocalClassPathExclude',
                       cluster.peerClassLoadingLocalClassPathExclude);
            }

            return cfg;
        }

        // Generate discovery group.
        static clusterDiscovery(discovery, cfg = this.igniteConfigurationBean()) {
            if (discovery) {
                let discoveryCfg = cfg.findProperty('discovery');

                if (_.isNil(discoveryCfg)) {
                    discoveryCfg = new Bean('org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi', 'discovery',
                        discovery, DFLT_CLUSTER.discovery);
                }

                discoveryCfg.stringProperty('localAddress')
                    .intProperty('localPort')
                    .intProperty('localPortRange')
                    .emptyBeanProperty('addressResolver')
                    .intProperty('socketTimeout')
                    .intProperty('ackTimeout')
                    .intProperty('maxAckTimeout')
                    .intProperty('networkTimeout')
                    .intProperty('joinTimeout')
                    .intProperty('threadPriority')
                    .intProperty('heartbeatFrequency')
                    .intProperty('maxMissedHeartbeats')
                    .intProperty('maxMissedClientHeartbeats')
                    .intProperty('topHistorySize')
                    .emptyBeanProperty('listener')
                    .emptyBeanProperty('dataExchange')
                    .emptyBeanProperty('metricsProvider')
                    .intProperty('reconnectCount')
                    .intProperty('statisticsPrintFrequency')
                    .intProperty('ipFinderCleanFrequency')
                    .emptyBeanProperty('authenticator')
                    .intProperty('forceServerMode')
                    .intProperty('clientReconnectDisabled');

                if (discoveryCfg.nonEmpty())
                    cfg.beanProperty('discoverySpi', discoveryCfg);
            }

            return cfg;
        }

        // Generate events group.
        static clusterEvents(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            if (_.nonEmpty(cluster.includeEventTypes))
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
                            'failoverSpi', spi.JobStealing, DFLT_CLUSTER.failoverSpi.JobStealing);

                        failoverSpi.intProperty('maximumFailoverAttempts');

                        break;
                    case 'Never':
                        failoverSpi = new Bean('org.apache.ignite.spi.failover.never.NeverFailoverSpi',
                            'failoverSpi', spi.Never);

                        break;
                    case 'Always':
                        failoverSpi = new Bean('org.apache.ignite.spi.failover.always.AlwaysFailoverSpi',
                            'failoverSpi', spi.Always, DFLT_CLUSTER.failoverSpi.Always);

                        failoverSpi.intProperty('maximumFailoverAttempts');

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
                    if (logger.Log4j && (logger.Log4j.mode === 'Default' || logger.Log4j.mode === 'Path' && _.nonEmpty(logger.Log4j.path))) {
                        loggerBean = new Bean('org.apache.ignite.logger.log4j.Log4JLogger',
                            'logger', logger.Log4j, DFLT_CLUSTER.logger.Log4j);

                        if (loggerBean.valueOf('mode') === 'Path')
                            loggerBean.pathConstructorArgument('path');

                        loggerBean.enumProperty('level');
                    }

                    break;
                case 'Log4j2':
                    if (logger.Log4j2 && _.nonEmpty(logger.Log4j2.path)) {
                        loggerBean = new Bean('org.apache.ignite.logger.log4j2.Log4J2Logger',
                            'logger', logger.Log4j2, DFLT_CLUSTER.logger.Log4j2);

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
                    if (logger.Custom && _.nonEmpty(logger.Custom.class))
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

                        bean.intProperty('poolSize')
                            .intProperty('requireSerializable');

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

            cfg.intProperty('marshalLocalJobs')
                .intProperty('marshallerCacheKeepAliveTime')
                .intProperty('marshallerCacheThreadPoolSize', 'marshallerCachePoolSize');

            return cfg;
        }

        // Generate metrics group.
        static clusterMetrics(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            cfg.intProperty('metricsExpireTime')
                .intProperty('metricsHistorySize')
                .intProperty('metricsLogFrequency')
                .intProperty('metricsUpdateFrequency');

            return cfg;
        }

        // Generate ODBC group.
        static clusterODBC(odbc, cfg = this.igniteConfigurationBean()) {
            const bean = new Bean('org.apache.ignite.configuration.OdbcConfiguration', 'odbcConfiguration', odbc, DFLT_CLUSTER.odbcConfiguration);

            bean.stringProperty('endpointAddress')
                .intProperty('maxOpenCursors');

            cfg.beanProperty('odbcConfiguration', bean);

            return cfg;
        }

        // Java code generator for cluster's SSL configuration.
        static clusterSsl(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            if (cluster.sslEnabled && _.nonNil(cluster.sslContextFactory)) {
                const bean = new Bean('org.apache.ignite.ssl.SslContextFactory', 'sslContextFactory',
                    cluster.sslContextFactory);

                bean.intProperty('keyAlgorithm')
                    .pathProperty('keyStoreFilePath');

                if (_.nonEmpty(bean.valueOf('keyStoreFilePath')))
                    bean.propertyChar('keyStorePassword', 'ssl.key.storage.password');

                bean.intProperty('keyStoreType')
                    .intProperty('protocol');

                if (_.nonEmpty(cluster.sslContextFactory.trustManagers)) {
                    bean.arrayProperty('trustManagers', 'trustManagers',
                        _.map(cluster.sslContextFactory.trustManagers, (clsName) => new EmptyBean(clsName)),
                        'javax.net.ssl.TrustManager');
                }
                else {
                    bean.pathProperty('trustStoreFilePath');

                    if (_.nonEmpty(bean.valueOf('trustStoreFilePath')))
                        bean.propertyChar('trustStorePassword', 'ssl.trust.storage.password');

                    bean.intProperty('trustStoreType');
                }

                cfg.beanProperty('sslContextFactory', bean);
            }

            return cfg;
        }

        // Generate swap group.
        static clusterSwap(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            if (cluster.swapSpaceSpi && cluster.swapSpaceSpi.kind === 'FileSwapSpaceSpi') {
                const bean = new Bean('org.apache.ignite.spi.swapspace.file.FileSwapSpaceSpi', 'swapSpaceSpi',
                    cluster.swapSpaceSpi.FileSwapSpaceSpi);

                bean.pathProperty('baseDirectory')
                    .intProperty('readStripesNumber')
                    .intProperty('maximumSparsity')
                    .intProperty('maxWriteQueueSize')
                    .intProperty('writeBufferSize');

                cfg.beanProperty('swapSpaceSpi', bean);
            }

            return cfg;
        }

        // Generate time group.
        static clusterTime(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            cfg.intProperty('clockSyncSamples')
                .intProperty('clockSyncFrequency')
                .intProperty('timeServerPortBase')
                .intProperty('timeServerPortRange');

            return cfg;
        }

        // Generate thread pools group.
        static clusterPools(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            cfg.intProperty('publicThreadPoolSize')
                .intProperty('systemThreadPoolSize')
                .intProperty('managementThreadPoolSize')
                .intProperty('igfsThreadPoolSize')
                .intProperty('rebalanceThreadPoolSize');

            return cfg;
        }

        // Generate transactions group.
        static clusterTransactions(transactionConfiguration, cfg = this.igniteConfigurationBean()) {
            const bean = new Bean('org.apache.ignite.configuration.TransactionConfiguration', 'transactionConfiguration',
                transactionConfiguration, DFLT_CLUSTER.transactionConfiguration);

            bean.enumProperty('defaultTxConcurrency')
                .enumProperty('defaultTxIsolation')
                .intProperty('defaultTxTimeout')
                .intProperty('pessimisticTxLogLinger')
                .intProperty('pessimisticTxLogSize')
                .intProperty('txSerializableEnabled')
                .emptyBeanProperty('txManagerFactory');

            if (bean.nonEmpty())
                cfg.beanProperty('transactionConfiguration', bean);

            return cfg;
        }

        // Generate user attributes group.
        static clusterUserAttributes(cluster, cfg = this.igniteConfigurationBean(cluster)) {
            cfg.mapProperty('attributes', 'attributes', 'userAttributes');

            return cfg;
        }

        // Generate domain model for general group.
        static domainModelGeneral(domain, cfg = this.domainConfigurationBean(domain)) {
            switch (cfg.valueOf('queryMetadata')) {
                case 'Annotations':
                    if (_.nonNil(domain.keyType) && _.nonNil(domain.valueType))
                        cfg.varArgProperty('indexedTypes', 'indexedTypes', [domain.keyType, domain.valueType], 'java.lang.Class');

                    break;
                case 'Configuration':
                    cfg.classProperty('keyType')
                        .classProperty('valueType');

                    break;
                default:
            }

            return cfg;
        }

        // Generate domain model indexes.
        static _domainModelQueryIndexes(domain, cfg) {
            const indexes = domain.indexes;

            if (indexes && indexes.length > 0) {
                const indexBeans = [];

                _.forEach(indexes, function(index) {
                    const bean = new Bean('org.apache.ignite.cache.QueryIndex', 'index', index,
                        {indexType: {clsName: 'org.apache.ignite.cache.QueryIndexType'}})
                        .stringProperty('name')
                        // TODO IGNITE-2052 Enum fields is not generated in array bean.
                        .enumProperty('indexType');

                    const fields = index.fields;

                    if (fields && fields.length > 0)
                    // TODO IGNITE-2052 Differ from required field names.
                        bean.mapProperty('fields');

                    indexBeans.push(bean);
                });

                cfg.arrayProperty('indexes', 'indexes', indexBeans, 'org.apache.ignite.cache.QueryIndex');
            }
        }

        // Generate domain model for query group.
        static domainModelQuery(domain, cfg = this.domainConfigurationBean(domain)) {
            if (cfg.valueOf('queryMetadata') === 'Configuration') {
                cfg.mapProperty('fields', 'fields')
                    .mapProperty('aliases', 'aliases');

                this._domainModelQueryIndexes(domain, cfg);
            }

            return cfg;
        }


        // Generate domain model db fields.
        static _domainModelDatabaseFields(domain, cfg, fieldProp) {
            const fields = domain[fieldProp];

            if (fields && fields.length > 0) {
                const fieldBeans = [];

                _.forEach(fields, (field) => {
                    const fieldBean = new Bean('org.apache.ignite.cache.store.jdbc.JdbcTypeField', 'typeField', field,
                        {databaseFieldType: {clsName: 'java.sql.Types'}})
                        .stringProperty('databaseFieldName')
                        // TODO IGNITE-2052 There was generation something like <util:constant static-field="java.sql.Types.INTEGER"/>
                        .enumProperty('databaseFieldType')
                        .stringProperty('javaFieldName')
                        // TODO IGNITE-2052 Should be a .class property
                        .intProperty('javaFieldType');

                    fieldBeans.push(fieldBean);
                });

                cfg.arrayProperty(fieldProp, fieldProp, fieldBeans, 'org.apache.ignite.cache.store.jdbc.JdbcTypeField');
            }
        }

        // Generate domain model for store group.
        static domainStore(domain, cfg = this.domainConfigurationBean(domain)) {
            cfg.intProperty('databaseSchema')
                .intProperty('databaseTable');

            this._domainModelDatabaseFields(domain, cfg, 'keyFields');
            this._domainModelDatabaseFields(domain, cfg, 'valueFields');

            return cfg;
        }

        cacheConfiguration() {

        }

        /**
         * Generate eviction policy object.
         * @param {Object} cfg Parent configuration.
         * @param {String} name Property name.
         * @param {Object} src Source.
         * @param {Object} dflt Default.
         * @returns {Object} Parent configuration.
         * @private
         */
        static _evictionPolicy(cfg, name, src, dflt) {
            let bean;

            switch (src.kind) {
                case 'LRU':
                    bean = new Bean('org.apache.ignite.cache.eviction.lru.LruEvictionPolicy', 'evictionPlc',
                        src.LRU, dflt.LRU);

                    break;
                case 'FIFO':
                    bean = new Bean('org.apache.ignite.cache.eviction.fifo.FifoEvictionPolicy', 'evictionPlc',
                        src.FIFO, dflt.FIFO);

                    break;
                case 'SORTED':
                    bean = new Bean('org.apache.ignite.cache.eviction.sorted.SortedEvictionPolicy', 'evictionPlc',
                        src.SORTED, dflt.SORTED);

                    break;
                default:
                    return cfg;
            }

            bean.intProperty('batchSize')
                .intProperty('maxMemorySize')
                .intProperty('maxSize');

            cfg.beanProperty(name, bean);

            return cfg;
        }

        // Generate cache general group.
        static cacheGeneral(cache, cfg = this.cacheConfigurationBean(cache)) {
            cfg.stringProperty('name')
                .enumProperty('cacheMode')
                .enumProperty('atomicityMode');

            if (cfg.valueOf('cacheMode') === 'PARTITIONED' && cfg.valueOf('backups')) {
                cfg.intProperty('backups')
                    .intProperty('readFromBackup');
            }

            cfg.intProperty('copyOnRead');

            if (cfg.valueOf('cacheMode') === 'PARTITIONED' && cfg.valueOf('atomicityMode') === 'TRANSACTIONAL')
                cfg.intProperty('invalidate');

            return cfg;
        }

        // Generate cache memory group.
        static cacheMemory(cache, cfg = this.cacheConfigurationBean(cache)) {
            cfg.enumProperty('memoryMode');

            if (cfg.valueOf('memoryMode') !== 'OFFHEAP_VALUES')
                cfg.intProperty('offHeapMaxMemory');

            this._evictionPolicy(cfg, 'evictionPolicy', cache.evictionPolicy, DFLT_CACHE.evictionPolicy);

            cfg.intProperty('startSize')
                .intProperty('swapEnabled');

            return cfg;
        }

        // Generate cache queries & Indexing group.
        static cacheQuery(cache, domains, cfg = this.cacheConfigurationBean(cache)) {
            const indexedTypes = _.reduce(domains, (acc, domain) => {
                if (domain.queryMetadata === 'Annotations')
                    acc.push(domain.keyType, domain.valueType);

                return acc;
            }, []);

            cfg.stringProperty('sqlSchema')
                .intProperty('sqlOnheapRowCacheSize')
                .intProperty('longQueryWarningTimeout')
                .arrayProperty('indexedTypes', 'indexedTypes', indexedTypes, 'java.lang.Class')
                .arrayProperty('sqlFunctionClasses', 'sqlFunctionClasses', cache.sqlFunctionClasses, 'java.lang.Class')
                .intProperty('snapshotableIndex')
                .intProperty('sqlEscapeAll');

            return cfg;
        }

        // Generate cache store group.
        static cacheStore(cache, domains, cfg = this.cacheConfigurationBean(cache)) {
            if (cache.cacheStoreFactory && cache.cacheStoreFactory.kind) {
                const factoryKind = cache.cacheStoreFactory.kind;

                const storeFactory = cache.cacheStoreFactory[factoryKind];

                let bean;

                if (storeFactory) {
                    if (factoryKind === 'CacheJdbcPojoStoreFactory') {
                        // TODO IGNITE-2052 implement generation of correct store factory.
                        bean = new Bean('org.apache.ignite.cache.store.jdbc.CacheJdbcPojoStoreFactory', 'cacheStoreFactory', storeFactory);

                        const dialectClsName = DFLT_DIALECTS[storeFactory.dialect] ||
                            'Unknown database: ' + (storeFactory.dialect || 'Choose JDBC dialect');

                        bean.intProperty('dataSourceBean')
                            .beanProperty('dialect', new EmptyBean(dialectClsName));

                        const domainConfigs = _.filter(domains, (domain) => _.nonNil(domain.databaseTable));

                        if (domainConfigs.length > 0) {
                            const types = [];

                            // TODO IGNITE-2052 In Java generation every type should be generated in separate method.
                            _.forEach(domainConfigs, (domain) => {
                                const typeBean = new MethodBean('org.apache.ignite.cache.store.jdbc.JdbcType', 'type',
                                    angular.merge({}, domain, {cacheName: cache.name}))
                                    .stringProperty('cacheName')
                                    .intProperty('keyType')
                                    .intProperty('valueType');

                                this.domainStore(domain, typeBean);

                                types.push(typeBean);
                            });

                            bean.arrayProperty('types', 'types', types, 'org.apache.ignite.cache.store.jdbc.JdbcType');
                        }
                    }
                    else if (factoryKind === 'CacheJdbcBlobStoreFactory') {
                        bean = new Bean('org.apache.ignite.cache.store.jdbc.CacheJdbcBlobStoreFactory', 'cacheStoreFactory', storeFactory);

                        if (storeFactory.connectVia === 'DataSource')
                            bean.intProperty('dataSourceBean');
                        else {
                            storeFactory.password = '${ds.' + storeFactory.user + '.password}';

                            cfg.intProperty('connectionUrl')
                                .intProperty('user')
                                .intProperty('password');
                        }

                        bean.intProperty('initSchema')
                            .stringProperty('createTableQuery')
                            .stringProperty('loadQuery')
                            .stringProperty('insertQuery')
                            .stringProperty('updateQuery')
                            .stringProperty('deleteQuery');
                    }
                    else {
                        bean = new Bean('org.apache.ignite.cache.store.hibernate.CacheHibernateBlobStoreFactory', 'cacheStoreFactory', storeFactory);

                        // TODO IGNITE-2052 Should be translated to Properties variable.
                        bean.mapProperty('hibernateProperties');
                    }

                    // TODO IGNITE-2052 Common generation of datasources.
                    // if (storeFactory.dataSourceBean && (storeFactory.connectVia ? (storeFactory.connectVia === 'DataSource' ? storeFactory.dialect : null) : storeFactory.dialect)) {
                    //    if (!_.find(res.datasources, { dataSourceBean: storeFactory.dataSourceBean})) {
                    //        res.datasources.push({
                    //            dataSourceBean: storeFactory.dataSourceBean,
                    //            dialect: storeFactory.dialect
                    //        });
                    //    }
                    // }

                    if (bean)
                        cfg.beanProperty('cacheStoreFactory', bean);
                }
            }

            cfg.intProperty('storeKeepBinary')
                .intProperty('loadPreviousValue')
                .intProperty('readThrough')
                .intProperty('writeThrough');

            if (cache.writeBehindEnabled) {
                cfg.intProperty('writeBehindEnabled')
                    .intProperty('writeBehindBatchSize')
                    .intProperty('writeBehindFlushSize')
                    .intProperty('writeBehindFlushFrequency')
                    .intProperty('writeBehindFlushThreadCount');
            }

            return cfg;
        }

        // Generate cache concurrency control group.
        static cacheConcurrency(cache, cfg = this.cacheConfigurationBean(cache)) {
            cfg.intProperty('maxConcurrentAsyncOperations')
                .intProperty('defaultLockTimeout')
                .enumProperty('atomicWriteOrderMode')
                .enumProperty('writeSynchronizationMode');

            return cfg;
        }

        // Generate cache node filter group.
        static cacheNodeFilter(cache, igfss, cfg = this.cacheConfigurationBean(cache)) {
            const kind = _.get(cache, 'nodeFilter.kind');

            if (kind && cache.nodeFilter[kind]) {
                let bean = null;

                switch (kind) {
                    case 'IGFS':
                        const foundIgfs = _.find(igfss, (igfs) => igfs._id === cache.nodeFilter.IGFS.igfs);

                        if (foundIgfs) {
                            bean = new Bean('org.apache.ignite.internal.processors.igfs.IgfsNodePredicate', 'nodeFilter', foundIgfs)
                                .stringConstructorArgument('name');
                        }

                        break;
                    case 'Custom':
                        bean = new Bean(cache.nodeFilter.Custom.className, 'nodeFilter');

                        break;
                    default:
                        return cfg;
                }

                if (bean)
                    cfg.beanProperty('nodeFilter', bean);
            }

            return cfg;
        }

        // Generate cache rebalance group.
        static cacheRebalance(cache, cfg = this.cacheConfigurationBean(cache)) {
            if (cache.cacheMode !== 'LOCAL') {
                cfg.enumProperty('rebalanceMode')
                    .intProperty('rebalanceThreadPoolSize')
                    .intProperty('rebalanceBatchSize')
                    .intProperty('rebalanceBatchesPrefetchCount')
                    .intProperty('rebalanceOrder')
                    .intProperty('rebalanceDelay')
                    .intProperty('rebalanceTimeout')
                    .intProperty('rebalanceThrottle');
            }

            if (cache.igfsAffinnityGroupSize) {
                const bean = new Bean('org.apache.ignite.igfs.IgfsGroupDataBlocksKeyMapper', 'affinityMapper', cache)
                    .intConstructorArgument('igfsAffinnityGroupSize');

                cfg.beanProperty('affinityMapper', bean);
            }

            return cfg;
        }

        // Generate server near cache group.
        static cacheServerNearCache(cache, cfg = this.cacheConfigurationBean(cache)) {
            if (cache.cacheMode === 'PARTITIONED' && cache.nearCacheEnabled) {
                const bean = new Bean('org.apache.ignite.configuration.NearCacheConfiguration', 'nearConfiguration',
                    cache.nearConfiguration, {nearStartSize: 375000});

                bean.intProperty('nearStartSize');

                this._evictionPolicy(bean, 'nearEvictionPolicy',
                    bean.valueOf('nearEvictionPolicy'), DFLT_CACHE.evictionPolicy);

                cfg.beanProperty('nearConfiguration', bean);
            }

            return cfg;
        }

        // Generate cache statistics group.
        static cacheStatistics(cache, cfg = this.cacheConfigurationBean(cache)) {
            cfg.intProperty('statisticsEnabled')
                .intProperty('managementEnabled');

            return cfg;
        }

        // Generate IGFS general group.
        static igfsGeneral(igfs, cfg = this.igfsConfigurationBean(igfs)) {
            if (_.isEmpty(igfs.name))
                return cfg;

            cfg.intProperty('name')
                .virtualProperty('dataCacheName', igfs.name + '-data')
                .virtualProperty('metaCacheName', igfs.name + '-meta')
                .enumProperty('defaultMode');

            return cfg;
        }

        // Generate IGFS secondary file system group.
        static igfsSecondFS(igfs, cfg = this.igfsConfigurationBean(igfs)) {
            if (igfs.secondaryFileSystemEnabled) {
                const secondFs = igfs.secondaryFileSystem || {};

                const bean = new Bean('org.apache.ignite.hadoop.fs.IgniteHadoopIgfsSecondaryFileSystem',
                    'secondaryFileSystem', secondFs, DFLT_IGFS.secondaryFileSystem);

                bean.stringProperty('userName', 'defaultUserName');

                const factoryBean = new Bean('org.apache.ignite.hadoop.fs.CachingHadoopFileSystemFactory',
                    'fac', secondFs);

                factoryBean.stringProperty('uri')
                    .pathProperty('cfgPath', 'configPaths');

                bean.beanProperty('fileSystemFactory', factoryBean);

                cfg.beanProperty('secondaryFileSystem', bean);
            }

            return cfg;
        }

        // Generate IGFS IPC group.
        static igfsIPC(igfs, cfg = this.igfsConfigurationBean(igfs)) {
            if (igfs.ipcEndpointEnabled) {
                const bean = new Bean('org.apache.ignite.igfs.IgfsIpcEndpointConfiguration', 'ipcEndpointConfiguration',
                    igfs.ipcEndpointConfiguration, DFLT_IGFS.ipcEndpointConfiguration);

                bean.enumProperty('type')
                    .stringProperty('host')
                    .intProperty('port')
                    .intProperty('memorySize')
                    .pathProperty('tokenDirectoryPath')
                    .intProperty('threadCount');

                cfg.beanProperty('ipcEndpointConfiguration', bean);
            }

            return cfg;
        }

        // Generate IGFS fragmentizer group.
        static igfsFragmentizer(igfs, cfg = this.igfsConfigurationBean(igfs)) {
            if (igfs.fragmentizerEnabled) {
                cfg.intProperty('fragmentizerConcurrentFiles')
                    .intProperty('fragmentizerThrottlingBlockLength')
                    .intProperty('fragmentizerThrottlingDelay');
            }
            else
                cfg.intProperty('fragmentizerEnabled');

            return cfg;
        }

        // Generate IGFS Dual mode group.
        static igfsDualMode(igfs, cfg = this.igfsConfigurationBean(igfs)) {
            cfg.intProperty('dualModeMaxPendingPutsSize')
                .emptyBeanProperty('dualModePutExecutorService')
                .intProperty('dualModePutExecutorServiceShutdown');

            return cfg;
        }

        // Generate IGFS miscellaneous group.
        static igfsMisc(igfs, cfg = this.igfsConfigurationBean(igfs)) {
            cfg.intProperty('blockSize')
                .intProperty('streamBufferSize')
                .intProperty('maxSpaceSize')
                .intProperty('maximumTaskRangeLength')
                .intProperty('managementPort')
                .intProperty('perNodeBatchSize')
                .intProperty('perNodeParallelBatchCount')
                .intProperty('prefetchBlocks')
                .intProperty('sequentialReadsBeforePrefetch')
                .intProperty('trashPurgeTimeout')
                .intProperty('colocateMetadata')
                .intProperty('relaxedConsistency')
                .mapProperty('pathModes', 'pathModes');

            return cfg;
        }
    }

    return ConfigurationGenerator;
}]];
