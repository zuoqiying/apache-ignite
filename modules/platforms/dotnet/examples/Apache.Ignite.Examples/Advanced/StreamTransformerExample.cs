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
using Apache.Ignite.Core;
using Apache.Ignite.Core.Cache.Configuration;
using Apache.Ignite.Core.Cache.Query;
using Apache.Ignite.Core.Datastream;
using Apache.Ignite.ExamplesDll.Advanced;

namespace Apache.Ignite.Examples.Advanced
{

    public class StreamTransformerExample
    {
        /** Cache name. */
        private const string CacheName = "dotnet_stream_transformer_example";

        /** Range within which to generate numbers. */
        private const int Range = 1000;

        /** Number of entries to stream. */
        private const int EntryCount = 1000000;

        [STAThread]
        public static void Main()
        {
            using (var ignite = Ignition.StartFromApplicationConfiguration())
            {
                Console.WriteLine();
                Console.WriteLine(">>> Stream transformer example started.");

                var cacheCfg = new CacheConfiguration(CacheName, new QueryEntity(typeof(int), typeof(long)));

                var cache = ignite.GetOrCreateCache<int, long>(cacheCfg);

                var random = new Random();

                using (var streamer = ignite.GetDataStreamer<int, long>(cache.Name))
                {
                    streamer.Receiver = new StreamTransformer<int, long, object, object>(new AddOneProcessor());

                    // Stream random numbers into the streamer cache.
                    for (var i = 1; i <= EntryCount; i++)
                    {
                        streamer.AddData(random.Next(Range), 1);

                        if (i % 50000 == 0)
                            Console.WriteLine("Number of tuples streamed into Ignite: " + i);
                    }
                }

                // Query top 10 most popular numbers.
                Console.WriteLine();
                Console.WriteLine(">>> Top 10 most frequent numbers:");

                var top10 = cache.QueryFields(new SqlFieldsQuery(
                    "select _key, _val from Long order by _val desc limit 10"));

                foreach (var fields in top10)
                    Console.WriteLine("{0} ({1} times)", fields[0], fields[1]);

                Console.WriteLine();
            }

            Console.WriteLine();
            Console.WriteLine(">>> Example finished, press any key to exit ...");
            Console.ReadKey();
        }
    }
}
