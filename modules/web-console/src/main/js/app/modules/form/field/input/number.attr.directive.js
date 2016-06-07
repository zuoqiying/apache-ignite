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

export default ['igniteFormFieldInputNumber', ['$parse', 'IgniteFormGUID', '$table', ($parse, guid, $table) => {
    const link = (scope, el, attrs, [formCtrl, ngModelCtrl]) => {
        const {id, name, ngModel} = attrs;
        const getter = $parse(ngModel);

        // const field = form[name];

        // scope.id = id || guid();
        // scope.field = field;

        // if (label) {
        //     label.for = scope.id;

        //     scope.$watch('required', (required) => {
        //         label.required = required || false;
        //     });
        // }

        const setDefault = () => {
            const value = getter(scope);

            formCtrl.$defaults = formCtrl.$defaults || {};
            formCtrl.$defaults[name] = _.cloneDeep(value);

            if (parseFloat(ngModelCtrl.$viewValue) === parseFloat(value))
                return;

            ngModelCtrl.$setViewValue(formCtrl.$defaults[name]);
            ngModelCtrl.$render();

            formCtrl.$setPristine();
        };

        const updateDefault = (newV, oldV) => {
            if (!formCtrl.$pristine)
                return;

            setDefault();
        };

        scope.$watch(() => getter(scope), updateDefault);
        scope.$watch(() => formCtrl.$pristine, updateDefault);

        // scope.$watch(() => formCtrl.$pristine, (pristine) => {
        //     if (!pristine) return;
        //     if (parseFloat(ngModelCtrl.$viewValue) === parseFloat(formCtrl.$defaults[name])) return;

        //     console.log('attr');


        // });
        // scope.$watch('value', setAsDefault);

        // // TODO LEGACY
        // scope.tableReset = () => {
        //     $table.tableSaveAndReset();
        // };

        // setDefault();
    };

    return {
        restrict: 'A',
        link,
        replace: true,
        transclude: true,
        require: ['^form', 'ngModel']
    };
}]];
