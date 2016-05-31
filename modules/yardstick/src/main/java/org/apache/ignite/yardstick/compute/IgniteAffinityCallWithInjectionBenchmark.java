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

package org.apache.ignite.yardstick.compute;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.ignite.Ignite;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.resources.SpringApplicationContextResource;
import org.apache.ignite.yardstick.IgniteAbstractBenchmark;
import org.apache.ignite.yardstick.cache.model.Account;
import org.apache.ignite.yardstick.cache.model.Organization;
import org.springframework.context.ApplicationContext;

/**
 * Ignite benchmark that performs affinity call operations.
 */
public class IgniteAffinityCallWithInjectionBenchmark extends IgniteAbstractBenchmark {
    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> ctx) throws Exception {
        ignite().compute().affinityCall("compute", ThreadLocalRandom.current().nextInt(), new IgniteCallable<Object>() {
            @IgniteInstanceResource
            Ignite ignite;

            @SpringApplicationContextResource
            ApplicationContext ctx;

            Object bean1;
            Object bean2;

            @Override public Object call() throws Exception {
                bean1 = ctx.getBean(Account.class);
                bean2 = ctx.getBean(Organization.class);

                return null;
            }
        });

        return true;
    }
}