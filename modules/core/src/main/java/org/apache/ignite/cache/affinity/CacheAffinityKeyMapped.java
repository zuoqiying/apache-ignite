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

package org.apache.ignite.cache.affinity;

import java.lang.annotation.*;
import java.util.concurrent.*;

/**
 * Optional annotation to specify custom key-to-node affinity. Affinity key is a key
 * which will be used to determine a node on which given cache key will be stored. This
 * annotation allows to mark a field or a method in the cache key object that will be
 * used as an affinity key (instead of the entire cache key object that is used for
 * affinity by default). Note that a class can have only one field or method annotated
 * with {@code @CacheAffinityKeyMapped} annotation.
 * <p>
 * One of the major use cases for this annotation is the routing of grid computations
 * to the nodes where the data for this computation is cached, the concept
 * otherwise known as {@code Collocation Of Computations And Data}.
 * <p>
 * <h1 class="header">Mapping Cache Keys</h1>
 * The default implementation of {@link CacheAffinityKeyMapper}, which will be used
 * if no explicit affinity mapper is specified in cache configuration, will first look
 * for any field or method annotated with {@code @CacheAffinityKeyMapped} annotation.
 * If such field or method is not found, then the cache key itself will be used for
 * key-to-node affinity (this means that all objects with the same cache key will always
 * be routed to the same node). If such field or method is found, then the value of this
 * field or method will be used for key-to-node affinity. This allows to specify alternate
 * affinity key, other than the cache key itself, whenever needed.
 * <p>
 * For example, if a {@code Person} object is always accessed together with a {@code Company} object
 * for which this person is an employee, then for better performance and scalability it makes sense to
 * collocate {@code Person} objects together with their {@code Company} object when storing them in
 * cache. To achieve that, cache key used to cache {@code Person} objects should have a field or method
 * annotated with {@code @CacheAffinityKeyMapped} annotation, which will provide the value of
 * the company key for which that person works, like so:
 * <pre name="code" class="java">
 * public class PersonKey {
 *     // Person ID used to identify a person.
 *     private String personId;
 *
 *     // Company ID which will be used for affinity.
 *     &#64;CacheAffinityKeyMapped
 *     private String companyId;
 *     ...
 * }
 * ...
 * // Instantiate person keys.
 * Object personKey1 = new PersonKey("myPersonId1", "myCompanyId");
 * Object personKey2 = new PersonKey("myPersonId2", "myCompanyId");
 *
 * // Both, the company and the person objects will be cached on the same node.
 * cache.put("myCompanyId", new Company(..));
 * cache.put(personKey1, new Person(..));
 * cache.put(personKey2, new Person(..));
 * </pre>
 * <p>
 * <h2 class="header">CacheAffinityKey</h2>
 * For convenience, you can also optionally use {@link CacheAffinityKey} class. Here is how a
 * {@code PersonKey} defined above would look using {@link CacheAffinityKey}:
 * <pre name="code" class="java">
 * Object personKey1 = new CacheAffinityKey("myPersonId1", "myCompanyId");
 * Object personKey2 = new CacheAffinityKey("myPersonId2", "myCompanyId");
 *
 * // Both, the company and the person objects will be cached on the same node.
 * cache.put(myCompanyId, new Company(..));
 * cache.put(personKey1, new Person(..));
 * cache.put(personKey2, new Person(..));
 * </pre>
 * <p>
 * <h1 class="header">Collocating Computations And Data</h1>
 * It is also possible to route computations to the nodes where the data is cached. This concept
 * is otherwise known as {@code Collocation Of Computations And Data}. In this case,
 * {@code @CacheAffinityKeyMapped} annotation allows to specify a routing affinity key for a
 * {@link org.apache.ignite.compute.ComputeJob} or any other grid computation, such as {@link Runnable},
 * {@link Callable}, or {@link org.apache.ignite.lang.IgniteClosure}. It should be attached to a method or
 * field that provides affinity key for the computation. Only one annotation per class is allowed.
 * Whenever such annotation is detected, then {@link org.apache.ignite.spi.loadbalancing.LoadBalancingSpi}
 * will be bypassed, and computation will be routed to the grid node where the specified affinity key is cached.
 * <p>
 * Here is how this annotation can be used to route a job to a node where Person object
 * is cached with ID "1234":
 * <pre name="code" class="java">
 * G.grid().run(new Runnable() {
 *     // This annotation is optional. If omitted, then default
 *     // no-name cache will be used.
 *     &#64;CacheName
 *     private String cacheName = "myCache";
 *
 *     // This annotation specifies that computation should be routed
 *     // precisely to the node where key '1234' is cached.
 *     &#64;CacheAffinityKeyMapped
 *     private String personKey = "1234";
 *
 *     &#64;Override public void run() {
 *         // Some computation logic here.
 *         ...
 *     }
 * };
 * </pre>
 * The same can be achieved by annotating method instead of field as follows:
 * <pre name="code" class="java">
 * G.grid().run(new Runnable() {
 *     &#64;Override public void run() {
 *         // Some computation logic here.
 *         ...
 *     }
 *
 *     // This annotation is optional. If omitted, then default
 *     // no-name cache will be used.
 *     &#64;CacheName
 *     public String cacheName() {
 *         return "myCache";
 *     }
 *
 *     // This annotation specifies that computation should be routed
 *     // precisely to the node where key '1234' is cached.
 *     &#64;CacheAffinityKeyMapped
 *     public String personKey() {
 *         return "1234";
 *     }
 * };
 * </pre>
 * <p>
 * For more information about cache affinity also see {@link CacheAffinityKeyMapper} and
 * {@link CacheAffinityFunction} documentation.
 * Affinity for a key can be found from any node, regardless of whether it has cache started
 * or not. If cache is not started, affinity function will be fetched from the remote node
 * which does have the cache running.
 *
 * @see CacheAffinityFunction
 * @see CacheAffinityKeyMapper
 * @see CacheAffinityKey
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface CacheAffinityKeyMapped {
    // No-op.
}
