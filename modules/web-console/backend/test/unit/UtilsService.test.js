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

const assert = require('chai').assert;
const injector = require('../injector');

let utilsService;

suite('UtilsServiceTestsSuite', () => {
    suiteSetup(() => {
        return Promise.all([injector('services/utils')])
            .then(([_utilsService]) => {
                utilsService = _utilsService;
            });
    });

    test('Check random string generation', () => {
        const len = 16;

        const token1 = utilsService.randomString(len);
        const token2 = utilsService.randomString(len);

        assert.equal(token1.length, len);
        assert.equal(token2.length, len);

        assert.notEqual(token1, token2);
    });
});
