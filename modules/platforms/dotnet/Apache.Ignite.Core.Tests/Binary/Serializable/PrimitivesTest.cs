﻿/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// ReSharper disable StringLiteralTypo
// ReSharper disable IdentifierTypo
namespace Apache.Ignite.Core.Tests.Binary.Serializable
{
    using System;
    using System.Linq;
    using System.Runtime.Serialization;
    using Apache.Ignite.Core.Binary;
    using Apache.Ignite.Core.Impl.Binary;
    using NUnit.Framework;

    /// <summary>
    /// Tests [Serializable] mechanism handling primitive types.
    /// </summary>
    public class PrimitivesTest
    {
        /// <summary>
        /// Tests the DateTime which is ISerializable struct.
        /// </summary>
        [Test]
        public void TestDateTime()
        {
            var marsh = GetMarshaller();

            var val = DateTime.Now;

            Assert.AreEqual(val, marsh.Unmarshal<DateTime>(marsh.Marshal(val)));

            Assert.AreEqual(new[] {val}, marsh.Unmarshal<DateTime[]>(marsh.Marshal(new[] {val})));

            Assert.AreEqual(new DateTime?[] {val, null},
                marsh.Unmarshal<DateTime?[]>(marsh.Marshal(new DateTime?[] {val, null})));
        }

        /// <summary>
        /// Tests that primitive types can be serialized with ISerializable mechanism.
        /// </summary>
        [Test]
        public void TestPrimitives()
        {
            var marsh = GetMarshaller();

            var val1 = new Primitives
            {
                Byte = 1,
                Bytes = new byte[] {2, 3, byte.MinValue, byte.MaxValue},
                Sbyte = -64,
                Sbytes = new sbyte[] {sbyte.MinValue, sbyte.MaxValue, 1, 2, -4, -5},
                Bool = true,
                Bools = new[] {true, true, false},
                Char = 'x',
                Chars = new[] {'a', 'z', char.MinValue, char.MaxValue},
                Short = -25,
                Shorts = new short[] {5, -7, 9, short.MinValue, short.MaxValue},
                Ushort = 99,
                Ushorts = new ushort[] {10, 20, 12, ushort.MinValue, ushort.MaxValue},
                Int = -456,
                Ints = new[] {-100, 200, -300, int.MinValue, int.MaxValue},
                Uint = 456,
                Uints = new uint[] {100, 200, 300, uint.MinValue, uint.MaxValue},
                Long = long.MaxValue,
                Longs = new[] {long.MinValue, long.MaxValue, 33, -44},
                Ulong = ulong.MaxValue,
                Ulongs = new ulong[] {ulong.MinValue, ulong.MaxValue, 33},
                Float = 1.33f,
                Floats = new[]
                {
                    float.MinValue, float.MaxValue,
                    float.Epsilon, float.NegativeInfinity, float.PositiveInfinity, float.NaN,
                    1.23f, -2.5f
                },
                Double = -6.78,
                Doubles = new[]
                {
                    double.MinValue, double.MaxValue, double.Epsilon,
                    double.NegativeInfinity, double.PositiveInfinity,
                    3.76, -9.89
                },
                Decimal = 1.23456789m,
                Decimals = new[]
                {
                    decimal.MinValue, decimal.MaxValue, decimal.One, decimal.MinusOne, decimal.Zero,
                    1.35m, -2.46m
                },
                DateTime = DateTime.UtcNow,
                DateTimes = new[] {DateTime.Now, DateTime.MinValue, DateTime.MaxValue, DateTime.UtcNow},
                Guid = Guid.NewGuid(),
                Guids = new[] {Guid.NewGuid(), Guid.NewGuid(), Guid.NewGuid()},
                String = "hello world",
                Strings = new[] {"hello", "world"}
            };

            var vals = new[] {new Primitives(), val1};

            foreach (var val in vals)
            {
                Assert.IsFalse(val.GetObjectDataCalled);
                Assert.IsFalse(val.SerializationCtorCalled);

                // Unmarshal in full and binary form.
                var bytes = marsh.Marshal(val);
                var res = marsh.Unmarshal<Primitives>(bytes);
                var bin = marsh.Unmarshal<IBinaryObject>(bytes, BinaryMode.ForceBinary);

                // Verify flags.
                Assert.IsTrue(val.GetObjectDataCalled);
                Assert.IsFalse(val.SerializationCtorCalled);

                Assert.IsFalse(res.GetObjectDataCalled);
                Assert.IsTrue(res.SerializationCtorCalled);

                // Verify values.
                Assert.AreEqual(val.Byte, res.Byte);
                Assert.AreEqual(val.Byte, bin.GetField<byte>("byte"));

                Assert.AreEqual(val.Bytes, res.Bytes);
                Assert.AreEqual(val.Bytes, bin.GetField<byte[]>("bytes"));

                Assert.AreEqual(val.Sbyte, res.Sbyte);
                Assert.AreEqual(val.Sbyte, bin.GetField<sbyte>("sbyte"));

                Assert.AreEqual(val.Sbytes, res.Sbytes);
                Assert.AreEqual(val.Sbytes, bin.GetField<sbyte[]>("sbytes"));

                Assert.AreEqual(val.Bool, res.Bool);
                Assert.AreEqual(val.Bool, bin.GetField<bool>("bool"));

                Assert.AreEqual(val.Bools, res.Bools);
                Assert.AreEqual(val.Bools, bin.GetField<bool[]>("bools"));

                Assert.AreEqual(val.Char, res.Char);
                Assert.AreEqual(val.Char, bin.GetField<char>("char"));

                Assert.AreEqual(val.Chars, res.Chars);
                Assert.AreEqual(val.Chars, bin.GetField<char[]>("chars"));

                Assert.AreEqual(val.Short, res.Short);
                Assert.AreEqual(val.Short, bin.GetField<short>("short"));

                Assert.AreEqual(val.Shorts, res.Shorts);
                Assert.AreEqual(val.Shorts, bin.GetField<short[]>("shorts"));

                Assert.AreEqual(val.Ushort, res.Ushort);
                Assert.AreEqual(val.Ushort, bin.GetField<ushort>("ushort"));

                Assert.AreEqual(val.Ushorts, res.Ushorts);
                Assert.AreEqual(val.Ushorts, bin.GetField<ushort[]>("ushorts"));

                Assert.AreEqual(val.Int, res.Int);
                Assert.AreEqual(val.Int, bin.GetField<int>("int"));

                Assert.AreEqual(val.Ints, res.Ints);
                Assert.AreEqual(val.Ints, bin.GetField<int[]>("ints"));

                Assert.AreEqual(val.Uint, res.Uint);
                Assert.AreEqual(val.Uint, bin.GetField<uint>("uint"));

                Assert.AreEqual(val.Uints, res.Uints);
                Assert.AreEqual(val.Uints, bin.GetField<uint[]>("uints"));

                Assert.AreEqual(val.Long, res.Long);
                Assert.AreEqual(val.Long, bin.GetField<long>("long"));

                Assert.AreEqual(val.Longs, res.Longs);
                Assert.AreEqual(val.Longs, bin.GetField<long[]>("longs"));

                Assert.AreEqual(val.Ulong, res.Ulong);
                Assert.AreEqual(val.Ulong, bin.GetField<ulong>("ulong"));

                Assert.AreEqual(val.Ulongs, res.Ulongs);
                Assert.AreEqual(val.Ulongs, bin.GetField<ulong[]>("ulongs"));

                Assert.AreEqual(val.Float, res.Float);
                Assert.AreEqual(val.Float, bin.GetField<float>("float"));

                Assert.AreEqual(val.Floats, res.Floats);
                Assert.AreEqual(val.Floats, bin.GetField<float[]>("floats"));

                Assert.AreEqual(val.Double, res.Double);
                Assert.AreEqual(val.Double, bin.GetField<double>("double"));

                Assert.AreEqual(val.Doubles, res.Doubles);
                Assert.AreEqual(val.Doubles, bin.GetField<double[]>("doubles"));

                Assert.AreEqual(val.Decimal, res.Decimal);
                Assert.AreEqual(val.Decimal, bin.GetField<decimal>("decimal"));

                Assert.AreEqual(val.Decimals, res.Decimals);
                Assert.AreEqual(val.Decimals, bin.GetField<decimal[]>("decimals"));

                Assert.AreEqual(val.Guid, res.Guid);
                Assert.AreEqual(val.Guid, bin.GetField<Guid>("guid"));

                Assert.AreEqual(val.Guids, res.Guids);
                Assert.AreEqual(val.Guids, bin.GetField<Guid[]>("guids"));

                Assert.AreEqual(val.DateTime, res.DateTime);
                Assert.AreEqual(val.DateTime, bin.GetField<IBinaryObject>("datetime").Deserialize<DateTime>());

                Assert.AreEqual(val.DateTimes, res.DateTimes);
                var dts = bin.GetField<IBinaryObject[]>("datetimes");
                Assert.AreEqual(val.DateTimes, dts == null ? null : dts.Select(x => x.Deserialize<DateTime>()));

                Assert.AreEqual(val.String, res.String);
                Assert.AreEqual(val.String, bin.GetField<string>("string"));

                Assert.AreEqual(val.Strings, res.Strings);
                Assert.AreEqual(val.Strings, bin.GetField<string[]>("strings"));
            }
        }

        /// <summary>
        /// Tests that primitive types in nullable form can be serialized with ISerializable mechanism.
        /// </summary>
        [Test]
        public void TestPrimitivesNullable()
        {
            var marsh = GetMarshaller();

            var val1 = new PrimitivesNullable
            {
                Byte = 1,
                Bytes = new byte?[] {2, 3, byte.MinValue, byte.MaxValue, null},
                Sbyte = -64,
                Sbytes = new sbyte?[] {sbyte.MinValue, sbyte.MaxValue, 1, 2, -4, -5, null},
                Bool = true,
                Bools = new bool?[] {true, true, false, null},
                Char = 'x',
                Chars = new char?[] {'a', 'z', char.MinValue, char.MaxValue, null},
                Short = -25,
                Shorts = new short?[] {5, -7, 9, short.MinValue, short.MaxValue, null},
                Ushort = 99,
                Ushorts = new ushort?[] {10, 20, 12, ushort.MinValue, ushort.MaxValue, null},
                Int = -456,
                Ints = new int?[] {-100, 200, -300, int.MinValue, int.MaxValue, null},
                Uint = 456,
                Uints = new uint?[] {100, 200, 300, uint.MinValue, uint.MaxValue, null},
                Long = long.MaxValue,
                Longs = new long?[] {long.MinValue, long.MaxValue, 33, -44, null},
                Ulong = ulong.MaxValue,
                Ulongs = new ulong?[] {ulong.MinValue, ulong.MaxValue, 33, null},
                Float = 1.33f,
                Floats = new float?[]
                {
                    float.MinValue, float.MaxValue,
                    float.Epsilon, float.NegativeInfinity, float.PositiveInfinity, float.NaN,
                    1.23f, -2.5f, null
                },
                Double = -6.78,
                Doubles = new double?[]
                {
                    double.MinValue, double.MaxValue, double.Epsilon,
                    double.NegativeInfinity, double.PositiveInfinity,
                    3.76, -9.89, null
                },
                Decimal = 1.23456789m,
                Decimals = new decimal?[]
                {
                    decimal.MinValue, decimal.MaxValue, decimal.One, decimal.MinusOne, decimal.Zero,
                    1.35m, -2.46m, null
                },
                DateTime = DateTime.UtcNow,
                DateTimes = new DateTime?[]
                {
                    DateTime.Now, DateTime.MinValue, DateTime.MaxValue, DateTime.UtcNow, null
                },
                Guid = Guid.NewGuid(),
                Guids = new Guid?[] {Guid.NewGuid(), Guid.NewGuid(), Guid.NewGuid(), null},
            };

            var vals = new[] {new PrimitivesNullable(), val1};

            foreach (var val in vals)
            {
                Assert.IsFalse(val.GetObjectDataCalled);
                Assert.IsFalse(val.SerializationCtorCalled);

                // Unmarshal in full and binary form.
                var bytes = marsh.Marshal(val);
                var res = marsh.Unmarshal<PrimitivesNullable>(bytes);
                var bin = marsh.Unmarshal<IBinaryObject>(bytes, BinaryMode.ForceBinary);

                // Verify flags.
                Assert.IsTrue(val.GetObjectDataCalled);
                Assert.IsFalse(val.SerializationCtorCalled);

                Assert.IsFalse(res.GetObjectDataCalled);
                Assert.IsTrue(res.SerializationCtorCalled);

                // Verify values.
                Assert.AreEqual(val.Byte, res.Byte);
                Assert.AreEqual(val.Byte, bin.GetField<byte?>("byte"));

                Assert.AreEqual(val.Bytes, res.Bytes);
                Assert.AreEqual(val.Bytes, bin.GetField<byte?[]>("bytes"));

                Assert.AreEqual(val.Sbyte, res.Sbyte);
                Assert.AreEqual(val.Sbyte, bin.GetField<sbyte?>("sbyte"));

                Assert.AreEqual(val.Sbytes, res.Sbytes);
                Assert.AreEqual(val.Sbytes, bin.GetField<sbyte?[]>("sbytes"));

                Assert.AreEqual(val.Bool, res.Bool);
                Assert.AreEqual(val.Bool, bin.GetField<bool?>("bool"));

                Assert.AreEqual(val.Bools, res.Bools);
                Assert.AreEqual(val.Bools, bin.GetField<bool?[]>("bools"));

                Assert.AreEqual(val.Char, res.Char);
                Assert.AreEqual(val.Char, bin.GetField<char?>("char"));

                Assert.AreEqual(val.Chars, res.Chars);
                Assert.AreEqual(val.Chars, bin.GetField<char?[]>("chars"));

                Assert.AreEqual(val.Short, res.Short);
                Assert.AreEqual(val.Short, bin.GetField<short?>("short"));

                Assert.AreEqual(val.Shorts, res.Shorts);
                Assert.AreEqual(val.Shorts, bin.GetField<short?[]>("shorts"));

                Assert.AreEqual(val.Ushort, res.Ushort);
                Assert.AreEqual(val.Ushort, bin.GetField<ushort?>("ushort"));

                Assert.AreEqual(val.Ushorts, res.Ushorts);
                Assert.AreEqual(val.Ushorts, bin.GetField<ushort?[]>("ushorts"));

                Assert.AreEqual(val.Int, res.Int);
                Assert.AreEqual(val.Int, bin.GetField<int?>("int"));

                Assert.AreEqual(val.Ints, res.Ints);
                Assert.AreEqual(val.Ints, bin.GetField<int?[]>("ints"));

                Assert.AreEqual(val.Uint, res.Uint);
                Assert.AreEqual(val.Uint, bin.GetField<uint?>("uint"));

                Assert.AreEqual(val.Uints, res.Uints);
                Assert.AreEqual(val.Uints, bin.GetField<uint?[]>("uints"));

                Assert.AreEqual(val.Long, res.Long);
                Assert.AreEqual(val.Long, bin.GetField<long?>("long"));

                Assert.AreEqual(val.Longs, res.Longs);
                Assert.AreEqual(val.Longs, bin.GetField<long?[]>("longs"));

                Assert.AreEqual(val.Ulong, res.Ulong);
                Assert.AreEqual(val.Ulong, bin.GetField<ulong?>("ulong"));

                Assert.AreEqual(val.Ulongs, res.Ulongs);
                Assert.AreEqual(val.Ulongs, bin.GetField<ulong?[]>("ulongs"));

                Assert.AreEqual(val.Float, res.Float);
                Assert.AreEqual(val.Float, bin.GetField<float?>("float"));

                Assert.AreEqual(val.Floats, res.Floats);
                Assert.AreEqual(val.Floats, bin.GetField<float?[]>("floats"));

                Assert.AreEqual(val.Double, res.Double);
                Assert.AreEqual(val.Double, bin.GetField<double?>("double"));

                Assert.AreEqual(val.Doubles, res.Doubles);
                Assert.AreEqual(val.Doubles, bin.GetField<double?[]>("doubles"));

                Assert.AreEqual(val.Decimal, res.Decimal);
                Assert.AreEqual(val.Decimal, bin.GetField<decimal?>("decimal"));

                Assert.AreEqual(val.Decimals, res.Decimals);
                Assert.AreEqual(val.Decimals, bin.GetField<decimal?[]>("decimals"));

                Assert.AreEqual(val.Guid, res.Guid);
                Assert.AreEqual(val.Guid, bin.GetField<Guid?>("guid"));

                Assert.AreEqual(val.Guids, res.Guids);
                Assert.AreEqual(val.Guids, bin.GetField<Guid?[]>("guids"));

                Assert.AreEqual(val.DateTime, res.DateTime);
                var dt = bin.GetField<IBinaryObject>("datetime");
                Assert.AreEqual(val.DateTime, dt == null ? null : dt.Deserialize<DateTime?>());

                Assert.AreEqual(val.DateTimes, res.DateTimes);
                var dts = bin.GetField<IBinaryObject[]>("datetimes");
                Assert.AreEqual(val.DateTimes, dts == null
                    ? null
                    : dts.Select(x => x == null ? null : x.Deserialize<DateTime?>()));
            }
        }

        /// <summary>
        /// Gets the marshaller.
        /// </summary>
        private static Marshaller GetMarshaller()
        {
            return new Marshaller(null) {CompactFooter = false};
        }

        [Serializable]
        public class Primitives : ISerializable
        {
            public bool GetObjectDataCalled { get; private set; }
            public bool SerializationCtorCalled { get; private set; }

            public byte Byte { get; set; }
            public byte[] Bytes { get; set; }
            public sbyte Sbyte { get; set; }
            public sbyte[] Sbytes { get; set; }
            public bool Bool { get; set; }
            public bool[] Bools { get; set; }
            public char Char { get; set; }
            public char[] Chars { get; set; }
            public short Short { get; set; }
            public short[] Shorts { get; set; }
            public ushort Ushort { get; set; }
            public ushort[] Ushorts { get; set; }
            public int Int { get; set; }
            public int[] Ints { get; set; }
            public uint Uint { get; set; }
            public uint[] Uints { get; set; }
            public long Long { get; set; }
            public long[] Longs { get; set; }
            public ulong Ulong { get; set; }
            public ulong[] Ulongs { get; set; }
            public float Float { get; set; }
            public float[] Floats { get; set; }
            public double Double { get; set; }
            public double[] Doubles { get; set; }
            public decimal Decimal { get; set; }
            public decimal[] Decimals { get; set; }
            public Guid Guid { get; set; }
            public Guid[] Guids { get; set; }
            public DateTime DateTime { get; set; }
            public DateTime[] DateTimes { get; set; }
            public string String { get; set; }
            public string[] Strings { get; set; }

            public Primitives()
            {
                // No-op.
            }

            protected Primitives(SerializationInfo info, StreamingContext context)
            {
                SerializationCtorCalled = true;

                Byte = info.GetByte("byte");
                Bytes = (byte[]) info.GetValue("bytes", typeof(byte[]));

                Sbyte = info.GetSByte("sbyte");
                Sbytes = (sbyte[]) info.GetValue("sbytes", typeof(sbyte[]));

                Bool = info.GetBoolean("bool");
                Bools = (bool[]) info.GetValue("bools", typeof(bool[]));

                Char = info.GetChar("char");
                Chars = (char[]) info.GetValue("chars", typeof(char[]));

                Short = info.GetInt16("short");
                Shorts = (short[]) info.GetValue("shorts", typeof(short[]));

                Ushort = info.GetUInt16("ushort");
                Ushorts = (ushort[]) info.GetValue("ushorts", typeof(ushort[]));

                Int = info.GetInt32("int");
                Ints = (int[]) info.GetValue("ints", typeof(int[]));

                Uint = info.GetUInt32("uint");
                Uints = (uint[]) info.GetValue("uints", typeof(uint[]));

                Long = info.GetInt64("long");
                Longs = (long[]) info.GetValue("longs", typeof(long[]));

                Ulong = info.GetUInt64("ulong");
                Ulongs = (ulong[]) info.GetValue("ulongs", typeof(ulong[]));

                Float = info.GetSingle("float");
                Floats = (float[]) info.GetValue("floats", typeof(float[]));

                Double = info.GetDouble("double");
                Doubles = (double[]) info.GetValue("doubles", typeof(double[]));

                Decimal = info.GetDecimal("decimal");
                Decimals = (decimal[]) info.GetValue("decimals", typeof(decimal[]));

                Guid = (Guid) info.GetValue("guid", typeof(Guid));
                Guids = (Guid[]) info.GetValue("guids", typeof(Guid[]));

                DateTime = info.GetDateTime("datetime");
                DateTimes = (DateTime[]) info.GetValue("datetimes", typeof(DateTime[]));

                String = info.GetString("string");
                Strings = (string[]) info.GetValue("strings", typeof(string[]));
            }

            public void GetObjectData(SerializationInfo info, StreamingContext context)
            {
                GetObjectDataCalled = true;

                info.AddValue("byte", Byte);
                info.AddValue("bytes", Bytes);
                info.AddValue("sbyte", Sbyte);
                info.AddValue("sbytes", Sbytes);
                info.AddValue("bool", Bool);
                info.AddValue("bools", Bools);
                info.AddValue("char", Char);
                info.AddValue("chars", Chars);
                info.AddValue("short", Short);
                info.AddValue("shorts", Shorts);
                info.AddValue("ushort", Ushort);
                info.AddValue("ushorts", Ushorts);
                info.AddValue("int", Int);
                info.AddValue("ints", Ints);
                info.AddValue("uint", Uint);
                info.AddValue("uints", Uints);
                info.AddValue("long", Long);
                info.AddValue("longs", Longs);
                info.AddValue("ulong", Ulong);
                info.AddValue("ulongs", Ulongs);
                info.AddValue("float", Float);
                info.AddValue("floats", Floats);
                info.AddValue("double", Double);
                info.AddValue("doubles", Doubles);
                info.AddValue("decimal", Decimal);
                info.AddValue("decimals", Decimals);
                info.AddValue("guid", Guid);
                info.AddValue("guids", Guids);
                info.AddValue("datetime", DateTime);
                info.AddValue("datetimes", DateTimes);
                info.AddValue("string", String);
                info.AddValue("strings", Strings);
            }
        }

        [Serializable]
        private class PrimitivesNullable : ISerializable
        {
            public bool GetObjectDataCalled { get; private set; }
            public bool SerializationCtorCalled { get; private set; }

            public byte? Byte { get; set; }
            public byte?[] Bytes { get; set; }
            public sbyte? Sbyte { get; set; }
            public sbyte?[] Sbytes { get; set; }
            public bool? Bool { get; set; }
            public bool?[] Bools { get; set; }
            public char? Char { get; set; }
            public char?[] Chars { get; set; }
            public short? Short { get; set; }
            public short?[] Shorts { get; set; }
            public ushort? Ushort { get; set; }
            public ushort?[] Ushorts { get; set; }
            public int? Int { get; set; }
            public int?[] Ints { get; set; }
            public uint? Uint { get; set; }
            public uint?[] Uints { get; set; }
            public long? Long { get; set; }
            public long?[] Longs { get; set; }
            public ulong? Ulong { get; set; }
            public ulong?[] Ulongs { get; set; }
            public float? Float { get; set; }
            public float?[] Floats { get; set; }
            public double? Double { get; set; }
            public double?[] Doubles { get; set; }
            public decimal? Decimal { get; set; }
            public decimal?[] Decimals { get; set; }
            public Guid? Guid { get; set; }
            public Guid?[] Guids { get; set; }
            public DateTime? DateTime { get; set; }
            public DateTime?[] DateTimes { get; set; }

            public PrimitivesNullable()
            {
                // No-op.
            }

            protected PrimitivesNullable(SerializationInfo info, StreamingContext context)
            {
                SerializationCtorCalled = true;

                Byte = (byte?) info.GetValue("byte", typeof(byte?));
                Bytes = (byte?[]) info.GetValue("bytes", typeof(byte?[]));

                Sbyte = (sbyte?) info.GetValue("sbyte", typeof(sbyte?));
                Sbytes = (sbyte?[]) info.GetValue("sbytes", typeof(sbyte?[]));

                Bool = (bool?) info.GetValue("bool", typeof(bool?));
                Bools = (bool?[]) info.GetValue("bools", typeof(bool?[]));

                Char = (char?) info.GetValue("char", typeof(char?));
                Chars = (char?[]) info.GetValue("chars", typeof(char?[]));

                Short = (short?) info.GetValue("short", typeof(short?));
                Shorts = (short?[]) info.GetValue("shorts", typeof(short?[]));

                Ushort = (ushort?) info.GetValue("ushort", typeof(ushort?));
                Ushorts = (ushort?[]) info.GetValue("ushorts", typeof(ushort?[]));

                Int = (int?) info.GetValue("int", typeof(int?));
                Ints = (int?[]) info.GetValue("ints", typeof(int?[]));

                Uint = (uint?) info.GetValue("uint", typeof(uint?));
                Uints = (uint?[]) info.GetValue("uints", typeof(uint?[]));

                Long = (long?) info.GetValue("long", typeof(long?));
                Longs = (long?[]) info.GetValue("longs", typeof(long?[]));

                Ulong = (ulong?) info.GetValue("ulong", typeof(ulong?));
                Ulongs = (ulong?[]) info.GetValue("ulongs", typeof(ulong?[]));

                Float = (float?) info.GetValue("float", typeof(float?));
                Floats = (float?[]) info.GetValue("floats", typeof(float?[]));

                Double = (double?) info.GetValue("double", typeof(double?));
                Doubles = (double?[]) info.GetValue("doubles", typeof(double?[]));

                Decimal = (decimal?) info.GetValue("decimal", typeof(decimal?));
                Decimals = (decimal?[]) info.GetValue("decimals", typeof(decimal?[]));

                Guid = (Guid?) info.GetValue("guid", typeof(Guid?));
                Guids = (Guid?[]) info.GetValue("guids", typeof(Guid?[]));

                DateTime = (DateTime?) info.GetValue("datetime", typeof(DateTime?));
                DateTimes = (DateTime?[]) info.GetValue("datetimes", typeof(DateTime?[]));
            }

            public void GetObjectData(SerializationInfo info, StreamingContext context)
            {
                GetObjectDataCalled = true;

                info.AddValue("byte", Byte);
                info.AddValue("bytes", Bytes);
                info.AddValue("sbyte", Sbyte);
                info.AddValue("sbytes", Sbytes);
                info.AddValue("bool", Bool);
                info.AddValue("bools", Bools);
                info.AddValue("char", Char);
                info.AddValue("chars", Chars);
                info.AddValue("short", Short);
                info.AddValue("shorts", Shorts);
                info.AddValue("ushort", Ushort);
                info.AddValue("ushorts", Ushorts);
                info.AddValue("int", Int);
                info.AddValue("ints", Ints);
                info.AddValue("uint", Uint);
                info.AddValue("uints", Uints);
                info.AddValue("long", Long);
                info.AddValue("longs", Longs);
                info.AddValue("ulong", Ulong);
                info.AddValue("ulongs", Ulongs);
                info.AddValue("float", Float);
                info.AddValue("floats", Floats);
                info.AddValue("double", Double);
                info.AddValue("doubles", Doubles);
                info.AddValue("decimal", Decimal);
                info.AddValue("decimals", Decimals);
                info.AddValue("guid", Guid);
                info.AddValue("guids", Guids);
                info.AddValue("datetime", DateTime);
                info.AddValue("datetimes", DateTimes);
            }
        }
    }
}