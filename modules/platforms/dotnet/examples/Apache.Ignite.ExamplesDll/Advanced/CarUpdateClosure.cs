using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
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
        public static readonly string CacheName = "dotnet_binary_objects_cache";

        private int key;

        [InstanceResource] private IIgnite ignite;

        public CarUpdateClosure(int key)
        {
            this.key = key;
        }

        public String Invoke()
        {
            ICache<int, IBinaryObject> cache = ignite.GetCache<int, int>(CacheName).
                WithKeepBinary<int, IBinaryObject>();

            IBinaryObject obj = cache.Get(key);

            Console.WriteLine();
            Console.WriteLine(">>> Binary object {0} for key {1}", obj, key);

            // Updating object's year.
            IBinaryObjectBuilder builder = obj.ToBuilder().SetIntField("year", 
                obj.GetField<int>("year") - 20);

            cache.Put(key, builder.Build());

            return obj.GetField<string>("model");
        }
    }
}
