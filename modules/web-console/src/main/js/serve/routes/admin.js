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

module.exports = {
    implements: 'routes/admin',
    inject: ['require(lodash)', 'require(express)', 'settings', 'mongo', 'services/spaces', 'services/mails', 'services/sessions', 'services/users']
};

/**
 * @param _
 * @param express
 * @param settings
 * @param mongo
 * @param spacesService
 * @param {MailsService} mailsService
 * @param {SessionsService} sessionsService
 * @param {UsersService} usersService
 * @returns {Promise}
 */
module.exports.factory = function(_, express, settings, mongo, spacesService, mailsService, sessionsService, usersService) {
    return new Promise((factoryResolve) => {
        const router = new express.Router();

        /**
         * Get list of user accounts.
         */
        router.post('/list', (req, res) => {
            usersService.list()
                .then(res.api.ok)
                .catch(res.api.error);
        });

        // Remove user.
        router.post('/remove', (req, res) => {
            usersService.remove(req.headers.referer, req.body.userId)
                .then(() => res.sendStatus(200))
                .catch(res.api.error);
        });

        // Save user.
        router.post('/save', (req, res) => {
            const params = req.body;

            mongo.Account.findByIdAndUpdate(params.userId, {admin: params.adminFlag}).exec()
                .then(() => res.sendStatus(200))
                .catch((err) => mongo.handleError(res, err));
        });

        // Become user.
        router.get('/become', (req, res) => {
            sessionsService.become(req.session, req.query.viewedUserId)
                .then(() => res.sendStatus(200))
                .catch(() => res.sendStatus(401));
        });

        // Revert to your identity.
        router.get('/revert/identity', (req, res) => {
            sessionsService.become(req.session)
                .then(() => res.sendStatus(200))
                .catch(() => res.sendStatus(401));
        });

        factoryResolve(router);
    });
};

