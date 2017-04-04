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
import java.util.LinkedList;
import java.util.List;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobAdapter;
import org.apache.ignite.compute.ComputeJobContext;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskFuture;
import org.apache.ignite.compute.ComputeTaskSession;
import org.apache.ignite.compute.ComputeTaskSessionAttributeListener;
import org.apache.ignite.compute.ComputeTaskSessionFullSupport;
import org.apache.ignite.compute.ComputeTaskSplitAdapter;
import org.apache.ignite.resources.JobContextResource;
import org.apache.ignite.resources.TaskSessionResource;
import org.jetbrains.annotations.Nullable;

/** */
public class ComputeTaskControlExample {
    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws IgniteException If example execution failed.
     */
    public static void main(String[] args) throws IgniteException, InterruptedException {
        Ignition.setClientMode(true);

        try (Ignite ignite = Ignition.start("examples/config/example-ignite.xml")) {
            System.out.println();
            System.out.println("Compute task job reconfiguration example started.");

            final int total = 1;

            List<Long> numbers = new ArrayList<>(total);

            for (long i = 1; i <= total; i++)
                numbers.add(i);

            // Execute task on the cluster and wait for its completion.
            IgniteCompute compute = ignite.compute().withAsync();

            compute.execute(NoOpTask.class, numbers);

            ComputeTaskFuture<Object> fut = compute.future();

            // Perform reconfiguration after 5 seconds if test.
            Thread.sleep(5_000);
            System.out.println("Suppressing logging.");
            fut.getTaskSession().setAttribute("suppressLogging", true);

            Thread.sleep(3_000);
            System.out.println("Suspending job for 3 secs.");
            fut.getTaskSession().setAttribute("suspend", Boolean.TRUE);

            Thread.sleep(3_000);
            System.out.println("Resuming job.");
            fut.getTaskSession().setAttribute("suspend", null);

            Thread.sleep(5_000);
            System.out.println("Cancelling, expecting immediate return.");
            fut.cancel();

            try {
                fut.get();
            }
            catch (Exception e) {
                // Exception'll be thrown.
                e.printStackTrace();
            }

            System.out.println(">>> Task is cancelled");
        }
    }

    /**
     * Task for summing of squares numbers very slowly.
     */
    @ComputeTaskSessionFullSupport
    private static class NoOpTask extends ComputeTaskSplitAdapter<List<Long>, Long> {
        /** {@inheritDoc} */
        @Override protected Collection<? extends ComputeJob> split(int clusterSize, List<Long> arg) {
            Collection<ComputeJob> jobs = new LinkedList<>();

            for (final Long jobId : arg)
                jobs.add(new NoOpJob(jobId));

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

    private static class NoOpJob extends ComputeJobAdapter implements ComputeTaskSessionAttributeListener {
        private Long id;

        @TaskSessionResource
        private ComputeTaskSession taskSes;

        @JobContextResource
        private ComputeJobContext jobCtx;

        /**
         * @param id Job id.
         */
        public NoOpJob(Long id) {
            this.id = id;
        }

        @Nullable @Override public Object execute() {
            taskSes.addAttributeListener(this, true);

            while(!isCancelled()) {
                if (taskSes.getAttribute("suppressLogging") == null)
                    System.out.println("Exec: " + id);
                else if (taskSes.getAttribute("suspend") != null)
                    return jobCtx.holdcc();

                try {
                    Thread.sleep(1_000);
                }
                catch (InterruptedException ignored) {
                    // No-op.
                }
            }

            return 1;
        }

        /** {@inheritDoc} */
        @Override public void onAttributeSet(Object key, Object val) {
            if (key.equals("suspend") && val == null) {
                taskSes.setAttribute("suppressLogging", null); // Enable logging.

                jobCtx.callcc();
            }
        }
    }
}