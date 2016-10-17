using System;
using System.Collections.Generic;
using Apache.Ignite.Core;
using Apache.Ignite.Core.Cache;
using Apache.Ignite.Core.Cache.Configuration;
using Apache.Ignite.Core.Cluster;

namespace Apache.Ignite.Examples.Advanced
{
    /// <summary>
    /// Example demonstrates the usage of ICacheAffinity API.
    /// <para />
    /// 1) Build the project Apache.Ignite.ExamplesDll (select it -> right-click -> Build).
    ///    Apache.Ignite.ExamplesDll.dll must appear in %IGNITE_HOME%/platforms/dotnet/examples/Apache.Ignite.ExamplesDll/bin/${Platform]/${Configuration} folder.
    /// 2) Set this class as startup object (Apache.Ignite.Examples project -> right-click -> Properties ->
    ///     Application -> Startup object);
    /// 3) Start example (F5 or Ctrl+F5).
    /// <para />
    /// This example can be run with standalone Apache Ignite.NET node:
    /// 1) Run %IGNITE_HOME%/platforms/dotnet/bin/Apache.Ignite.exe:
    /// Apache.Ignite.exe -configFileName=platforms\dotnet\examples\apache.ignite.examples\app.config -assembly=[path_to_Apache.Ignite.ExamplesDll.dll]
    /// 2) Start example.
    /// </summary>
    class CacheAffinityExample
    {
        /// <summary>Example cache name.</summary>
        private const string CacheName = "dotnet_affinity_example_cache";

        [STAThread]
        public static void Main()
        {
            // Starting the Ignite node.
            using (var ignite = Ignition.StartFromApplicationConfiguration())
            {
                if (ignite.GetCluster().GetNodes().Count == 1)
                {
                    Console.WriteLine("Start one more Ignite node before and restart the cluster");
                    Console.WriteLine("Press any key to exit...");

                    Console.ReadKey();

                    return;
                }

                // Creating the cache.

                CacheConfiguration cfg = new CacheConfiguration
                {
                    Name = CacheName,
                    Backups = 1
                };

                ICache<int, int> cache = ignite.GetOrCreateCache<int, int>(cfg);

                cache.Clear();

                // Populating the cache.
                using (var streamer = ignite.GetDataStreamer<int, int>(CacheName))
                {
                    Console.WriteLine(">>> Populating the cache...");

                    for (int i = 0; i < 3000; i++)
                        streamer.AddData(i, i*10);
                }

                Console.WriteLine(">>> Cache size: " + cache.GetSize());

                // Getting affinity based information. 
                ICacheAffinity affinity = ignite.GetAffinity(CacheName);
                ICollection<IClusterNode> nodes = ignite.GetCluster().GetNodes();

                Console.WriteLine(">>> Affinity Based Information");
                Console.WriteLine(">>>    Partitions Number: " + affinity.Partitions);

                foreach (var node in nodes)
                {
                    Console.WriteLine(">>>   Primary partitions number is {0} on node {1}",
                        affinity.GetPrimaryPartitions(node).Length, node);
                    Console.WriteLine(">>>   Backup partitions number is {0} on node {1}", 
                        affinity.GetBackupPartitions(node).Length, node);
                    Console.WriteLine();
                }

                Console.WriteLine();

                int key = 1045;

                Console.WriteLine(">>> Mapping key to partition");
                Console.WriteLine(">>>    Entry with key {0} stored in partition {1}",
                    key, affinity.GetPartition(key));

                Console.WriteLine();

                Console.WriteLine(">>> Mapping key to node");
                Console.WriteLine(">>>    Entry with key {0} stored on node {1}", key, affinity.MapKeyToNode(key));

                Console.WriteLine();
                Console.WriteLine(">>> Example finished, press any key to exit ...");
                Console.ReadKey();
            }
        }
    }
}
