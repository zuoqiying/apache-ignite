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
    implements: 'agents-handler',
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
 * @returns {AgentsHandler}
 */
module.exports.factory = function(_, fs, path, JSZip, socketio, settings, mongo, AgentSocket) {
    class AgentSockets {
        constructor() {
            /**
             * @type {Map.<String, Array.<String>>}
             */
            this.sockets = new Map();
        }

        /**
         * @param {String} token
         * @param {AgentSocket} sock
         * @return {Array.<AgentSocket>}
         */
        add(tokens, sock) {
            const sockets = this.sockets.get(token);

            _.pull(sockets, sock);

            return sockets;
        }

        /**
         * @param {Socket} browserSocket
         * @return {AgentSocket}
         */
        find(browserSocket) {
            const token = browserSocket.request.user.token;

            const sockets = this.sockets.get(token);

            return _.find(sockets, (sock) => _.includes(sock.demo.browserSockets, browserSocket));
        }
    }

    /**
     * Connected agents manager.
     */
    class AgentsHandler {
        /**
         * @constructor
         */
        constructor() {
            /**
             * Connected agents.
             * @type {AgentSockets}
             */
            this._agentSockets = new AgentSockets();
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
         * Link agent with browsers by account.
         *
         * @param {Socket} sock
         * @param {Array.<String>} tokens
         * @param {boolean} demoEnabled
         *
         * @private
         */
        onConnect(sock, tokens, demoEnabled) {
            const agentSocket = new AgentSocket(sock, tokens, demoEnabled);

            sock.on('disconnect', () => {
                _.forEach(tokens, (token) => {
                    this._agentSockets.pull(token, agentSocket);

                    this.emitAgentsCount(token);

                    // TODO reconnect to different agents.
                });
            });

            _.forEach(tokens, (token) => {
                const agentSockets = this._agentSockets[token];

                if (_.isEmpty(agentSockets)) {
                    this._agentSockets[token] = [agentSocket];

                    const browserSockets = _.filter(this._browserSockets[token], 'request._query.IgniteDemoMode');

                    // First agent join after user start demo.
                    if (_.size(browserSockets))
                        agentSocket.runDemoCluster(token, browserSockets);
                }
                else
                    agentSockets.add(agentSocket);

                this.emitAgentsCount(token);
            });

            // ioSocket.on('cluster:topology', (top) => {
            //
            // });
        }

        /**
         * @param {http.Server|https.Server} srv Server instance that we want to attach agent handler.
         * @param {BrowsersHandler} browsersHnd
         */
        attach(srv, browsersHnd) {
            this._browsersHnd = browsersHnd;

            if (this.io)
                throw 'Agent server already started!';

            this._collectSupportedAgents()
                .then((supportedAgents) => {
                    this.currentAgent = _.get(supportedAgents, 'current');

                    this.io = socketio(srv, {path: '/agents'});

                    this.io.on('connection', (sock) => {
                        sock.on('agent:auth', ({ver, bt, tokens, disableDemo}, cb) => {
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

                                    if (_.isEmpty(activeTokens))
                                        return cb(`Failed to authenticate with token(s): ${tokens.join(',')}. Please reload agent archive or check settings`);

                                    cb(activeTokens);

                                    return this.onConnect(sock, activeTokens, disableDemo);
                                })
                                // TODO IGNITE-1379 send error to web master.
                                .catch(() => cb(`Invalid token(s): ${tokens.join(',')}. Please reload agent archive or check settings`));
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

            const socks = this._agentSockets[token];

            if (_.isEmpty(socks))
                return Promise.reject(new Error('Failed to find connected agent for this token'));

            return Promise.resolve(socks);
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

        tryStopDemo(browserSocket) {
            const agentSock = this._agentSockets.find(browserSocket);
        }

        /**
         * @param {String} token
         */
        emitAgentsCount(token) {
            const agentSockets = this._agentSockets[token];

            const count = _.size(agentSockets);
            const hasDemo = !!_.find(agentSockets, (sock) => _.get(sock, 'demo.enabled'));

            _.forEach(this._browserSockets[token], (sock) => sock.emit('agent:count', {count, hasDemo}));
        }

        /**
         * @param {ObjectId} token
         * @param {Socket} browserSock
         * @returns {int} Connected agent count.
         */
        onBrowserConnect(token, browserSock) {
            this.emitAgentsCount(token);

            // If connect from browser with enabled demo.
            const demo = browserSock.request._query.IgniteDemoMode === 'true';

            // Agents where possible to run demo.
            const agentSockets = _.filter(this._agentSockets[token], 'demo.enabled');

            if (demo && _.size(agentSockets)) {
                const agentSocket = _.find(agentSockets, (agent) => _.includes(agent.demo.tokens, token));

                if (agentSocket)
                    agentSocket.attachToDemoCluster(browserSock);
                else
                    _.head(agentSockets).runDemoCluster(token, [browserSock]);
            }
        }

        /**
         * @param {Socket} browserSock.
         */
        onBrowserDisconnect(browserSock) {
            const token = browserSock.client.request.user.token;

            this._browserSockets.pull(token, browserSock);

            // If connect from browser with enabled demo.
            if (browserSock.request._query.IgniteDemoMode === 'true')
                this._agentSockets.find(token, (agent) => _.includes(agent.demo.browserSockets, browserSock));

            // TODO If latest browser with demo need stop demo cluster on agent.
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

    return new AgentsHandler();
};
