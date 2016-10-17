using System;
using Apache.Ignite.Core.Binary;
using Apache.Ignite.Core.Cache;

namespace Apache.Ignite.ExamplesDll.Advanced
{
    [Serializable]
    public class CarEntryProcessor: ICacheEntryProcessor<int, IBinaryObject, string, string>
    {
        public string Process(IMutableCacheEntry<int, IBinaryObject> entry, string newModel)
        {
            IBinaryObject oldValue = entry.Value;

            Console.WriteLine();
            Console.WriteLine(">>> Entry Processor is called for {0}", oldValue);

            // Updating Car model's value.
            IBinaryObjectBuilder builder = oldValue.ToBuilder();

            builder.SetStringField("model", newModel);

            entry.Value = builder.Build();

            return oldValue.GetField<string>("model");
        }
    }
}
