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
    implements: 'services/cluster',
    inject: ['require(lodash)', 'mongo', 'services/space', 'errors']
};

module.exports.factory = (_, mongo, spaceService, errors) => {
    class ClusterService {
        /**
         * Create or update cluster.
         * @param {Object} cluster - The cluster
         * @returns {Promise.<mongo.ObjectId>} that resolves cluster id of merge operation.
         */
        static merge(cluster) {

        }

        /**
         * Get clusters and linked objects by user.
         * @param {mongo.ObjectId|String} userId - The user id that own cluster.
         * @param {Boolean} demo - The flag indicates that need lookup in demo space.
         * @returns {Promise.<[mongo.Cache[], mongo.Cluster[], mongo.DomainModel[], mongo.Space[]]>} - contains requested caches and array of linked objects: clusters, domains, spaces.
         */
        static listByUser(userId, demo) {

        }

        /**
         * Remove cluster.
         * @param {mongo.ObjectId|String} clusterId - The cluster id for remove.
         * @returns {Promise.<{rowsAffected}>} - The number of affected rows.
         */
        static remove(clusterId) {

        }

        /**
         * Remove all clusters by user.
         * @param {mongo.ObjectId|String} userId - The user id that own cluster.
         * @param {Boolean} demo - The flag indicates that need lookup in demo space.
         * @returns {Promise.<{rowsAffected}>} - The number of affected rows.
         */
        static removeAll(userId, demo) {

        }
    }

    return ClusterService;
};
