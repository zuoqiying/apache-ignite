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

import template from './field.jade!';
import './field.css!';

export default ['igniteFormField', ['$timeout', ($timeout) => {
    const controller = [function() {
        const ctrl = this;
        const models = {};

        ctrl.type = ctrl.type || 'external';

        ctrl.models = models;

        ctrl.$addModel = (ngModel) => {
            models[ngModel.$name] = ngModel;
        };
    }];

    const link = ($scope, $element, $attrs, [form, field]) => {
        let focusout;

        $element.on('focusin', () => {
            let valid = _.every(field.models, ({ $valid }) => $valid);
            $timeout.cancel(focusout);
        });

        $element.on('focusout', () => {
            let valid = _.every(field.models, ({ $valid }) => $valid);
            focusout = $timeout(valid ? field.validFocusout : field.invalidFocusout);
        });
    };

    return {
        restrict: 'E',
        scope: {},
        bindToController: {
            for: '@',
            label: '@',
            type: '@',
            name: '@',
            validFocusout: '&',
            invalidFocusout: '&'
        },
        link,
        template,
        controller,
        controllerAs: 'field',
        replace: true,
        transclude: true,
        require: ['^form', 'igniteFormField']
    };
}]];
