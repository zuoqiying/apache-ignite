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

#ifndef IGNITE_INTEROP_API_CPP
#define IGNITE_INTEROP_API_CPP

#define IGNITE_API_EXPORT __declspec(dllexport)
#define IGNITE_API_IMPORT __declspec(dllimport)

#ifdef IGNITE_API_IMPL
#define IGNITE_API_IMPORT_EXPORT IGNITE_API_EXPORT
#else
#define IGNITE_API_IMPORT_EXPORT IGNITE_API_IMPORT
#endif

#include <jni.h>

/*
* Unmanaged context.
*/
struct JniContext {
	JavaVM* jvm;

	jclass c_Throwable;
	jclass c_Class;

	jclass c_Ignition;
	jclass c_IgnitionEx;
	jclass c_IgniteInteropProcessor;
	jclass c_IgniteInteropCache;

	jmethodID m_Throwable_toString;
	jmethodID m_Throwable_getMessage;
	jmethodID m_Class_getName;
	jmethodID m_Ignition_ignite;
	jmethodID m_IgnitionEx_startWithClo;
};

class IGNITE_API_IMPORT_EXPORT IgniteInteropAbstractTarget {
protected:
	/** Target class for non-virtual invokes. */
	jclass cls;

	/** Target. */
	jobject obj;

public:
	IgniteInteropAbstractTarget(jclass cls, jobject obj) : cls(cls), obj(obj) {
	}

	~IgniteInteropAbstractTarget();

	jint inOp(jint type, void* ptr, jint len);

	void* inOutOp(jint type, void* ptr, jint len);
};

class IGNITE_API_IMPORT_EXPORT IgniteInteropCache : public IgniteInteropAbstractTarget {
public:
	IgniteInteropCache(jobject obj);

	void put(void* ptr, jint len);

	void* get(void* ptr, jint len);
};

class IGNITE_API_IMPORT_EXPORT IgniteInteropNode {
private:
	jobject ignite;

public:
	IgniteInteropNode(jobject ignite) : ignite(ignite) {
	}

	~IgniteInteropNode();

	IgniteInteropCache* getCache(char* cacheName);
};

/*
* Initialize unmanaged context.
*/
IGNITE_API_IMPORT_EXPORT extern int ContextInit(JavaVMInitArgs args, char** errClsName, char** errMsg);

/*
* Get current contex.
*/
IGNITE_API_IMPORT_EXPORT extern JniContext* Context();

IGNITE_API_IMPORT_EXPORT extern IgniteInteropNode* StartNode();

#endif
