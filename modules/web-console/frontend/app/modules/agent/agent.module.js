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

import angular from 'angular';
import io from 'socket.io-client'; // eslint-disable-line no-unused-vars

const maskNull = (val) => _.isEmpty(val) ? 'null' : val;

class IgniteAgentMonitor {
    static $inject = ['igniteSocketFactory', '$rootScope', '$q', '$state', '$modal', 'IgniteMessages'];

    constructor(socketFactory, $root, $q, $state, $modal, Messages) {
        this._modalScope = $root.$new();

        $root.$watch('user', () => {
            this._modalScope.user = $root.user;
        });

        $root.$on('$stateChangeStart', this.stopWatch);

        // Pre-fetch modal dialogs.
        this.modal = $modal({
            scope: this._modalScope,
            templateUrl: '/templates/agent-download.html',
            show: false,
            backdrop: 'static',
            keyboard: false
        });

        const _modalHide = this.modal.hide;

        /**
         * Special dialog hide function.
         */
        this.modal.hide = () => {
            Messages.hideAlert();

            _modalHide();
        };

        /**
         * @param {Object} err
         */
        this.modal.error = (err) => {
            if (this._modalScope.showModal) {
                this.modal.$promise.then(this.modal.show);

                Messages.showError(err);
            }
        };

        /**
         * Close dialog and go by specified link.
         */
        this._modalScope.back = () => {
            this.stopWatch();

            if (this._modalScope.backState)
                this._modalScope.$$postDigest(() => $state.go(this._modalScope.backState));
        };

        this._modalScope.demo = false;
        this._modalScope.hasAgents = null;
        this._modalScope.showModal = false;

        /**
         * @type {Socket}
         */
        this._socket = null;

        this._socketFactory = socketFactory;

        this._$q = $q;
    }

    /**
     * @private
     */
    checkModal() {
        if (this._modalScope.showModal && !this._modalScope.hasAgents)
            this.modal.$promise.then(this.modal.show);
        else if ((this._modalScope.hasAgents || !this._modalScope.showModal) && this.modal.$isShown)
            this.modal.hide();
    }

    /**
     * @returns {Promise}
     */
    awaitAgent() {
        if (this._modalScope.hasAgents)
            return this._$q.when();

        const latch = this._$q.defer();

        const offConnected = this._modalScope.$on('agent:watch', (event, state) => {
            if (state !== 'DISCONNECTED')
                offConnected();

            if (state === 'CONNECTED')
                return latch.resolve();

            if (state === 'STOPPED')
                return latch.reject('Agent watch stopped.');
        });

        return latch.promise;
    }

    init() {
        if (this._socket)
            return;

        this._socket = this._socketFactory();

        const disconnectFn = () => {
            this._modalScope.hasAgents = false;

            this.checkModal();

            this._modalScope.$broadcast('agent:watch', 'DISCONNECTED');
        };

        this._socket.on('connect_error', disconnectFn);
        this._socket.on('disconnect', disconnectFn);

        this._socket.on('agent:count', ({count, demo}) => {
            this._modalScope.demo = demo;
            this._modalScope.hasAgents = count > 0;

            this.checkModal();

            this._modalScope.$broadcast('agent:watch', this._modalScope.hasAgents ? 'CONNECTED' : 'DISCONNECTED');
        });
    }

    /**
     * @param {Object} back
     * @returns {Promise}
     */
    startWatch(back) {
        this._modalScope.backState = back.state;
        this._modalScope.backText = back.text;

        this._modalScope.agentGoal = back.goal;

        if (back.onDisconnect) {
            this._modalScope.offDisconnect = this._modalScope.$on('agent:watch', (e, state) =>
                state === 'DISCONNECTED' && back.onDisconnect());
        }

        this._modalScope.showModal = true;

        // Remove blinking on init.
        if (this._modalScope.hasAgents !== null)
            this.checkModal();

        return this.awaitAgent();
    }

    stopWatch() {
        this._modalScope.showModal = false;

        this.checkModal();

        this._modalScope.offDisconnect && this._modalScope.offDisconnect();

        this._modalScope.$broadcast('agent:watch', 'STOPPED');
    }

    /**
     *
     * @param {String} event
     * @param {Object} [args]
     * @returns {Promise}
     * @private
     */
    _emit(event, ...args) {
        if (!this._socket)
            return this._$q.reject('Failed to connect to server');

        const latch = this._$q.defer();

        const onDisconnect = () => {
            this._socket.removeListener('disconnect', onDisconnect);

            latch.reject('Connection to server was closed');
        };

        this._socket.on('disconnect', onDisconnect);

        args.push((err, res) => {
            this._socket.removeListener('disconnect', onDisconnect);

            if (err)
                latch.reject(err);

            latch.resolve(res);
        });

        this._socket.emit(event, ...args);

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
     * @param {int} [queryId]
     * @returns {Promise}
     */
    queryClose(nid, queryId) {
        return this._rest('node:query:close', nid, queryId);
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
    next(nid, queryId, pageSize) {
        return this._rest('node:query:fetch', nid, queryId, pageSize)
            .then(({result}) => result);
    }

    /**
     * @param {String} [cacheName] Cache name.
     * @returns {Promise}
     */
    metadata(cacheName) {
        return this._rest('node:cache:metadata', maskNull(cacheName));
    }
}

angular
    .module('ignite-console.agent', [

    ])
    .service('IgniteAgentMonitor', IgniteAgentMonitor);
