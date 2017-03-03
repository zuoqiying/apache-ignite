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
    implements: 'browsers-server',
    inject: ['require(lodash)', 'require(socket.io)', 'configure', 'agents-server']
};

module.exports.factory = (_, socketio, configure, agentMgr) => {
    const _errorToJson = (err) => {
        return {
            code: err.code || 1,
            message: err.message || err
        };
    };

    class BrowsersServer {
        attach(server) {
            const io = socketio(server);

            configure.socketio(io);

            io.sockets.on('connection', (socket) => {
                const demo = socket.request._query.IgniteDemoMode === 'true';

                const currentToken = () => socket.request.user.token;

                const clusterId = () => demo ? 'DEMO' : 'CLUSTER';

                const resendToAgent = (event) => {
                    return (...args) => {
                        const cb = _.last(args);

                        return agentMgr.forToken(currentToken())
                            .then((agent) => agent.emitEvent(event, ..._.dropRight(args)))
                            .then((res) => cb(null, res))
                            .catch((err) => cb(_errorToJson(err)));
                    };
                };

                // Return available drivers to browser.
                socket.on('schemaImport:drivers', resendToAgent('schemaImport:drivers'));
                // Return schemas from database to browser.
                socket.on('schemaImport:schemas', resendToAgent('schemaImport:schemas'));
                // Return tables from database to browser.
                socket.on('schemaImport:tables', resendToAgent('schemaImport:tables'));

                // const resendToCluster = (event) => {
                //     return (...args) => {
                //         const cb = _.last(args);
                //
                //         return agentMgr.forCluster(currentToken(), clusterId())
                //             .then((agent) => agent.emitEvent(event, ..._.dropRight(args)))
                //             .then((res) => cb(null, res))
                //             .catch((err) => cb(_errorToJson(err)));
                //     };
                // };

                // Return topology command result from grid to browser.
                socket.on('node:topology', (attr, mtr, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.topology(demo, attr, mtr))
                        .then((clusters) => cb(null, clusters))
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Close query on node.
                socket.on('node:query:close', (nid, queryId, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.queryClose(demo, nid, queryId))
                        .then(() => cb())
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Execute query on node and return first page to browser.
                socket.on('node:query', (nid, cacheName, query, distributedJoins, local, pageSize, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.fieldsQuery(demo, nid, cacheName, query, distributedJoins, local, pageSize))
                        .then((res) => cb(null, res))
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Fetch next page for query and return result to browser.
                socket.on('node:query:fetch', (nid, queryId, pageSize, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.queryFetch(demo, nid, queryId, pageSize))
                        .then((res) => cb(null, res))
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Execute query on node and return full result to browser.
                socket.on('node:query:getAll', (nid, cacheName, query, distributedJoins, local, cb) => {
                    // Set page size for query.
                    const pageSize = 1024;

                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => {
                            const firstPage = agent.fieldsQuery(demo, nid, cacheName, query, distributedJoins, local, pageSize)
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
                socket.on('node:query:metrics', (nids, since, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.queryDetailMetrics(demo, nids, since))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Collect cache query metrics and return result to browser.
                socket.on('node:query:reset:metrics', (nids, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.queryResetDetailMetrics(demo, nids))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Collect running queries from all nodes in grid.
                socket.on('node:query:running', (duration, cb) => {
                    agentMgr.findAgent(accountId())
                        .then((agent) => agent.queryCollectRunning(demo, duration))
                        .then((data) => {

                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Cancel running query by query id on node.
                socket.on('node:query:cancel', (nid, queryId, cb) => {
                    agentMgr.findAgent(accountId())
                        .then((agent) => agent.queryCancel(demo, nid, queryId))
                        .then((data) => {

                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Return cache metadata from all nodes in grid.
                socket.on('node:cache:metadata', (cacheName, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
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
                socket.on('node:visor:collect', (evtOrderKey, evtThrottleCntrKey, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.collect(demo, evtOrderKey, evtThrottleCntrKey))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Gets node configuration for specified node.
                socket.on('node:configuration', (nid, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.collectNodeConfiguration(demo, nid))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Gets cache configurations for specified node and caches deployment IDs.
                socket.on('cache:configuration', (nid, caches, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.collectCacheConfigurations(demo, nid, caches))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Swap backups specified caches on specified node and return result to browser.
                socket.on('node:cache:swap:backups', (nid, cacheNames, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.cacheSwapBackups(demo, nid, cacheNames))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Reset metrics specified cache on specified node and return result to browser.
                socket.on('node:cache:reset:metrics', (nid, cacheName, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.cacheResetMetrics(demo, nid, cacheName))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Clear specified cache on specified node and return result to browser.
                socket.on('node:cache:clear', (nid, cacheName, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.cacheClear(demo, nid, cacheName))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Start specified cache and return result to browser.
                socket.on('node:cache:start', (nids, near, cacheName, cfg, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.cacheStart(demo, nids, near, cacheName, cfg))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Stop specified cache on specified node and return result to browser.
                socket.on('node:cache:stop', (nid, cacheName, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.cacheStop(demo, nid, cacheName))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });


                // Ping node and return result to browser.
                socket.on('node:ping', (taskNid, nid, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.ping(demo, taskNid, nid))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // GC node and return result to browser.
                socket.on('node:gc', (nids, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.gc(demo, nids))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Thread dump for node.
                socket.on('node:thread:dump', (nid, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.threadDump(demo, nid))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Collect cache partitions.
                socket.on('node:cache:partitions', (nids, cacheName, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.partitions(demo, nids, cacheName))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Stops given node IDs
                socket.on('node:stop', (nids, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.stopNodes(demo, nids))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Restarts given node IDs.
                socket.on('node:restart', (nids, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.restartNodes(demo, nids))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Collect service information from grid.
                socket.on('service:collect', (nid, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.services(demo, nid))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                // Collect service information from grid.
                socket.on('service:cancel', (nid, name, cb) => {
                    agentMgr.forCluster(currentToken(), clusterId())
                        .then((agent) => agent.serviceCancel(demo, nid, name))
                        .then((data) => {
                            if (data.finished)
                                return cb(null, data.result);

                            cb(_errorToJson(data.error));
                        })
                        .catch((err) => cb(_errorToJson(err)));
                });

                agentMgr.onBrowserConnect(currentToken(), socket);
            });

            // Handle browser disconnect event.
            io.sockets.on('disconnect', agentMgr.onBrowserDisconnect);
        }
    }

    return new BrowsersServer();
};
