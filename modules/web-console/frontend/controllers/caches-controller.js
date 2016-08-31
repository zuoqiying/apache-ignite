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

// List of code completions.
const DFLT_COMPLETIONS = [{
    caption: 'bean',
    meta: 'bean - Bean node with class attribute',
    snippet: '<bean class=\"${1:class}\">\n\t${2}\n</bean>'
}, {
    caption: 'bean',
    meta: 'bean - Bean node with id and class attributes',
    snippet: '<bean id=\"${1:id}\" class=\"${2:class}\">\n\t${3}\n</bean>'
}, {
    caption: 'description',
    meta: 'description - Description node',
    snippet: '<description>\n\t${1}\n<description/>'
}, {
    caption: 'entry',
    meta: 'entry - Entry node with key and value attributes',
    snippet: '<entry key=\"${1:key}\" value=\"${2:value}\"/>'
}, {
    caption: 'import',
    meta: 'import - Import node with resource attribute',
    snippet: '<import resource=\"${1:resource}\"/>'
}, {
    caption: 'list',
    meta: 'list - List node',
    snippet: '<list>\n\t${1}\n</list>'
}, {
    caption: 'list',
    meta: 'list - List node with id attribute',
    snippet: '<list id=\"${1:id}\">\n\t${2}\n</list>'
}, {
    caption: 'list',
    meta: 'list - List node with id and list-class attributes',
    snippet: '<list id=\"${1:id}\" list-class=\"${2:class}\">\n\t${3}\n</list>'
}, {
    caption: 'map',
    meta: 'map - Map node',
    snippet: '<map>\n\t${cursor}\n</map>'
}, {
    caption: 'map',
    meta: 'map - Map node with id attribute',
    snippet: '<map id=\"${id}\">\n\t${cursor}\n</map>'
}, {
    caption: 'map',
    meta: 'map - Map node with id and map-class attributes',
    snippet: '<map id=\"${id}\" map-class=\"${class}\">\n\t${cursor}\n</map>'
}, {
    caption: 'properties',
    meta: 'properties - Properties node with id and location attributes',
    snippet: '<properties id=\"${id}\" location=\"${location}\"/>'
}, {
    caption: 'property',
    meta: 'property - Property node with name attribute',
    snippet: '<property name=\"${name}\">\n\t${cursor}\n</property>'
}, {
    caption: 'property',
    meta: 'property - Property node with name and value attributes',
    snippet: '<property name=\"${name}\" value=\"${value}\"/>'
}, {
    caption: 'set',
    meta: 'set - Set node',
    snippet: '<set>\n\t${cursor}\n</set>'
}, {
    caption: 'set',
    meta: 'set - Set node with id attribute',
    snippet: '<set id=\"${id}\">\n\t${cursor}\n</set>'
}, {
    caption: 'set',
    meta: 'set - Set node with id and set-class attributes',
    snippet: '<set id=\"${id}\" set-class=\"${class}\">\n\t${cursor}\n</set>'
}, {
    caption: 'value',
    meta: 'value - Value node',
    snippet: '<value>${cursor}</value>'
}];

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

        const _updateCustomStore = () => {
            let config = '';

            try {
                const parser = new DOMParser();

                const dom = parser.parseFromString(_.get($scope.backupItem, 'cacheStoreFactory.custom.config'), 'text/xml');

                const bean = dom.getElementsByTagName('bean').item(0);

                if (bean)
                    config = bean.innerHTML.trim();
            }
            catch (ignore) {
                // No-op.
            }

            _.set($scope.backupItem, 'cacheStoreFactory.custom.config', '<property name="cacheStoreFactory">\n' +
                '    <bean class="' + (_.get($scope.backupItem, 'cacheStoreFactory.custom.className') || 'Input class name') + '">\n' +
                '        ' + config + '\n' +
                '    </bean>\n' +
                '</property>');
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

                $scope.$watch('backupItem.cacheStoreFactory.custom.className', _updateCustomStore);
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

        const completions = _.clone(DFLT_COMPLETIONS);

        const autoCompleter = {
            getCompletions(_editor, session, pos, prefix, callback) {
                if (prefix.length === 0) {
                    callback(null, []);

                    return;
                }

                callback(null, completions);
            }
        };

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

                const firstEditable = 2;
                const lastEditable = editor.session.getLength() - 2;

                // Change selection to exclude not editable part.
                if (editor.selection.selectionAnchor.row < 2) {
                    editor.selection.selectionAnchor.row = 2;
                    editor.selection.selectionAnchor.column = 0;
                }

                if (editor.selection.selectionAnchor.row >= lastEditable) {
                    editor.selection.selectionAnchor.row = lastEditable - 1;
                    editor.selection.selectionAnchor.column = editor.session.$rowLengthCache[lastEditable - 1];
                }

                const skipInBegin = cur.row <= 1 ||
                    (e.command.name === 'backspace' && cur.row === 2 && cur.column === 0);
                const skipInLast = cur.row >= lastEditable ||
                    (e.command.name === 'del' && cur.row === lastEditable - 1 &&
                        cur.column === editor.session.$rowLengthCache[cur.row]);

                if (!(skipInBegin || skipInLast))
                    return;

                if (AVAILABLE_CMDS.indexOf(e.command.name) !== -1)
                    return;

                const endOfFirstRow = cur.row == 1 && editor.session.$rowLengthCache[1] === cur.column;
                const startOfLastRow = cur.row == lastEditable && cur.column === 0;

                const newLine = e.command.name === 'insertstring' && e.args === '\n';

                if ((endOfFirstRow || startOfLastRow) && newLine)
                    return;

                e.preventDefault();
                e.stopPropagation();
            });
        };
    }
]];
