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
                if (_.isNil(arg.value)) {
                    sb.startBlock('<constructor-arg>');
                    sb.append('<null/>');
                    sb.endBlock('</constructor-arg>');
                }
                else
                    sb.append(`<constructor-arg value="${this._toObject(arg.clsName, arg.value)}"/>`);
            });

            this._setProperties(sb, bean);

            sb.endBlock('</bean>');
        }

        static _toObject(clsName, ...items) {
            return _.map(items, (item) => {
                switch (clsName) {
                    case 'Property':
                    case 'PropertyChar':
                        return `\${${item}}`;
                    case 'Class':
                        return JavaTypes.fullClassName(item);
                    default:
                        return item;
                }
            });
        }

        static _setCollection(sb, prop, tag) {
            sb.startBlock(`<property name="${prop.name}">`);
            sb.startBlock(`<${tag}>`);

            const arrClsName = JavaTypes.shortClassName(prop.typeClsName);

            const isBean = JavaTypes.nonBuiltInClass(arrClsName) && JavaTypes.nonEnum(arrClsName);

            _.forEach(prop.items, (item) => {
                if (isBean)
                    this.appendBean(sb, item);
                else
                    sb.append(`<value>${item}</value>`);
            });

            sb.endBlock(`</${tag}>`);
            sb.endBlock('</property>');

        }

        /**
         *
         * @param {StringBuilder} sb
         * @param {Bean} bean
         * @returns {StringBuilder}
         */
        static _setProperties(sb, bean) {
            _.forEach(bean.properties, (prop) => {
                const clsName = JavaTypes.shortClassName(prop.clsName);

                switch (clsName.toUpperCase()) {
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
                    case 'VARARG':
                        this._setCollection(sb, prop, 'array');

                        break;
                    case 'COLLECTION':
                        this._setCollection(sb, prop, 'list');

                        break;
                    case 'HASHMAP':
                    case 'LINKEDHASHMAP':
                        sb.startBlock(`<property name="${prop.name}">`);
                        sb.startBlock('<map>');

                        const keyClsName = JavaTypes.shortClassName(prop.keyClsName);
                        const valClsName = JavaTypes.shortClassName(prop.valClsName);

                        const isKeyEnum = JavaTypes.nonBuiltInClass(keyClsName);
                        const isValEnum = JavaTypes.nonBuiltInClass(valClsName);

                        _.forEach(prop.entries, (entry) => {
                            const key = isKeyEnum ? `${keyClsName}.${entry[prop.keyField]}` : entry[prop.keyField];
                            const val = isValEnum ? `${valClsName}.${entry[prop.valField]}` : entry[prop.valField];

                            sb.append(`<entry key="${key}" value="${val}"/>`);
                        });

                        sb.endBlock('</map>');
                        sb.endBlock('</property>');

                        break;
                    case 'BEAN':
                        sb.startBlock(`<property name="${prop.name}">`);

                        this.appendBean(sb, prop.value);

                        sb.endBlock('</property>');

                        break;
                    default:
                        sb.append(`<property name="${prop.name}" value="${this._toObject(clsName, prop.value)}"/>`);
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

        // Generate IGFSs configs.
        static igfss(igfss, res) {
            return $generatorSpring.igfss(igfss, res);
        }
    };
}]];
