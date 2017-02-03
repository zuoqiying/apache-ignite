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

export class IgniteAgentModal {
    static $inject = ['$rootScope', '$state', '$modal', 'IgniteMessages'];

    constructor($root, $state, $modal, Messages) {
        const controller = this;

        controller.$state = $state;
        controller.Messages = Messages;

        // Pre-fetch modal dialogs.
        controller.modal = $modal({
            templateUrl: '/templates/agent-download.html',
            show: false,
            backdrop: 'static',
            keyboard: false,
            controller,
            controllerAs: 'ctrl'
        });

        controller.modal.$scope.$on('modal.hide.before', () => {
            Messages.hideAlert();
        });

        $root.$on('user', (event, user) => controller.user = user);
    }

    /**
     * @param {Object} err
     */
    error(err, backState) {
        if (this.modal.$scope.showModal) {
            this.modal.$promise.then(this.modal.show);

            this.Messages.showError(err);
        }
    }

    hide() {
        return this.modal.hide();
    }

    /**
     * Close dialog and go by specified link.
     */
    back() {
        this.hide();

        if (this.backState)
            this.$state.go(this.backState);
    }

    agentDisconnected(backState) {

    }

    clusterDisconnected() {
        
    }
}
