/*
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

#ifndef _MSC_VER
#   define BOOST_TEST_DYN_LINK
#endif

#include <boost/test/unit_test.hpp>

#include <ignite/binary/binary_any.h>

#include <ignite/test_type.h>

using namespace ignite;
using namespace ignite::binary;
using namespace ignite::common;

bool IsAnyCastError(const ignite::IgniteError& err)
{
    return err.GetCode() == ignite::IgniteError::IGNITE_ERR_ANY_CAST;
}

template<typename T>
void TestBasicValid(const T& actual)
{
    BinaryAny any(actual);

    BOOST_CHECK(!any.IsEmpty());

    T val = BinaryAnyCast<T>(any);
    BOOST_CHECK(val == actual);

    T& ref = BinaryAnyCast<T&>(any);
    BOOST_CHECK(ref == actual);

    const T& cref = BinaryAnyCast<const T&>(any);
    BOOST_CHECK(cref == actual);

    T* ptr = BinaryAnyCast<T*>(any);
    BOOST_CHECK(ptr != 0);
    BOOST_CHECK(*ptr == actual);

    const T* cptr = BinaryAnyCast<const T*>(any);
    BOOST_CHECK(cptr != 0);
    BOOST_CHECK(*cptr == actual);
}

template<typename T1, typename T2>
void TestBasicInvalid(const T1& actual)
{
    BinaryAny any(actual);

    BOOST_CHECK(!any.IsEmpty());

    BOOST_CHECK_EXCEPTION(BinaryAnyCast<T2>(any), ignite::IgniteError, IsAnyCastError);
    BOOST_CHECK_EXCEPTION(BinaryAnyCast<T2&>(any), ignite::IgniteError, IsAnyCastError);
    BOOST_CHECK_EXCEPTION(BinaryAnyCast<const T2&>(any), ignite::IgniteError, IsAnyCastError);

    T2* ptr = BinaryAnyCast<T2*>(any);
    BOOST_CHECK(ptr == 0);

    const T2* cptr = BinaryAnyCast<const T2*>(any);
    BOOST_CHECK(cptr == 0);
}

// General case for the different stored and requested types.
template<typename T1, typename T2>
struct ConditionalTest
{
    static void Do(const T1& actual)
    {
        TestBasicInvalid<T1, T2>(actual);
    }
};

// Specialization for the same stored and requested types.
template<typename T>
struct ConditionalTest<T, T>
{
    static void Do(const T& actual)
    {
        TestBasicValid<T>(actual);
    }
};

template<typename T>
void TestAllTypes(const T& actual)
{
    ConditionalTest<T, int8_t>::Do(actual);
    ConditionalTest<T, int16_t>::Do(actual);
    ConditionalTest<T, int32_t>::Do(actual);
    ConditionalTest<T, int64_t>::Do(actual);
    ConditionalTest<T, bool>::Do(actual);
    ConditionalTest<T, float>::Do(actual);
    ConditionalTest<T, double>::Do(actual);
    ConditionalTest<T, Guid>::Do(actual);
    ConditionalTest<T, Date>::Do(actual);
    ConditionalTest<T, Time>::Do(actual);
    ConditionalTest<T, Timestamp>::Do(actual);
    ConditionalTest<T, std::string>::Do(actual);
    ConditionalTest<T, TestType>::Do(actual);
}

BOOST_AUTO_TEST_SUITE(BinaryAnyTestSuite)

BOOST_AUTO_TEST_CASE(Empty)
{
    BinaryAny any;

    BOOST_CHECK(any.IsEmpty());

    BOOST_CHECK_EXCEPTION(BinaryAnyCast<int32_t>(any), ignite::IgniteError, IsAnyCastError);
    BOOST_CHECK_EXCEPTION(BinaryAnyCast<int32_t&>(any), ignite::IgniteError, IsAnyCastError);
    BOOST_CHECK_EXCEPTION(BinaryAnyCast<const int32_t&>(any), ignite::IgniteError, IsAnyCastError);

    std::string* ptr = BinaryAnyCast<std::string*>(any);
    BOOST_CHECK(ptr == 0);

    const std::string* cptr = BinaryAnyCast<const std::string*>(any);
    BOOST_CHECK(cptr == 0);
}

BOOST_AUTO_TEST_CASE(BasicInt8)
{
    TestAllTypes<int8_t>(42);
    TestAllTypes<int8_t>(0);
    TestAllTypes<int8_t>(-127);
}

BOOST_AUTO_TEST_CASE(BasicInt16)
{
    TestAllTypes<int16_t>(4242);
    TestAllTypes<int16_t>(0);
    TestAllTypes<int16_t>(-32768);
}

BOOST_AUTO_TEST_CASE(BasicInt32)
{
    TestAllTypes<int32_t>(42424242);
    TestAllTypes<int32_t>(0);
    TestAllTypes<int32_t>(-2147483647);
}

BOOST_AUTO_TEST_CASE(BasicInt64)
{
    TestAllTypes<int64_t>(4242424242424242);
    TestAllTypes<int64_t>(0);
    TestAllTypes<int64_t>(-9223372036854775807);
}

BOOST_AUTO_TEST_CASE(BasicBool)
{
    TestAllTypes<bool>(true);
    TestAllTypes<bool>(false);
}

BOOST_AUTO_TEST_CASE(BasicFloat)
{
    TestAllTypes<float>(42.42f);
    TestAllTypes<float>(0.0f);
    TestAllTypes<float>(-1.0f);
}

BOOST_AUTO_TEST_CASE(BasicDouble)
{
    TestAllTypes<double>(42.42);
    TestAllTypes<double>(0.0);
    TestAllTypes<double>(-1.0);
}

BOOST_AUTO_TEST_CASE(BasicGuid)
{
    TestAllTypes<Guid>(Guid(0x198A423B4FE96730LL, 0xD82AB007263F82C6LL));
    TestAllTypes<Guid>(Guid(0, 0));
}

BOOST_AUTO_TEST_CASE(BasicDate)
{
    TestAllTypes<Date>(MakeDateGmt(1989, 10, 26));
    TestAllTypes<Date>(MakeDateGmt(2057, 01, 31));
    TestAllTypes<Date>(MakeDateGmt());
}

BOOST_AUTO_TEST_CASE(BasicTimestamp)
{
    TestAllTypes<Timestamp>(MakeTimestampGmt(1998, 12, 27, 1, 2, 3, 456));
    TestAllTypes<Timestamp>(MakeTimestampGmt(2057, 01, 31, 14, 29, 10, 110394867));
    TestAllTypes<Timestamp>(MakeTimestampGmt());
}

BOOST_AUTO_TEST_CASE(BasicTime)
{
    TestAllTypes<Time>(MakeTimeGmt(1, 2, 3));
    TestAllTypes<Time>(MakeTimeGmt(14, 29, 10));
    TestAllTypes<Time>(MakeTimeGmt());
}

BOOST_AUTO_TEST_CASE(BasicString)
{
    TestAllTypes<std::string>("Lorem ipsum");
    TestAllTypes<std::string>("x");
    TestAllTypes<std::string>("");
}

BOOST_AUTO_TEST_CASE(BasicTestType)
{
    TestAllTypes<TestType>(TestType(1, 2, 3, 4, "5", 6.0f, 7.0, true, Guid(8, 9),
        MakeDateGmt(1987, 6, 5), MakeTimeGmt(13, 32, 9), MakeTimestampGmt(1998, 12, 27, 1, 2, 3, 456)));

    TestAllTypes<TestType>(TestType());
}

BOOST_AUTO_TEST_SUITE_END()