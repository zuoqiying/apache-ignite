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

export default class Bean {
    /**
     * @param {String} clsName
     * @param {String} [id]
     * @param {Object} [src]
     * @param {Object} [dflts]
     */
    constructor(clsName, id, src, dflts) {
        this.properties = [];

        this.clsName = clsName;
        this.id = id;
        this.src = src;
        this.dflts = dflts;
    }

    valueOf(path) {
        return (this.src && this.src[path]) || this.dflts[path];
    }

    property(model, name = model) {
        if (!this.src)
            return this;

        const value = this.src[model];

        if (!_.isNil(value) && value !== this.dflts[model])
            this.properties.push({name, value});

        return this;
    }

    enumProperty(model, name = model) {
        if (!this.src)
            return this;

        const value = this.src[model];
        const dflt = this.dflts[model];

        if (!_.isNil(value) && value !== dflt.value)
            this.properties.push({type: 'ENUM', clsName: dflt.clsName, name, value, mapper: dflt.mapper });

        return this;
    }

    beanProperty(name, bean) {
        this.properties.push({type: 'BEAN', name, value: bean});

        return this;
    }

    emptyBeanProperty(model, name = model) {
        if (!this.src)
            return this;

        const cls = this.src[model];

        if (!_.isEmpty(cls) && cls !== this.dflts[model])
            this.properties.push({type: 'BEAN', name, value: new Bean(cls)});

        return this;
    }

    mapProperty(id, model, name = model) {
        if (!this.src)
            return this;

        const value = this.src[model];
        const dflt = this.dflts[model];

        if (!_.isEmpty(value) && value !== dflt.value) {
            this.properties.push({
                type: 'MAP',
                id,
                name,
                clsName: dflt.clsName,
                keyClsName: dflt.keyClsName,
                valClsName: dflt.valClsName,
                value
            });
        }

        return this;
    }

    // TODO default value check.
    isEmpty() {
        return !this.src || _.isEmpty(this.src);
    }

    collectClasses() {
        const classes = [this.clsName];

        _.forEach(this.properties, (prop) => {
            switch (prop.type) {
                case 'ENUM':
                    classes.push(prop.clsName);

                    break;
                case 'BEAN':
                    classes.push(...prop.value.collectClasses());

                    break;

                case 'MAP':
                    classes.push(prop.clsName, prop.keyClsName, prop.valClsName);

                    break;
                default:
                    // No-op.
            }
        });

        return _.uniq(classes);
    }
}
