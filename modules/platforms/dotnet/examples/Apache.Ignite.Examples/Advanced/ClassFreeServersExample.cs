using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Apache.Ignite.Core;
using Apache.Ignite.Core.Binary;
using Apache.Ignite.Core.Cache;
using Apache.Ignite.Core.Discovery.Tcp;
using Apache.Ignite.Core.Discovery.Tcp.Multicast;
using Apache.Ignite.ExamplesDll.Advanced;

namespace Apache.Ignite.Examples.Advanced
{
    /// <summary>
    /// Example demonstrates ability to work with binary objects in a class-free way on the server nodes side.
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
    public class ClassFreeServersExample
    {
        [STAThread]
        public static void Main()
        {
            // Client's node configuration.
            // A server node should be started using App.config that intentially doesn't have Car type 
            // specified in its configuration. The server will not work with Car objects in the deserialized form
            // and this is the reason why Car class is not added into Apache.Ignite.ExamplesDll.dll assembly.
            IgniteConfiguration cfg = new IgniteConfiguration()
            {
                ClientMode = true,

                BinaryConfiguration = new BinaryConfiguration()
                {
                    Types = new[]
                    {
                        typeof(Car).FullName
                    }
                },

                DiscoverySpi = new TcpDiscoverySpi()
                {
                    IpFinder = new TcpDiscoveryMulticastIpFinder()
                    {
                        Endpoints = new[] {"127.0.0.1:47500..47502"}
                    }
                }
            };

            // Starting the client node. Make sure to start the server first.
            using (var ignite = Ignition.Start(cfg))
            {
                // Creating and populating the cache.
                ICache<int, Car> cache = ignite.GetOrCreateCache<int, Car>(CarUpdateClosure.CacheName);

                PopulateCache(cache);

                ExecuteIgniteCompute(cache, ignite);

                //ExecuteEntryProcessor(cache);

                Console.WriteLine();
                Console.WriteLine(">>> Example finished, press any key to exit ...");
                Console.ReadKey();
            }
        }

        private static void ExecuteIgniteCompute(ICache<int, Car> cache, IIgnite ignite)
        {
            // Sending closure to the server node. The closure will update a Car's year without 
            // having its class at all.
            int carKey = 1;

            Console.WriteLine();
            Console.WriteLine(">>> Actual car data before closure execution: {0}", cache.Get(carKey));

            ignite.GetCompute().AffinityCall(CarUpdateClosure.CacheName, carKey,
                new CarUpdateClosure(carKey));

            Console.WriteLine();
            Console.WriteLine(">>> Actual car data after closure execution: {0}", cache.Get(carKey));
        }

        private static void ExecuteEntryProcessor(ICache<int, Car> cache)
        {
            // Sending entry processor to the server node. The processor will update a Car's model without 
            // having its class at all.
            int carKey = 2;

            Console.WriteLine();
            Console.WriteLine(">>> Actual car data before processor execution: {0}", cache.Get(carKey));

            ICache<int, IBinaryObject> binaryCache = cache.WithKeepBinary<int, IBinaryObject>();

            binaryCache.Invoke(carKey, new CarEntryProcessor(), "Lexus");

            Console.WriteLine();
            Console.WriteLine(">>> Actual car data after processor execution: {0}", cache.Get(carKey));
        }

        private static void PopulateCache(ICache<int, Car> cache)
        {
            Console.WriteLine(">>> Populating the cache...");

            cache.Put(1, new Car("Toyota Corolla", 2016, 18000));
            cache.Put(2, new Car("Toyota Camry", 2014, 14500));
            cache.Put(1, new Car("BMW X5", 2015, 33000));
            cache.Put(1, new Car("Hyandai Sonate", 2016, 22000));
        }
    }

    // The class will be available to the client node only, started as a part of the example.
    // A server node that is supposed to be started with Apache.Ignite.ExamplesDll.dll assembly 
    // won't have access to the class definition.
    class Car
    {
        public Car(string model, int year, int price)
        {
            Model = model;
            Year = year;
            Price = price;
        }

        public string Model { get; set; }

        public int Year { get; set; }

        public int Price { get; set; }

        /// <summary>
        /// Returns a string that represents the current object.
        /// </summary>
        /// <returns>
        /// A string that represents the current object.
        /// </returns>
        /// <filterpriority>2</filterpriority>
        public override string ToString()
        {
            return string.Format("{0} [Model={1}, Year={2}, Price={3}]",
                typeof(Car).Name, Model, Year, Price);
        }
    }
}
