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
    implements: 'services/statistics',
    inject: ['mongo']
};

/**
 * @param mongo
 * @returns {StatisticsService}
 */
module.exports.factory = (mongo) => {
    class StatisticsService {
        /**
         * Update page statistics.
         *
         * @param {String} owner - User ID
         * @param {Object} page - The page
         * @returns {Promise.<mongo.ObjectId>} that resolve activity
         */
        static merge(owner, {title, url, name}) {
            mongo.Account.findById(owner)
                .then((user) => {
                    user.lastActivity = new Date();

                    return user.save();
                });

            return mongo.Statistics.findOne({ owner, url }).exec()
                .then((statistic) => {
                    if (statistic) {
                        statistic.title = title;
                        statistic.amount++;

                        return statistic.save();
                    }

                    return mongo.Statistics.create({ owner, title, url, name });
                });
        }

        /**
         * Get user activities
         * @param {String} owner - User ID
         * @returns {Promise.<mongo.ObjectId>} that resolve activities
         */
        static listByUser(owner) {
            return mongo.Statistics.find({ owner });
        }
    }

    return StatisticsService;
};
