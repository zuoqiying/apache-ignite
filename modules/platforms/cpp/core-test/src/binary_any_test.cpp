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

using namespace ignite::binary;

bool IsAnyCastError(const ignite::IgniteError& err)
{
    return err.GetCode() == ignite::IgniteError::IGNITE_ERR_ANY_CAST;
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

BOOST_AUTO_TEST_SUITE_END()