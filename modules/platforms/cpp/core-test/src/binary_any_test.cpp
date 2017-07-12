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

BOOST_AUTO_TEST_SUITE(BinaryAnyTestSuite)

BOOST_AUTO_TEST_CASE(Empty)
{
    BinaryAny any;

    BOOST_CHECK(any.IsEmpty());

    BOOST_CHECK_EXCEPTION(int32_t val = BinaryAnyCast<int32_t>(any), ignite::IgniteError, IsAnyCastError);
    BOOST_CHECK_EXCEPTION(int32_t& val = BinaryAnyCast<int32_t&>(any), ignite::IgniteError, IsAnyCastError);
    BOOST_CHECK_EXCEPTION(const int32_t& val = BinaryAnyCast<const int32_t&>(any), ignite::IgniteError, IsAnyCastError);

    std::string* ptr = BinaryAnyCast<std::string*>(any);
    BOOST_CHECK(ptr == 0);

    const std::string* cptr = BinaryAnyCast<const std::string*>(any);
    BOOST_CHECK(cptr == 0);
}

BOOST_AUTO_TEST_CASE(BasicInt8)
{
    TestBasicValid<int8_t>(42);
    TestBasicValid<int8_t>(0);
    TestBasicValid<int8_t>(-127);
}

BOOST_AUTO_TEST_CASE(BasicInt16)
{
    TestBasicValid<int16_t>(4242);
    TestBasicValid<int16_t>(0);
    TestBasicValid<int16_t>(-32768);
}

BOOST_AUTO_TEST_CASE(BasicInt32)
{
    TestBasicValid<int32_t>(42424242);
    TestBasicValid<int32_t>(0);
    TestBasicValid<int32_t>(-2147483647);
}

BOOST_AUTO_TEST_CASE(BasicInt64)
{
    TestBasicValid<int64_t>(4242424242424242);
    TestBasicValid<int64_t>(0);
    TestBasicValid<int64_t>(-9223372036854775807);
}

BOOST_AUTO_TEST_CASE(BasicBool)
{
    TestBasicValid<bool>(true);
    TestBasicValid<bool>(false);
}

BOOST_AUTO_TEST_CASE(BasicFloat)
{
    TestBasicValid<float>(42.42f);
    TestBasicValid<float>(0.0f);
    TestBasicValid<float>(-1.0f);
}

BOOST_AUTO_TEST_CASE(BasicDouble)
{
    TestBasicValid<double>(42.42);
    TestBasicValid<double>(0.0);
    TestBasicValid<double>(-1.0);
}

BOOST_AUTO_TEST_CASE(BasicGuid)
{
    TestBasicValid<Guid>(Guid(0x198A423B4FE96730LL, 0xD82AB007263F82C6LL));
    TestBasicValid<Guid>(Guid(0, 0));
}

BOOST_AUTO_TEST_CASE(BasicDate)
{
    TestBasicValid<Date>(MakeDateGmt(1989, 10, 26));
    TestBasicValid<Date>(MakeDateGmt(2057, 01, 31));
}

BOOST_AUTO_TEST_CASE(BasicTimestamp)
{
    TestBasicValid<Timestamp>(MakeTimestampGmt(1998, 12, 27, 1, 2, 3, 456));
    TestBasicValid<Timestamp>(MakeTimestampGmt(2057, 01, 31, 14, 29, 10, 110394867));
}

BOOST_AUTO_TEST_CASE(BasicTime)
{
    TestBasicValid<Time>(MakeTimeGmt(1, 2, 3));
    TestBasicValid<Time>(MakeTimeGmt(0, 0, 0));
    TestBasicValid<Time>(MakeTimeGmt(14, 29, 10));
}

BOOST_AUTO_TEST_CASE(BasicString)
{
    TestBasicValid<std::string>("Lorem ipsum");
    TestBasicValid<std::string>("");
    TestBasicValid<std::string>("x");
}

BOOST_AUTO_TEST_SUITE_END()