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

// Controller for Caches screen.
export default ['cachesController', [
    '$scope', '$http', '$state', '$filter', '$timeout', 'IgniteLegacyUtils', 'IgniteMessages', 'IgniteConfirm', 'IgniteClone', 'IgniteLoading', 'IgniteModelNormalizer', 'IgniteUnsavedChangesGuard', 'igniteConfigurationResource',
    function($scope, $http, $state, $filter, $timeout, LegacyUtils, Messages, Confirm, Clone, Loading, ModelNormalizer, UnsavedChangesGuard, Resource) {
        UnsavedChangesGuard.install($scope);

        const emptyCache = {empty: true};

        let __original_value;

        const blank = {
            evictionPolicy: {},
            cacheStoreFactory: {},
            nearConfiguration: {}
        };

        // We need to initialize backupItem with empty object in order to properly used from angular directives.
        $scope.backupItem = emptyCache;

        $scope.ui = LegacyUtils.formUI();
        $scope.ui.activePanels = [0];
        $scope.ui.topPanels = [0, 1, 2, 3];

        $scope.hidePopover = LegacyUtils.hidePopover;
        $scope.saveBtnTipText = LegacyUtils.saveBtnTipText;
        $scope.widthIsSufficient = LegacyUtils.widthIsSufficient;
        $scope.offHeapMode = 'DISABLED';

        const showPopoverMessage = LegacyUtils.showPopoverMessage;

        $scope.contentVisible = function() {
            const item = $scope.backupItem;

            return !item.empty && (!item._id || _.find($scope.displayedRows, {_id: item._id}));
        };

        $scope.toggleExpanded = function() {
            $scope.ui.expanded = !$scope.ui.expanded;

            LegacyUtils.hidePopover();
        };

        $scope.caches = [];
        $scope.domains = [];

        function _cacheLbl(cache) {
            return cache.name + ', ' + cache.cacheMode + ', ' + cache.atomicityMode;
        }

        function selectFirstItem() {
            if ($scope.caches.length > 0)
                $scope.selectItem($scope.caches[0]);
        }

        function cacheDomains(item) {
            return _.reduce($scope.domains, function(memo, domain) {
                if (item && _.includes(item.domains, domain.value))
                    memo.push(domain.meta);

                return memo;
            }, []);
        }

        const setOffHeapMode = (item) => {
            if (_.isNil(item.offHeapMaxMemory))
                return;

            return item.offHeapMode = Math.sign(item.offHeapMaxMemory);
        };

        const setOffHeapMaxMemory = (value) => {
            const item = $scope.backupItem;

            if (_.isNil(value) || value <= 0)
                return item.offHeapMaxMemory = value;

            item.offHeapMaxMemory = item.offHeapMaxMemory > 0 ? item.offHeapMaxMemory : null;
        };

        Loading.start('loadingCachesScreen');

        // When landing on the page, get caches and show them.
        Resource.read()
            .then(({spaces, clusters, caches, domains, igfss}) => {
                const validFilter = $filter('domainsValidation');

                $scope.spaces = spaces;
                $scope.caches = caches;
                $scope.igfss = _.map(igfss, (igfs) => ({
                    label: igfs.name,
                    value: igfs._id,
                    igfs
                }));

                _.forEach($scope.caches, (cache) => cache.label = _cacheLbl(cache));

                $scope.clusters = _.map(clusters, (cluster) => ({
                    value: cluster._id,
                    label: cluster.name,
                    discovery: cluster.discovery,
                    caches: cluster.caches
                }));

                $scope.domains = _.sortBy(_.map(validFilter(domains, true, false), (domain) => ({
                    label: domain.valueType,
                    value: domain._id,
                    kind: domain.kind,
                    meta: domain
                })), 'label');

                if ($state.params.linkId)
                    $scope.createItem($state.params.linkId);
                else {
                    const lastSelectedCache = angular.fromJson(sessionStorage.lastSelectedCache);

                    if (lastSelectedCache) {
                        const idx = _.findIndex($scope.caches, function(cache) {
                            return cache._id === lastSelectedCache;
                        });

                        if (idx >= 0)
                            $scope.selectItem($scope.caches[idx]);
                        else {
                            sessionStorage.removeItem('lastSelectedCache');

                            selectFirstItem();
                        }
                    }
                    else
                        selectFirstItem();
                }

                $scope.$watch('ui.inputForm.$valid', function(valid) {
                    if (valid && ModelNormalizer.isEqual(__original_value, $scope.backupItem))
                        $scope.ui.inputForm.$dirty = false;
                });

                $scope.$watch('backupItem', function(val) {
                    const form = $scope.ui.inputForm;

                    if (form.$pristine || (form.$valid && ModelNormalizer.isEqual(__original_value, val)))
                        form.$setPristine();
                    else
                        form.$setDirty();
                }, true);

                $scope.$watch('backupItem.offHeapMode', setOffHeapMaxMemory);
            })
            .catch(Messages.showError)
            .then(() => {
                $scope.ui.ready = true;
                $scope.ui.inputForm.$setPristine();
                Loading.finish('loadingCachesScreen');
            });

        $scope.selectItem = function(item, backup) {
            function selectItem() {
                $scope.selectedItem = item;

                if (item && !_.get(item.cacheStoreFactory.CacheJdbcBlobStoreFactory, 'connectVia'))
                    _.set(item.cacheStoreFactory, 'CacheJdbcBlobStoreFactory.connectVia', 'DataSource');

                try {
                    if (item && item._id)
                        sessionStorage.lastSelectedCache = angular.toJson(item._id);
                    else
                        sessionStorage.removeItem('lastSelectedCache');
                }
                catch (ignored) {
                    // No-op.
                }

                if (backup)
                    $scope.backupItem = backup;
                else if (item)
                    $scope.backupItem = angular.copy(item);
                else
                    $scope.backupItem = emptyCache;

                $scope.backupItem = angular.merge({}, blank, $scope.backupItem);
                $scope.ui.inputForm.$error = {};
                $scope.ui.inputForm.$setPristine();

                setOffHeapMode($scope.backupItem);

                __original_value = ModelNormalizer.normalize($scope.backupItem);

                if (LegacyUtils.getQueryVariable('new'))
                    $state.go('base.configuration.caches');
            }

            LegacyUtils.confirmUnsavedChanges($scope.backupItem && $scope.ui.inputForm.$dirty, selectItem);
        };

        $scope.linkId = () => $scope.backupItem._id ? $scope.backupItem._id : 'create';

        function prepareNewItem(linkId) {
            return {
                space: $scope.spaces[0]._id,
                cacheMode: 'PARTITIONED',
                atomicityMode: 'ATOMIC',
                readFromBackup: true,
                copyOnRead: true,
                clusters: linkId && _.find($scope.clusters, {value: linkId})
                    ? [linkId] : _.map($scope.clusters, function(cluster) { return cluster.value; }),
                domains: linkId && _.find($scope.domains, { value: linkId }) ? [linkId] : [],
                cacheStoreFactory: {CacheJdbcBlobStoreFactory: {connectVia: 'DataSource'}}
            };
        }

        // Add new cache.
        $scope.createItem = function(linkId) {
            $timeout(() => LegacyUtils.ensureActivePanel($scope.ui, 'general', 'cacheName'));

            $scope.selectItem(null, prepareNewItem(linkId));
        };

        function cacheClusters() {
            return _.filter($scope.clusters, (cluster) => _.includes($scope.backupItem.clusters, cluster.value));
        }

        function clusterCaches(cluster) {
            const caches = _.filter($scope.caches,
                (cache) => cache._id !== $scope.backupItem._id && _.includes(cluster.caches, cache._id));

            caches.push($scope.backupItem);

            return caches;
        }

        function checkDataSources() {
            const clusters = cacheClusters();

            let checkRes = {checked: true};

            const failCluster = _.find(clusters, (cluster) => {
                const caches = clusterCaches(cluster);

                checkRes = LegacyUtils.checkDataSources(cluster, caches, $scope.backupItem);

                return !checkRes.checked;
            });

            if (!checkRes.checked) {
                if (_.get(checkRes.secondObj, 'discovery.kind') === 'Jdbc') {
                    return showPopoverMessage($scope.ui, 'store', checkRes.firstObj.cacheStoreFactory.kind === 'CacheJdbcPojoStoreFactory' ? 'pojoDialect' : 'blobDialect',
                        'Found cluster "' + failCluster.label + '" with the same data source bean name "' +
                        checkRes.secondObj.discovery.Jdbc.dataSourceBean + '" and different database: "' +
                        LegacyUtils.cacheStoreJdbcDialectsLabel(checkRes.firstDB) + '" in current cache and "' +
                        LegacyUtils.cacheStoreJdbcDialectsLabel(checkRes.secondDB) + '" in"' + checkRes.secondObj.label + '" cluster', 10000);
                }

                return showPopoverMessage($scope.ui, 'store', checkRes.firstObj.cacheStoreFactory.kind === 'CacheJdbcPojoStoreFactory' ? 'pojoDialect' : 'blobDialect',
                    'Found cache "' + checkRes.secondObj.name + '" in cluster "' + failCluster.label + '" ' +
                    'with the same data source bean name "' + checkRes.firstObj.cacheStoreFactory[checkRes.firstObj.cacheStoreFactory.kind].dataSourceBean +
                    '" and different database: "' + LegacyUtils.cacheStoreJdbcDialectsLabel(checkRes.firstDB) + '" in current cache and "' +
                    LegacyUtils.cacheStoreJdbcDialectsLabel(checkRes.secondDB) + '" in "' + checkRes.secondObj.name + '" cache', 10000);
            }

            return true;
        }

        function checkSQLSchemas() {
            const clusters = cacheClusters();

            let checkRes = {checked: true};

            const failCluster = _.find(clusters, (cluster) => {
                const caches = clusterCaches(cluster);

                checkRes = LegacyUtils.checkCacheSQLSchemas(caches, $scope.backupItem);

                return !checkRes.checked;
            });

            if (!checkRes.checked) {
                return showPopoverMessage($scope.ui, 'query', 'sqlSchema',
                    'Found cache "' + checkRes.secondCache.name + '" in cluster "' + failCluster.label + '" ' +
                    'with the same SQL schema name "' + checkRes.firstCache.sqlSchema + '"', 10000);
            }

            return true;
        }

        function checkStoreFactoryBean(storeFactory, beanFieldId) {
            if (!LegacyUtils.isValidJavaIdentifier('Data source bean', storeFactory.dataSourceBean, beanFieldId, $scope.ui, 'store'))
                return false;

            return checkDataSources();
        }

        function checkStoreFactory(item) {
            const cacheStoreFactorySelected = item.cacheStoreFactory && item.cacheStoreFactory.kind;

            if (cacheStoreFactorySelected) {
                const storeFactory = item.cacheStoreFactory[item.cacheStoreFactory.kind];

                if (item.cacheStoreFactory.kind === 'CacheJdbcPojoStoreFactory' && !checkStoreFactoryBean(storeFactory, 'pojoDataSourceBean'))
                    return false;

                if (item.cacheStoreFactory.kind === 'CacheJdbcBlobStoreFactory' && storeFactory.connectVia !== 'URL'
                    && !checkStoreFactoryBean(storeFactory, 'blobDataSourceBean'))
                    return false;
            }

            if ((item.readThrough || item.writeThrough) && !cacheStoreFactorySelected)
                return showPopoverMessage($scope.ui, 'store', 'cacheStoreFactory', (item.readThrough ? 'Read' : 'Write') + ' through are enabled but store is not configured!');

            if (item.writeBehindEnabled && !cacheStoreFactorySelected)
                return showPopoverMessage($scope.ui, 'store', 'cacheStoreFactory', 'Write behind enabled but store is not configured!');

            if (cacheStoreFactorySelected && !item.readThrough && !item.writeThrough)
                return showPopoverMessage($scope.ui, 'store', 'readThroughTooltip', 'Store is configured but read/write through are not enabled!');

            return true;
        }

        // Check cache logical consistency.
        function validate(item) {
            LegacyUtils.hidePopover();

            if (LegacyUtils.isEmptyString(item.name))
                return showPopoverMessage($scope.ui, 'general', 'cacheName', 'Cache name should not be empty!');

            if (item.memoryMode === 'ONHEAP_TIERED' && item.offHeapMaxMemory > 0 && !LegacyUtils.isDefined(item.evictionPolicy.kind))
                return showPopoverMessage($scope.ui, 'memory', 'evictionPolicyKind', 'Eviction policy should not be configured!');

            if (!LegacyUtils.checkFieldValidators($scope.ui))
                return false;

            if (item.memoryMode === 'OFFHEAP_VALUES' && !_.isEmpty(item.domains))
                return showPopoverMessage($scope.ui, 'memory', 'memoryMode', 'Query indexing could not be enabled while values are stored off-heap!');

            if (item.memoryMode === 'OFFHEAP_TIERED' && item.offHeapMaxMemory === -1)
                return showPopoverMessage($scope.ui, 'memory', 'offHeapMode', 'Invalid value!');

            if (!checkSQLSchemas())
                return false;

            if (!checkStoreFactory(item))
                return false;

            if (item.writeBehindFlushSize === 0 && item.writeBehindFlushFrequency === 0)
                return showPopoverMessage($scope.ui, 'store', 'writeBehindFlushSize', 'Both "Flush frequency" and "Flush size" are not allowed as 0!');

            if (item.nodeFilter && item.nodeFilter.kind === 'OnNodes' && _.isEmpty(item.nodeFilter.OnNodes.nodeIds))
                return showPopoverMessage($scope.ui, 'nodeFilter', 'nodeFilter-title', 'At least one node ID should be specified!');

            return true;
        }

        // Save cache in database.
        function save(item) {
            $http.post('/api/v1/configuration/caches/save', item)
                .success(function(_id) {
                    item.label = _cacheLbl(item);

                    $scope.ui.inputForm.$setPristine();

                    const idx = _.findIndex($scope.caches, function(cache) {
                        return cache._id === _id;
                    });

                    if (idx >= 0)
                        angular.merge($scope.caches[idx], item);
                    else {
                        item._id = _id;
                        $scope.caches.push(item);
                    }

                    _.forEach($scope.clusters, (cluster) => {
                        if (_.includes(item.clusters, cluster.value))
                            cluster.caches = _.union(cluster.caches, [_id]);
                        else
                            _.remove(cluster.caches, (id) => id === _id);
                    });

                    _.forEach($scope.domains, (domain) => {
                        if (_.includes(item.domains, domain.value))
                            domain.meta.caches = _.union(domain.meta.caches, [_id]);
                        else
                            _.remove(domain.meta.caches, (id) => id === _id);
                    });

                    $scope.selectItem(item);

                    Messages.showInfo('Cache "' + item.name + '" saved.');
                })
                .error(Messages.showError);
        }

        // Save cache.
        $scope.saveItem = function() {
            const item = $scope.backupItem;

            angular.extend(item, LegacyUtils.autoCacheStoreConfiguration(item, cacheDomains(item)));

            if (validate(item))
                save(item);
        };

        function _cacheNames() {
            return _.map($scope.caches, function(cache) {
                return cache.name;
            });
        }

        // Clone cache with new name.
        $scope.cloneItem = function() {
            if (validate($scope.backupItem)) {
                Clone.confirm($scope.backupItem.name, _cacheNames()).then(function(newName) {
                    const item = angular.copy($scope.backupItem);

                    delete item._id;

                    item.name = newName;

                    delete item.sqlSchema;

                    save(item);
                });
            }
        };

        // Remove cache from db.
        $scope.removeItem = function() {
            const selectedItem = $scope.selectedItem;

            Confirm.confirm('Are you sure you want to remove cache: "' + selectedItem.name + '"?')
                .then(function() {
                    const _id = selectedItem._id;

                    $http.post('/api/v1/configuration/caches/remove', {_id})
                        .success(function() {
                            Messages.showInfo('Cache has been removed: ' + selectedItem.name);

                            const caches = $scope.caches;

                            const idx = _.findIndex(caches, function(cache) {
                                return cache._id === _id;
                            });

                            if (idx >= 0) {
                                caches.splice(idx, 1);

                                $scope.ui.inputForm.$setPristine();

                                if (caches.length > 0)
                                    $scope.selectItem(caches[0]);
                                else
                                    $scope.backupItem = emptyCache;

                                _.forEach($scope.clusters, (cluster) => _.remove(cluster.caches, (id) => id === _id));
                                _.forEach($scope.domains, (domain) => _.remove(domain.meta.caches, (id) => id === _id));
                            }
                        })
                        .error(Messages.showError);
                });
        };

        // Remove all caches from db.
        $scope.removeAllItems = function() {
            Confirm.confirm('Are you sure you want to remove all caches?')
                .then(function() {
                    $http.post('/api/v1/configuration/caches/remove/all')
                        .success(function() {
                            Messages.showInfo('All caches have been removed');

                            $scope.caches = [];

                            _.forEach($scope.clusters, (cluster) => cluster.caches = []);
                            _.forEach($scope.domains, (domain) => domain.meta.caches = []);

                            $scope.backupItem = emptyCache;
                            $scope.ui.inputForm.$error = {};
                            $scope.ui.inputForm.$setPristine();
                        })
                        .error(Messages.showError);
                });
        };

        $scope.resetAll = function() {
            Confirm.confirm('Are you sure you want to undo all changes for current cache?')
                .then(function() {
                    $scope.backupItem = $scope.selectedItem ? angular.copy($scope.selectedItem) : prepareNewItem();
                    $scope.ui.inputForm.$error = {};
                    $scope.ui.inputForm.$setPristine();
                });
        };

        const autoCompleter = {
            getCompletions(_editor, session, pos, prefix, callback) {
                if (prefix.length === 0) {
                    callback(null, []);

                    return;
                }

                callback(null, completions);
            }
        };

        // List of code completions.
        const DFLT_COMPLETIONS = [{
            caption: 'affinity',
            meta: 'Node group resolver',
            snippet: '<property name=\"affinity\">\n\t<bean class=\"${1:class}\">\n\t\t${2}\n\t</bean>\n</property>'
        }, {
            caption: 'affinityMapper',
            meta: 'Affinity key mapper',
            snippet: '<property name=\"affinityMapper\">\n\t<bean class=\"${1:class}\">\n\t\t${2}\n\t</bean>\n</property>'
        //}, {
        //    caption: 'atomicWriteOrderMode',
        //    meta: 'Write ordering mode',
        //    snippet: '<property name=\"atomicWriteOrderMode\" value=\"${1:' + DFLT_CFG.atomicWriteOrderMode + '}\"/>'
        //}, {
        //    caption: 'atomicityMode',
        //    meta: 'Cache atomicity mode',
        //    snippet: '<property name=\"atomicityMode\" value=\"${1:' + DFLT_CFG.atomicityMode + '}\"/>'
        //}, {
        //    caption: 'backups',
        //    meta: 'Number of backups for cache',
        //    snippet: '<property name=\"backups\" value=\"${1:' + DFLT_CFG.backups + '}\"/>'
        //}, {
        //    caption: 'cacheMode',
        //    meta: 'Cache mode',
        //    snippet: '<property name=\"cacheMode\" value=\"${1:' + DFLT_CFG.cacheMode + '}\"/>'
        //}, {
        //    caption: 'copyOnRead',
        //    meta: 'Copy on read',
        //    snippet: '<property name=\"copyOnRead\" value=\"${1:' + DFLT_CFG.copyOnRead + '}\"/>'
        //}, {
        //    caption: 'defaultLockTimeout',
        //    meta: 'Default lock acquisition timeout',
        //    snippet: '<property name=\"defaultLockTimeout\" value=\"${1:' + DFLT_CFG.defaultLockTimeout + '}\"/>'
        //}, {
        //    caption: 'eagerTtl',
        //    meta: 'Eager ttl',
        //    snippet: '<property name=\"eagerTtl\" value=\"${1:' + DFLT_CFG.eagerTtl + '}\"/>'
        //}, {
        //    caption: 'evictMaxOverflowRatio',
        //    meta: 'Maximum eviction overflow ratio',
        //    snippet: '<property name=\"evictMaxOverflowRatio\" value=\"${1:' + DFLT_CFG.evictMaxOverflowRatio + '}\"/>'
        //}, {
        //    caption: 'evictSynchronized',
        //    meta: 'a flag indicating whether eviction is synchronized',
        //    snippet: '<property name=\"evictSynchronized\" value=\"${1:' + DFLT_CFG.evictSynchronized + '}\"/>'
        //}, {
        //    caption: 'evictSynchronizedConcurrencyLevel',
        //    meta: 'Synchronous eviction concurrency level',
        //    snippet: '<property name=\"evictSynchronizedConcurrencyLevel\" value=\"${1:' + DFLT_CFG.evictSynchronizedConcurrencyLevel + '}\"/>'
        //}, {
        //    caption: 'evictSynchronizedKeyBufferSize',
        //    meta: 'Eviction key buffer size',
        //    snippet: '<property name=\"evictSynchronizedKeyBufferSize\" value=\"${1:' + DFLT_CFG.evictSynchronizedKeyBufferSize + '}\"/>'
        //}, {
        //    caption: 'evictSynchronizedTimeout',
        //    meta: 'Synchronous eviction timeout',
        //    snippet: '<property name=\"evictSynchronizedTimeout\" value=\"${1:' + DFLT_CFG.evictSynchronizedTimeout + '}\"/>'
        }, {
            caption: 'evictionFilter',
            meta: 'Eviction filter',
            snippet: '<property name=\"evictionFilter\">\n\t<bean class=\"${1:class}\">\n\t\t${2}\n\t</bean>\n</property>'
        }, {
            caption: 'evictionPolicy',
            meta: 'Cache expiration policy',
            snippet: '<property name=\"evictionPolicy\">\n\t<bean class=\"${1:class}\">\n\t\t${2}\n\t</bean>\n</property>'
        }, {
            caption: 'indexedTypes',
            meta: 'Key and value type pairs',
            snippet: '<property name=\"indexedTypes\">\n\t<list>\n\t\t<value>${1:keyType}</value>\n\t\t<value>${2:valueType}</value>\n\t</list>\n</property>'
        }, {
            caption: 'interceptor',
            meta: 'Cache interceptor',
            snippet: '<property name=\"interceptor\">\n\t<bean class=\"${1:class}\">\n\t\t${2}\n\t</bean>\n</property>'
        //}, {
        //    caption: 'invalidate',
        //    meta: 'Invalidation flag',
        //    snippet: '<property name=\"invalidate\" value=\"${1:' + DFLT_CFG.invalidate + '}\"/>'
        //}, {
        //    caption: 'loadPreviousValue',
        //    meta: 'Load previous value flag',
        //    snippet: '<property name=\"loadPreviousValue\" value=\"${1:' + DFLT_CFG.loadPreviousValue + '}\"/>'
        //}, {
        //    caption: 'longQueryWarningTimeout',
        //    meta: 'Timeout in milliseconds',
        //    snippet: '<property name=\"longQueryWarningTimeout\" value=\"${1:' + DFLT_CFG.longQueryWarningTimeout + '}\"/>'
        //}, {
        //    caption: 'managementEnabled',
        //    meta: 'Whether management is enabled',
        //    snippet: '<property name=\"managementEnabled\" value=\"${1:' + DFLT_CFG.managementEnabled + '}\"/>'
        //}, {
        //    caption: 'maxConcurrentAsyncOperations',
        //    meta: 'Maximum number of concurrent asynchronous operations',
        //    snippet: '<property name=\"maxConcurrentAsyncOperations\" value=\"${1:' + DFLT_CFG.maxConcurrentAsyncOperations + '}\"/>'
        //}, {
        //    caption: 'memoryMode',
        //    meta: 'Memory mode',
        //    snippet: '<property name=\"memoryMode\" value=\"${1:' + DFLT_CFG.memoryMode + '}\"/>'
        }, {
            caption: 'name',
            meta: 'Cache name',
            snippet: '<property name=\"name\" value=\"${1:name}\"/>'
        }, {
            caption: 'nearConfiguration',
            meta: 'Near cache configuration',
            snippet: '<property name=\"nearConfiguration\">\n\t<bean class=\"org.apache.ignite.configuration.NearCacheConfiguration\">\n\t\t${1}\n\t</bean>\n</property>'
        }, {
            caption: 'nearEvictionPolicy',
            meta: 'Near cache eviction policy',
            snippet: '<property name=\"nearEvictionPolicy\">\n\t<bean class=\"${1:class}\">\n\t\t${2}\n\t</bean>\n</property>'
        //}, {
        //    caption: 'nearStartSize',
        //    meta: 'Memory mode',
        //    snippet: '<property name=\"nearStartSize\" value=\"${1:' + (DFLT_CFG.startSize / 4) + '}\"/>'
        }, {
            caption: 'nodeFilter',
            meta: 'Node filter',
            snippet: '<property name=\"nodeFilter\">\n\t<bean class=\"${1:class}\">\n\t\t${2}\n\t</bean>\n</property>'
        //}, {
        //    caption: 'offHeapMaxMemory',
        //    meta: 'Maximum memory in bytes available to off-heap memory space',
        //    snippet: '<property name=\"offHeapMaxMemory\" value=\"${1:' + DFLT_CFG.offHeapMaxMemory + '}\"/>'
        }, {
            caption: 'pluginConfigurations',
            meta: 'Cache plugin configurations',
            snippet: '<property name=\"pluginConfigurations\">\n\t<bean class=\"${1:class}\">\n\t\t${2}\n\t</bean>\n</property>'
        //}, {
        //    caption: 'readFromBackup',
        //    meta: 'A flag indicating whether data can be read from backup',
        //    snippet: '<property name=\"readFromBackup\" value=\"${1:' + DFLT_CFG.readFromBackup + '}\"/>'
        //}, {
        //    caption: 'readThrough',
        //    meta: 'A flag indicating if \"read-through\" mode is required',
        //    snippet: '<property name=\"readThrough\" value=\"${1:' + DFLT_CFG.readThrough + '}\"/>'
        //}, {
        //    caption: 'rebalanceBatchSize',
        //    meta: 'Rebalance batch size',
        //    snippet: '<property name=\"rebalanceBatchSize\" value=\"${1:' + DFLT_CFG.rebalanceBatchSize + '}\"/>'
        //}, {
        //    caption: 'rebalanceDelay',
        //    meta: 'Rebalance delay',
        //    snippet: '<property name=\"rebalanceDelay\" value=\"${1:' + DFLT_CFG.rebalanceDelay + '}\"/>'
        //}, {
        //    caption: 'rebalanceMode',
        //    meta: 'Rebalance mode',
        //    snippet: '<property name=\"rebalanceMode\" value=\"${1:' + DFLT_CFG.rebalanceMode + '}\"/>'
        //}, {
        //    caption: 'rebalanceOrder',
        //    meta: 'Cache rebalance order',
        //    snippet: '<property name=\"rebalanceOrder\" value=\"${1:' + DFLT_CFG.rebalanceOrder + '}\"/>'
        //}, {
        //    caption: 'rebalanceThreadPoolSize',
        //    meta: 'Size of rebalance thread pool',
        //    snippet: '<property name=\"rebalanceThreadPoolSize\" value=\"${1:' + DFLT_CFG.rebalanceThreadPoolSize + '}\"/>'
        //}, {
        //    caption: 'rebalanceThrottle',
        //    meta: 'Time in milliseconds to wait between rebalance messages to avoid overloading of CPU',
        //    snippet: '<property name=\"rebalanceThrottle\" value=\"${1:' + DFLT_CFG.rebalanceThrottle + '}\"/>'
        //}, {
        //    caption: 'rebalanceTimeout',
        //    meta: 'Rebalance timeout',
        //    snippet: '<property name=\"rebalanceTimeout\" value=\"${1:' + DFLT_CFG.rebalanceTimeout + '}\"/>'
        //}, {
        //    caption: 'sqlEscapeAll',
        //    meta: 'Escaping of query and field names flag',
        //    snippet: '<property name=\"sqlEscapeAll\" value=\"${1:' + DFLT_CFG.sqlEscapeAll + '}\"/>'
        }, {
            caption: 'sqlFunctionClasses',
            meta: 'One or more classes with SQL functions',
            snippet: '<property name=\"sqlFunctionClasses\">\n\t<list>\n\t\t<value>${1:class}</value>\n\t</list>\n</property>'
        //}, {
        //    caption: 'sqlOnheapRowCacheSize',
        //    meta: 'Cache size',
        //    snippet: '<property name=\"sqlOnheapRowCacheSize\" value=\"${1:' + DFLT_CFG.sqlOnheapRowCacheSize + '}\"/>'
        //}, {
        //    caption: 'startSize',
        //    meta: 'Initial cache size',
        //    snippet: '<property name=\"startSize\" value=\"${1:' + DFLT_CFG.startSize + '}\"/>'
        //}, {
        //    caption: 'statisticsEnabled',
        //    meta: 'A flag indicating if statistics gathering is enabled',
        //    snippet: '<property name=\"statisticsEnabled\" value=\"${1:' + DFLT_CFG.statisticsEnabled + '}\"/>'
        //}, {
        //    caption: 'storeByValue',
        //    meta: 'A flag indicating if the cache will be store-by-value or store-by-reference',
        //    snippet: '<property name=\"storeByValue\" value=\"${1:' + DFLT_CFG.storeByValue + '}\"/>'
        //}, {
        //    caption: 'swapEnabled',
        //    meta: 'a flag indicating whether Ignite should use swap storage by default',
        //    snippet: '<property name=\"swapEnabled\" value=\"${1:' + DFLT_CFG.swapEnabled + '}\"/>'
        }, {
            caption: 'topologyValidator',
            meta: 'Cache topology validator',
            snippet: '<property name=\"topologyValidator\">\n\t<bean class=\"${1:class}\">\n\t\t${2}\n\t</bean>\n</property>'
        }, {
            caption: 'typeMetadata',
            meta: 'Collection of type metadata',
            snippet: '<property name=\"typeMetadata\">\n\t<list>\n\t\t${1}\n\t</list>\n</property>'
        //}, {
        //    caption: 'writeBehindBatchSize',
        //    meta: 'Maximum batch size for store operations',
        //    snippet: '<property name=\"writeBehindBatchSize\" value=\"${1:' + DFLT_CFG.writeBehindBatchSize + '}\"/>'
        //}, {
        //    caption: 'writeBehindEnabled',
        //    meta: 'Write-behind feature',
        //    snippet: '<property name=\"writeBehindEnabled\" value=\"${1:' + DFLT_CFG.writeBehindEnabled + '}\"/>'
        //}, {
        //    caption: 'writeBehindFlushFrequency',
        //    meta: 'Write-behind flush frequency in milliseconds',
        //    snippet: '<property name=\"writeBehindFlushFrequency\" value=\"${1:' + DFLT_CFG.writeBehindFlushFrequency + '}\"/>'
        //}, {
        //    caption: 'writeBehindFlushSize',
        //    meta: 'Maximum object count in write-behind cache',
        //    snippet: '<property name=\"writeBehindFlushSize\" value=\"${1:' + DFLT_CFG.writeBehindFlushSize + '}\"/>'
        //}, {
        //    caption: 'writeBehindFlushThreadCount',
        //    meta: 'Count of flush threads',
        //    snippet: '<property name=\"writeBehindFlushThreadCount\" value=\"${1:' + DFLT_CFG.writeBehindFlushThreadCount + '}\"/>'
        //}, {
        //    caption: 'writeSynchronizationMode',
        //    meta: 'Write synchronization mode',
        //    snippet: '<property name=\"writeSynchronizationMode\" value=\"${1:' + DFLT_CFG.writeSynchronizationMode + '}\"/>'
        //}, {
        //    caption: 'writeThrough',
        //    meta: 'A flag indicating if \"write-through\" mode is required',
        //    snippet: '<property name=\"writeThrough\" value=\"${1:' + DFLT_CFG.writeThrough + '}\"/>'
        }, {
            caption: 'LOCAL',
            meta: 'Cache mode',
            value: 'LOCAL'
        }, {
            caption: 'REPLICATED',
            meta: 'Cache mode',
            value: 'REPLICATED'
        }, {
            caption: 'PARTITIONED',
            meta: 'Cache mode',
            value: 'PARTITIONED'
        }, {
            caption: 'TRANSACTIONAL',
            meta: 'Cache atomicity mode',
            value: 'TRANSACTIONAL'
        }, {
            caption: 'ATOMIC',
            meta: 'Cache atomicity mode',
            value: 'ATOMIC'
        }, {
            caption: 'SYNC',
            meta: 'Cache rebalance mode',
            value: 'SYNC'
        }, {
            caption: 'ASYNC',
            meta: 'Cache rebalance mode',
            value: 'ASYNC'
        }, {
            caption: 'NONE',
            meta: 'Cache rebalance mode',
            value: 'NONE'
        }, {
            caption: 'ONHEAP_TIERED',
            meta: 'Cache memory mode',
            value: 'ONHEAP_TIERED'
        }, {
            caption: 'OFFHEAP_TIERED',
            meta: 'Cache memory mode',
            value: 'OFFHEAP_TIERED'
        }, {
            caption: 'OFFHEAP_VALUES',
            meta: 'Cache memory mode',
            value: 'OFFHEAP_VALUES'
        }, {
            caption: 'FULL_SYNC',
            meta: 'Cache write synchronization mode',
            value: 'FULL_SYNC'
        }, {
            caption: 'FULL_ASYNC',
            meta: 'Cache write synchronization mode',
            value: 'FULL_ASYNC'
        }, {
            caption: 'PRIMARY_SYNC',
            meta: 'Cache write synchronization mode',
            value: 'PRIMARY_SYNC'
        }, {
            caption: 'CLOCK',
            meta: 'Cache write atomic mode',
            value: 'CLOCK'
        }, {
            caption: 'PRIMARY',
            meta: 'Cache write atomic mode',
            value: 'PRIMARY'
        }];

        const completions = _.clone(DFLT_COMPLETIONS);

        const AVAILABLE_CMDS = [
            'Esc', 'gotoleft', 'golineup', 'gotoright', 'golinedown',
            'selectleft', 'selectup', 'selectright', 'selectdown',
            'selectwordleft', 'selectwordright',
            'selectlinestart', 'selectlineend',
            'gotolinestart', 'gotolineend'
        ];

        $scope.onLoad = (editor) => {
            editor.setAutoScrollEditorIntoView(true);
            editor.$blockScrolling = Infinity;

            editor.setOption('enableBasicAutocompletion', [autoCompleter]);
            editor.setOption('enableLiveAutocompletion', true);

            const renderer = editor.renderer;

            renderer.setHighlightGutterLine(false);
            renderer.setShowPrintMargin(false);
            renderer.setOption('fontFamily', 'monospace');
            renderer.setOption('fontSize', '12px');
            renderer.setOption('minLines', '5');
            renderer.setOption('maxLines', '15');

            editor.commands.on('exec', (e) => {
                const cur = editor.selection.getCursor();

                const firstRow = cur.row === 0;
                const lastRow = (cur.row + 1) === editor.session.getLength();

                if (!(firstRow || lastRow))
                    return;

                if (AVAILABLE_CMDS.indexOf(e.command.name) !== -1)
                    return;

                const endOfFirstRow = firstRow && editor.session.getLine(0).length === cur.column;
                const startOfLastRow = lastRow && cur.column === 0;

                const newLine = e.command.name === 'insertstring' && e.args === '\n';

                if ((endOfFirstRow || startOfLastRow) && newLine)
                    return;

                e.preventDefault();
                e.stopPropagation();
            });
        };
    }
]];
