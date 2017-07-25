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

package org.apache.ignite.internal.processors.query.schema.operation;

import org.apache.ignite.internal.processors.query.QueryField;
import org.apache.ignite.internal.util.typedef.internal.S;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Schema index drop operation.
 */
public class SchemaAlterTableAddColumnOperation extends SchemaAbstractAlterTableOperation {
    /** */
    private static final long serialVersionUID = 0L;

    /** Target table name. */
    private final String tblName;

    /** Columns to add. */
    private final List<QueryField> cols;

    /** Column name before which new columns should be added. */
    private final String beforeColName;

    /** Column name after which new columns should be added. */
    private final String afterColName;

    /** Ignore operation if target table doesn't exist. */
    private final boolean ifTableExists;

    /** Ignore operation if column exists. */
    private final boolean ifNotExists;

    /**
     * Constructor.
     *
     * @param opId Operation id.
     * @param schemaName Schema name.
     * @param tblName Target table name.
     * @param cols Columns to add.
     * @param beforeColName Column name before which new columns should be added.
     * @param afterColName Column name after which new columns should be added.
     * @param ifTableExists Ignore operation if target table doesn't exist.
     * @param ifNotExists Ignore operation if column exists.
     */
    public SchemaAlterTableAddColumnOperation(UUID opId, String cacheName, String schemaName, String tblName,
        List<QueryField> cols, String beforeColName, String afterColName, boolean ifTableExists, boolean ifNotExists) {
        super(opId, cacheName, schemaName);

        this.tblName = tblName;
        this.cols = cols;

        this.beforeColName = beforeColName;
        this.afterColName = afterColName;

        this.ifTableExists = ifTableExists;
        this.ifNotExists = ifNotExists;
    }

    /**
     * @return Ignore operation if index doesn't exist.
     */
    public boolean ifTableExists() {
        return ifTableExists;
    }

    /**
     * @return Name of the column after which new columns must be added.
     */
    public String afterColumnName() {
        return afterColName;
    }

    /**
     * @return Name of the column before which new columns must be added.
     */
    public String beforeColumnName() {
        return beforeColName;
    }

    /**
     * @return Columns to add.
     */
    public List<QueryField> columns() {
        return cols;
    }

    /**
     * @return Quietly abort this command if column exists (honored only in single column case).
     */
    public boolean ifNotExists() {
        return ifNotExists;
    }

    /**
     * @return Target table name.
     */
    public String tableName() {
        return tblName;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(SchemaAlterTableAddColumnOperation.class, this, "parent", super.toString());
    }
}
