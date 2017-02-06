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
 * Module interaction with agents.
 */
module.exports = {
    implements: 'agents-server',
    inject: ['require(lodash)', 'require(fs)', 'require(path)', 'require(jszip)', 'require(socket.io)', 'settings', 'mongo', 'agent-socket']
};

/**
 * @param _
 * @param fs
 * @param path
 * @param JSZip
 * @param socketio
 * @param settings
 * @param mongo
 * @param {AgentSocket} AgentSocket
 * @returns {AgentsServer}
 */
module.exports.factory = function(_, fs, path, JSZip, socketio, settings, mongo, AgentSocket) {
    /**
     * Connected agents manager.
     */
    class AgentsServer {
        /**
         * @constructor
         */
        constructor() {
            /**
             * Connected agents by user id.
             * @type {Object.<String, Array.<AgentSocket>>}
             */
            this._agentSockets = {};

            /**
             * Connected browsers by user id.
             * @type {Object.<ObjectId, Array.<Socket>>}
             */
            this._browserSockets = {};

            /**
             * Connected browsers by user id.
             * @type {Object.<ObjectId, Array.<Socket>>}
             */
            this._browserSockets = {};
        }

        /**
         * Collect supported agents list.
         * @private
         */
        _collectSupportedAgents() {
            const jarFilter = (file) => path.extname(file) === '.jar';

            const agentArchives = fs.readdirSync(settings.agent.dists)
                .filter((file) => path.extname(file) === '.zip');

            const agentsPromises = _.map(agentArchives, (fileName) => {
                const filePath = path.join(settings.agent.dists, fileName);

                return JSZip.loadAsync(fs.readFileSync(filePath))
                    .then((zip) => {
                        const jarPath = _.find(_.keys(zip.files), jarFilter);

                        return JSZip.loadAsync(zip.files[jarPath].async('nodebuffer'))
                            .then((jar) => jar.files['META-INF/MANIFEST.MF'].async('string'))
                            .then((lines) => lines.trim()
                                .split(/\s*\n+\s*/)
                                .map((line, r) => {
                                    r = line.split(/\s*:\s*/);

                                    this[r[0]] = r[1];

                                    return this;
                                }, {})[0])
                            .then((manifest) => {
                                const ver = manifest['Implementation-Version'];
                                const buildTime = manifest['Build-Time'];

                                if (ver && buildTime)
                                    return { fileName, filePath, ver, buildTime };
                            });
                    });
            });

            return Promise.all(agentsPromises)
                .then((descs) => {
                    const agentDescs = _.keyBy(_.remove(descs, null), 'ver');

                    const latestVer = _.head(Object.keys(agentDescs).sort((a, b) => {
                        const aParts = a.split('.');
                        const bParts = b.split('.');

                        for (let i = 0; i < aParts.length; ++i) {
                            if (aParts[i] !== bParts[i])
                                return aParts[i] < bParts[i] ? 1 : -1;
                        }

                        if (aParts.length === bParts.length)
                            return 0;

                        return aParts.length < bParts.length ? 1 : -1;
                    }));

                    // Latest version of agent distribution.
                    if (latestVer)
                        agentDescs.current = agentDescs[latestVer];

                    return agentDescs;
                });
        }

        /**
         * @param {http.Server|https.Server} srv Server instance that we want to attach agent handler.
         */
        attach(srv) {
            if (this.io)
                throw 'Agent server already started!';

            this._collectSupportedAgents()
                .then((supportedAgents) => {
                    this.currentAgent = _.get(supportedAgents, 'current');

                    this.io = socketio(srv, {path: '/agents'});

                    this.io.on('connection', (agentSock) => {
                        agentSock.on('agent:auth', ({ver, bt, tokens, disableDemo}, cb) => {
                            if (_.isEmpty(tokens))
                                return cb('Tokens not set. Please reload agent archive or check settings');

                            if (!_.isEmpty(supportedAgents)) {
                                const btDistr = _.get(supportedAgents, `${ver}.buildTime`);

                                if (_.isEmpty(btDistr) || btDistr < bt)
                                    return cb('You are using an older version of the agent. Please reload agent');
                            }

                            return mongo.Account.find({token: {$in: tokens}}, '_id token').lean().exec()
                                .then((accounts) => {
                                    const activeTokens = _.uniq(_.map(accounts, 'token'));

                                    cb(activeTokens);

                                    if (!_.isEmpty(activeTokens))
                                        this.onAgentConnect(activeTokens, agentSock);
                                })
                                // TODO IGNITE-1379 send error to web master.
                                .catch(() => cb([]));
                        });
                    });
                });
        }

        /**
         * @param {String} token
         */
        forToken(token) {
            if (!this.io)
                return Promise.reject(new Error('Agent server not started yet!'));

            const socket = _.head(this._agentSockets[token]);

            if (_.isNil(socket))
                return Promise.reject(new Error('Failed to find connected agent for this token'));

            return Promise.resolve(socket);
        }

        /**
         * @param {String} token
         * @param {String} clusterId
         * @returns {Promise.<AgentSocket>}
         */
        forCluster(token, clusterId) {
            if (!this.io)
                return Promise.reject(new Error('Agent server not started yet!'));

            const agentSockets = this._agentSockets[token];

            const socket = _.find(agentSockets, ({activeClusterIds}) => _.includes(activeClusterIds, clusterId));

            if (_.isNil(socket))
                return Promise.reject(new Error('Failed to find connected agent for this token'));

            return Promise.resolve(socket);
        }

        /**
         * @param {String} token
         * @param {Array.<AgentSocket>} agentSockets
         */
        sendAgentsCount(token, agentSockets) {
            const browserSockets = this._browserSockets[token];

            const count = _.size(agentSockets);
            const hasDemo = !!_.find(agentSockets, (sock) => _.get(sock, 'demo.enabled'));

            _.forEach(browserSockets, (sock) => sock.emit('agent:count', {count, hasDemo}));
        }

        /**
         * Link agent with browsers by account.
         *
         * @param {Array.<String>} tokens
         * @param {AgentSocket} agentSock
         *
         * @private
         */
        onAgentConnect(tokens, agentSock) {
            const sock = new AgentSocket(agentSock, false);

            agentSock.on('disconnect', () => {
                _.forEach(tokens, (token) => {
                    const agentSockets = this._agentSockets[token];

                    _.pull(agentSockets, sock);

                    // TODO reconnect to different agents.

                    this.sendAgentsCount(token, agentSockets);
                });
            });

            _.forEach(tokens, (token) => {
                let agentSockets = this._agentSockets[token];

                if (_.isEmpty(agentSockets)) {
                    agentSockets = this._agentSockets[token] = [sock];

                    const browserSockets = _.filter(this._browserSockets[token],
                        (socket) => socket.request._query.IgniteDemoMode === 'true');

                    // If first agent join after user start demo.
                    sock.listenDemo(token, browserSockets);
                }
                else
                    agentSockets.push(sock);

                this.sendAgentsCount(token, agentSockets);
            });

            // ioSocket.on('cluster:topology', (top) => {
            //
            // });
        }

        /**
         * @param {ObjectId} token
         * @param {AgentSocket} browserSock
         * @returns {int} Connected agent count.
         */
        onBrowserConnect(token, browserSock) {
            let browserSockets = this._browserSockets[token];

            if (_.isNil(browserSockets))
                this._browserSockets[token] = browserSockets = [];

            browserSockets.push(browserSock);

            const agentSockets = this._agentSockets[token];

            this.sendAgentsCount(token, agentSockets);

            // If user start demo and agents was connected before.
            const demo = browserSock.request._query.IgniteDemoMode === 'true';

            if (demo && _.size(agentSockets) > 0 && !_.find(agentSockets, (agent) => agent.demoStarted(token)))
                _.head(agentSockets).listenDemo(token, [browserSock]);
        }

        /**
         * @param {AgentSocket} browserSocket.
         */
        onBrowserDisconnect(browserSocket) {
            const token = browserSocket.client.request.user.token;

            const browserSockets = this._browserSockets[token];

            _.pull(browserSockets, browserSocket);
        }

        /**
         * Try stop agent for token if not used by others.
         *
         * @param {String} token
         */
        onTokenReset(token) {
            if (_.isNil(this.io))
                return;

            const sockets = this._agentSockets[token];

            _.forEach(sockets, (socket) => socket._emit('agent:reset:token', token));
        }
    }

    return new AgentsServer();
};
