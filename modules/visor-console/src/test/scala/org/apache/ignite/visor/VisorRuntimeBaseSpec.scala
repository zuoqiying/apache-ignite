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

package org.apache.ignite.visor

import java.util.regex.Pattern

import org.apache.ignite.Ignition
import org.apache.ignite.configuration.IgniteConfiguration
import org.apache.ignite.visor.commands.open.VisorOpenCommand._

import org.scalatest._

/**
 * Base abstract class for unit tests requiring Visor runtime.
 */
abstract class VisorRuntimeBaseSpec(private[this] val num: Int) extends FunSpec with Matchers
    with BeforeAndAfterAll with BeforeAndAfterEach {
    assert(num >= 1)

    /**  */
    val out = new java.io.ByteArrayOutputStream

    /**
     * Gets grid configuration.
     *
     * @param name Grid name.
     * @return Grid configuration.
     */
    protected def config(name: String): IgniteConfiguration = {
        val cfg = new IgniteConfiguration

        cfg.setGridName(name)

        cfg
    }

    /**
      * Redirect stdout and compare output with specified text.
      *
      * @param block Function to execute.
      * @param text Text to compare with.
      * @param exp If `true` then stdout must contain `text` otherwise must not.
      */
    protected def checkOut(block: => Unit, text: String, exp: Boolean = true) {
        try {
            Console.withOut(out)(block)

            assertResult(exp)(out.toString.contains(text))
        }
        finally {
            out.reset()
        }
    }

    /**
      * Redirect stdout and compare output with specified regexp.
      *
      * @param block Function to execute.
      * @param regex Regexp to match with.
      */
    protected def matchOut(block: => Unit, regex: String) {
        try {
            Console.withOut(out)(block)

            assertResult(true)(Pattern.compile(regex, Pattern.MULTILINE).matcher(out.toString).find())
        }
        finally {
            out.reset()
        }
    }

    protected def openVisor() {
        visor.open(config("visor-demo-node"), "n/a")
    }

    protected def closeVisorQuiet() {
        if (visor.isConnected)
            visor.close()
    }

    /**
     * Runs before all tests.
     */
    override protected def beforeAll() {
        (1 to num).foreach((n: Int) => Ignition.start(config("node-" + n)))
    }

    /**
     * Runs after all tests.
     */
    override def afterAll() {
        (1 to num).foreach((n: Int) => Ignition.stop("node-" + n, false))

        out.close()
    }

    override protected def beforeEach() {
        openVisor()
    }

    override protected def afterEach() {
        closeVisorQuiet()
    }
}
