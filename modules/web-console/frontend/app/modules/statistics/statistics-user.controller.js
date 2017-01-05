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

const COLUMNS_DEFS = [
    {displayName: 'Url', field: 'url', minWidth: 65 },
    {displayName: 'Description', field: 'title', minWidth: 65 },
    {displayName: 'Visited', field: 'amount', minWidth: 65 }
];

export default class StatisticsCtrl {
    static $inject = ['user', 'IgniteStatisticsData'];

    constructor(user, StatisticsData) {
        const ctrl = this;
        const userId = user._id;

        ctrl.user = user;

        ctrl.gridOptions = {
            data: [],
            columnVirtualizationThreshold: 30,
            columnDefs: COLUMNS_DEFS,
            categories: [
                {name: 'URL', visible: true, selectable: true},
                {name: 'Description', visible: true, selectable: true},
                {name: 'Visited', visible: true, selectable: true}
            ],
            enableRowSelection: false,
            enableRowHeaderSelection: false,
            enableColumnMenus: false,
            multiSelect: false,
            modifierKeysToMultiSelect: true,
            noUnselect: true,
            flatEntityAccess: true,
            fastWatch: true,
            onRegisterApi: (api) => {
                ctrl.gridApi = api;
            }
        };

        StatisticsData.listByUser(userId)
            .then((data) => {
                ctrl.data = data;
            });

        const _enableColumns = (categories, visible) => {
            _.forEach(categories, (cat) => {
                cat.visible = visible;

                _.forEach(ctrl.gridOptions.columnDefs, (col) => {
                    if (col.displayName === cat.name)
                        col.visible = visible;
                });
            });

            ctrl.gridApi.grid.refresh();
        };

        const _selectableColumns = () => _.filter(ctrl.gridOptions.categories, (cat) => cat.selectable);

        ctrl.toggleColumns = (category, visible) => _enableColumns([category], visible);
        ctrl.selectAllColumns = () => _enableColumns(_selectableColumns(), true);
        ctrl.clearAllColumns = () => _enableColumns(_selectableColumns(), false);
    }
}
