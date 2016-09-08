package org.apache.ignite.internal.pagemem.wal.record;

import org.apache.ignite.internal.pagemem.FullPageId;

/**
 *
 */
public class PageAllocatedRecord extends WALRecord {
    /** */
    private final FullPageId pageId;

    /** */
    public PageAllocatedRecord(FullPageId pageId) {
        this.pageId = pageId;
    }

    public FullPageId fullPageId() {
        return pageId;
    }

    /** {@inheritDoc} */
    @Override public RecordType type() {
        return RecordType.PAGE_ALLOCATED_RECORD;
    }
}
