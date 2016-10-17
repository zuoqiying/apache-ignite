using System;
using Apache.Ignite.Core;
using Apache.Ignite.Core.Binary;
using Apache.Ignite.Core.Cache;
using Apache.Ignite.Core.Compute;
using Apache.Ignite.Core.Resource;

namespace Apache.Ignite.ExamplesDll.Advanced
{
    [Serializable]
    public class CarUpdateClosure : IComputeFunc<String>
    {
        public const string CacheName = "dotnet_binary_objects_cache";

        private readonly int _key;

        [InstanceResource] private IIgnite _ignite;

        public CarUpdateClosure(int key)
        {
            _key = key;
        }

        public String Invoke()
        {
            ICache<int, IBinaryObject> cache = _ignite.GetCache<int, int>(CacheName).
                WithKeepBinary<int, IBinaryObject>();

            IBinaryObject obj = cache.Get(_key);

            Console.WriteLine();
            Console.WriteLine(">>> Binary object {0} for key {1}", obj, _key);

            // Updating object's year.
            IBinaryObjectBuilder builder = obj.ToBuilder().SetIntField("year", 
                obj.GetField<int>("year") - 20);

            cache.Put(_key, builder.Build());

            return obj.GetField<string>("model");
        }
    }
}
