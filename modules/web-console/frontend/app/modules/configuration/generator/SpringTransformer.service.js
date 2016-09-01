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

import _ from 'lodash';

import AbstractTransformer from './AbstractTransformer';
import StringBuilder from './StringBuilder';

import $generatorSpring from './generator-spring';

export default ['SpringTransformer', ['JavaTypes', 'igniteEventGroups', 'ConfigurationGenerator', (JavaTypes, eventGroups, generator) => {
    return class SpringTransformer extends AbstractTransformer {
        static generator = generator;

        static comment(sb, ...lines) {
            _.forEach(lines, (line) => sb.append(`<!-- ${line} -->`));
        }

        static commentBlock(sb, ...lines) {
            sb.append('<!--');

            _.forEach(lines, (line) => sb.append(`  ${line}`));

            sb.append('-->');
        }

        static appendBean(sb, bean) {
            sb.startBlock(`<bean class="${bean.clsName}">`);

            _.forEach(bean.arguments, (arg) => {
                switch (arg.type) {
                    case 'CLASS':
                    case 'STRING':
                    case 'PROPERTY':
                        sb.append(`<constructor-arg value="${arg.value}"/>`);

                        break;

                    default:
                        // No-op.
                }
            });

            this._setProperties(sb, bean);

            sb.endBlock('</bean>');
        }

        /**
         *
         * @param {StringBuilder} sb
         * @param {Bean} bean
         * @returns {StringBuilder}
         */
        static _setProperties(sb, bean) {
            _.forEach(bean.properties, (prop) => {
                switch (prop.type) {
                    case 'PROPERTY':
                    case 'STRING':
                    case 'ENUM':
                        sb.append(`<property name="${prop.name}" value="${prop.value}"/>`);

                        break;
                    case 'DATASOURCE':
                        sb.append(`<property name="${prop.name}" ref="${prop.id}"/>`);

                        break;
                    case 'EVENT_TYPES':
                        sb.startBlock(`<property name="${prop.name}">`);

                        if (prop.eventTypes.length === 1) {
                            const evtGrp = _.find(eventGroups, {value: _.head(prop.eventTypes)});

                            evtGrp && sb.append(`<util:constant static-field="${evtGrp.class}.${evtGrp.value}"/>`);
                        }
                        else {
                            sb.startBlock('<list>');

                            _.forEach(prop.eventTypes, (item, ix) => {
                                ix > 0 && sb.emptyLine();

                                const evtGrp = _.find(eventGroups, {value: item});

                                if (evtGrp) {
                                    sb.append(`<!-- EventType.${item} -->`);

                                    _.forEach(evtGrp.events, (event) =>
                                        sb.append(`<util:constant static-field="${evtGrp.class}.${event}"/>`));
                                }
                            });

                            sb.endBlock('</list>');
                        }

                        sb.endBlock('</property>');

                        break;
                    case 'ARRAY':
                        sb.startBlock(`<property name="${prop.name}">`);
                        sb.startBlock('<array>');

                        _.forEach(prop.items, (item) => {
                            if (_.isString(item) || _.isNumber(item))
                                sb.append(`<value>${item}</value>`);
                            else
                                this.appendBean(sb, item);
                        });

                        sb.endBlock('</array>');
                        sb.endBlock('</property>');

                        break;

                    case 'COLLECTION':
                        sb.startBlock(`<property name="${prop.name}">`);
                        sb.startBlock('<list>');

                        _.forEach(prop.items, (item) => {
                            if (_.isString(item) || _.isNumber(item))
                                sb.append(`<value>${item}</value>`);
                            else
                                this.appendBean(sb, item);
                        });

                        sb.endBlock('</list>');
                        sb.endBlock('</property>');

                        break;

                    case 'MAP':
                        sb.startBlock(`<property name="${prop.name}">`);
                        sb.startBlock('<map>');

                        _.forEach(prop.entries, (entry) => {
                            sb.append(`<entry key="${entry.name}" value="${entry.value}"/>`);
                        });

                        sb.endBlock('</map>');
                        sb.endBlock('</property>');

                        break;

                    default:
                        sb.startBlock(`<property name="${prop.name}">`);

                        this.appendBean(sb, prop.value);

                        sb.endBlock('</property>');
                }
            });

            return sb;
        }

        /**
         * @param {Bean} bean
         * @param {StringBuilder} sb
         * @returns {String}
         */
        static generateSection(bean, sb = new StringBuilder()) {
            this._setProperties(sb, bean);

            return sb.asString();
        }

        /**
         * @param {Bean} root
         * @returns {String}
         */
        static generate(root) {
            // Build final XML:
            const sb = new StringBuilder();

            // 0. Add header.
            sb.append('<?xml version="1.0" encoding="UTF-8"?>');
            sb.emptyLine();

            this.mainComment(sb);
            sb.emptyLine();

            // 1. Start beans section.
            sb.startBlock(
                '<beans xmlns="http://www.springframework.org/schema/beans"',
                '       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"',
                '       xmlns:util="http://www.springframework.org/schema/util"',
                '       xsi:schemaLocation="http://www.springframework.org/schema/beans',
                '                           http://www.springframework.org/schema/beans/spring-beans.xsd',
                '                           http://www.springframework.org/schema/util',
                '                           http://www.springframework.org/schema/util/spring-util.xsd">');

            // 2. Add external property file
            // TODO

            // 3. Add data sources.
            // TODO

            // 3. Add main content.
            this.appendBean(sb, root);

            // 4. Close beans section.
            sb.endBlock('</beans>');

            return sb.asString();
        }

        static cluster(cluster, clientNearCfg) {
            return $generatorSpring.cluster(cluster, clientNearCfg);
        }

        static clusterCaches(caches, igfss, isSrvCfg, res) {
            return $generatorSpring.clusterCaches(caches, igfss, isSrvCfg, res);
        }

        static clusterConfiguration(cluster, clientNearCfg, res) {
            return $generatorSpring.clusterConfiguration(cluster, clientNearCfg, res);
        }

        // Generate logger group.
        static clusterLogger(logger, res) {
            return $generatorSpring.clusterLogger(logger, res);
        }

        // Generate marshaller group.
        static clusterMarshaller(cluster, res) {
            return $generatorSpring.clusterMarshaller(cluster, res);
        }

        // Generate metrics group.
        static clusterMetrics(cluster, res) {
            return $generatorSpring.clusterMetrics(cluster, res);
        }

        // Generate thread pools group.
        static clusterPools(cluster, res) {
            return $generatorSpring.clusterPools(cluster, res);
        }

        /**
         * Generate cluster's SSL configuration.
         *
         * @param cluster Cluster to get SSL configuration.
         * @param res Optional configuration presentation builder object.
         * @returns Configuration presentation builder object
         */
        static clusterSsl(cluster, res) {
            return $generatorSpring.clusterSsl(cluster, res);
        }

        // Generate swap group.
        static clusterSwap(cluster, res) {
            return $generatorSpring.clusterSwap(cluster, res);
        }

        // Generate time group.
        static clusterTime(cluster, res) {
            return $generatorSpring.clusterTime(cluster, res);
        }

        // Generate transactions group.
        static clusterTransactions(transactionConfiguration, res) {
            return $generatorSpring.clusterTransactions(transactionConfiguration, res);
        }

        // Generate user attributes group.
        static clusterUserAttributes(cluster, res) {
            return $generatorSpring.clusterUserAttributes(cluster, res);
        }

        // Generate IGFSs configs.
        static igfss(igfss, res) {
            return $generatorSpring.igfss(igfss, res);
        }

        // Generate IGFS dual mode configuration.
        static igfsDualMode(igfs, res) {
            return $generatorSpring.igfsDualMode(igfs, res);
        }

        // Generate IGFS IPC configuration.
        static igfsIPC(igfs, res) {
            return $generatorSpring.igfsIPC(igfs, res);
        }

        // Generate IGFS fragmentizer configuration.
        static igfsFragmentizer(igfs, res) {
            return $generatorSpring.igfsFragmentizer(igfs, res);
        }

        // Generate IGFS general configuration.
        static igfsGeneral(igfs, res) {
            return $generatorSpring.igfsGeneral(igfs, res);
        }

        // Generate IGFS misc configuration.
        static igfsMisc(igfs, res) {
            return $generatorSpring.igfsMisc(igfs, res);
        }

        static igfsSecondFS(igfs, res) {
            return $generatorSpring.igfsSecondFS(igfs, res);
        }
    };
}]];
