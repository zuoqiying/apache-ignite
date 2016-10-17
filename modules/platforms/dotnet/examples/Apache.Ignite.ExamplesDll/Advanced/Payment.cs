using System;
using Apache.Ignite.Core.Cache.Configuration;

namespace Apache.Ignite.ExamplesDll.Advanced
{
    public class Payment
    {
        public Payment(int accountId, float amount, DateTime date)
        {
            AccountId = accountId;
            Amount = amount;
            Date = date;
        }

        [QuerySqlField(IsIndexed = true)]
        public int AccountId { get; set; }

        [QuerySqlField]
        public float Amount { get; set; }

        public DateTime Date { get; set; }

        /// <summary>
        /// Returns a string that represents the current object.
        /// </summary>
        /// <returns>
        /// A string that represents the current object.
        /// </returns>
        /// <filterpriority>2</filterpriority>
        public override string ToString()
        {
            return string.Format("{0} [AccountId={1}, Amount={2}, Date={3}]",
                typeof(Payment).Name, AccountId, Amount, Date);
        }
    }
}