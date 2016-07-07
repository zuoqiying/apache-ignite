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
    implements: 'services/account',
    inject: ['require(lodash)', 'mongo', 'services/space', 'errors']
};

module.exports.factory = (_, mongo, spaceService, errors) => {
    class AccountService {

        /**
         * Create or update account.
         * @param {Object} account - The account
         * @returns {Promise.<mongo.ObjectId>} that resolves account id of merge operation.
         */
        static merge(account) {

        }

        /**
         * Save profile information.
         * @param {Object} account - The account
         * @returns {Promise.<mongo.ObjectId>} that resolves account id of merge operation.
         */
        // TODO move profile saving logic from profile router || merge with save logic.
        static profile(account) {

        }

        /**
         * Get list of user accounts and summary information.
         * @returns {mongo.Account[]} - returns all accounts with counters object
         */
        static list() {

        }

        /**
         * Remove account.
         * @param {mongo.ObjectId|String} accountId - The account id for remove.
         * @returns {Promise.<{rowsAffected}>} - The number of affected rows.
         */
        static remove(accountId) {

        }

        /**
         * Get account information.
         */
        static get() {

        }

    }

    return AccountService;
};
