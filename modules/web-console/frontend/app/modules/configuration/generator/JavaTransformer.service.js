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
import { MethodBean } from './Beans';

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
         * @param {Bean} bean
         */
        static _newBean(bean) {
            const shortClsName = JavaTypes.shortClassName(bean.clsName);

            if (bean.isEmptyConstructor())
                return `new ${shortClsName}()`;

            const args = _.map(bean.arguments, (arg) => {
                switch (arg.type) {
                    case 'PROPERTY':
                        return arg.value;
                    case 'STRING':
                        return `"${arg.value}"`;
                    case 'CLASS':
                        return `${JavaTypes.shortClassName(arg.value)}.class`;
                    default:
                        return 'null';
                }
            });

            return `new ${shortClsName}(${args.join(', ')})`;
        }

        /**
         * @param {StringBuilder} sb
         * @param {Bean} bean
         */
        static _defineBean(sb, bean) {
            const shortClsName = JavaTypes.shortClassName(bean.clsName);

            sb.append(`${shortClsName} ${bean.id} = ${this._newBean(bean)};`);
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
         * @param {StringBuilder} sb
         * @param {Bean} bean
         * @param {Boolean} limitLines
         * @private
         */
        static constructBean(sb, bean, limitLines = false) {
            this._defineBean(sb, bean);

            sb.emptyLine();

            this._setProperties(sb, bean, limitLines);
        }

        /**
         *
         * @param {StringBuilder} sb
         * @param {Bean} bean
         * @param {Boolean} limitLines
         * @returns {StringBuilder}
         */
        static _setProperties(sb = new StringBuilder(), bean, limitLines = false) {
            _.forEach(bean.properties, (prop) => {
                switch (prop.type) {
                    case 'STRING':
                        this._setProperty(sb, bean, prop.name, `"${prop.value}"`);

                        break;

                    case 'CLASS':
                        this._setProperty(sb, bean, prop.name, `${JavaTypes.shortClassName(prop.value)}.class`);

                        break;

                    case 'PROPERTY':
                        this._setProperty(sb, bean, prop.name, prop.value);

                        break;

                    case 'ENUM':
                        const value = `${JavaTypes.shortClassName(prop.clsName)}.${prop.value}`;

                        this._setProperty(sb, bean, prop.name, value);

                        break;

                    case 'ARRAY':
                        const arrTypeClsName = JavaTypes.shortClassName(prop.typeClsName);

                        sb.append(`${arrTypeClsName}[] ${prop.name} = new ${arrTypeClsName}[${prop.items.length}];`);

                        sb.emptyLine();

                        _.forEach(prop.items, (item, idx) => {
                            sb.append(`${prop.name}[${idx}] = ${this._newBean(item)};`);
                        });

                        sb.emptyLine();

                        sb.append(`${bean.id}.set${_.upperFirst(prop.name)}(${prop.id});`);

                        break;

                    case 'COLLECTION':
                        const clsName = JavaTypes.shortClassName(prop.clsName);
                        const colTypeClsName = JavaTypes.shortClassName(prop.typeClsName);
                        const implClsName = JavaTypes.shortClassName(prop.implClsName);

                        sb.append(`${clsName}<${colTypeClsName}> ${prop.name} = new ${implClsName}<>();`);

                        sb.emptyLine();

                        _.forEach(prop.items, (item) => {
                            if (_.isString(item))
                                sb.append(`types.add("${item}");`);
                            else if (_.isNumber(item))
                                sb.append(`types.add(${item});`);
                            else if (item instanceof MethodBean && limitLines)
                                sb.append(`types.add(${item.id}());`);
                            else {
                                this._defineBean(sb, item);

                                sb.emptyLine();

                                this._setProperties(sb, item, limitLines);

                                if (item.properties.length)
                                    sb.emptyLine();

                                sb.append(`types.add(${item.id});`);
                            }

                            sb.emptyLine();
                        });

                        sb.append(`${bean.id}.set${_.upperFirst(prop.name)}(types);`);

                        break;

                    case 'MAP':
                        const keyCls = JavaTypes.shortClassName(prop.keyClsName);
                        const valCls = JavaTypes.shortClassName(prop.valClsName);

                        sb.append(`Map<${keyCls}, ${valCls}> ${prop.id} = new HashMap<>();`);

                        sb.emptyLine();

                        _.forEach(prop.value, (entry) => {
                            sb.append(`${bean.id}.put("${entry.name}", "${entry.value}")`);
                        });

                        if (!_.isEmpty(prop.value))
                            sb.emptyLine();

                        sb.append(`${bean.id}.set${_.upperFirst(prop.name)}(${prop.id});`);

                        break;

                    default:
                        const embedded = prop.value;

                        if (embedded.nonEmpty()) {
                            this.constructBean(sb, embedded);

                            sb.emptyLine();

                            sb.append(`${bean.id}.set${_.upperFirst(prop.name)}(${embedded.id});`);
                        }
                        else {
                            const shortClsName = JavaTypes.shortClassName(bean.clsName);

                            sb.append(`${bean.id}.set${_.upperFirst(prop.name)}(new ${shortClsName}());`);
                        }
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

            this.constructBean(sb, cfg, true);

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
