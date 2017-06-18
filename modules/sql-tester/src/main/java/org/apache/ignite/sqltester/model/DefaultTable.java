package org.apache.ignite.sqltester.model;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by gridgain on 23.03.2016.
 */
public class DefaultTable implements Serializable {

    /** Id */
    private Long id;

    /** Boolean */
    private Boolean col_Boolean_1;
    private Boolean col_Boolean_2;
    private Boolean col_Boolean_3;
    private Boolean col_Boolean_4;
    private Boolean col_Boolean_5;

    /** Double */
    private Date col_Date_1;
    private Date col_Date_2;
    private Date col_Date_3;
    private Date col_Date_4;
    private Date col_Date_5;

    /** Double */
    private Double col_Double_1;
    private Double col_Double_2;
    private Double col_Double_3;
    private Double col_Double_4;
    private Double col_Double_5;

    /** Integer */
    private Integer col_Integer_1;
    private Integer col_Integer_2;
    private Integer col_Integer_3;
    private Integer col_Integer_4;
    private Integer col_Integer_5;

    /** Long */
    private Long col_Long_1;
    private Long col_Long_2;
    private Long col_Long_3;
    private Long col_Long_4;
    private Long col_Long_5;

    /** Short */
    private Short col_Short_1;
    private Short col_Short_2;
    private Short col_Short_3;
    private Short col_Short_4;
    private Short col_Short_5;

    /** String. */
    public String col_String_1;
    public String col_String_2;
    public String col_String_3;
    public String col_String_4;
    public String col_String_5;

    /** Timestamp */
    private Time col_Time_1;
    private Time col_Time_2;
    private Time col_Time_3;
    private Time col_Time_4;
    private Time col_Time_5;

    /** Timestamp
    private Timestamp col_Timestamp_1;
    private Timestamp col_Timestamp_2;
    private Timestamp col_Timestamp_3;
    private Timestamp col_Timestamp_4;
    private Timestamp col_Timestamp_5;
     */

    private static final HashMap<String, String> javaTypeToSql;
    static
    {
        javaTypeToSql = new HashMap<String, String>();
        javaTypeToSql.put("Boolean", "BOOLEAN");
        javaTypeToSql.put("Date", "DATE");
        javaTypeToSql.put("Double", "FLOAT");
        javaTypeToSql.put("Integer", "INT");
        javaTypeToSql.put("Long", "BIGINT");
        javaTypeToSql.put("Short", "SMALLINT");
        javaTypeToSql.put("String", "VARCHAR(200)");
        javaTypeToSql.put("Time", "TIME");
        javaTypeToSql.put("Timestamp", "TIMESTAMP");
    }

    private void init(Long key) {
        this.id = key;
        Long dateOffset = 1000000000000L;
        this.col_Boolean_1 = false;
        this.col_Boolean_2 = false;
        this.col_Boolean_3 = false;
        this.col_Boolean_4 = false;
        this.col_Boolean_5 = false;
        if (key % 2 == 0)
            this.col_Boolean_1 = true;
        if (key % 4 == 0)
            this.col_Boolean_2 = true;
        if (key % 8 == 0)
            this.col_Boolean_3 = true;
        if (key % 16 == 0)
            this.col_Boolean_4 = true;
        if (key % 32 == 0)
            this.col_Boolean_5 = true;

        this.col_Date_1 = new Date(dateOffset + key);
        this.col_Date_2 = new Date(dateOffset + key - 86400000L); // - 1 day
        this.col_Date_3 = new Date(dateOffset + key + 2592000000L); // + 1 month
        this.col_Date_4 = new Date(dateOffset + key - 31622400000L); // - 1 year
        this.col_Date_5 = new Date(dateOffset + key - 316224000000L); // +10 year

        this.col_Double_1 = key + 0.1;
        this.col_Double_2 = -1*key - 0.01;
        this.col_Double_3 = key + 0.001;
        this.col_Double_4 = -1*key - 0.0001;
        this.col_Double_5 = key + 0.00001;

        this.col_Integer_1 = key.intValue()/2;
        this.col_Integer_2 = -1 * key.intValue()/4;
        this.col_Integer_3 = key.intValue()/8;
        this.col_Integer_4 = -1 * key.intValue()/16;
        this.col_Integer_5 = key.intValue()/32;

        this.col_Long_1 = key;
        this.col_Long_2 = -1L * key/2;
        this.col_Long_3 = key/4;
        this.col_Long_4 = -1L * key/8;
        this.col_Long_4 = key/16;

        this.col_Short_1 = (short)(key/10);
        this.col_Short_2 = (short)(-1L * key/100);
        this.col_Short_3 = (short)(key/1000);
        this.col_Short_4 = (short)(-1L * key/10000);
        this.col_Short_5 = (short)(key/100000);

        this.col_String_1 = "String_1_" + key.toString();
        this.col_String_2 = "String_2_" + key.toString();
        this.col_String_3 = "String_3_" + key.toString();
        this.col_String_4 = "String_4_" + key.toString();
        this.col_String_5 = "String_5_" + key.toString();

        this.col_Time_1 = new Time(dateOffset + key);
        this.col_Time_2 = new Time(dateOffset + key - 1000L); // - 1 sec
        this.col_Time_3 = new Time(dateOffset + key + 60000L); // + 1 min
        this.col_Time_4 = new Time(dateOffset + key - 3600000L); // - 1 hour
        this.col_Time_5 = new Time(dateOffset + key + 86400000L); // + 1 day

        /**
        this.col_Timestamp_1 = new Timestamp(dateOffset + key);
        this.col_Timestamp_2 = new Timestamp(dateOffset + key - 1000L); // - 1 sec
        this.col_Timestamp_3 = new Timestamp(dateOffset + key + 60000L); // + 1 min
        this.col_Timestamp_4 = new Timestamp(dateOffset + key - 3600000L); // - 1 hour
        this.col_Timestamp_5 = new Timestamp(dateOffset + key + 86400000L); // + 1 day
        */

        String lastChar = Long.toString(key);
        lastChar = lastChar.substring(lastChar.length()-1, lastChar.length());
        if (lastChar.equals("10")) {
            this.col_Boolean_5 = null;
            this.col_Date_5 = null;
            this.col_Double_5 = null;
            this.col_Integer_5 = null;
            this.col_Long_5 = null;
            this.col_Short_5 = null;
            this.col_String_5 = null;
            this.col_Time_5 = null;
            //this.col_Timestamp_5 = null;
        }
    }

    public DefaultTable(Long key) {
        this.init(key);
    }

    private String getNullOrString(Object o) {
        String outVal;
        if (o != null) {
            if (o instanceof Date) {
                outVal = new SimpleDateFormat("yyyy-MM-dd").format(o);
                outVal = "'" + outVal + "'";
            } else if (o instanceof String || o instanceof Timestamp) {
                outVal = "'" + o + "'";
            } else {
                outVal = o.toString();
            }
        } else {
            outVal = "NULL";
        }
        return outVal;
    }

    private String getNullOrStringForTime(Object o) {
        String outVal;
        if (o != null) {
            outVal = new SimpleDateFormat("HH:mm:ss").format(o);
            outVal = "'" + outVal + "'";
        } else {
            outVal = "NULL";
        }
        return outVal;
    }

    public String toSqlString(String sqlType) throws IllegalAccessException{
        String output = "";
        ArrayList<String> clsFields = new ArrayList<>();
        for (Field f: this.getClass().getDeclaredFields()) {
            if (f.getName().startsWith("col_")) {
                if (sqlType.equalsIgnoreCase("createFields"))
                    output += f.getName() + " " + javaTypeToSql.get(f.getName().substring(4, f.getName().length()-2)) + ", ";
                 else if (sqlType.equalsIgnoreCase("insertFields"))
                    output += f.getName() + ", ";
                 else if (sqlType.equalsIgnoreCase("insertValues")) {
                    if (f.getName().startsWith("col_Time_"))
                        output += getNullOrStringForTime(f.get(this)) + ", ";
                    else
                        output += getNullOrString(f.get(this)) + ", ";
                } else if (sqlType.equalsIgnoreCase("name"))
                    output = this.getClass().getName().substring(34);

            } else if (sqlType.equalsIgnoreCase("name"))
                output = this.getClass().getName().substring(34);

        }
        if (output.length() > 0 && output.endsWith(", "))
            output = output.substring(0, output.length() - 2);
        return output;
    }

    /**
     * {@inheritDoc}
     */
    @Override public String toString() {
        return getNullOrString(col_Boolean_1) + ", "
                + getNullOrString(col_Boolean_2) + ", "
                + getNullOrString(col_Boolean_3) + ", "
                + getNullOrString(col_Boolean_4) + ", "
                + getNullOrString(col_Boolean_5) + ", "
                + getNullOrString(col_Date_1) + ", "
                + getNullOrString(col_Date_2) + ", "
                + getNullOrString(col_Date_3) + ", "
                + getNullOrString(col_Date_4) + ", "
                + getNullOrString(col_Date_5) + ", "
                + getNullOrString(col_Double_1) + ", "
                + getNullOrString(col_Double_2) + ", "
                + getNullOrString(col_Double_3) + ", "
                + getNullOrString(col_Double_4) + ", "
                + getNullOrString(col_Double_5) + ", "
                + getNullOrString(col_Integer_1) + ", "
                + getNullOrString(col_Integer_2) + ", "
                + getNullOrString(col_Integer_3) + ", "
                + getNullOrString(col_Integer_4) + ", "
                + getNullOrString(col_Integer_5) + ", "
                + getNullOrString(col_Long_1) + ", "
                + getNullOrString(col_Long_2) + ", "
                + getNullOrString(col_Long_3) + ", "
                + getNullOrString(col_Long_4) + ", "
                + getNullOrString(col_Long_5) + ", "
                + getNullOrString(col_Short_1) + ", "
                + getNullOrString(col_Short_2) + ", "
                + getNullOrString(col_Short_3) + ", "
                + getNullOrString(col_Short_4) + ", "
                + getNullOrString(col_Short_5) + ", '"
                + getNullOrString(col_String_1) + "', '"
                + getNullOrString(col_String_2) + "', '"
                + getNullOrString(col_String_3) + "', '"
                + getNullOrString(col_String_4) + "', '"
                + getNullOrString(col_String_5);/** + "', "

            + getNullOrString(col_Timestamp_1) + ", "
                + getNullOrString(col_Timestamp_2) + ", "
                + getNullOrString(col_Timestamp_3) + ", "
                + getNullOrString(col_Timestamp_4) + ", "
                + getNullOrString(col_Timestamp_5); */
    }

}