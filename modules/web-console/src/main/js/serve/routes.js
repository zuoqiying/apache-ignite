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
    implements: 'routes',
    inject: ['routes/public', 'routes/admin', 'routes/profiles', 'routes/demo', 'routes/clusters', 'routes/domains',
        'routes/caches', 'routes/igfss', 'routes/notebooks', 'routes/agents', 'routes/configurations']
};

module.exports.factory = function(publicRoute, adminRoute, profilesRoute, demoRoute,
    clustersRoute, domainsRoute, cachesRoute, igfssRoute, notebooksRoute, agentsRoute, configurationsRoute) {
    return {
        register: (app) => {
            const _mustAuthenticated = (req, res, next) => req.isAuthenticated() ? next() : res.redirect('/');

            const _adminOnly = (req, res, next) => req.isAuthenticated() && req.user.admin ? next() : res.sendStatus(403);

            // Registering the standard routes
            app.use('/', publicRoute);
            app.use('/admin', _mustAuthenticated, _adminOnly, adminRoute);
            app.use('/profile', _mustAuthenticated, profilesRoute);
            app.use('/demo', _mustAuthenticated, demoRoute);

            app.all('/configuration/*', _mustAuthenticated);

            app.use('/configuration', configurationsRoute);
            app.use('/configuration/clusters', clustersRoute);
            app.use('/configuration/domains', domainsRoute);
            app.use('/configuration/caches', cachesRoute);
            app.use('/configuration/igfs', igfssRoute);

            app.use('/notebooks', _mustAuthenticated, notebooksRoute);
            app.use('/agent', _mustAuthenticated, agentsRoute);

            // Catch 404 and forward to error handler.
            app.use((req, res, next) => {
                const err = new Error('Not Found: ' + req.originalUrl);

                err.status = 404;

                next(err);
            });

            // Production error handler: no stacktraces leaked to user.
            app.use((err, req, res) => {
                res.status(err.status || 500);

                res.render('error', {
                    message: err.message,
                    error: {}
                });
            });
        }
    };
};
