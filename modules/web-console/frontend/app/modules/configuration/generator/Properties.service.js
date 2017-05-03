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

import StringBuilder from './StringBuilder';

/**
 * Properties generation entry point.
 */
export default class IgnitePropertiesGenerator {
    _collectProperties(bean) {
        const props = [];

        // Generate properties for complex object.
        const processItem = (item) => {
            const itemLines = _.difference(this._collectProperties(item), props);

            if (!_.isEmpty(itemLines)) {
                props.push(...itemLines);

                if (props[props.length - 1] !== '')
                    props.push('');
            }
        };

        // Generate properties for all objects in keys and values of map.
        const processMapEntries = (entry) => {
            processItem(entry.name);
            processItem(entry.value);
        };

        // Generate properties for object.
        const processObject = (obj) => {
            switch (obj.clsName) {
                case 'DATA_SOURCE':
                    processItem(obj.value);

                    break;
                case 'BEAN':
                    processItem(obj.value);

                    break;
                case 'PROPERTY':
                case 'PROPERTY_CHAR':
                case 'PROPERTY_INT':
                    props.push(..._.difference([`${obj.value}=${obj.hint}`], props));

                    break;
                case 'ARRAY':
                case 'COLLECTION':
                    _.forEach(obj.items, processItem);

                    break;

                case 'MAP':
                    _.forEach(obj.entries, processMapEntries);

                    break;
                default:
                // No-op.
            }
        };

        // Generate properties for object arguments.
        _.forEach(_.get(bean, 'arguments'), processObject);

        // Generate properties for object properties.
        _.forEach(_.get(bean, 'properties'), processObject);

        return props;
    }

    generate(cfg) {
        const lines = this._collectProperties(cfg);

        if (_.isEmpty(lines))
            return null;

        const sb = new StringBuilder();

        sb.append(`# ${sb.generatedBy()}`).emptyLine();

        _.forEach(lines, (line) => sb.append(line));

        return sb.asString();
    }
}
