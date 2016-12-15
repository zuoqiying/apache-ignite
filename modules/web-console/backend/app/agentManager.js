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
    implements: 'agent-manager',
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
 * @param AgentSocket
 * @returns {AgentManager}
 */
module.exports.factory = function(_, fs, path, JSZip, socketio, settings, mongo, AgentSocket) {
    /**
     * Connected agents manager.
     */
    class AgentManager {
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
             * Supported agents distribution.
             * @type {Object.<String, String>}
             */
            this.supportedAgents = {};

            this.collectSupportedAgents()
                .then((agentDescs) => this.supportedAgents = agentDescs);
        }

        /**
         * Collect supported agents list.
         */
        collectSupportedAgents() {
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
                            if (bParts.length === i)
                                return 1;

                            if (aParts[i] === aParts[i])
                                continue;

                            return aParts[i] > bParts[i] ? 1 : -1;
                        }
                    }));

                    // Latest version of agent distribution.
                    if (latestVer)
                        agentDescs.latest = agentDescs[latestVer];

                    return agentDescs;
                });
        }

        /**
         * @param {http.Server|https.Server} srv Server instance that we want to attach agent handler.
         */
        attach(srv) {
            if (this.io)
                throw 'Agent server already started!';

            /**
             * @type {socketIo.Server}
             */
            this.io = socketio(srv, {path: '/agents'});

            this.io.on('connection', (socket) => {
                socket.on('agent:auth', ({ver, bt, tokens}, cb) => {
                    if (_.isEmpty(tokens))
                        return cb('Tokens not set. Please reload agent archive or check settings');

                    if (!_.isEmpty(this.supportedAgents)) {
                        const btDistr = _.get(this.supportedAgents, `${ver}.buildTime`);

                        if (_.isEmpty(btDistr) || btDistr < bt)
                            return cb('You are using an older version of the agent. Please reload agent archive');
                    }

                    return mongo.Account.find({token: {$in: tokens}}, '_id token').lean().exec()
                        .then((accounts) => {
                            if (_.isEmpty(accounts))
                                return cb('Agent is failed to authenticate. Please check agent\'s tokens');

                            const activeTokens = _.map(accounts, 'token');
                            const missedTokens = _.difference(tokens, activeTokens);

                            if (missedTokens.length)
                                socket.emit('log:warn', `Invalid token(s): ${missedTokens.join(', ')}.`);

                            this.onAgentConnect(activeTokens, new AgentSocket(socket));

                            cb();
                        })
                        // TODO IGNITE-1379 send error to web master.
                        .catch(() => cb('Agent is failed to authenticate. Please check agent\'s tokens'));
                });
            });
        }

        /**
         * @param {String} token
         * @returns {Promise.<AgentSocket>}
         */
        forToken(token, demo) {
            if (!this.io)
                return Promise.reject(new Error('Agent server not started yet!'));

            const agentSockets = this._agentSockets[token];

            const socket = demo ? _.find(agentSockets, 'demoCluster') : 0;

            if (_.isNil(socket))
                return Promise.reject(new Error('Failed to find connected agent for this token'));

            if (demo)
                return Promise.resolve(_.find(agentSockets, 'demoCluster'));

            return Promise.resolve(socket);
        }

        /**
         * @param {ObjectId} token
         * @param {AgentSocket} browserSocket
         * @returns {int} Connected agent count.
         */
        onBrowserConnect(token, browserSocket) {
            let browserSockets = this._browserSockets[token];

            if (_.isNil(browserSockets))
                this._browserSockets[token] = browserSockets = [];

            browserSockets.push(browserSocket);

            const agentSockets = this._agentSockets[token];

            const cnt = _.size(agentSockets);

            browserSocket.emit('agent:count', cnt);

            // If user start demo and agents was connected before.
            if (cnt > 0) {
                const demo = browserSocket.request._query.IgniteDemoMode === 'true';

                if (demo && !_.find(agentSockets, 'demoCluster'))
                    _.head(agentSockets).startCollectTopology(true);
            }
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
         * Link agent with browsers by account.
         *
         * @param {Array.<String>} tokens
         * @param {AgentSocket} agentSocket
         */
        onAgentConnect(tokens, agentSocket) {
            const emitAgentCnt = (token, cnt) => {
                const sockets = this._browserSockets[token];

                _.forEach(sockets, (socket) => socket.emit('agent:count', cnt));
            };

            agentSocket.socket.on('disconnect', () => {
                _.forEach(tokens, (token) => {
                    const agentSockets = this._agentSockets[token];

                    _.pull(agentSockets, agentSocket);

                    emitAgentCnt(token, agentSockets.length);
                });
            });

            _.forEach(tokens, (token) => {
                const agentSockets = this._agentSockets[token];

                if (_.isNil(agentSockets)) {
                    this._agentSockets[token] = [agentSocket];

                    // If first agent join after user start demo.
                    if (_.find(this._browserSockets, (socket) => socket.request._query.IgniteDemoMode === 'true'))
                        agentSockets.startCollectTopology(true);
                }
                else
                    agentSockets.push(agentSocket);

                emitAgentCnt(token, agentSockets.length);
            });
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

    return new AgentManager();
};
