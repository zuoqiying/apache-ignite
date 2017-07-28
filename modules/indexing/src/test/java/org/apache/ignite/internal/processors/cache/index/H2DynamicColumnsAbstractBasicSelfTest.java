package org.apache.ignite.internal.processors.cache.index;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.processors.query.QueryField;

/**
 * Test to check dynamic columns related features.
 */
public abstract class H2DynamicColumnsAbstractBasicSelfTest extends DynamicColumnsAbstractTest {
    /**
     * Index of coordinator node.
     */
    protected final static int SRV_CRD_IDX = 0;

    /**
     * Index of non coordinator server node.
     */
    protected final static int SRV_IDX = 1;

    /**
     * Index of client.
     */
    protected final static int CLI_IDX = 2;

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        for (IgniteConfiguration cfg : configurations())
            Ignition.start(cfg);
    }

    /**
     * @return Grid configurations to start.
     * @throws Exception if failed.
     */
    private IgniteConfiguration[] configurations() throws Exception {
        return new IgniteConfiguration[] {
            commonConfiguration(0),
            commonConfiguration(1),
            clientConfiguration(2)
        };
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();

        super.afterTestsStopped();
    }

    /**
     * Test column addition to the end of the columns list.
     */
    public void testAddColumnSimple() {
        run("ALTER TABLE Person ADD COLUMN age int");

        doSleep(500);

        QueryField c = c("AGE", Integer.class.getName());

        for (Ignite node : Ignition.allGrids())
            checkNodeState((IgniteEx)node, "PERSON", "NAME", c);
    }

    /**
     * Test column addition before specified column.
     */
    public void testAddColumnBefore() {
        run("ALTER TABLE Person ADD COLUMN age int before id");

        doSleep(500);

        QueryField c = c("AGE", Integer.class.getName());

        for (Ignite node : Ignition.allGrids())
            checkNodeState((IgniteEx)node, "PERSON", null, c);
    }

    /**
     * Test column addition after specified column.
     */
    public void testAddColumnAfter() {
        run("ALTER TABLE Person ADD COLUMN age int after id");

        doSleep(500);

        QueryField c = c("AGE", Integer.class.getName());

        for (Ignite node : Ignition.allGrids())
            checkNodeState((IgniteEx)node, "PERSON", "ID", c);
    }

    /**
     * Test column addition to the end of the columns list.
     */
    public void testAddFewColumnsSimple() {
        run("ALTER TABLE Person ADD COLUMN (age int, \"city\" varchar)");

        doSleep(500);

        for (Ignite node : Ignition.allGrids())
            checkNodeState((IgniteEx)node, "PERSON", "NAME",
                c("AGE", Integer.class.getName()),
                c("city", String.class.getName()));
    }

    /**
     * Test column addition before specified column.
     */
    public void testAddFewColumnsBefore() {
        run("ALTER TABLE Person ADD COLUMN (age int, \"city\" varchar) before id");

        doSleep(500);

        for (Ignite node : Ignition.allGrids())
            checkNodeState((IgniteEx)node, "PERSON", null,
                c("AGE", Integer.class.getName()),
                c("city", String.class.getName()));
    }

    /**
     * Test column addition after specified column.
     */
    public void testAddFewColumnsAfter() {
        run("ALTER TABLE Person ADD COLUMN (age int, \"city\" varchar) after id");

        doSleep(500);

        for (Ignite node : Ignition.allGrids())
            checkNodeState((IgniteEx)node, "PERSON", "ID",
                c("AGE", Integer.class.getName()),
                c("city", String.class.getName()));
    }

    /**
     * Test {@code IF EXISTS} handling.
     */
    public void testIfTableExists() {
        run("ALTER TABLE if exists City ADD COLUMN population int after name");
    }

    /**
     * Test {@code IF NOT EXISTS} handling.
     */
    public void testIfColumnNotExists() {
        run("ALTER TABLE Person ADD COLUMN if not exists name varchar after id");
    }

    /**
     * Test {@code IF NOT EXISTS} handling.
     */
    public void testDuplicateColumnName() {
        assertThrows("ALTER TABLE Person ADD COLUMN name varchar after id",
            "Column already exists [tblName=PERSON, colName=NAME]");
    }

    /**
     * Test behavior in case of missing table.
     */
    public void testMissingTable() {
        assertThrows("ALTER TABLE City ADD COLUMN name varchar after id", "Table doesn't exist: CITY");
    }

    /**
     * Test missing "before" column..
     */
    public void testMissingBeforeColumn() {
        assertThrows("ALTER TABLE Person ADD COLUMN city varchar before x",
            "Column \"X\" not found");
    }

    /**
     * Test missing "after" column..
     */
    public void testMissingAfterColumn() {
        assertThrows("ALTER TABLE Person ADD COLUMN city varchar after x",
            "Column \"X\" not found");
    }
}
