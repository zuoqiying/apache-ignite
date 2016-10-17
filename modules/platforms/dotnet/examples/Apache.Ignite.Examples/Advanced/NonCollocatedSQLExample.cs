using System;
using System.Collections;
using Apache.Ignite.Core;
using Apache.Ignite.Core.Binary;
using Apache.Ignite.Core.Discovery.Tcp;
using Apache.Ignite.Core.Discovery.Tcp.Multicast;
using Apache.Ignite.Core.Cache.Configuration;
using Apache.Ignite.Core.Cache;
using Apache.Ignite.Core.Cache.Query;
using Apache.Ignite.Core.Transactions;
using Apache.Ignite.ExamplesDll.Advanced;


namespace Apache.Ignite.Examples.Advanced
{
    /// <summary>
    /// Example demonstrates the usage of non-collocated distributed joins.
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
    class NonCollocatedSQLExample
    {
        /// <summary>Employee cache name.</summary>
        private const string AccountCacheName = "dotnet_account_cache";

        /// <summary>Organization cache name.</summary>
        private const string PaymentCacheName = "dotnet_payment_cache";

        [STAThread]
        public static void Main()
        {
            // Ignite node configuration.
            IgniteConfiguration cfg = new IgniteConfiguration
            {
                BinaryConfiguration = new BinaryConfiguration
                {
                    Types = new []
                    {
                        typeof(Account).FullName, typeof(Payment).FullName 
                    }
                    //TypeConfigurations = new[] {
                    //    new BinaryTypeConfiguration(typeof(Account)),
                    //    new BinaryTypeConfiguration(typeof(Payment)),
                    //}
                },

                DiscoverySpi = new TcpDiscoverySpi()
                {
                    IpFinder = new TcpDiscoveryMulticastIpFinder()
                    {
                        Endpoints = new[] {"127.0.0.1:47500..47502"}
                    }
                }
            };

            // Starting the Ignite node.
            using (var ignite = Ignition.Start(cfg))
            {
                if (ignite.GetCluster().GetNodes().Count == 1)
                {
                    Console.WriteLine("Start one more Ignite node before and restart the cluster");
                    Console.WriteLine("Press any key to exit...");

                    Console.ReadKey();

                    return;
                }

                // Setting up caches to be used in the example.
                CacheConfiguration accountCacheCfg = new CacheConfiguration()
                {
                    Name = AccountCacheName,
                    AtomicityMode = CacheAtomicityMode.Transactional,
                    QueryEntities = new[]
                    {
                        new QueryEntity(typeof(int), typeof(Account))
                    }
                };

                CacheConfiguration payCacheCfg = new CacheConfiguration()
                {
                    Name = PaymentCacheName,
                    AtomicityMode = CacheAtomicityMode.Transactional,
                    QueryEntities = new[]
                    {
                        new QueryEntity(typeof(int), typeof(Payment))
                    }
                };

                // Starting the caches.
                ICache<int, Account> accountCache = ignite.GetOrCreateCache<int, Account>(accountCacheCfg);

                ICache<int, Payment> paymentCache = ignite.GetOrCreateCache<int, Payment>(payCacheCfg);

                accountCache.Clear();

                paymentCache.Clear();

                // Populating the caches.
                PopulateCache(ignite, accountCache, paymentCache);

                // Quering data that resides in teh caches.
                int accountId = 1;

                var query = new SqlFieldsQuery("SELECT count(*) FROM Account as a, " +
                    "\"" + PaymentCacheName + "\".Payment as p WHERE a._key = p.accountId and a._key = ?", accountId);

                // Enabling distributed Joins.
                //query.EnableDistributedJoins = true;

                IQueryCursor<IList> cursor = accountCache.QueryFields(query);

                Account account = accountCache.Get(accountId);
                
                foreach (IList fields in cursor) {
                    if (account.TotalPayments == (long) fields[0])
                        Console.WriteLine(">>> Valid number of rows returned {0}", account.TotalPayments);
                    else
                    {
                        Console.WriteLine(">>> Wrong number of rows returned {0} and {1}", account.TotalPayments,
                            fields[0]);
                        Console.WriteLine(">>> Enable non-collocated joins by setting query.EnableDistributedJoins = true or " +
                                          "use affinity collocation approach");
                    }
                }

                Console.WriteLine();
                Console.WriteLine(">>> Example finished, press any key to exit ...");
                Console.ReadKey();
            }
        }

        private static void PopulateCache(IIgnite ignite, ICache<int, Account> accountCache, ICache<int, Payment> paymentCache)
        {
            Console.WriteLine(">>> Populating caches...");

            Random rand = new Random();

            int accountsNumber = 2;

            for (int i = 0; i < accountsNumber; i++)
                accountCache.Put(i, new Account("AccountName-" + i, rand.Next(70)));

            ITransactions txs = ignite.GetTransactions();

            for (int i = 0; i < 100; i++)
            {
                int accountId = rand.Next(0, accountsNumber);

                using (var tx = txs.TxStart(TransactionConcurrency.Pessimistic, TransactionIsolation.ReadCommitted))
                {
                    Account account = accountCache.Get(accountId);

                    Payment payment = new Payment(accountId, rand.Next(100, 10000), DateTime.Now);

                    // A copy of account is returned. It's safe to modify it.
                    account.TotalPayments += 1;
                    account.TotalAmount += payment.Amount;

                    accountCache.Put(accountId, account);

                    paymentCache.Put(i, payment);

                    tx.Commit();
                }
            }

            Console.WriteLine(">>> Populated caches [AccountsNumber=" + accountCache.GetSize(), ", PaymentsNumber=" +
                paymentCache.GetSize());

            Console.WriteLine();
        }
    }
}
