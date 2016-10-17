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

using System;
using System.Threading;
using System.Threading.Tasks;
using Apache.Ignite.Core;
using Apache.Ignite.Core.Cache;
using Apache.Ignite.Core.Transactions;

namespace Apache.Ignite.Examples.Advanced
{
    public class OptimisticTransactionExample
    {
        /** Cache name. */
        private const string CacheName = "dotnet_optimistic_tx_example";

        [STAThread]
        public static void Main()
        {
            using (var ignite = Ignition.StartFromApplicationConfiguration())
            {
                Console.WriteLine();
                Console.WriteLine(">>> Optimistic transaction example started.");


                var cache = ignite.GetOrCreateCache<int, int>(CacheName);

                // Put a value.
                cache[1] = 1;

                // Increment a value in parallel within a transaction.
                var task1 = Task.Factory.StartNew(() => IncrementCacheValue(cache));
                var task2 = Task.Factory.StartNew(() => IncrementCacheValue(cache));

                Task.WaitAll(task1, task2);

                Console.WriteLine(">>> Resulting value in cache: " + cache[1]);
            }
        }

        private static void IncrementCacheValue(ICache<int, int> cache)
        {
            var transactions = cache.Ignite.GetTransactions();

            using (var tx = transactions.TxStart(TransactionConcurrency.Optimistic, TransactionIsolation.Serializable))
            {
                // Get a key to obtain a lock.
                var value = cache[1];

                // TODO
                Thread.Sleep(TimeSpan.FromSeconds(1));

                cache[1] = value + 1;

                tx.Commit();
            }
        }
    }
}
