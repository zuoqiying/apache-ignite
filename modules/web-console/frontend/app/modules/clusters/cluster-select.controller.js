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

export default ['$scope', 'AgentManager', function($scope, agentMonitor) {
    const ctrl = this;

    ctrl.counter = 1;

    ctrl.cluster = null;
    ctrl.clusters = [];

    $scope.$watch(() => agentMonitor.cluster, (cluster) => {
        if (_.isNil(cluster))
            return ctrl.cluster = cluster;

        ctrl.cluster = _.find(ctrl.clusters, {id: cluster.id});
    }, true);

    $scope.$watchCollection(() => agentMonitor.clusters, (clusters) => {
        if (_.isEmpty(clusters)) {
            ctrl.cluster = null;

            return ctrl.clusters.length = 0;
        }

        const removed = _.differenceBy(ctrl.clusters, clusters, 'id');

        if (_.nonEmpty(removed))
            _.pullAll(ctrl.clusters, removed);

        _.forEach(clusters, (cluster) => {
            const id = cluster.id;

            if (!_.find(ctrl.clusters, {id})) {
                ctrl.clusters.push({
                    id,
                    name: `Cluster ${this.counter++}`,
                    click: () => {
                        agentMonitor.cluster = cluster;
                    }
                });
            }
        });

        if (_.isNil(ctrl.cluster) || _.isNil(_.find(ctrl.clusters, ctrl.cluster)))
            ctrl.cluster = _.head(ctrl.clusters);
    });
}];
