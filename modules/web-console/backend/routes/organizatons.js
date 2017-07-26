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
    implements: 'routes/organizations',
    inject: ['require(lodash)', 'require(express)', 'mongo', 'errors', 'settings', 'services/mails', 'services/utils']
};

/**
 *
 * @param _ Lodash module
 * @param express Express module
 * @param mongo
 * @param errors
 * @param settings
 * @param {MailsService} mailsService
 * @param {UtilsService} utilsService
 * @returns {Promise}
 */
module.exports.factory = function (_, express, mongo, errors, settings, mailsService, utilsService) {
    return new Promise((resolveFactory) => {
        const router = new express.Router();

        const _createOrganization = (data) => {
            if (_.isEmpty(data.organization))
                throw new Error('Organization name was not specified!');

            return mongo.Organization.create({name: data.organization})
                .then((savedOrganization) =>
                    mongo.Account.update({_id: data.account}, {$set: {organization: savedOrganization._id, organizationAdmin: true}}).exec()
                        .then(() => savedOrganization)
                )
                .catch((err) => {
                    if (err.code === mongo.errCodes.DUPLICATE_KEY_ERROR)
                        throw new Error(`Organization with name: "${data.organization}" already exist.`);
                    else
                        throw err;
                });
        };

        // Create organization.
        router.post('/create', (req, res) => {
            _createOrganization(req.body)
                .then((organization) => res.api.ok(organization._id))
                .catch(res.api.error);
        });

        const _createInvite = (host, user, data) => {
            if (_.isEmpty(data.email))
                throw new Error('User e-mail was not specified!');

            return mongo.Account.findOne({email: data.email})
                .then((foundAccount) => {
                        const invite = {
                            token: utilsService.randomString(settings.tokenLength),
                            organization: data.organization,
                            email: data.email
                        };

                        if (foundAccount)
                            invite.account = foundAccount._id;

                        return mongo.Invite.create(invite)
                            .then((savedInvite) => {
                                return mailsService.emailInvite(host, user, savedInvite);
                            })
                            .catch((err) => {
                                console.log(err);
                            });
                    }
                );
        };

        // Invite user to join organization.
        router.post('/invite', (req, res) => {
            _createInvite(req.origin(), {name: 'Test'},  req.body)
                .then(res.api.ok)
                .catch(res.api.error);
        });

        // Find invite and return data.
        router.post('/info', (req, res) => {
            mongo.Invite.findOne({token: '12345'})
                .then(res.api.ok({organization: {name: 'Test'}, email: 'test@test.com'}))
                .catch(res.api.error);
        });


        // // Add user to organization.
        // router.post('/add', (req, res) => {
        //     const data = res.body;
        //
        //     return mongo.Invite.findOne({token: data.token}).exec();
        //
        //     return mongo.Account.findOne({_id: data.account})
        //         .then((foundUser) => mailsService.emailInvite(foundUser));
        // });

        resolveFactory(router);
    });
};
