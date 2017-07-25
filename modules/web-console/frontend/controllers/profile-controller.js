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

// Controller for Profile screen.
export default ['profileController', [
    '$rootScope', '$scope', '$http', 'IgniteLegacyUtils', 'IgniteFocus', 'IgniteMessages', 'IgniteConfirm', 'IgniteInput', 'IgniteCountries', 'User',
    function($root, $scope, $http, LegacyUtils, Focus, Messages, Confirm, Input, Countries, User) {
        User.read()
            .then((user) => $scope.user = angular.copy(user));

        $scope.countries = Countries.getAll();

        $scope.generateToken = () => {
            Confirm.confirm('Are you sure you want to change security token?')
                .then(() => $scope.user.token = LegacyUtils.randomString(20));
        };

        const _passwordValid = () => {
            const cur = $scope.user;

            return !$scope.expandedPassword || (cur.password && cur.confirm && cur.password === cur.confirm);
        };

        const _profileChanged = () => {
            const old = $root.user;
            const cur = $scope.user;

            return !_.isEqual(old, cur);
        };

        $scope.toggleToken = () => {
            $scope.expandedToken = !$scope.expandedToken;

            if (!$scope.expandedToken)
                $scope.user.token = $root.user.token;
        };

        $scope.togglePassword = () => {
            $scope.expandedPassword = !$scope.expandedPassword;

            if ($scope.expandedPassword)
                Focus.move('profile_password');
            else {
                delete $scope.user.password;
                delete $scope.user.confirm;
            }
        };

        $scope.profileCouldBeSaved = () => _profileChanged() && $scope.profileForm && $scope.profileForm.$valid && _passwordValid();

        $scope.saveBtnTipText = () => {
            if (!_profileChanged())
                return 'Nothing to save';

            if (!_passwordValid())
                return 'Invalid password';

            return $scope.profileForm && $scope.profileForm.$valid ? 'Save profile' : 'Invalid profile settings';
        };

        $scope.saveUser = () => {
            $http.post('/api/v1/profile/save', $scope.user)
                .then(User.load)
                .then(() => {
                    if ($scope.expandedPassword)
                        $scope.togglePassword();

                    if ($scope.expandedToken)
                        $scope.toggleToken();

                    Messages.showInfo('Profile saved.');

                    Focus.move('profile-username');

                    $root.$broadcast('user', $scope.user);
                })
                .catch((err) => Messages.showError('Failed to save profile: ', err));
        };

        $scope.createOrganization = () => {
            Confirm.confirm(`Are you sure you want to create organization "${$scope.user.company}"?`)
                .then(() => {
                    const data = {
                        organization: $scope.user.company,
                        account: $scope.user._id
                    };

                    $http.post('/api/v1/organizations/create', data)
                        .then(() => Messages.showInfo(`Organization "${$scope.user.company}" created.`))
                        .catch((err) => Messages.showError('Failed to create organization: ', err));
                });
        };

        $scope.inviteUser = () => {
            Input.input('Invite user', 'e-mail')
                .then((email) => {
                    const data = {
                        email
                    };

                    $http.post('/api/v1/organizations/invite', data)
                        .then(() => Messages.showInfo(`Invite has been sent to: ${email}.`));
                })
                .catch((err) => Messages.showError('Failed to invite user: ', err));
        };
    }
]];
