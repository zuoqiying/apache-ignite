using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Apache.Ignite.Core.Cache.Store;
using Apache.Ignite.ExamplesDll.Binary;

namespace Apache.Ignite.ExamplesDll.Advanced
{
    public class ExpirationStore : CacheStoreAdapter
    {
        /// <summary>
        /// Dictionary representing the store.
        /// </summary>
        private readonly ConcurrentDictionary<object, object> _store = new ConcurrentDictionary<object, object>();


        public ExpirationStore()
        {
            for (int i = 0; i < 1000; i++)
                _store.AddOrUpdate(i, i * 10, (key, oldValue) => oldValue);
        }

        public override void LoadCache(Action<object, object> act, params object[] args)
        {
            Console.WriteLine(">>> CacheStore is triggered for data loading...");

            foreach (var key in _store.Keys)
                act(key, _store[key]);

            Console.WriteLine();
        }

        public override object Load(object key)
        {
            Console.WriteLine();
            Console.WriteLine(">>> Loading key {0} from the store", key);
            Console.WriteLine();

            return _store[key];
        }

        public override void Write(object key, object val)
        {
            Console.WriteLine("Updating key {0} int the store", key);

            _store[key] = val;
        }

        public override void Delete(object key)
        {
            Console.WriteLine("Deleting key {0} from the store", key);

            object val;

            _store.TryRemove(key, out val);
        }
    }
}
