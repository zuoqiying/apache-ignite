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

#include <stdint.h>

#include <string>
#include <vector>
#include <map>

#include <ignite/common/concurrent.h>
#include <ignite/common/common.h>

#include <ignite/guid.h>
#include <ignite/binary/binary_any.h>

namespace ignite
{
    namespace cluster
    {
        /**
         * Interface representing a single cluster node.
         *
         * Use GetAttribute() to get static information about cluster nodes.
         * ClusterNode list, which includes all nodes within task topology, is
         * provided ComputeTask::Map() method.
         *
         * @par Cluster Node Attributes
         * You can use cluster node attributes to provide static information
         * about a node. This information is initialized once within a cluster,
         * during the node startup, and remains the same throughout the lifetime
         * of a node. Here is an example of how to assign an attribute to a node
         * at startup:
         * @code{.xml}
         * <bean class="org.apache.ignite.configuration.IgniteConfiguration">
         *     ...
         *     <property name="userAttributes">
         *         <map>
         *             <entry key="worker" value="true"/>
         *         <map>
         *     <property>
         *     ...
         * <bean>
         * @endcode
         *
         * The system adds the following attributes automatically:
         *  - All system properties.
         *  - All environment properties.
         *  - @c org.ignite.build.ver - Ignite build version.
         *  - @c org.apache.ignite.jit.name - Name of JIT compiler used.
         *  - @c org.apache.ignite.net.itf.name - Name of network interface.
         *  - @c org.apache.ignite.user.name - Operating system user name.
         *  - @c org.apache.ignite.ignite.name - Ignite name (see Ignite::GetName()).
         *  - @c spiName.org.apache.ignite.spi.class - SPI implementation class
         *      for every SPI, where @c spiName is the name of the SPI.
         *  - @c spiName.org.apache.ignite.spi.ver - SPI version for every SPI,
         *      where @c spiName is the name of the SPI.
         *
         * @note All System and Environment properties for all nodes are
         *   automatically included into node attributes. So for example, in
         *   order to print out information about Operating System for all nodes
         *   you would do the following:
         * @code{.cpp}
         * for (const ClusterNode& node : ignite.GetCluster().GetNodes()) {
         *     std::cout << "Operating system name: " << node.GetAttribute<std::string>("os.name");
         *     std::cout << "Operating system architecture: " << node.GetAttribute<std::string>("os.arch");
         *     std::cout << "Operating system version: " << node.GetAttribute<std::string>("os.version");
         * }
         * @endcode
         */
        class IGNITE_IMPORT_EXPORT ClusterNode
        {
        public:
            /**
             * Default constructor.
             */
            ClusterNode() :
                guid(),
                consistentId(),
                order(0),
                local(false),
                daemon(false),
                client(false),
                hostNames(),
                addresses(),
                arguments()
            {
                // No-op.
            }

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
             *
             * Note that attributes cannot be changed at runtime.
             *
             * @tparam T Attribute type.
             *
             * @param name Attribute name.
             * @return Attribute value if found and specified type is valid,
             *  null pointer otherwise.
             */
            template<typename T>
            const T* GetAttribute(const std::string& name) const
            {
                std::map<std::string, binary::BinaryAny>::const_iterator it = arguments.find(name);
                if (it == arguments.end())
                    return 0;

                return binary::BinaryAnyCast<const T*>(it->second);
            }

            /**
             * Get all node attributes. Attributes are assigned to nodes
             * at startup.
             *
             * The system adds the following attributes automatically:
             *  - All system properties.
             *  - All environment properties.
             *
             * Note that attributes cannot be changed at runtime.
             *
             * @return All node attributes.
             */
            const std::map<std::string, binary::BinaryAny>& GetAttributes() const
            {
                return arguments;
            }

            /**
             * Get all addresses this node is known by.
             *
             * @return Vector of addresses.
             */
            const std::vector<std::string>& GetAddresses() const
            {
                return addresses;
            }

            /**
             * Get host names this node is known by.
             *
             * Note: the loopback address will be omitted in results.
             *
             * @return Collection of host names.
             */
            const std::vector<std::string>& GetHostNames() const
            {
                return hostNames;
            }

            /**
             * Get node order within grid topology. Discovery SPIs that support
             * node ordering will assign a proper order to each node and will
             * guarantee that discovery event notifications for new nodes will
             * come in proper order. All other SPIs not supporting ordering may
             * choose to return node startup time here.
             *
             * @note In cases when discovery SPI doesn't support ordering Ignite
             *    cannot guarantee that orders on all nodes will be unique or
             *    chronologically correct. If such guarantee is required - make
             *    sure to use discovery SPI that provides ordering.
             *
             * @return Node startup order.
             */
            int64_t GetOrder() const
            {
                return order;
            }

            /**
             * Test whether or not this node is a local node.
             *
             * @return True if this node is a local node, false otherwise.
             */
            bool IsLocal() const
            {
                return local;
            }

            /**
             * Test whether or not this node is a daemon.
             *
             * Daemon nodes are the usual cluster nodes that participate in
             * topology but are not visible on the main APIs, i.e. they are not
             * part of any cluster group. The only way to see daemon nodes is
             * to use Cluster::ForDaemons() method.
             *
             * Daemon nodes are used primarily for management and monitoring
             * functionality that is build on Ignite and needs to participate
             * in the topology, but should be excluded from the "normal"
             * topology, so that they won't participate in the task execution
             * or data grid operations.
             *
             * Application code should never use daemon nodes.
             *
             * @return True if this node is a daemon, false otherwise.
             */
            bool IsDaemon() const
            {
                return daemon;
            }

            /**
             * Tests whether or not this node is connected to cluster as
             * a client.
             *
             * Do not confuse client in terms of discovery and client in terms
             * of cache. Cache clients cannot carry data, while topology clients
             * connect to topology in a different way.
             *
             * @return True if this node is a client node, false otherwise.
             */
            bool IsClient() const
            {
                return client;
            }

        private:
            //common::concurrent::SharedPointer<impl::cluster::ClusterNodeImpl> impl;

            /** Node GUID. */
            Guid guid;

            /** Consistent ID. */
            std::string consistentId;

            /** Node order within grid topology. */
            int64_t order;

            /** Local node flag. */
            bool local;

            /** Daemon node flag. */
            bool daemon;

            /** Client node flag. */
            bool client;

            /** Host names. */
            std::vector<std::string> hostNames;

            /** Addresses. */
            std::vector<std::string> addresses;

            /** Arguments. */
            std::map<std::string, binary::BinaryAny> arguments;
        };

    }
}

#endif //_IGNITE_CLUSTER_CLUSTER
