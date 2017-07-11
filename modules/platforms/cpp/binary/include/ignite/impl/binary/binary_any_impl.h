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

/**
 * @file
 * Declares helper classes to be used by ignite::binary::BinaryAny.
 */

#ifndef _IGNITE_IMPL_BINARY_BINARY_ANY_IMPL
#define _IGNITE_IMPL_BINARY_BINARY_ANY_IMPL

#include <ignite/common/common.h>
#include <ignite/common/utils.h>
#include <ignite/binary/binary_type.h>

namespace ignite
{
    namespace impl
    {
        namespace binary
        {
            /**
             * Binary any helper template.
             * 
             * Contains declarations and definitions of types and functions used by BinaryAny class and
             * BinaryAnyCast functions.
             */
            template<typename T>
            struct IGNITE_IMPORT_EXPORT BinaryAnyHelper
            {
                typedef typename common::RemoveCv< typename common::RemoveReference<T>::Type >::Type ValueType;

                /**
                 * Checks type ID and throws IgniteError if it is not matching.
                 *
                 * @param typeId Type ID.
                 */
                static void CheckType(int32_t typeId)
                {
                    if (typeId != ignite::binary::BinaryType<ValueType>::GetTypeId())
                    {
                        IGNITE_ERROR_FORMATTED_2(IgniteError::IGNITE_ERR_ANY_CAST, "Bad cast.", "actualTypeId",
                            typeId, "castedToTypeId", ignite::binary::BinaryType<ValueType>::GetTypeId());
                    }
                }
            };

            template<typename T>
            struct IGNITE_IMPORT_EXPORT BinaryAnyCaster : BinaryAnyHelper<T>
            {
                typedef typename BinaryAnyHelper<T>::ValueType ValueType;

                static T DoCast(int32_t typeId, void* ptr)
                {
                    BinaryAnyHelper<T>::CheckType(typeId);

                    return *reinterpret_cast<ValueType*>(ptr);
                }

                static T DoCast(int32_t typeId, const void* ptr)
                {
                    BinaryAnyHelper<T>::CheckType(typeId);

                    return *reinterpret_cast<const ValueType*>(ptr);
                }
            };

            template<typename T>
            struct IGNITE_IMPORT_EXPORT BinaryAnyCaster<T*> : BinaryAnyHelper<T>
            {
                typedef typename BinaryAnyHelper<T>::ValueType ValueType;

                static T DoCast(int32_t typeId, void* ptr)
                {
                    BinaryAnyHelper<T>::CheckType(typeId);

                    return reinterpret_cast<T*>(ptr);
                }

                static T DoCast(int32_t typeId, const void* ptr)
                {
                    BinaryAnyHelper<T>::CheckType(typeId);

                    return *reinterpret_cast<const T*>(ptr);
                }
            };
        }
    }
}


#endif //_IGNITE_IMPL_BINARY_BINARY_ANY_IMPL