using System;
using Apache.Ignite.Core.Binary;
using Apache.Ignite.Core.Cache.Configuration;

namespace Apache.Ignite.ExamplesDll.Advanced
{
    public class Account
    {
        public Account (string name, int age)
        {
            Name = name;
            Age = age;
        }

        [QuerySqlField(IsIndexed = true)]
        public string Name { get; set; }

        [QuerySqlField(IsIndexed = true)]
        public int Age { get; set; }

        public float TotalAmount { get; set; }

        [QuerySqlField]
        public int TotalPayments { get; set; }

        /// <summary>
        /// Returns a string that represents the current object.
        /// </summary>
        /// <returns>
        /// A string that represents the current object.
        /// </returns>
        /// <filterpriority>2</filterpriority>
        public override string ToString()
        {
            return string.Format("{0} [Name={1}, Age={2}, TotalAmount={3}, TotalPayments={4}]",
                typeof(Account).Name, Name, Age, TotalAmount, TotalPayments);
        }
    }
}