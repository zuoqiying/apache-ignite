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

import $generatorJava from './generator-java';

export default ['JavaTransformer', ['JavaTypes', 'ConfigurationGenerator', (JavaTypes, generator) => {
    class JavaTransformer extends AbstractTransformer {
        static generator = generator;

        static comment(sb, ...lines) {
            _.forEach(lines, (line) => sb.append(`// ${line}`));
        }

        static commentBlock(sb, ...lines) {
            sb.append('/**');

            _.forEach(lines, (line) => sb.append(` * ${line}`));

            sb.append(' **/');
        }

        /**
         * @param {StringBuilder} sb
         * @param {Bean} bean
         */
        static _defineBean(sb, bean) {
            const shortClsName = this.shortClassName(bean.clsName);

            sb.append(`${shortClsName} ${bean.id} = new ${shortClsName}();`);
        }

        /**
         * @param {StringBuilder} sb
         * @param {Bean} parent
         * @param {Bean} propertyName
         * @param {String|Bean} value
         * @private
         */
        static _setProperty(sb, parent, propertyName, value) {
            sb.append(`${parent.id}.set${_.upperFirst(propertyName)}(${value});`);
        }

        /**
         *
         * @param {StringBuilder} sb
         * @param {Bean} parent
         * @param {String} propertyName
         * @param {Bean} bean
         * @private
         */
        static _setBeanProperty(sb, parent, propertyName, bean) {
            if (bean.id)
                sb.append(`${parent.id}.set${_.upperFirst(propertyName)}(${bean.id});`);
            else {
                const shortClsName = JavaTypes.shortClassName(bean.clsName);

                sb.append(`${parent.id}.set${_.upperFirst(propertyName)}(new ${shortClsName}());`);
            }
        }

        /**
         *
         * @param {StringBuilder} sb
         * @param {Bean} bean
         * @returns {Array}
         */
        static _setProperties(sb = new StringBuilder(), bean) {
            _.forEach(bean.properties, (prop) => {
                switch (prop.type) {
                    case 'BEAN':
                        const nestedBean = prop.value;

                        if (nestedBean.id) {
                            this._defineBean(sb, nestedBean);

                            sb.emptyLine();

                            this._setProperties(sb, nestedBean);

                            if (nestedBean.properties.length)
                                sb.emptyLine();
                        }

                        this._setBeanProperty(sb, bean, prop.name, nestedBean);

                        break;

                    case 'MAP':
                        const keyCls = this.shortClassName(prop.keyClsName);
                        const valCls = this.shortClassName(prop.valClsName);

                        sb.append(`Map<${keyCls}, ${valCls}> ${prop.id} = new HashMap<>();`);

                        sb.emptyLine();

                        if (!_.isEmpty(prop.value)) {
                            _.forEach(prop.value, (entry) => {
                                sb.append(`${bean.id}.put("${entry.name}", "${entry.value}")`);
                            });

                            sb.emptyLine();
                        }

                        sb.append(`${bean.id}.set${_.upperFirst(prop.name)}(${prop.id});`);

                        break;

                    case 'ENUM':
                        const value = `${this.shortClassName(prop.clsName)}.${prop.value}`;

                        this._setProperty(sb, bean, prop.name, value);

                        break;

                    default:
                        this._setProperty(sb, bean, prop.name, prop.value);
                }
            });

            return sb;
        }

        static generateSection(bean) {
            const sb = new StringBuilder();

            this._setProperties(sb, bean);

            return sb.asString();
        }

        /**
         * @param {Bean} bean
         * @returns {Array.<String>}
         */
        collectClasses(bean) {
            const classes = [bean.clsName];

            _.forEach(bean.properties, (prop) => {
                switch (prop.type) {
                    case 'ENUM':
                        classes.push(prop.clsName);

                        break;
                    case 'BEAN':
                        classes.push(...this.collectClasses(prop.value));

                        break;

                    case 'MAP':
                        classes.push('java.util.Map', 'java.util.HashMap', prop.keyClsName, prop.valClsName);

                        break;

                    default:
                    // No-op.
                }
            });

            return _.uniq(classes);
        }

        /**
         * Build Java startup class with configuration.
         *
         * @param {Bean} cfg
         * @param pkg Package name.
         * @param clsName Class name for generate factory class otherwise generate code snippet.
         * @param clientNearCfg Optional near cache configuration for client node.
         * @returns {String}
         */
        static toClassFile(cfg, pkg, clsName) {
            const sb = new StringBuilder();

            sb.append(`package ${pkg};`);
            sb.emptyLine();

            _.forEach(_.sortBy(_.filter(this.collectClasses(cfg), JavaTypes.nonBuiltInClass)), (cls) => sb.append(`import ${cls};`));
            sb.emptyLine();

            this.mainComment(sb);
            sb.startBlock('public class ' + clsName + ' {');

            this.commentBlock(sb, 'Configure grid.',
                '',
                '@return Ignite configuration.',
                '@throws Exception If failed to construct Ignite configuration instance.'
            );
            sb.startBlock('public static IgniteConfiguration createConfiguration() throws Exception {');

            this._defineBean(sb, cfg);

            sb.emptyLine();

            this._setProperties(sb, cfg);

            sb.emptyLine();

            sb.append(`return ${cfg.id};`);

            sb.endBlock('}');

            sb.endBlock('}');

            return sb.asString();
        }

        static clusterGeneral(cluster, clientNearCfg, res) {
            return $generatorJava.clusterGeneral(cluster, clientNearCfg, res);
        }

        static clusterCaches(caches, igfss, isSrvCfg, res) {
            return $generatorJava.clusterCaches(caches, igfss, isSrvCfg, res);
        }
    }

    return JavaTransformer;
}]];
