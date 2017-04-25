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

/** Simple example for local dense vector. */
function simpleVector() {
    var imports = new JavaImporter(org.apache.ignite.ml.math.impls.vector, org.apache.ignite.ml.math);

    with (imports) {
        var localVector = new DenseLocalOnHeapVector(100);

        for (var i = 0; i < localVector.size(); i++){
            localVector.set(i, i);
        }

        localVector.plus(1.0);

        printVector(localVector)
    }
}

/** Print vector using Tracer. */
function printVector(vector) {
    var Tracer = Java.type("org.apache.ignite.ml.math.Tracer");

    Tracer.showAscii(vector);
}

simpleVector();
