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

const maskNull = (val) => _.isEmpty(val) ? 'null' : val;

export default class IgniteAgentManager {
    static $inject = ['$rootScope', '$q', 'igniteSocketFactory', 'IgniteAgentModal'];

    constructor($root, $q, socketFactory, AgentModal) {
        this.$root = $root;
        this.$q = $q;
        this.socketFactory = socketFactory;

        /**
         * @type {IgniteAgentModal}
         */
        this.AgentModal = AgentModal;

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

        self.socket.on('agent:count', ({count, hasDemo}) => {
            self.hasDemo = hasDemo;
            self.connected = count > 0;
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
     * @param {Object} err
     */
    showNodeError(err) {
        if (this._modalScope.showModal) {
            this.modal.$promise.then(this.modal.show);

            this.Messages.showError(err);
        }
    }

    /**
     *
     * @param {String} event
     * @param {Object} [args]
     * @returns {Promise}
     * @private
     */
    _rest(event, ...args) {
        return this.modal.$promise
            .then(() => this._emit(event, ...args));
    }

    /**
     * @param {Boolean} [attr]
     * @param {Boolean} [mtr]
     * @returns {Promise}
     */
    topology(attr, mtr) {
        return this._rest('node:topology', !!attr, !!mtr);
    }

    /**
     * @param {String} nid Node id.
     * @param {String} cacheName Cache name.
     * @param {String} [query] Query if null then scan query.
     * @param {Boolean} nonCollocatedJoins Flag whether to execute non collocated joins.
     * @param {Boolean} local Flag whether to execute query locally.
     * @param {int} pageSize
     * @returns {Promise}
     */
    query(nid, cacheName, query, nonCollocatedJoins, local, pageSize) {
        return this._rest('node:query', nid, maskNull(cacheName), maskNull(query), nonCollocatedJoins, local, pageSize)
            .then(({result}) => {
                if (_.isEmpty(result.key))
                    return result.value;

                return Promise.reject(result.key);
            });
    }

    /**
     * @param {String} nid Node id.
     * @param {String} cacheName Cache name.
     * @param {String} [query] Query if null then scan query.
     * @param {Boolean} nonCollocatedJoins Flag whether to execute non collocated joins.
     * @param {Boolean} local Flag whether to execute query locally.
     * @returns {Promise}
     */
    queryGetAll(nid, cacheName, query, nonCollocatedJoins, local) {
        return this._rest('node:query:getAll', nid, maskNull(cacheName), maskNull(query), nonCollocatedJoins, local);
    }

    /**
     * @param {String} nid Node id.
     * @param {int} queryId
     * @param {int} pageSize
     * @returns {Promise}
     */
    queryNextPage(nid, queryId, pageSize) {
        return this._rest('node:query:fetch', nid, queryId, pageSize)
            .then(({result}) => result);
    }

    /**
     * @param {String} nid Node id.
     * @param {int} [queryId]
     * @returns {Promise}
     */
    queryClose(nid, queryId) {
        return this._rest('node:query:close', nid, queryId);
    }

    /**
     * @param {String} [cacheName] Cache name.
     * @returns {Promise}
     */
    metadata(cacheName) {
        return this._rest('node:cache:metadata', maskNull(cacheName));
    }
}
