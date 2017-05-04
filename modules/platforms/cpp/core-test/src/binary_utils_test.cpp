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

#include "ignite/ignite.h"
#include "ignite/ignition.h"

using namespace ignite;
using namespace impl::interop;
using namespace impl::binary;
using namespace boost::unit_test;

template<typename T, typename W, typename R>
void WriteReadType(const T& val, W wc, R rc)
{
    InteropUnpooledMemory mem(1024);
    InteropOutputStream out(&mem);

    wc(&out, val);

    out.Synchronize();

    InteropInputStream in(&mem);

    int32_t res = rc(&in);

    BOOST_CHECK_EQUAL(val, res);
}

void WriteReadSignedVarint(int32_t val)
{
    WriteReadType(val, BinaryUtils::WriteSignedVarint, BinaryUtils::ReadSignedVarint);
}

void WriteReadUnsignedVarint(int32_t val)
{
    WriteReadType(val, BinaryUtils::WriteUnsignedVarint, BinaryUtils::ReadUnsignedVarint);
}

BOOST_AUTO_TEST_SUITE(BinaryUtilsTestSuite)

BOOST_AUTO_TEST_CASE(SignedVarintWriteReadTest)
{
    WriteReadSignedVarint(0);

    WriteReadSignedVarint(1);
    WriteReadSignedVarint(42);
    WriteReadSignedVarint(100);
    WriteReadSignedVarint(100000000);
    WriteReadSignedVarint(999999999);
    WriteReadSignedVarint(1926348257);

    WriteReadSignedVarint(-1);
    WriteReadSignedVarint(-42);
    WriteReadSignedVarint(-100);
    WriteReadSignedVarint(-100000000);
    WriteReadSignedVarint(-999999999);
    WriteReadSignedVarint(-1926348257);
}

BOOST_AUTO_TEST_CASE(UnsignedVarintWriteReadTest)
{
    WriteReadUnsignedVarint(0);

    WriteReadUnsignedVarint(1);
    WriteReadUnsignedVarint(42);
    WriteReadUnsignedVarint(100);
    WriteReadUnsignedVarint(100000000);
    WriteReadUnsignedVarint(999999999);
    WriteReadUnsignedVarint(1926348257);

    WriteReadUnsignedVarint(-1);
    WriteReadUnsignedVarint(-42);
    WriteReadUnsignedVarint(-100);
    WriteReadUnsignedVarint(-100000000);
    WriteReadUnsignedVarint(-999999999);
    WriteReadUnsignedVarint(-1926348257);
}

BOOST_AUTO_TEST_SUITE_END()
