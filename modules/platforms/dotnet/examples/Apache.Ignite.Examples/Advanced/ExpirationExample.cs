using System;
using System.Threading;
using Apache.Ignite.Core;
using Apache.Ignite.Core.Cache;
using Apache.Ignite.Core.Cache.Configuration;
using Apache.Ignite.Core.Cache.Expiry;
using Apache.Ignite.ExamplesDll.Advanced;

namespace Apache.Ignite.Examples.Advanced
{
    /// <summary>
    /// Example demonstrates the usage of expiration policy.
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
    class ExpirationExample
    {
        /// <summary>Example cache name.</summary>
        private const string CacheName = "dotnet_expiration_example_cache";

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
                    ReadThrough = true,
                    WriteThrough = true,
                    CacheStoreFactory = new ExpirationStoreFactory()
                };

                ICache<int, int> cache = ignite.GetOrCreateCache<int, int>(cfg).
                    WithExpiryPolicy(new ExpiryPolicy(TimeSpan.MaxValue, TimeSpan.MaxValue,
                    TimeSpan.FromSeconds(5)));

                //Preloading data from store.
                cache.LoadCache(null);

                int key = 655;

                //Triggering expiration based on access to the entry.
                Console.WriteLine(">>> Entry value [key={0}, value={1}]", key, cache.Get(key));

                Console.WriteLine(">>> Putting current Thread into Sleep state...");

                Thread.Sleep(6000);

                //Entry had to expire. Reload in from the store.
                Console.WriteLine(">>> Entry value [key={0}, value={1}] had to expire", key, cache.Get(key));
                Console.WriteLine(">>>    There should be a message saying that the entry is reloaded from store");

                Console.WriteLine();

                Console.WriteLine();
                Console.WriteLine(">>> Example finished, press any key to exit ...");
                Console.ReadKey();
            }
        }
    }
}
