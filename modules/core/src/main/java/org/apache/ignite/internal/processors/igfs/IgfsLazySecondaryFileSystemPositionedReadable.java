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

package org.apache.ignite.internal.processors.igfs;

import java.util.concurrent.atomic.AtomicReference;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.igfs.IgfsPath;
import org.apache.ignite.igfs.secondary.IgfsSecondaryFileSystem;
import org.apache.ignite.igfs.secondary.IgfsSecondaryFileSystemPositionedReadable;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.util.future.GridFutureAdapter;
import org.apache.ignite.internal.util.typedef.internal.S;

import java.io.IOException;
import org.jetbrains.annotations.Nullable;

/**
 * Lazy readable entity which is opened on demand.
 */
public class IgfsLazySecondaryFileSystemPositionedReadable implements IgfsSecondaryFileSystemPositionedReadable {
    /** File system. */
    private final IgfsSecondaryFileSystem fs;

    /** Path. */
    private final IgfsPath path;

    /** Buffer size. */
    private final int bufSize;

    /** Lazy IgfsSecondaryFileSystemPositionedReadable value. */
    private final LazyValue<IgfsSecondaryFileSystemPositionedReadable> lazyVal
        = new LazyValue<IgfsSecondaryFileSystemPositionedReadable>() {
        /** {@inheritDoc} */
        @Override protected IgfsSecondaryFileSystemPositionedReadable create() {
            return fs.open(path, bufSize);
        }
    };

    /**
     * Constructor.
     *
     * @param fs File system.
     * @param path Path.
     * @param bufSize Buffer size.
     */
    public IgfsLazySecondaryFileSystemPositionedReadable(IgfsSecondaryFileSystem fs, IgfsPath path, int bufSize) {
        assert fs != null;
        assert path != null;

        this.fs = fs;
        this.path = path;
        this.bufSize = bufSize;
    }

    /** {@inheritDoc} */
    @Override public int read(long pos, byte[] buf, int off, int len) throws IOException {
        try {
            IgfsSecondaryFileSystemPositionedReadable r = lazyVal.getOrCreate();

            return r.read(pos, buf, off, len);
        }
        catch (Exception e) {
            throw LazyValue.asIOException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public void close() throws IOException {
        try {
            IgfsSecondaryFileSystemPositionedReadable r = lazyVal.getAsIs();

            if (r != null)
                r.close();
        }
        catch (Exception e) {
            throw LazyValue.asIOException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(IgfsLazySecondaryFileSystemPositionedReadable.class, this);
    }

    /**
     * Represents a generic purpose thread-safe lazy value container.
     * The value is initialized on demand only once.
     *
     * @param <T> The value type.
     */
    public static abstract class LazyValue<T> {
        /** */
        private final AtomicReference<IgniteInternalFuture<T>> futRef = new AtomicReference<>();

        /**
         * Creates the value. This method is guaranteed to be invoked not more than once on an instance.
         *
         * @return The just created value. Must never return null.
         * @throws Exception On error.
         */
        protected abstract T create() throws Exception;

        /**
         * Lazily gets the value or creates it as needed.
         *
         * @return The value.
         * @throws IgniteCheckedException On error.
         */
        public T getOrCreate() throws Exception {
            IgniteInternalFuture<T> fut = futRef.get();

            if (fut == null) {
                GridFutureAdapter<T> a = new GridFutureAdapter<>();

                if (futRef.compareAndSet(null, a)) {
                    try {
                        T r = create();

                        assert r != null;

                        a.onDone(r);
                    }
                    catch (Exception e) {
                        a.onDone(e);
                    }

                    fut = a;
                } else
                    fut = futRef.get();
            }

            assert fut != null;

            try {
                return fut.get();
            }
            catch (IgniteCheckedException ice) {
                throw unwrap(ice);
            }
        }

        /**
         * Gets the value, but does not invoke initialization.
         *
         * @return The value or null, if not yet initialized.
         * @throws IgniteCheckedException On error.
         */
        public @Nullable T getAsIs() throws Exception {
            IgniteInternalFuture<T> fut = futRef.get();

            if (fut == null)
                return null;

            try {
                return fut.get();
            }
            catch (IgniteCheckedException ice) {
                throw unwrap(ice);
            }
        }

        /**
         * Unwraps an exception by one level, if possible.
         *
         * @param ice An IgniteCheckedException possibly wrapping another exception.
         * @return The unwrapped Exception, or the argument, if it was not possible to unwrap an exception.
         */
        protected Exception unwrap(IgniteCheckedException ice) {
            Throwable t = ice.getCause();

            if (t instanceof Exception)
                return (Exception)t;

            return ice;
        }

        /**
         * Casts or wraps an exception as IOException. Utility function.
         *
         * @param e an Exception.
         * @return casted or wrapped IOException.
         */
        public static IOException asIOException(Exception e) {
            if (e instanceof IOException)
                return (IOException)e;

            return new IOException(e);
        }
    }
}
