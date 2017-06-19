package org.apache.ignite.internal.processors.query.h2.opt.database;

import org.apache.lucene.store.Directory;

/**
 * Created by amashenkov on 19.06.17.
 */
public class LuceneFile extends DirectoryItem {

    private Directory dir;

    public LuceneFile(Directory dir, byte[] fileName) {
        super(fileName);
        this.dir = dir;
    }

    public LuceneFile(Directory dir, byte[] fileName, long pageId) {
        super(fileName, pageId);
    }
}
