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
 * Declares ignite::binary::BinaryAny class.
 */

#ifndef _IGNITE_BINARY_BINARY_ANY
#define _IGNITE_BINARY_BINARY_ANY

#include <utility>

#include <ignite/common/common.h>
#include <ignite/common/concurrent.h>

#include <ignite/impl/binary/binary_utils.h>
#include <ignite/impl/binary/binary_any_impl.h>

namespace ignite
{
    namespace binary
    {
        /**
         * Binary any.
         *
         * Instances of this class can hold instances of any binary type.
         */
        class IGNITE_IMPORT_EXPORT BinaryAny
        {
        public:
            /**
             * Default constructor.
             *
             * Constructs empty BinaryAny instance.
             */
            BinaryAny() :
                value(0),
                typeId(0)
            {
                // No-op.
            }

            /**
             * Copy constructor.
             *
             * @param other Other instance.
             */
            BinaryAny(const BinaryAny& other) :
                value(other.value),
                typeId(other.typeId)
            {
                // No-op.
            }

            /**
             * Constructor.
             *
             * @param value Value of the binary type.
             */
            template<typename T>
            BinaryAny(const T& value) :
                value(new T(value)),
                typeId(impl::binary::BinaryUtils::GetTypeId<T>())
            {
                // No-op.
            }

            /**
             * Assignment operator.
             *
             * @param other Other instance.
             * @return *this.
             */
            BinaryAny& operator=(const BinaryAny& other)
            {
                if (this != &other)
                {
                    BinaryAny tmp(other);

                    Swap(tmp);
                }

                return *this;
            }

            /**
             * Assignment operator.
             *
             * @param value Value to consume.
             * @return *this.
             */
            template<typename T>
            BinaryAny& operator=(const T& value)
            {
                BinaryAny tmp(value);

                Swap(tmp);

                return *this;
            }

            /**
             * Swaps content of the instance with another one.
             *
             * @param other Other instance.
             */
            void Swap(BinaryAny& other)
            {
                using std::swap;

                value.Swap(other.value);
                swap(typeId, other.typeId);
            }

            /**
             * Check if the instance has stored value.
             *
             * @return True if instance has no stored value.
             */
            bool IsEmpty() const
            {
                return !value.IsValid();
            }

            /**
             * Cast function.
             * 
             * If passed a pointer, it returns a similarly qualified pointer to the value content
             * if successful, otherwise null is returned. If T is the type of the underlying value,
             * it returns a copy of the held value, otherwise, if T is a reference to (possibly
             * const qualified) type of the underlying value, it returns a reference to the held
             * value.
             * 
             * @throw Overloads taking a pointer do not throw; overloads taking a value or reference
             *     throws IgniteError with the code IgniteError::IGNITE_ERR_ANY_CAST if unsuccessful.
             *
             * @param any Binary any value.
             * @return Value of the requested type.
             */
            template<typename T>
            friend T BinaryAnyCast(BinaryAny& any)
            {
                return impl::binary::BinaryAnyCaster<T>::DoCast(any.typeId, any.value.Get());
            }

            /**
             * Cast function.
             * 
             * If passed a pointer, it returns a similarly qualified pointer to the value content
             * if successful, otherwise null is returned. If T is the type of the underlying value,
             * it returns a copy of the held value, otherwise, if T is a reference to (possibly
             * const qualified) type of the underlying value, it returns a reference to the held
             * value.
             * 
             * @throw Overloads taking a pointer do not throw; overloads taking a value or reference
             *     throws IgniteError with the code IgniteError::IGNITE_ERR_ANY_CAST if unsuccessful.
             *
             * @param any Binary any value.
             * @return Value of the requested type.
             */
            template<typename T>
            friend T BinaryAnyCast(const BinaryAny& any)
            {
                return impl::binary::BinaryAnyCaster<T>::DoCast(any.typeId, any.value.Get());
            }

        private:
            /** Value. */
            common::concurrent::SharedPointer<void> value;

            /** Type ID. */
            int32_t typeId;
        };
    }
}


#endif //_IGNITE_BINARY_BINARY_ANY