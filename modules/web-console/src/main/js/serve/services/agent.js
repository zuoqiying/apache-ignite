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
    implements: 'services/agent',
    inject: ['require(lodash)', 'mongo', 'services/space', 'errors']
};

module.exports.factory = (_, mongo, spaceService, errors) => {
    class AgentService {

        /**
         * Get agent archive with user agent configuration.
         * @returns {stream.Readable} - readable stream for further piping. (http://stuk.github.io/jszip/documentation/api_jszip/generate_node_stream.html)
         */
        static getArchive(token, serverUrl) {

        }
    }

    return AgentService;
};
