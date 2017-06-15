package org.apache.ignite.sqltester.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by gridgain on 23.03.2016.
 */
public class AllTypes implements Serializable {
    /** Data Long. */
    public Long longCol;

    /** Data double. */
    public double doubleCol;

    /** Data String. */
    public String stringCol;

    /** Data boolean. */
    public boolean booleanCol;

    /** Data int. */
    public int intCol;

    /** BigDecimal */
    public BigDecimal bigDecimalCol;

    /** Data bytes array. */
    public byte[] bytesCol;

    /** Data bytes array. */
    public Short shortCol;

    public InnerTypes innerTypesCol;

    public class InnerTypes implements Serializable {
        public Long longCol;

        public String stringCol;

        public ArrayList<Long> arrayListCol = new ArrayList<>();

        InnerTypes(Long key) {

            this.longCol = key;
            this.stringCol = Long.toString(key);
            Long m = key % 8;
            for (Integer i = 0; i < m; i++) {
                arrayListCol.add(key+i);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override public String toString() {
            return "[Long=" + Long.toString(longCol) +
                    ", String='" + stringCol + "'" +
                    ", ArrayList=" + arrayListCol.toString() +
                    "]";
        }
    }

    public enumType enumCol;

    public ConcurrentMap<String, String> mapCol = new ConcurrentHashMap<String, String>();

    public HashSet<String> hashSetCol = new HashSet<>();

    public enum enumType {
        ENUMTRUE, ENUMFALSE
    }

    private void init(Long key, String str) {
        this.longCol = key;
        this.doubleCol = Math.round(1000*Math.log10(longCol.doubleValue()));
        this.bigDecimalCol = BigDecimal.valueOf(doubleCol);
        this.doubleCol = (double)doubleCol/100;
        this.stringCol = str;
        if (key % 2 == 0) {
            this.booleanCol = true;
            this.enumCol = enumType.ENUMTRUE;
            this.innerTypesCol = new InnerTypes(key);
        }
        else {
            this.booleanCol = false;
            this.enumCol = enumType.ENUMFALSE;
            this.innerTypesCol = null;
        }
        this.intCol = key.intValue();
        this.bytesCol = this.stringCol.getBytes();
        this.shortCol = (short)(((1000*key) % 50000) - 25000);
        Long m = key % 7;
        for (Integer i = 0; i < m; i++) {
            this.mapCol.put("map_key_" + Long.toString(key + i), "map_value_" + Long.toString(key + i));
        }
        m = (long)(key % 11)/2;
        for (Integer i = 0; i < m; i++) {
            this.hashSetCol.add("hashset_" + Long.toString(key + i));
        }
    }

    public AllTypes(Long key) {
        this.init(key, Long.toString(key));
    }

    public AllTypes(Long key, String str) {
        this.init(key, str);
    }

    /**
     * {@inheritDoc}
     */
    @Override public String toString() {
        List hashSetCol_SortedList = new ArrayList(hashSetCol);
        Collections.sort(hashSetCol_SortedList);
        String mapCol_string =  "{";
        for(String key: new TreeSet<String>(mapCol.keySet())) {
            mapCol_string += key + "=" + mapCol.get(key) + ", ";
        }
        if (mapCol_string.length() > 1) {
            mapCol_string = mapCol_string.substring(0, mapCol_string.length()-2);
        }
        mapCol_string += "}";
        String innerTypesStr = "null";
        if (innerTypesCol != null)
            innerTypesStr = innerTypesCol.toString();
        return "AllTypes=[Long=" + Long.toString(longCol) +
                ", double=" + Double.toString(doubleCol) +
                ", String='" + stringCol +
                "', boolean=" + Boolean.toString(booleanCol) +
                ", int=" + Integer.toString(intCol) +
                ", bigDecimal=" + bigDecimalCol.toString() +
                ", bytes=" + Arrays.toString(bytesCol) +
                ", short=" + Short.toString(shortCol) +
                ", InnerTypes=" + innerTypesStr +
                ", Map=" + mapCol_string +
                ", HashSet=" + hashSetCol_SortedList.toString() +
                ", enum=" + enumCol.toString() +
                "]";
    }

}