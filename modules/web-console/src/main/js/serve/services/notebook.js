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
    implements: 'services/notebook',
    inject: ['require(lodash)', 'mongo', 'services/space', 'errors']
};

module.exports.factory = (_, mongo, spaceService, errors) => {
    class NotebookService {
        /**
         * Create or update Notebook.
         * @param {Object} Notebook - The Notebook
         * @returns {Promise.<mongo.ObjectId>} that resolves Notebook id of merge operation.
         */
        static merge(Notebook) {

        }

        /**
         * Get all Notebooks by user.
         * @param {mongo.ObjectId|String} userId - The user id that own Notebook.
         * @param {Boolean} demo - The flag indicates that need lookup in demo space.
         * @returns {Promise.<mongo.Notebook[]>} - contains requested notebooks
         */
        static listByUser(userId) {

        }

        /**
         * Get one Notebook.
         * @param {mongo.ObjectId|String} userId - The user id that own Notebook.
         * @param {mongo.ObjectId|String} notebookId - The notebook id .
         * @param {Boolean} demo - The flag indicates that need lookup in demo space.
         * @returns {Promise.<mongo.Notebook[]>} - contains requested notebooks
         */
        static findFirstByUser(userId, notebookId) {

        }

        /**
         * Remove Notebook.
         * @param {mongo.ObjectId|String} notebookId - The Notebook id for remove.
         * @returns {Promise.<{rowsAffected}>} - The number of affected rows.
         */
        static remove(notebookId) {

        }

    }

    return NotebookService;
};
