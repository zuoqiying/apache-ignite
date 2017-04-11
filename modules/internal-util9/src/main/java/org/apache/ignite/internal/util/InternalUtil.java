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

package org.apache.ignite.internal.util;

import java.io.IOException;
import java.lang.ref.Cleaner;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Base64;

import jdk.internal.loader.BuiltinClassLoader;
import jdk.internal.loader.URLClassPath;
import sun.misc.Unsafe;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.lang.reflect.Field;

/**
 * Util class for use in java >= 9
 */
public class InternalUtil {

    /** Unsafe. */
    private static final Unsafe UNSAFE = unsafe();

    public static String encodeBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static void createCleaner(Object obj, Runnable runnable) {
        Cleaner.create().register(obj, runnable);
    }

    public static void cleanDirectBuffer(ByteBuffer directBuffer) {
        UNSAFE.invokeCleaner(directBuffer);
    }

    public static URL[] getUrlsByAppClassloader(ClassLoader classLoader) {
        try {
            Field f = BuiltinClassLoader.class.getDeclaredField("ucp");
            f.setAccessible(true);
            return ((URLClassPath)f.get(classLoader)).getURLs();
        }
        catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
            return new URL[0];

    }

    public static void closeAppClassloader(ClassLoader classLoader) throws IOException {
        // No-op
    }

    /**
     * @return Instance of Unsafe class.
     */
    private static Unsafe unsafe() {
        try {
            return Unsafe.getUnsafe();
        }
        catch (SecurityException ignored) {
            try {
                return AccessController.doPrivileged
                    (new PrivilegedExceptionAction<Unsafe>() {
                        @Override public Unsafe run() throws Exception {
                            Field f = Unsafe.class.getDeclaredField("theUnsafe");

                            f.setAccessible(true);

                            return (Unsafe)f.get(null);
                        }
                    });
            }
            catch (PrivilegedActionException e) {
                throw new RuntimeException("Could not initialize intrinsics.", e.getCause());
            }
        }
    }
}
