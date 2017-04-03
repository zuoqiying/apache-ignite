package org.apache.ignite.cache.hibernate;

import java.io.Serializable;

/**
 *
 */
public interface HibernateKeyTransformer extends Serializable {
    /**
     * @param key Key.
     */
    Object transform(Object key);
}
