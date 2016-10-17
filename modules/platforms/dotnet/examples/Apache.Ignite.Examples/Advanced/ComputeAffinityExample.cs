using System;
using System.Collections.Generic;
using Apache.Ignite.Core;
using Apache.Ignite.Core.Cache;
using Apache.Ignite.Core.Cluster;
using Apache.Ignite.Core.Compute;
using Apache.Ignite.ExamplesDll.Advanced;

namespace Apache.Ignite.Examples.Advanced
{
    /// <summary>
    /// Example demonstrates the usage of data and computations collocation.
    /// <para />
    /// 1) Build the project Apache.Ignite.ExamplesDll (select it -> right-click -> Build).
    ///    Apache.Ignite.ExamplesDll.dll must appear in %IGNITE_HOME%/platforms/dotnet/examples/Apache.Ignite.ExamplesDll/bin/${Platform]/${Configuration} folder.
    /// 2) Set this class as startup object (Apache.Ignite.Examples project -> right-click -> Properties ->
    ///     Application -> Startup object);
    /// 3) Start example (F5 or Ctrl+F5).
    /// <para />
    /// This example can be run with standalone Apache Ignite.NET node:
    /// 1) Run %IGNITE_HOME%/platforms/dotnet/bin/Apache.Ignite.exe:
    /// Apache.Ignite.exe -assembly=[path_to_Apache.Ignite.ExamplesDll.dll]
    /// 2) Start example.
    /// </summary>
    public class ComputeAffinityExample
    {
        [STAThread]
        public static void Main()
        {
            using (var ignite = Ignition.StartFromApplicationConfiguration())
            {
                // Creating and filling up the cache.
                ICache<int, int> cache = ignite.GetOrCreateCache<int, int>(AffinityClosure.CacheName);

                Console.WriteLine("Populating the cache...");

                IDictionary<int, int> entries = new Dictionary<int, int>();

                for (int i = 0; i < 1000; i++)
                    entries.Add(i, i * 10);

                cache.PutAll(entries);

                Console.WriteLine("Sending affinity collocated computations...");

                // Sending computation to the node where a specific key's data resides.
                ICompute compute = ignite.GetCompute();

                ICacheAffinity affinity = ignite.GetAffinity(AffinityClosure.CacheName);

                IDictionary<IClusterNode, IList<int>> nodesToKeys = affinity.MapKeysToNodes(entries.Keys);

                foreach (var node in nodesToKeys.Keys)
                {
                    IList<int> keys;

                    if (!nodesToKeys.TryGetValue(node, out keys))
                        throw new Exception("Unexpected exception: unable to find node to keys mapping");

                    compute.AffinityCall(AffinityClosure.CacheName, keys[0], new AffinityClosure(keys[0]));
                }

                Console.WriteLine(">>> Check console output on all the server nodes. Key owners had to print " +
                                  " a special message.");

                Console.WriteLine();
                Console.WriteLine(">>> Example finished, press any key to exit ...");
                Console.ReadKey();
            }
        }
    }
}