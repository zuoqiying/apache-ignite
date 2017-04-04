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
    class BrowserSockets {
        constructor() {
            this.sockets = new Map();

            this.agentHnd = null;
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

    return class BrowsersHandler {
        /**
         * @constructor
         */
        constructor() {
            /**
             * Connected browsers.
             * @type {BrowserSockets}
             */
            this._browserSockets = new BrowserSockets();

            /**
             * Registered Visor task.
             * @type {Map}
             */
            this._visorTasks = new Map();
        }

        /**
         * @param {Error} err
         * @return {{code: number, message: *}}
         */
        errorTransformer(err) {
            return {
                code: err.code || 1,
                message: err.message || err
            };
        }

        /**
         * @param {String} token
         * @param {Array.<Socket>} [socks]
         */
        agentStats(token, socks = this._browserSockets.get(token)) {
            return this._agentHnd.agents(token)
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

        executeOnAgent(token, demo, event, ...args) {
            const cb = _.last(args);

            return this._agentHnd.agent(token(), demo)
                .then((agentSock) => agentSock.emitEvent(event, ..._.dropRight(args)))
                .then((res) => cb(null, res))
                .catch((err) => cb(this.errorTransformer(err)));
        }

        agentListeners(sock) {
            const demo = sock.request._query.IgniteDemoMode === 'true';
            const token = () => sock.request.user.token;

            // Return available drivers to browser.
            sock.on('schemaImport:drivers', (...args) => {
                this.executeOnAgent(token(), demo, 'schemaImport:drivers', ...args);
            });

            // Return schemas from database to browser.
            sock.on('schemaImport:schemas', (...args) => {
                this.executeOnAgent(token(), demo, 'schemaImport:schemas', ...args);
            });

            // Return tables from database to browser.
            sock.on('schemaImport:tables', (...args) => {
                this.executeOnAgent(token(), demo, 'schemaImport:tables', ...args);
            });
        }

        /**
         * @param {Promise.<AgentSocket>} agent
         * @param {Boolean} demo
         * @param {Object.<String, String>} params
         * @return {Promise.<T>}
         */
        executeOnNode(agent, demo, params) {
            return agent
                .then((agentSock) => agentSock.emitEvent('node:rest', {uri: 'ignite', demo, params, method: 'GET'}))
                .then((res) => {
                    if (res.status === 0)
                        return JSON.parse(res.data);

                    throw new Error(res.error);
                });
        }

        buildVisorTaskParams(taskId, nids, args) {
            const desc = this._visorTasks.get(taskId);

            console.log(taskId);

            const params = {
                cmd: 'exe',
                name: 'org.apache.ignite.internal.visor.compute.VisorGatewayTask',
                p1: nids,
                p2: desc.taskCls
            };

            _.forEach(_.concat(desc.argCls, args), (param, idx) => { params[`p${idx + 3}`] = param; });

            return params;
        }

        registerVisorTask(taskId, taskCls, ...argCls) {
            this._visorTasks.set(taskId, {
                taskCls,
                argCls
            });
        }

        nodeListeners(sock) {
            // Return command result from grid to browser.
            sock.on('node:rest', (clusterId, params, cb) => {
                const demo = sock.request._query.IgniteDemoMode === 'true';
                const token = sock.request.user.token;

                const agent = this._agentHnd.agent(token, demo, clusterId);

                this.executeOnNode(agent, demo, params)
                    .then((data) => cb(null, data))
                    .catch((err) => cb(this.errorTransformer(err)));
            });

            const internalVisor = (postfix) => `org.apache.ignite.internal.visor.${postfix}`;

            this.registerVisorTask('fieldsQuery', internalVisor('query.VisorQueryTask'), internalVisor('query.VisorQueryArg'));
            this.registerVisorTask('fieldsQueryV2', internalVisor('query.VisorQueryTask'), internalVisor('query.VisorQueryArgV2'));
            this.registerVisorTask('fieldsQueryV3', internalVisor('query.VisorQueryTask'), internalVisor('query.VisorQueryArgV3'));

            this.registerVisorTask('queryFetch', internalVisor('query.VisorQueryNextPageTask'),
                'org.apache.ignite.lang.IgniteBiTuple', 'java.lang.String', 'java.lang.Integer');

            this.registerVisorTask('queryClose', internalVisor('query.VisorQueryCleanupTask'),
                'java.util.Map', 'java.util.UUID', 'java.util.Set');

            // Return command result from grid to browser.
            sock.on('node:visor', (clusterId, taskId, nids, ...args) => {
                const demo = sock.request._query.IgniteDemoMode === 'true';
                const token = sock.request.user.token;

                const cb = _.last(args);
                args = _.dropRight(args);

                const agent = this._agentHnd.agent(token, demo, clusterId);
                const params = this.buildVisorTaskParams(taskId, nids, args);

                this.executeOnNode(agent, demo, params)
                    .then((data) => {
                        if (data.finished)
                            return cb(null, data.result);

                        cb(this.errorTransformer(data.error));
                    })
                    .catch((err) => cb(this.errorTransformer(err)));
            });

            // Execute query on node and return full result to browser.
            sock.on('node:query:getAll', (clusterId, nid, ...args) => {
                const demo = sock.request._query.IgniteDemoMode === 'true';
                const token = sock.request.user.token;

                // Set page size for query.
                const pageSize = 1024;

                const cb = _.last(args);
                args = _.concat(_.dropRight(args), pageSize, cb);

                const agent = this._agentHnd.agent(token, demo, clusterId);

                const fetchResult = (acc) => {
                    if (!acc.hasMore)
                        return acc;

                    const nextPageParams = this.buildVisorTaskParams('queryFetch', acc.responseNodeId, [acc.queryId, pageSize]);

                    return this.executeOnNode(agent, demo, nextPageParams)
                        .then(({result}) => {
                            acc.rows = acc.rows.concat(result.rows);

                            acc.hasMore = result.hasMore;

                            return fetchResult(acc);
                        });
                };

                const firstPageParams = this.buildVisorTaskParams('fieldsQuery', nid, args);

                return this.executeOnNode(agent, demo, firstPageParams)
                    .then(({result}) => {
                        if (result.key)
                            return Promise.reject(result.key);

                        return result.value;
                    })
                    .then(fetchResult)
                    .then((res) => cb(null, res))
                    .catch((err) => cb(this.errorTransformer(err)));
            });

            // // Collect cache query metrics and return result to browser.
            // sock.on('node:query:metrics', (clusterId, nids, since, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.queryDetailMetrics(demo, nids, since))
            //         .then((data) => {
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
            //
            // // Collect cache query metrics and return result to browser.
            // sock.on('node:query:reset:metrics', (clusterId, nids, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.queryResetDetailMetrics(demo, nids))
            //         .then((data) => {
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
            //
            // // Collect running queries from all nodes in grid.
            // sock.on('node:query:running', (clusterId, duration, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.queryCollectRunning(demo, duration))
            //         .then((data) => {
            //
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
            //
            // // Cancel running query by query id on node.
            // sock.on('node:query:cancel', (clusterId, nid, queryId, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.queryCancel(demo, nid, queryId))
            //         .then((data) => {
            //
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
            //
            // // Fetch next page for query and return result to browser.
            // sock.on('node:visor:collect', (clusterId, evtOrderKey, evtThrottleCntrKey, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.collect(demo, evtOrderKey, evtThrottleCntrKey))
            //         .then((data) => {
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
            //
            // // Gets node configuration for specified node.
            // sock.on('node:configuration', (clusterId, nid, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.collectNodeConfiguration(demo, nid))
            //         .then((data) => {
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
            //
            // // Gets cache configurations for specified node and caches deployment IDs.
            // sock.on('cache:configuration', (clusterId, nid, caches, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.collectCacheConfigurations(demo, nid, caches))
            //         .then((data) => {
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
            //
            // // Swap backups specified caches on specified node and return result to browser.
            // sock.on('node:cache:swap:backups', (clusterId, nid, cacheNames, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.cacheSwapBackups(demo, nid, cacheNames))
            //         .then((data) => {
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
            //
            // // Reset metrics specified cache on specified node and return result to browser.
            // sock.on('node:cache:reset:metrics', (clusterId, nid, cacheName, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.cacheResetMetrics(demo, nid, cacheName))
            //         .then((data) => {
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
            //
            // // Clear specified cache on specified node and return result to browser.
            // sock.on('node:cache:clear', (clusterId, nid, cacheName, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.cacheClear(demo, nid, cacheName))
            //         .then((data) => {
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
            //
            // // Start specified cache and return result to browser.
            // sock.on('node:cache:start', (clusterId, nids, near, cacheName, cfg, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.cacheStart(demo, nids, near, cacheName, cfg))
            //         .then((data) => {
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
            //
            // // Stop specified cache on specified node and return result to browser.
            // sock.on('node:cache:stop', (clusterId, nid, cacheName, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.cacheStop(demo, nid, cacheName))
            //         .then((data) => {
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
            //
            //
            // // Ping node and return result to browser.
            // sock.on('node:ping', (clusterId, taskNid, nid, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.ping(demo, taskNid, nid))
            //         .then((data) => {
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
            //
            // // GC node and return result to browser.
            // sock.on('node:gc', (clusterId, nids, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.gc(demo, nids))
            //         .then((data) => {
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
            //
            // // Thread dump for node.
            // sock.on('node:thread:dump', (clusterId, nid, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.threadDump(demo, nid))
            //         .then((data) => {
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
            //
            // // Collect cache partitions.
            // sock.on('node:cache:partitions', (clusterId, nids, cacheName, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.partitions(demo, nids, cacheName))
            //         .then((data) => {
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
            //
            // // Stops given node IDs
            // sock.on('node:stop', (clusterId, nids, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.stopNodes(demo, nids))
            //         .then((data) => {
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
            //
            // // Restarts given node IDs.
            // sock.on('node:restart', (clusterId, nids, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.restartNodes(demo, nids))
            //         .then((data) => {
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
            //
            // // Collect service information from grid.
            // sock.on('service:collect', (clusterId, nid, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.services(demo, nid))
            //         .then((data) => {
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
            //
            // // Collect service information from grid.
            // sock.on('service:cancel', (clusterId, nid, name, cb) => {
            //     agentHnd.agent(currentToken(), demo, clusterId)
            //         .then((agent) => agent.serviceCancel(demo, nid, name))
            //         .then((data) => {
            //             if (data.finished)
            //                 return cb(null, data.result);
            //
            //             cb(this.errorTransformer(data.error));
            //         })
            //         .catch((err) => cb(this.errorTransformer(err)));
            // });
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
                this._browserSockets.add(sock);

                // Handle browser disconnect event.
                sock.on('disconnect', () => {
                    this._browserSockets.remove(sock);

                    const demo = sock.request._query.IgniteDemoMode === 'true';

                    // Stop demo if latest demo tab for this token.
                    demo && agentHnd.tryStopDemo(sock);
                });

                this.agentListeners(sock);
                this.nodeListeners(sock);

                this.agentStats(sock.request.user.token, [sock]);
            });
        }
    };
};
