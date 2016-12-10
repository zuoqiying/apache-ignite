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

package org.apache.ignite.internal.visor;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Base class for data transfer objects.
 */
abstract public class VisorDataTransferObject implements Externalizable {
    /**  */
    public static int FIRST_VER = 1;

    /** Object version */
    protected int ver = FIRST_VER;

    /**
     * Save object's specific data content.
     *
     * @param out Output object to write data content.
     * @throws IOException
     */
    abstract protected void writeExternalData(ObjectOutput out) throws IOException;

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(ver);

        writeExternalData(out);
    }

    /**
     * Load object's specific data content.
     *
     * @param in Input object to load data content.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    abstract protected void readExternalData(ObjectInput in) throws IOException, ClassNotFoundException;

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        ver = in.readInt();

        readExternalData(in);
    }
}
