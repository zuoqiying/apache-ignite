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
             * @type {Object.<ObjectId, Array.<AgentSocket>>}
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
            if (this.socket)
                throw 'Agent server already started!';

            /**
             * @type {socketIo.Server}
             */
            this.socket = socketio(srv);

            this.socket.on('connection', (socket) => {
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

                            const missedTokens = _.difference(tokens, _.map(accounts, 'token'));

                            if (missedTokens.length)
                                socket.emit('agent:warning', `Failed to authenticate with token(s): ${missedTokens.join(', ')}.`);

                            this.attachAgent(accounts, new AgentSocket(socket));

                            cb();
                        })
                        // TODO IGNITE-1379 send error to web master.
                        .catch(() => cb('Agent is failed to authenticate. Please check agent\'s tokens'));
                });
            });
        }

        /**
         * Send active browsers new agent count.
         * @param {mongo.ObjectId} accId Account id.
         * @param count New agents count.
         * @private
         */
        _sendNewAgentCount(accId, count) {
            const browserSockets = this._browserSockets[accId];

            _.forEach(browserSockets, (socket) => socket.emit('agent:count', {count}));
        }

        /**
         * Link agent with browsers by account.
         *
         * @param {Array.<mongo.Account>} accounts
         * @param {AgentSocket} agentSocket
         */
        attachAgent(accounts, agentSocket) {
            const accIds = _.map(accounts, '_id');

            agentSocket.socket.on('disconnect', () => {
                _.forEach(accIds, (accId) => {
                    const agentSockets = this._agentSockets[accId];

                    if (agentSockets && agentSockets.length)
                        _.pull(agentSockets, agentSocket);

                    this._sendNewAgentCount(accId, agentSockets.length);
                });
            });

            _.forEach(accIds, (accId) => {
                let agentSockets = this._agentSockets[accId];

                if (_.isNil(agentSockets))
                    this._agentSockets[accId] = agentSockets = [];

                agentSockets.push(agentSocket);

                this._sendNewAgentCount(accId, agentSockets.length);
            });
        }

        /**
         * @param {ObjectId} accId
         * @returns {Promise.<AgentSocket>}
         */
        forAccount(accId) {
            if (!this.socket)
                return Promise.reject(new Error('Agent server not started yet!'));

            const agentSockets = this._agentSockets[accId];

            if (_.isEmpty(agentSockets))
                return Promise.reject(new Error('Failed to connect to agent'));

            return Promise.resolve(agentSockets[0]);
        }

        /**
         * @param {ObjectId} accId
         * @param {AgentSocket} browserSocket
         * @returns {int} Connected agent count.
         */
        connectBrowser(accId, browserSocket) {
            let browserSockets = this._browserSockets[accId];

            if (!browserSockets)
                this._browserSockets[accId] = browserSockets = [];

            browserSockets.push(browserSocket);

            const count = _.get(this._agentSockets[accId], 'length') || 0;

            browserSocket.emit('agent:count', {count});
        }

        /**
         * @param {AgentSocket} browserSocket.
         */
        disconnectBrowser(browserSocket) {
            const accId = browserSocket.client.request.user._id;

            const browserSockets = this._browserSockets[accId];

            _.pull(browserSockets, browserSocket);
        }

        /**
         * Try stop agent for account if not used by others.
         *
         * @param {mongo.<Account>} account
         */
        tryStopAgents(account) {
            if (_.isNil(this.socket))
                return;

            const accId = account._id;

            const agentsForClose = this._agentSockets[accId];

            this._agentSockets[accId] = [];

            const agentsForWarning = _.clone(agentsForClose);

            _.forEach(this._agentSockets, (agentSockets) => _.pullAll(agentsForClose, agentSockets));

            _.pullAll(agentsForWarning, agentsForClose);

            const msg = `Security token has been reset: ${account.token}`;

            _.forEach(agentsForWarning, (socket) => socket._emit('agent:warning', msg));

            _.forEach(agentsForClose, (socket) => socket._emit('agent:close', msg));

            this._sendNewAgentCount(accId, 0);
        }
    }

    return new AgentManager();
};
