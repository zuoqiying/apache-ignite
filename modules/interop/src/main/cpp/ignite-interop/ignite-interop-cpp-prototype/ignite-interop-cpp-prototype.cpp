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

#include "stdafx.h"

#include <jni.h>

#include <iostream>
#include <stdlib.h>
#include <string>

#include "ignite-interop-api.h"

using namespace std;

class IgniteCache {
private:
	IgniteInteropCache* interopCache;

public:
	IgniteCache(IgniteInteropCache* interopCache) : interopCache(interopCache) {
	}

	~IgniteCache() {
		delete interopCache;
	}

	void put(int key, int val) {
	}

	int get(int key) {
		return 0;
	}
};

int _tmain(int argc, _TCHAR* argv[])
{
	char* igniteHome;

	igniteHome = getenv("IGNITE_HOME");

	if (!igniteHome) {
		cout << "IGNITE_HOME is not set" << endl;

		return 0;
	}

	string home(igniteHome);

	cout << "IGNITE_HOME:" << home << endl;

	string classpath = "-Djava.class.path=" + home + "\\modules\\core\\target\\classes;";

	classpath += home + "\\modules\\core\\target\\libs\\cache-api-1.0.0.jar;";
	classpath += home + "\\modules\\spring\\target\\classes;";
	classpath += home + "\\modules\\spring\\target\\libs\\spring-core-4.1.0.RELEASE.jar;";
	classpath += home + "\\modules\\spring\\target\\libs\\spring-context-4.1.0.RELEASE.jar;";
	classpath += home + "\\modules\\spring\\target\\libs\\spring-beans-4.1.0.RELEASE.jar;";
	classpath += home + "\\modules\\spring\\target\\libs\\spring-tx-4.1.0.RELEASE.jar;";
	classpath += home + "\\modules\\spring\\target\\libs\\spring-aop-4.1.0.RELEASE.jar;";
	classpath += home + "\\modules\\spring\\target\\libs\\spring-expression-4.1.0.RELEASE.jar;";
	classpath += home + "\\modules\\spring\\target\\libs\\commons-logging-1.1.1.jar;";

	cout << "Classpath:" << classpath << endl;

	JavaVMInitArgs args;

	JavaVMOption* options = new JavaVMOption[2];

	options[0].optionString = const_cast<char*>(classpath.c_str());
	options[1].optionString = "-DIGNITE_QUIET=false";

	args.version = JNI_VERSION_1_6;
	args.nOptions = 2;
	args.options = options;
	args.ignoreUnrecognized = 0;

	char* errClsName;
	char* errMsg;

	int res = ContextInit(args, &errClsName, &errMsg);

	if (res != JNI_OK) {
		printf("Failed to create JVM: %s\n", errMsg);

		return 0;
	}

	delete[] options;

	cout << "Created JVM" << endl;

	IgniteInteropNode* node = StartNode();

	if (!node)
		return 0;

	IgniteInteropCache* cache = node->getCache("cache1");

	if (!cache)
		return 0;

	IgniteCache igniteCache(cache);

	igniteCache.put(1, 10);

	int val = igniteCache.get(1);

	cout << "Cache get: " << val << endl;

	return 0;
}

