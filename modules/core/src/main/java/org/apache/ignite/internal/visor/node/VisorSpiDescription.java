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

package org.apache.ignite.internal.visor.node;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.internal.visor.VisorDataTransferObject;

/**
 * Transfer object for single spi description.
 */
public class VisorSpiDescription extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** SPI class name. */
    private String clsName;

    /** SPI fields description. */
    private Map<String, Object> fldDesc;

    /**
     * Default constructor.
     */
    public VisorSpiDescription() {
        // No-op.
    }

    /**
     * Construct Visor spi description object.
     *
     * @param clsName SPI class name.
     * @param fldDesc SPI fields description.
     */
    public VisorSpiDescription(String clsName, Map<String, Object> fldDesc) {
        this.clsName = clsName;
        this.fldDesc = fldDesc;
    }

    /**
     * @return SPI class name.
     */
    public String getClassName() {
        return clsName;
    }

    /**
     * @return SPI fields description.
     */
    public Map<String, Object> getFieldDescriptions() {
        return fldDesc;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        U.writeString(out, clsName);
        U.writeMap(out, fldDesc);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(ObjectInput in) throws IOException, ClassNotFoundException {
        clsName = U.readString(in);
        fldDesc = U.readMap(in);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorSpiDescription.class, this);
    }
}
