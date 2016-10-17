using System;
using Apache.Ignite.Core;
using Apache.Ignite.Core.Discovery.Tcp;
using Apache.Ignite.Core.Discovery.Tcp.Multicast;

namespace Apache.Ignite.Examples.Advanced
{
    public class ExampleNodeStartup
    {
        [STAThread]
        public static void Main()
        {
            // Preparing Ignite node configuraiton.
            IgniteConfiguration cfg = new IgniteConfiguration
            {
                //GridName = "FirstName",

                DiscoverySpi = new TcpDiscoverySpi
                {
                    IpFinder = new TcpDiscoveryMulticastIpFinder
                    {
                        Endpoints = new[] {"127.0.0.1:47500..47502"}
                    }
                }
            };

            // Starting the first node.
            var node = Ignition.Start(cfg);

            // Starting the second node in the same JVM process.
            var node2 = Ignition.StartFromApplicationConfiguration();

            Console.WriteLine();
            Console.WriteLine(">>> Example finished, press any key to exit ...");
            Console.ReadKey();

            node.Dispose();
            node2.Dispose();
        }
    }
}
