using System;
using Apache.Ignite.Core.Cache.Store;
using Apache.Ignite.Core.Common;

namespace Apache.Ignite.ExamplesDll.Advanced
{
    [Serializable]
    public class ExpirationStoreFactory: IFactory<ICacheStore>
    {
        /// <summary>
        /// Creates an instance of the cache store.
        /// </summary>
        public ICacheStore CreateInstance()
        {
           return new ExpirationStore();
        }
    }
}
