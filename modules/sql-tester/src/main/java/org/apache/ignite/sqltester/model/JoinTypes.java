package org.apache.ignite.sqltester.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.ignite.cache.query.annotations.QuerySqlField;

/**
 * Created by gridgain on 23.03.2016.
 */
public class JoinTypes implements Serializable {
    /** Data Long. */
    @QuerySqlField(index = true)
    private Long longCol;

    /** Data double. */
    @QuerySqlField(index = true)
    private double doubleCol;

    /** Data String. */
    @QuerySqlField(index = true)
    private String stringCol;

    /** Data boolean. */
    private boolean  booleanCol;

    /** Data int. */
    @QuerySqlField(index = true)
    private int intCol;

    /** Data bytes array. */
    private byte[] bytesCol;
    /**
     * @param randomKey Max value.
     */

    private InnerTypes innerTypesCol;

    private class InnerTypes implements Serializable {
        private Long longCol;

        InnerTypes(Long key) {
            this.longCol = key;

        }

        /**
         * {@inheritDoc}
         */
        @Override public String toString() {
            return "InnerTypes [Long=" + Long.toString(longCol) + "]";
        }
    }

    private enum enumType {
        ENUMTRUE, ENUMFALSE
    }

    private enumType enumCol;

    private ConcurrentMap<String, String> mapCol = new ConcurrentHashMap<String, String>();

    private void init(Long key, String str) {
        this.longCol = key;
        this.doubleCol = key.doubleValue()/100;
        this.stringCol = str;
        if (key % 2 == 0) {
            this.booleanCol = true;
            this.enumCol = enumType.ENUMTRUE;
        }
        else {
            this.booleanCol = false;
            this.enumCol = enumType.ENUMFALSE;
        }
        this.intCol = key.intValue();
        this.bytesCol = this.stringCol.getBytes();
        this.innerTypesCol = new InnerTypes(key);
        this.mapCol.put("map_key_" + Long.toString(key), "map_value_" + Long.toString(key));
        this.mapCol.put("map_key_" + Long.toString(key+1), "map_value_" + Long.toString(key+1));
    }

    public JoinTypes(Long key) {
        this.init(key, Long.toString(key));
    }

    public JoinTypes(Long key, String str) {
        this.init(key, str);
    }

    /**
     * {@inheritDoc}
     */
    @Override public String toString() {
        return "JoinTypes [Long=" + Long.toString(longCol) +
                ", double=" + Double.toString(doubleCol) +
                ", String='" + stringCol +
                "', boolean=" + Boolean.toString(booleanCol) +
                ", int=" + Integer.toString(intCol) +
                ", bytes='" + Arrays.toString(bytesCol) +
                "', InnerTypes=" + innerTypesCol.toString() +
                "', Map='" + mapCol.toString() +
                "', enum='" + enumCol.toString() +
                "']";
    }

}