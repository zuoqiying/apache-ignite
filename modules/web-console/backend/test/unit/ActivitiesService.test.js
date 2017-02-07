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

let activitiesService;
let mongo;
let errors;
let db;


suite('ActivitiesServiceTestsSuite', () => {
    suiteSetup(() => {
        console.log(1);
        return Promise.all([
            // [injector('services/activities'),
            // injector('mongo')
            injector('errors'),
            injector('dbHelper')
            ])
            .then(([_activitiesService, _mongo, _errors, _db]) => {
                console.log(2);
                // mongo = _mongo;
                // activitiesService = _activitiesService;
                // errors = _errors;
                // db = _db;
            })
            .catch((...args) => {
                console.log(3, args);
            });
    });

    setup(() => {
      // db.init();
    });

    test('', (done) => {
        done();
    });
});