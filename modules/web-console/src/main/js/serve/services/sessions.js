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
    implements: 'services/sessions',
    inject: ['require(lodash)', 'mongo']
};

/**
 * @param _
 * @param mongo
 * @returns {SessionsService}
 */
module.exports.factory = (_, mongo) => {
    class SessionsService {
        /**
         * Become user.
         * @param {Session} session - current session of user.
         * @param {mongo.ObjectId|String} viewedUserId - id of user to become.
         */
        static become(session, viewedUserId) {
            return mongo.Account.findById(viewedUserId).exec()
                .then((viewedUser) => {
                    session.viewedUser = viewedUser;
                });
        }

        /**
         * Revert to your identity.
         */
        static revert(session) {
            session.viewedUser = null;
        }
    }

    return SessionsService;
};
