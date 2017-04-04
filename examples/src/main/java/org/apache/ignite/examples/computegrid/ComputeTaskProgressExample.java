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

package org.apache.ignite.examples.computegrid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobAdapter;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskFuture;
import org.apache.ignite.compute.ComputeTaskSession;
import org.apache.ignite.compute.ComputeTaskSessionAttributeListener;
import org.apache.ignite.compute.ComputeTaskSessionFullSupport;
import org.apache.ignite.compute.ComputeTaskSplitAdapter;
import org.apache.ignite.resources.TaskSessionResource;
import org.jetbrains.annotations.Nullable;

/** */
public class ComputeTaskProgressExample {
    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws IgniteException If example execution failed.
     */
    public static void main(String[] args) throws IgniteException {
        Ignition.setClientMode(true);

        try (Ignite ignite = Ignition.start("examples/config/example-ignite.xml")) {
            System.out.println();
            System.out.println("Compute task progress example started.");

            final int total = args.length == 0 ? 1000 : Integer.parseInt(args[0]);

            List<Long> numbers = new ArrayList<>(total);

            for (long i = 1; i <= total; i++)
                numbers.add(i);

            // Execute task on the cluster and wait for its completion.
            IgniteCompute compute = ignite.compute().withAsync();

            compute.execute(LongSumTask.class, numbers);

            ComputeTaskFuture<Object> fut = compute.future();

            fut.getTaskSession().addAttributeListener(new ComputeTaskSessionAttributeListener() {
                private Set<Object> completed = new HashSet<Object>();

                @Override public void onAttributeSet(Object key, Object val) {
                    completed.add(key);

                    System.out.println("Completed: " + (int)(completed.size()/(float)total * 100 * 100)/100.+ " %");
                }
            }, true);

            Long sum = (Long)fut.get();

            long expSum = 0;

            for (Long number : numbers)
                expSum += number * number;

            System.out.println();
            System.out.println(">>> Squares sum is '" + sum + "'. Expected " + expSum);
        }
    }

    /**
     * Task for summing of squares numbers very slowly.
     */
    @ComputeTaskSessionFullSupport
    private static class LongSumTask extends ComputeTaskSplitAdapter<List<Long>, Long> {
        /** {@inheritDoc} */
        @Override protected Collection<? extends ComputeJob> split(int clusterSize, List<Long> arg) {
            Collection<ComputeJob> jobs = new LinkedList<>();

            for (final Long num : arg)
                jobs.add(new SquareJob(num));

            return jobs;
        }

        /** {@inheritDoc} */
        @Nullable @Override public Long reduce(List<ComputeJobResult> results) {
            long sum = 0;

            for (ComputeJobResult res : results)
                sum += res.<Long>getData();

            return sum;
        }
    }

    /** */
    private static class SquareJob extends ComputeJobAdapter {
        private Long num;

        @TaskSessionResource
        private ComputeTaskSession taskSes;

        /**
         * @param num Number.
         */
        public SquareJob(Long num) {
            this.num = num;
        }

        @Nullable @Override public Object execute() {
            System.out.println("Exec: " + num);

            try {
                Thread.sleep(500);
            }
            catch (InterruptedException ignored) {
                // No-op.
            }

            taskSes.setAttribute("task" + num, true);

            return num * num;
        }
    }
}