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

package org.apache.ignite.internal.visor.binary;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.ignite.IgniteBinary;
import org.apache.ignite.binary.BinaryType;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.internal.visor.VisorDataTransferObject;
import org.jetbrains.annotations.Nullable;

/**
 * Binary object metadata to show in Visor.
 */
public class VisorBinaryMetadata extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Type name */
    private String typeName;

    /** Type Id */
    private Integer typeId;

    /** Affinity key field name. */
    private String affinityKeyFieldName;

    /** Filed list */
    private Collection<VisorBinaryMetadataField> fields;

    /**
     * Default constructor.
     */
    public VisorBinaryMetadata() {
        // No-op.
    }

    /**
     * @param binary Binary objects.
     * @return List of data transfer objects for binary objects metadata.
     */
    public static Collection<VisorBinaryMetadata> list(IgniteBinary binary) {
        if (binary == null)
            return new ArrayList<>(0);

        Collection<BinaryType> metas = binary.types();

        Collection<VisorBinaryMetadata> res = new ArrayList<>(metas.size());

        for (BinaryType meta : metas) {
            VisorBinaryMetadata m = new VisorBinaryMetadata();

            m.typeName = meta.typeName();
            m.typeId = binary.typeId(meta.typeName());
            m.affinityKeyFieldName = meta.affinityKeyFieldName();

            Collection<String> metaFields = meta.fieldNames();

            m.fields = new ArrayList<>(metaFields.size());

            for (String metaField : metaFields)
                m.fields.add(new VisorBinaryMetadataField(metaField, meta.fieldTypeName(metaField), null));

            res.add(m);
        }

        return res;
    }

    /**
     * @return Type name.
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * @return Type Id.
     */
    public Integer getTypeId() {
        return typeId;
    }

    /**
     * @return Fields list.
     */
    public Collection<VisorBinaryMetadataField> getFields() {
        return fields;
    }

    /**
     * @return Affinity key field name.
     */
    @Nullable public String getAffinityKeyFieldName() {
        return affinityKeyFieldName;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        U.writeString(out, typeName);
        out.writeObject(typeId);
        U.writeString(out, affinityKeyFieldName);
        U.writeCollection(out, fields);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(ObjectInput in) throws IOException, ClassNotFoundException {
        typeName = U.readString(in);
        typeId = (Integer)in.readObject();
        affinityKeyFieldName = U.readString(in);
        fields = U.readCollection(in);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorBinaryMetadata.class, this);
    }
}
