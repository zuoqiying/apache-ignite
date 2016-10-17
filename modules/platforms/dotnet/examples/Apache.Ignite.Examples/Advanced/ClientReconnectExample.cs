using System;
using Apache.Ignite.Core;
using Apache.Ignite.Core.Common;
using Apache.Ignite.Core.Cache;

namespace Apache.Ignite.Examples.Advanced
{
    /// <summary>
    /// Example demonstrates the usage of automatic client reconnect feature.
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
    public class ClientReconnectExample
    {
        private const string CacheName = "dotnet_client_reconnect_cache";

        [STAThread]
        public static void Main()
        {
            // Make sure to start an Ignite server node before.

            Ignition.ClientMode = true;

            using (var ignite = Ignition.StartFromApplicationConfiguration())
            {
                Console.WriteLine(">>> Client node connected to the cluster");

                var cache = ignite.GetOrCreateCache<int, String>(CacheName);

                Random rand = new Random();

                while (true)
                {
                    try
                    {
                        int key = rand.Next(10000);

                        cache.Put(key, "val" + key);

                        System.Threading.Thread.Sleep(3000);

                        Console.WriteLine(">>> Put value with key:" + key);
                    }
                    catch (CacheException e)
                    {
                        if (e.InnerException is ClientDisconnectedException)
                        {
                            Console.WriteLine(">>> Client disconnected from the cluster");

                            ClientDisconnectedException ex = (ClientDisconnectedException)e.InnerException;

                            var task = ex.ClientReconnectTask;

                            Console.WriteLine(">>> Waiting while client gets reconnected to the cluster");

                            while (!task.isCompleted) // workaround.
                                task.Wait();

                            Console.WriteLine(">>> Client has reconnected successfully");

                            //Workaround.
                            System.Threading.Thread.Sleep(3000);

                            // Updating the reference to the cache. The cliet reconnected to the new cluster.
                            cache = ignite.GetOrCreateCache<int, String>(CacheName);
                        }
                        else {
                            Console.WriteLine(e);

                            break;
                        }
                    }

                }

                Console.WriteLine();
                Console.WriteLine(">>> Example finished, press any key to exit ...");
                Console.ReadKey();
            }
        }
    }
}
