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
    implements: 'profile-routes',
    inject: ['require(lodash)', 'require(express)', 'mongo', 'services/user', 'agent-manager']
};

/**
 *
 * @param _ Lodash module
 * @param express Express module
 * @param mongo
 * @param {UserService} userService
 * @returns {Promise}
 */
module.exports.factory = function(_, express, mongo, userService) {
    return new Promise((resolveFactory) => {
        const router = new express.Router();

        /**
         * Save user profile.
         */
        router.post('/save', (req, res) => {
            if (req.body.password && _.isEmpty(req.body.password))
                return res.status(500).send('Wrong value for new password!');

            userService.save(req.body)
                .then((user) => {
                    const becomeUsed = req.session.viewedUser && user.admin;

                    if (becomeUsed)
                        req.session.viewedUser = user;

                    return user;
                })
                .then(res.api.ok)
                .catch(res.api.error);
        });

        resolveFactory(router);
    });
};
