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

#ifndef _IGNITE_CLUSTER_CLUSTER
#define _IGNITE_CLUSTER_CLUSTER

#include <ignite/common/common.h>

#include <ignite/guid.h>

#include <ignite/common/concurrent.h>

namespace ignite
{
    namespace cluster
    {
        /**
         * TODO: document me.
         */
        class IGNITE_IMPORT_EXPORT ClusterNode
        {
        public:
            /**
             * Default constructor.
             */
            ClusterNode()
            {
                // No-op.
            }

            /**
             * Constructor.
             *
             * @param impl Implementation.
             */
            //ClusterNode(common::concurrent::SharedPointer<impl::cluster::ClusterNodeImpl> impl) :
            //    impl(impl)
            //{
            //    // No-op.
            //}

            /**
             * Gets globally unique node ID. A new ID is generated every time a node restarts.
             *
             * @return Globally unique node ID.
             */
            const Guid& GetId() const
            {
                return guid;
            }

            /**
             * Gets consistent globally unique node ID. Unlike id() method, this
             * method returns consistent node ID which survives node restarts.
             *
             * @return Consistent globally unique node ID.
             */
            const std::string& GetConsistentId() const
            {
                return consistentId;
            }

            /**
             * Get a node attribute. Attributes are assigned to nodes at startup.
             * 
             * The system adds the following attributes automatically:
             *  - All system properties.
             *  - All environment properties.
             *  - All attributes defined in {@link org.apache.ignite.internal.IgniteNodeAttributes}
             *
             * Note that attributes cannot be changed at runtime.
             *
             * @tparam T Attribute type.
             *
             * @param name Attribute name.
             * @return Attribute value.
             */
            template<typename T>
            T GetAttribute(const std::string& name) const
            {
                // TODO: implement
            }

        private:
            //common::concurrent::SharedPointer<impl::cluster::ClusterNodeImpl> impl;

            /** Node GUID. */
            Guid guid;

            /** Consistent ID. */
            std::string consistentId;
        };

    }
}

#endif //_IGNITE_CLUSTER_CLUSTER
