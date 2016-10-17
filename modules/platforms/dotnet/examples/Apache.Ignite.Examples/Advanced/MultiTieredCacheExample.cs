using System;
using System.Threading;
using Apache.Ignite.Core;
using Apache.Ignite.Core.Cache;
using Apache.Ignite.Core.Cache.Configuration;
using Apache.Ignite.Core.Cache.Eviction;
using Apache.Ignite.Core.Discovery.Tcp;

namespace Apache.Ignite.Examples.Advanced
{
    /// <summary>
    /// Example demonstrates the usage of Ignite memory multi-tiered architecture.
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
    public class MultiTieredCacheExample
    {
        /// <summary>Example cache name.</summary>
        private const string CacheName = "dotnet_multi_tired_example_cache";

        [STAThread]
        public static void Main()
        {
            // Starting the Ignite node.
            using (var ignite = Ignition.StartFromApplicationConfiguration())
            {
                // Creating the cache.
                CacheConfiguration cfg = new CacheConfiguration
                {
                    Name = CacheName,
                    Backups = 1,
                    EvictionPolicy = new LruEvictionPolicy
                    {
                        // Maximum number of entries that will be stored in Java heap. 
                        MaxSize = 10
                    },
                    OffHeapMaxMemory = 1024 * 10, // 10 KB (maximum allowed off-heap space).
                    // Won't use in the example. SwapSpaceSPI can be set over Spring XML only for now.
                    // EnableSwap = true,
                };

                ICache<int, byte[]> cache = ignite.GetOrCreateCache<int, byte[]>(cfg);

                // Sample data.
                byte[] dataBytes = new byte[1024];

                // Filling out cache and printing its metrics.
                PrintCacheMetrics(cache);

                for (int i = 0; i < 100; i++)
                {
                    cache.Put(i, dataBytes);

                    if (i % 10 == 0)
                        PrintCacheMetrics(cache);
                }

                Console.WriteLine("Waiting for metrics final update");

                Thread.Sleep(((TcpDiscoverySpi)ignite.GetConfiguration().DiscoverySpi).HeartbeatFrequency);

                PrintCacheMetrics(cache);

                Console.WriteLine();
                Console.WriteLine(">>> Example finished, press any key to exit ...");
                Console.ReadKey();
            }
        }

        // Printing out entries disposition in multiple memory tires.
        private static void PrintCacheMetrics(ICache<int, byte[]> cache)
        {
            ICacheMetrics metrics = cache.GetMetrics();

            Console.WriteLine();
            Console.WriteLine("Cache entries layout [Java heap={0}, Off-Heap={1}, Swap={2}]",
                metrics.Size, metrics.OffHeapEntriesCount, metrics.OverflowSize);
            Console.WriteLine();
        }
    }
}
