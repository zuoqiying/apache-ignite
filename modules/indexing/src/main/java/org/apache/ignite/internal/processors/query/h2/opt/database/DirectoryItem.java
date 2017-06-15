package org.apache.ignite.internal.processors.query.h2.opt.database;

import org.apache.ignite.internal.util.typedef.internal.U;

/**
 *
 */
class DirectoryItem {
    /** */
    private byte[] fileName;

    /** */
    private long pageId;

    /**
     * @param fileName File name.
     */
    public DirectoryItem(byte[] fileName) {
        this.fileName = fileName;
    }

    /**
     * @param fileName File name.
     * @param pageId Page ID.
     */
    DirectoryItem(final byte[] fileName, final long pageId) {
        this.fileName = fileName;
        this.pageId = pageId;
    }

    /**
     * @return fileName bytes.
     */
    public byte[] getFileName() {
        return fileName;
    }

    /**
     * @return page Id.
     */
    public long getPageId() {
        return pageId;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return "I [FileName=" + new String(fileName) + ", pageId=" + U.hexLong(pageId) + ']';
    }
}
