using System;
using Apache.Ignite.Core;
using Apache.Ignite.Core.Cache;
using Apache.Ignite.Core.Compute;
using Apache.Ignite.Core.Resource;

namespace Apache.Ignite.ExamplesDll.Advanced
{
    [Serializable]
    public class AffinityClosure : IComputeFunc<int>
    {
        public const string CacheName = "dotnet_affinity_example_cache";

        private readonly int _key;

        [InstanceResource] private IIgnite _ignite;

        public AffinityClosure(int key)
        {
            _key = key;
        }

        public int Invoke()
        {
            ICache<int, int> cache = _ignite.GetCache<int, int>(CacheName);

            int val = cache.Get(_key);

            if (!_ignite.GetAffinity(CacheName).MapKeyToNode(_key).Equals(
                _ignite.GetCluster().GetLocalNode()))
                throw new Exception("Affinity wrongly mapped computation!");

            Console.WriteLine();
            Console.WriteLine(">>> Data {0} for key {1} is located on node {2}",
                val, _key, _ignite.GetCluster().GetLocalNode());

            return val;
        }
    }
}
