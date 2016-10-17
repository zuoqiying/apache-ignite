using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Apache.Ignite.Core;
using Apache.Ignite.Core.Cache;
using Apache.Ignite.Core.Compute;
using Apache.Ignite.Core.Resource;

namespace Apache.Ignite.ExamplesDll.Advanced
{
    [Serializable]
    public class AffinityClosure : IComputeFunc<int>
    {
        public static readonly string CacheName = "dotnet_affinity_example_cache";

        private int key;

        [InstanceResource] private IIgnite ignite;

        public AffinityClosure(int key)
        {
            this.key = key;
        }

        public int Invoke()
        {
            ICache<int, int> cache = ignite.GetCache<int, int>(CacheName);

            int val = cache.Get(key);

            if (!ignite.GetAffinity(CacheName).MapKeyToNode(key).Equals(
                ignite.GetCluster().GetLocalNode()))
                throw new Exception("Affinity wrongly mapped computation!");

            Console.WriteLine();
            Console.WriteLine(">>> Data {0} for key {1} is located on node {2}",
                val, key, ignite.GetCluster().GetLocalNode());

            return val;
        }
    }
}
