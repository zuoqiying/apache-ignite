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
    implements: 'services/configuration',
    inject: ['require(lodash)', 'mongo', 'services/space', 'services/cache']
};

module.exports.factory = (_, mongo, spaceService, cacheService) => {
    class ConfigurationService {
        static list(userId, demo) {
            let spaces;

            return spaceService.spaces(userId, demo)
                .then((_spaces) => {
                    spaces = _spaces;
                    return spaces.map((space) => space._id);
                })
                .then((spacesIds) => Promise.all([
                    cacheService.listBySpaces(spacesIds),
                    mongo.DomainModel.find({space: {$in: spacesIds}}).sort('name').lean().exec(),
                    mongo.Igfs.find({space: {$in: spacesIds}}).sort('name').lean().exec(),
                    mongo.Cluster.find({space: {$in: spacesIds}}).sort('name').deepPopulate(mongo.ClusterDefaultPopulate).lean().exec()
                ]))
                .then(([caches, domains, igfss, clusters]) => ({caches, domains, igfss, clusters, spaces}));
        }
    }

    return ConfigurationService;
};
