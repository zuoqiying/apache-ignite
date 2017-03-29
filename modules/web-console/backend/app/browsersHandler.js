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

'use strict';

// Fire me up!

/**
 * Module interaction with browsers.
 */
module.exports = {
    implements: 'browsers-handler',
    inject: ['require(lodash)', 'require(socket.io)', 'configure']
};

module.exports.factory = (_, socketio, configure) => {
    const _errorToJson = (err) => {
        return {
            code: err.code || 1,
            message: err.message || err
        };
    };

    class BrowserSockets {
        constructor() {
            this.sockets = new Map();
        }

        /**
         * @param {Socket} sock
         */
        add(sock) {
            const token = sock.request.user.token;

            if (this.sockets.has(token))
                this.sockets.get(token).push(sock);
            else
                this.sockets.set(token, [sock]);

            return this.sockets.get(token);
        }

        /**
         * @param {Socket} sock
         */
        remove(sock) {
            const token = sock.request.user.token;

            const sockets = this.sockets.get(token);

            _.pull(sockets, sock);

            return sockets;
        }

        get(token) {
            if (this.sockets.has(token))
                return this.sockets.get(token);

            return [];
        }

        demo(token) {
            return _.filter(this.sockets.get(token), (sock) => sock.request._query.IgniteDemoMode === 'true');
        }
    }

    class BrowsersHandler {
        /**
         * @constructor
         */
        constructor() {
            /**
             * Connected browsers.
             * @type {BrowserSockets}
             */
            this._browserSockets = new BrowserSockets();
        }

        /**
         * @param {String} token
         * @param {Array.<Socket>} [socks]
         */
        sendAgents(token, socks = this._browserSockets.get(token)) {
            return this._agentHnd.forToken(token)
                .then((agentSocks) => {
                    const stat = _.reduce(agentSocks, (acc, agentSock) => {
                        acc.count += 1;
                        acc.hasDemo |= _.get(agentSock, 'demo.enabled');

                        if (agentSock.cluster) {
                            acc.clusters.add({
                                id: agentSock.cluster.id
                            });
                        }

                        return acc;
                    }, {count: 0, hasDemo: false, clusters: new Set()});

                    stat.clusters = Array.from(stat.clusters);

                    return stat;
                })
                .catch(() => ({count: 0, hasDemo: false, clusters: []}))
                .then((stat) => _.forEach(socks, (sock) => sock.emit('agents:stat', stat)));
        }

        /**
         *
         * @param server
         * @param {AgentsHandler} agentHnd
         */
        attach(server, agentHnd) {
            this._agentHnd = agentHnd;

            if (this.io)
                throw 'Browser server already started!';

            const io = socketio(server);

            configure.socketio(io);

            // Handle browser connect event.
            io.sockets.on('connection', (sock) => {
                const demo = sock.request._query.IgniteDemoMode === 'true';

                // Handle browser disconnect event.
                sock.on('disconnect', () => {
                    this._browserSockets.remove(sock);

                    // Stop demo if latest demo tab for this token.
                    demo && agentHnd.tryStopDemo(sock);
                });

                this._browserSockets.add(sock);

                const currentToken = () => sock.request.user.token;

                this.sendAgents(currentToken(), [sock]);

                // const clusterId = () => demo ? 'DEMO' : 'CLUSTER';

                const resendToAgent = (event) => {
                    return (...args) => {
                        const cb = _.last(args);

                        return agentHnd.forToken(currentToken())
                            .then((agent) => agent.emitEvent(event, ..._.dropRight(args)))
                            .then((res) => cb(null, res))
                            .catch((err) => cb(_errorToJson(err)));
                    };
                };

                // Return available drivers to browser.
                sock.on('schemaImport:drivers', resendToAgent('schemaImport:drivers'));

                // Return schemas from database to browser.
                sock.on('schemaImport:schemas', resendToAgent('schemaImport:schemas'));

                // Return tables from database to browser.
                sock.on('schemaImport:tables', resendToAgent('schemaImport:tables'));

                const resendToCluster = (demo, name, ...args) => {
                    const cb = _.last(args);

                    return agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.gatewayTask('node:rest', {method: 'GET', uri: 'ignite', demo, args}))
                        .then(this.restResultParse)
                        .then((res) => cb(null, res))
                        .catch((err) => cb(_errorToJson(err)));
                };

                // Return topology command result from grid to browser.
                sock.on('node:rest', resendToCluster);

                // Return topology command result from grid to browser.
                sock.on('node:restGateway', (demo, nids, taskCls, argCls, ...args) => {
                    const cb = _.last(args);

                    return agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.gatewayTask('node:rest', {method: 'GET', uri: 'ignite', demo, args}))
                        .then(this.restResultParse)
                        .then((res) => cb(null, res))
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Return topology command result from grid to browser.
                sock.on('node:topology', (clusterId, attr, mtr, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.topology(demo, attr, mtr))
                        .then((clusters) => cb(null, clusters))
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Close query on node.
                sock.on('node:query:close', (clusterId, nid, queryId, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.queryClose(demo, nid, queryId))
                        .then(() => cb())
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Execute query on node and return first page to browser.
                sock.on('node:query', (clusterId, nid, cacheName, query, distributedJoins, enforceJoinOrder, local, pageSize, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.fieldsQuery(demo, nid, cacheName, query, distributedJoins, enforceJoinOrder, local, pageSize))
                        .then((res) => cb(null, res))
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Fetch next page for query and return result to browser.
                sock.on('node:query:fetch', (clusterId, nid, queryId, pageSize, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.queryFetch(demo, nid, queryId, pageSize))
                        .then((res) => cb(null, res))
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Execute query on node and return full result to browser.
                sock.on('node:query:getAll', (clusterId, nid, cacheName, query, distributedJoins, enforceJoinOrder, local, cb) => {
                    // Set page size for query.
                    const pageSize = 1024;

                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => {
                            const firstPage = agent.fieldsQuery(demo, nid, cacheName, query, distributedJoins, enforceJoinOrder, local, pageSize)
                                .then(({result}) => {
                                    if (result.key)
                                        return Promise.reject(result.key);

                                    return result.value;
                                });

                            const fetchResult = (acc) => {
                                if (!acc.hasMore)
                                    return acc;

                                return agent.queryFetch(demo, acc.responseNodeId, acc.queryId, pageSize)
                                    .then(({result}) => {
                                        acc.rows = acc.rows.concat(result.rows);

                                        acc.hasMore = result.hasMore;

                                        return fetchResult(acc);
                                    });
                            };

                            return firstPage
                                .then(fetchResult);
                        })
                        .then((res) => cb(null, res))
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Collect cache query metrics and return result to browser.
                sock.on('node:query:metrics', (clusterId, nids, since, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.queryDetailMetrics(demo, nids, since))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Collect cache query metrics and return result to browser.
                sock.on('node:query:reset:metrics', (clusterId, nids, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.queryResetDetailMetrics(demo, nids))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Collect running queries from all nodes in grid.
                sock.on('node:query:running', (clusterId, duration, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.queryCollectRunning(demo, duration))
                        .then((data) => {

                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Cancel running query by query id on node.
                sock.on('node:query:cancel', (clusterId, nid, queryId, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.queryCancel(demo, nid, queryId))
                        .then((data) => {

                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Return cache metadata from all nodes in grid.
                sock.on('node:cache:metadata', (clusterId, cacheName, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.metadata(demo, cacheName))
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

                            return cb(null, types);
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Fetch next page for query and return result to browser.
                sock.on('node:visor:collect', (clusterId, evtOrderKey, evtThrottleCntrKey, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.collect(demo, evtOrderKey, evtThrottleCntrKey))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Gets node configuration for specified node.
                sock.on('node:configuration', (clusterId, nid, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.collectNodeConfiguration(demo, nid))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Gets cache configurations for specified node and caches deployment IDs.
                sock.on('cache:configuration', (clusterId, nid, caches, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.collectCacheConfigurations(demo, nid, caches))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Swap backups specified caches on specified node and return result to browser.
                sock.on('node:cache:swap:backups', (clusterId, nid, cacheNames, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.cacheSwapBackups(demo, nid, cacheNames))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Reset metrics specified cache on specified node and return result to browser.
                sock.on('node:cache:reset:metrics', (clusterId, nid, cacheName, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.cacheResetMetrics(demo, nid, cacheName))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Clear specified cache on specified node and return result to browser.
                sock.on('node:cache:clear', (clusterId, nid, cacheName, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.cacheClear(demo, nid, cacheName))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Start specified cache and return result to browser.
                sock.on('node:cache:start', (clusterId, nids, near, cacheName, cfg, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.cacheStart(demo, nids, near, cacheName, cfg))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Stop specified cache on specified node and return result to browser.
                sock.on('node:cache:stop', (clusterId, nid, cacheName, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.cacheStop(demo, nid, cacheName))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });


                // Ping node and return result to browser.
                sock.on('node:ping', (clusterId, taskNid, nid, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.ping(demo, taskNid, nid))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // GC node and return result to browser.
                sock.on('node:gc', (clusterId, nids, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.gc(demo, nids))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Thread dump for node.
                sock.on('node:thread:dump', (clusterId, nid, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.threadDump(demo, nid))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Collect cache partitions.
                sock.on('node:cache:partitions', (clusterId, nids, cacheName, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.partitions(demo, nids, cacheName))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Stops given node IDs
                sock.on('node:stop', (clusterId, nids, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.stopNodes(demo, nids))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Restarts given node IDs.
                sock.on('node:restart', (clusterId, nids, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.restartNodes(demo, nids))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Collect service information from grid.
                sock.on('service:collect', (clusterId, nid, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.services(demo, nid))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Collect service information from grid.
                sock.on('service:cancel', (clusterId, nid, name, cb) => {
                    agentHnd.forCluster(currentToken(), clusterId)
                        .then((agent) => agent.serviceCancel(demo, nid, name))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });
            });
        }
    }

    return new BrowsersHandler();
};
