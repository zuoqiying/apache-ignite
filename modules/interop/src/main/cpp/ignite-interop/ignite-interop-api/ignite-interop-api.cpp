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

#define IGNITE_API_IMPL

#include <stdio.h>
#include <string.h>
#include <iostream>
#include <stdlib.h>

#include "ignite-interop-api.h"

using namespace std;

/* --- JNI METHOD DEFINITIONS. */

struct JniMethod {
	const char* name;
	const char* sign;
	bool isStatic;

	JniMethod(const char* name, const char* sign, bool isStatic) {
		this->name = name;
		this->sign = sign;
		this->isStatic = isStatic;
	}
};

const char* C_THROWABLE = "java/lang/Throwable";
JniMethod M_THROWABLE_TOSTRING = JniMethod("toString", "()Ljava/lang/String;", false);
JniMethod M_THROWABLE_GET_MESSAGE = JniMethod("getMessage", "()Ljava/lang/String;", false);

const char* C_CLASS = "java/lang/Class";
JniMethod M_CLASS_GET_NAME = JniMethod("getName", "()Ljava/lang/String;", false);

const char* C_ILLEGAL_ARGUMENT_EXCEPTION = "java/lang/IllegalArgumentException";
const char* C_ILLEGAL_STATE_EXCEPTION = "java/lang/IllegalStateException";
const char* C_NULL_POINTER_EXCEPTION = "java/lang/NullPointerException";
const char* C_UNSUPPORTED_OPERATION_EXCEPTION = "java/lang/UnsupportedOperationException";

const char* C_IGNITION = "org/apache/ignite/Ignition";
JniMethod M_IGNITION_IGNITE = JniMethod("ignite", "(Ljava/lang/String;)Lorg/apache/ignite/Ignite;", true);

const char* C_IGNITION_EX = "org/apache/ignite/internal/IgnitionEx";
JniMethod M_IGNITION_EX_START_WITH_CLO = JniMethod("startWithClosure", "(Ljava/lang/String;Ljava/lang/String;Lorg/apache/ignite/lang/IgniteClosure;)Lorg/apache/ignite/Ignite;", true);

const char* C_INTEROP_PROCESSOR = "org/apache/ignite/internal/processors/interop/InteropProcessor";
JniMethod M_INTEROP_PROCESSOR_CACHE = JniMethod("cache", "(Ljava/lang/String;)Lorg/apache/ignite/internal/processors/interop/InteropTarget;", false);

const char* C_INTEROP_TARGET = "org/apache/ignite/internal/processors/interop/InteropTargetAdapter";
JniMethod M_INTEROP_TARGET_IN_OP = JniMethod("inOp", "(IJI)I", false);
JniMethod M_INTEROP_TARGET_IN_OUT_OP = JniMethod("inOutOp", "(IJI)J", false);


/* Static context instance. */
JniContext* ctx = new JniContext();

/* --- PRIVATE HELPER METHODS. ---*/

/* Find class in running JVM. */
jclass FindClass(JNIEnv* env, const char *name) {
	jclass res = env->FindClass(name);

	if (res) {
		jclass res0 = (jclass)env->NewGlobalRef(res);

		env->DeleteLocalRef(res);

		return res0;
	}
	else
		return NULL;
}

/* Find method in running JVM. */
jmethodID FindMethod(JNIEnv* env, jclass cls, JniMethod mthd) {
	jmethodID res = mthd.isStatic ? env->GetStaticMethodID(cls, mthd.name, mthd.sign) :
		env->GetMethodID(cls, mthd.name, mthd.sign);

	return res;
}

/* Internal context initialization routine. */
int ContextInit0(JavaVMInitArgs args, JavaVM** retJvm, JNIEnv** retEnv) {
	// 1. Check if another JVM is already started.
	if (ctx->jvm) {
		*retJvm = ctx->jvm;

		return JNI_OK;
	}
	else {
		// 2. Start JVM.
		JavaVM *jvm = 0;
		JNIEnv* env = 0;

		jint res = JNI_CreateJavaVM(&jvm, (void**)&env, &args);

		if (res != JNI_OK)
			return res;

		*retJvm = jvm;
		*retEnv = env;

		// 3. Initialize members.
		ctx->c_Throwable = FindClass(env, C_THROWABLE);

		if (!ctx->c_Throwable)
			return JNI_ERR;

		ctx->m_Throwable_toString = FindMethod(env, ctx->c_Throwable, M_THROWABLE_TOSTRING);

		if (!ctx->m_Throwable_toString)
			return JNI_ERR;

		ctx->m_Throwable_getMessage = FindMethod(env, ctx->c_Throwable, M_THROWABLE_GET_MESSAGE);

		if (!ctx->m_Throwable_getMessage)
			return JNI_ERR;

		ctx->c_Class = FindClass(env, C_CLASS);

		if (!ctx->c_Class)
			return JNI_ERR;

		ctx->m_Class_getName = FindMethod(env, ctx->c_Class, M_CLASS_GET_NAME);

		if (!ctx->m_Class_getName)
			return JNI_ERR;

		ctx->c_Ignition = FindClass(env, C_IGNITION);

		if (!ctx->c_Ignition)
			return JNI_ERR;

		ctx->m_Ignition_ignite = FindMethod(env, ctx->c_Ignition, M_IGNITION_IGNITE);

		if (!ctx->m_Ignition_ignite)
			return JNI_ERR;

		ctx->c_IgnitionEx = FindClass(env, C_IGNITION_EX);

		if (!ctx->c_IgnitionEx)
			return JNI_ERR;

		ctx->m_IgnitionEx_startWithClo = FindMethod(env, ctx->c_IgnitionEx, M_IGNITION_EX_START_WITH_CLO);

		if (!ctx->m_IgnitionEx_startWithClo)
			return JNI_ERR;

		ctx->c_IgniteInteropProcessor = FindClass(env, C_INTEROP_PROCESSOR);

		if (!ctx->c_IgniteInteropProcessor)
			return JNI_ERR;



		// 4. Register natives.
		/*
		{
			JNINativeMethod methods[22];

			int idx = 0;

			methods[idx].name = (char*)M_GRID_INTEROP_UTILS_ON_START.name;
			methods[idx].signature = (char*)M_GRID_INTEROP_UTILS_ON_START.sign;
			methods[idx++].fnPtr = JniOnStart;

			res = env->RegisterNatives(ctx->c_GridInteropUtils, methods, idx);

			if (res != JNI_OK)
				return res;
		}
		*/

		// JNI Env is only necessary for error handling, so we nullify to keep invariant.
		*retEnv = NULL;

		return JNI_OK;
	}
}

void printError(JNIEnv* env) {
	jthrowable err = env->ExceptionOccurred();

	if (!err) {
		cout << "No java exception" << endl;

		return;
	}

	env->ExceptionDescribe();

	env->ExceptionClear();
}

/* --- EXPORTED METHODS. --- */

IGNITE_API_IMPORT_EXPORT int ContextInit(JavaVMInitArgs args, char** errClsName, char** errMsg) {
	// TODO AcquireSRWLockExclusive(&lock);

	JavaVM* jvm = 0;
	JNIEnv* env = 0;

	int res = ContextInit0(args, &jvm, &env);

	if (res == JNI_OK) {
		if (!ctx->jvm) {
			// Apply volatile semantics to ensure that when another thread "see" jvm, it will see all other fields as well.
			// TODO MemoryBarrier();

			ctx->jvm = jvm;

			// TODO MemoryBarrier();
		}
	}
	else {
		if (env) {
			// If env is defined, then JVM has been started. We must get error message and stop it.
			if (env->ExceptionCheck()) {
				jthrowable err = env->ExceptionOccurred();

				env->ExceptionClear();

				jclass errCls = env->GetObjectClass(err);

				// Try getting error class name.
				if (ctx->m_Class_getName) {
					jstring clsName = (jstring)env->CallObjectMethod(errCls, ctx->m_Class_getName);

					// Could have failed due to OOME.
					if (clsName) {
						const char* clsNameChars = env->GetStringUTFChars(clsName, 0);

						const size_t clsNameCharsLen = strlen(clsNameChars);

						char* clsNameChars0 = new char[clsNameCharsLen];

						strcpy(clsNameChars0, clsNameChars);

						env->ReleaseStringUTFChars(clsName, clsNameChars);

						*errClsName = clsNameChars0;
					}

					// Sanity check for another exception.
					if (env->ExceptionCheck())
						env->ExceptionClear();
				}

				// Try getting error message.
				if (ctx->m_Throwable_toString) {
					jstring msg = (jstring)env->CallObjectMethod(err, ctx->m_Throwable_toString);

					// Could have failed due to OOME.
					if (msg) {
						const char* msgChars = env->GetStringUTFChars(msg, 0);

						const size_t msgCharsLen = strlen(msgChars);

						char* msgChars0 = new char[msgCharsLen];

						strcpy(msgChars0, msgChars);

						env->ReleaseStringUTFChars(msg, msgChars);

						// Pass error message backwards.
						*errMsg = msgChars0;
					}

					// Sanity check for another exception.
					if (env->ExceptionCheck())
						env->ExceptionClear();
				}
			}

			// Stop JVM.
			jvm->DestroyJavaVM();
		}
	}

	// TODO ReleaseSRWLockExclusive(&lock);

	return res;
}

IGNITE_API_IMPORT_EXPORT JniContext* Context() {
	// TODO MemoryBarrier();

	return ctx;
}

/*
* Attach current thread to JVM.
*/
inline JNIEnv* Attach() {
	JNIEnv* env;

	jint res = Context()->jvm->AttachCurrentThread((void**)&env, NULL);

	if (res != JNI_OK)
		return 0;

	return env;
}

IgniteInteropNode::~IgniteInteropNode() {
	JNIEnv* env = Attach();

	env->DeleteGlobalRef(ignite);
}


IgniteInteropAbstractTarget::~IgniteInteropAbstractTarget() {
	JNIEnv* env = Attach();

	env->DeleteGlobalRef(obj);
}

jint IgniteInteropAbstractTarget::inOp(jint type, void* ptr, jint len) {
	return 0;
}

void* IgniteInteropAbstractTarget::inOutOp(jint type, void* ptr, jint len) {
	return 0;
}

IGNITE_API_IMPORT_EXPORT IgniteInteropNode* StartNode() {
	JNIEnv* env = Attach();

	char* cfgPath = "modules\\interop\\src\\main\\cpp\\ignite-interop\\ignite-interop-cpp-prototype\\config\\test.xml";
	char* gridName = "grid1";

	jstring cfgPath0 = env->NewStringUTF(cfgPath);
	jstring gridName0 = env->NewStringUTF(gridName);

	jobject clo = 0;

	jobject ignite = env->CallStaticObjectMethod(Context()->c_Ignition,
		Context()->m_IgnitionEx_startWithClo,
		cfgPath0, gridName0, clo);

	env->DeleteLocalRef(cfgPath0);
	env->DeleteLocalRef(gridName0);
	env->DeleteLocalRef(clo);

	if (env->ExceptionCheck() || !ignite) {
		printError(env);

		return 0;
	}

	cout << "Started ignite" << endl;

	return new IgniteInteropNode(ignite);
}

IgniteInteropCache* IgniteInteropNode::getCache(char* cacheName) {
	return 0;
}
