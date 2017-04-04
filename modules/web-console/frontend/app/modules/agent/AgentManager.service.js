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

import io from 'socket.io-client'; // eslint-disable-line no-unused-vars

const maskNull = (val) => _.isNil(val) ? 'null' : val;

export default class IgniteAgentManager {
    static $inject = ['$rootScope', '$q', 'igniteSocketFactory', 'AgentModal'];

    constructor($root, $q, socketFactory, AgentModal) {
        this.$root = $root;
        this.$q = $q;
        this.socketFactory = socketFactory;

        /**
         * @type {AgentModal}
         */
        this.AgentModal = AgentModal;

        this.clusters = [];

        $root.$on('$stateChangeSuccess', _.bind(this.stopWatch, this));

        /**
         * Connection to backend.
         * @type {Socket}
         */
        this.socket = null;

        /**
         * One or more agent are connected with user token.
         * @type {boolean}
         */
        this.connected = null;

        /**
         * Has agent with enabled demo mode.
         * @type {boolean}
         */
        this.hasDemo = false;

        this.clusters = [];
    }

    connect() {
        const self = this;

        if (_.nonNil(self.socket))
            return;

        self.socket = self.socketFactory();

        const onDisconnect = () => {
            self.connected = false;

            self.AgentModal.agentDisconnected(this.backText, this.backState);
        };

        self.socket.on('connect_error', onDisconnect);
        self.socket.on('disconnect', onDisconnect);

        self.connected = null;

        self.socket.on('agents:stat', ({count, hasDemo, clusters}) => {
            self.connected = count > 0;
            self.hasDemo = hasDemo;

            const removed = _.difference(self.clusters, clusters);

            if (_.nonEmpty(removed))
                _.pullAll(self.clusters, removed);

            const added = _.difference(clusters, self.clusters);

            if (_.nonEmpty(added))
                self.clusters.push(...added);

            if (_.isNil(self.cluster) || _.isNil(_.find(self.clusters, self.cluster)))
                self.cluster = _.head(self.clusters);
        });
    }

    /**
     * @returns {Promise}
     */
    awaitAgent() {
        this.latchAwaitAgent = this.$q.defer();

        this.offAwaitAgent = this.$root.$watch(() => this.connected, (connected) => {
            if (connected) {
                this.offAwaitAgent();

                this.latchAwaitAgent.resolve();
            }
        });

        return this.latchAwaitAgent.promise;
    }

    /**
     * @param {String} backText
     * @param {String} [backState]
     * @returns {Promise}
     */
    startWatch(backText, backState) {
        const self = this;

        self.backText = backText;
        self.backState = backState;

        self.offWatch = this.$root.$watch(() => self.connected, (connected) => {
            switch (connected) {
                case false:
                    this.AgentModal.agentDisconnected(self.backText, self.backState);

                    break;
                case true:
                    this.AgentModal.hide();

                    break;
                default:
                    // Connection to backend is not established yet.
            }
        });

        return self.awaitAgent();
    }

    stopWatch() {
        if (!_.isFunction(this.offWatch))
            return;

        this.offWatch();

        this.AgentModal.hide();

        if (this.latchAwaitAgent) {
            this.offAwaitAgent();

            this.latchAwaitAgent.reject('Agent watch stopped.');
        }
    }

    /**
     *
     * @param {String} event
     * @param {Object} [args]
     * @returns {Promise}
     * @private
     */
    _emit(event, ...args) {
        if (!this.socket)
            return this.$q.reject('Failed to connect to server');

        const latch = this.$q.defer();

        const onDisconnect = () => {
            this.socket.removeListener('disconnect', onDisconnect);

            latch.reject('Connection to server was closed');
        };

        this.socket.on('disconnect', onDisconnect);

        args.push((err, res) => {
            this.socket.removeListener('disconnect', onDisconnect);

            if (err)
                latch.reject(err);

            latch.resolve(res);
        });

        this.socket.emit(event, ...args);

        return latch.promise;
    }

    drivers() {
        return this._emit('schemaImport:drivers');
    }

    /**
     * @param {Object} driverPath
     * @param {Object} driverClass
     * @param {Object} url
     * @param {Object} user
     * @param {Object} password
     * @returns {Promise}
     */
    schemas({driverPath, driverClass, url, user, password}) {
        const info = {user, password};

        return this._emit('schemaImport:schemas', {driverPath, driverClass, url, info});
    }

    /**
     * @param {Object} driverPath
     * @param {Object} driverClass
     * @param {Object} url
     * @param {Object} user
     * @param {Object} password
     * @param {Object} schemas
     * @param {Object} tablesOnly
     * @returns {Promise} Promise on list of tables (see org.apache.ignite.schema.parser.DbTable java class)
     */
    tables({driverPath, driverClass, url, user, password, schemas, tablesOnly}) {
        const info = {user, password};

        return this._emit('schemaImport:tables', {driverPath, driverClass, url, info, schemas, tablesOnly});
    }

    /**
     *
     * @param {String} event
     * @param {Object} [args]
     * @returns {Promise}
     * @private
     */
    _rest(event, ...args) {
        return this._emit(event, _.get(this, 'cluster.id'), ...args);
    }

    /**
     * @param {Boolean} [attr]
     * @param {Boolean} [mtr]
     * @returns {Promise}
     */
    topology(attr = false, mtr = false) {
        return this._rest('node:rest', {cmd: 'top', attr, mtr});
    }

    /**
     * @param {String} [cacheName] Cache name.
     * @returns {Promise}
     */
    metadata(cacheName) {
        return this._rest('node:rest', {cmd: 'metadata', cacheName: maskNull(cacheName)})
            .then((caches) => {
                let types = [];

                const _compact = (className) => {
                    return className.replace('java.lang.', '').replace('java.util.', '').replace('java.sql.', '');
                };

                const _typeMapper = (meta, typeName) => {
                    const maskedName = _.isEmpty(meta.cacheName) ? '<default>' : meta.cacheName;

                    let fields = meta.fields[typeName];

                    let columns = [];

                    for (const fieldName in fields) {
                        if (fields.hasOwnProperty(fieldName)) {
                            const fieldClass = _compact(fields[fieldName]);

                            columns.push({
                                type: 'field',
                                name: fieldName,
                                clazz: fieldClass,
                                system: fieldName === '_KEY' || fieldName === '_VAL',
                                cacheName: meta.cacheName,
                                typeName,
                                maskedName
                            });
                        }
                    }

                    const indexes = [];

                    for (const index of meta.indexes[typeName]) {
                        fields = [];

                        for (const field of index.fields) {
                            fields.push({
                                type: 'index-field',
                                name: field,
                                order: index.descendings.indexOf(field) < 0,
                                unique: index.unique,
                                cacheName: meta.cacheName,
                                typeName,
                                maskedName
                            });
                        }

                        if (fields.length > 0) {
                            indexes.push({
                                type: 'index',
                                name: index.name,
                                children: fields,
                                cacheName: meta.cacheName,
                                typeName,
                                maskedName
                            });
                        }
                    }

                    columns = _.sortBy(columns, 'name');

                    if (!_.isEmpty(indexes)) {
                        columns = columns.concat({
                            type: 'indexes',
                            name: 'Indexes',
                            cacheName: meta.cacheName,
                            typeName,
                            maskedName,
                            children: indexes
                        });
                    }

                    return {
                        type: 'type',
                        cacheName: meta.cacheName || '',
                        typeName,
                        maskedName,
                        children: columns
                    };
                };

                for (const meta of caches) {
                    const cacheTypes = meta.types.map(_typeMapper.bind(null, meta));

                    if (!_.isEmpty(cacheTypes))
                        types = types.concat(cacheTypes);
                }

                return types;
            });
    }

    /**
     * @param {String} taskId
     * @param {Array.<String>|String} nids
     * @param {Array.<Object>} args
     */
    visorTask(taskId, nids, ...args) {
        args = _.map(args, (arg) => maskNull(arg));

        return this._rest('node:visor', taskId, maskNull(nids), ...args);
    }

    /**
     * @param {String} nid Node id.
     * @param {String} cacheName Cache name.
     * @param {String} [query] Query if null then scan query.
     * @param {Boolean} nonCollocatedJoins Flag whether to execute non collocated joins.
     * @param {Boolean} enforceJoinOrder Flag whether enforce join order is enabled.
     * @param {Boolean} local Flag whether to execute query locally.
     * @param {int} pageSz
     * @returns {Promise}
     */
    query(nid, cacheName, query, nonCollocatedJoins, enforceJoinOrder, local, pageSz) {
        return this.visorTask('fieldsQuery', nid, cacheName, query, nonCollocatedJoins, enforceJoinOrder, local, pageSz)
            .then(({key, value}) => {
                if (_.isEmpty(key))
                    return value;

                return Promise.reject(key);
            });
    }

    /**
     * @param {String} nid Node id.
     * @param {int} queryId
     * @param {int} pageSize
     * @returns {Promise}
     */
    queryNextPage(nid, queryId, pageSize) {
        return this.visorTask('queryFetch', nid, queryId, pageSize);
    }

    /**
     * @param {String} nid Node id.
     * @param {String} cacheName Cache name.
     * @param {String} [query] Query if null then scan query.
     * @param {Boolean} nonCollocatedJoins Flag whether to execute non collocated joins.
     * @param {Boolean} enforceJoinOrder Flag whether enforce join order is enabled.
     * @param {Boolean} local Flag whether to execute query locally.
     * @returns {Promise}
     */
    queryGetAll(nid, cacheName, query, nonCollocatedJoins, enforceJoinOrder, local) {
        // Page size for query.
        const pageSz = 1024;

        const fetchResult = (acc) => {
            if (!acc.hasMore)
                return acc;

            return this.queryNextPage(acc.responseNodeId, acc.queryId, pageSz)
                .then((res) => {
                    acc.rows = acc.rows.concat(res.rows);

                    acc.hasMore = res.hasMore;

                    return fetchResult(acc);
                });
        };

        return this.query(nid, cacheName, query, nonCollocatedJoins, enforceJoinOrder, local, pageSz)
            .then(fetchResult);
    }

    /**
     * @param {String} nid Node id.
     * @param {int} [queryId]
     * @returns {Promise}
     */
    queryClose(nid, queryId) {
        return this.visorTask('queryClose', nid, queryId);
    }

    /**
     * @param {Object} err
     */
    showNodeError(err) {
        if (this._modalScope.showModal) {
            this.modal.$promise.then(this.modal.show);

            this.Messages.showError(err);
        }
    }
}
