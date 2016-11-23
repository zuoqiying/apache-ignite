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

package org.apache.ignite.internal.visor.debug;

import java.io.Serializable;
import java.lang.management.ThreadInfo;

/**
 * Data transfer object for Visor {@link ThreadInfo}.
 */
public class VisorThreadInfo implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private static final int MAX_FRAMES = 8;

    /** Thread name. */
    private String name;

    /** Thread ID. */
    private Long id;

    /** Thread state. */
    private Thread.State state;

    /** Lock information. */
    private VisorThreadLockInfo lock;

    /** Lock name. */
    private String lockName;

    /** Lock owner thread ID. */
    private Long lockOwnerId;

    /** Lock owner name. */
    private String lockOwnerName;

    /** Thread executing native code. */
    private Boolean inNative;

    /** Thread is suspended. */
    private Boolean suspended;

    /** Waited count. */
    private Long waitedCnt;

    /** Waited time. */
    private Long waitedTime;

    /** Blocked count. */
    private Long blockedCnt;

    /** Blocked time. */
    private Long blockedTime;

    /** Stack trace. */
    private StackTraceElement[] stackTrace;

    /** Locks info. */
    private VisorThreadLockInfo[] locks;

    /** Locked monitors. */
    private VisorThreadMonitorInfo[] lockedMonitors;

    /**
     * Create data transfer object for given thread info.
     *
     * @param ti Thread info.
     */
    public VisorThreadInfo(ThreadInfo ti) {
        assert ti != null;

        name = ti.getThreadName();
        id = ti.getThreadId();
        state = ti.getThreadState();
        lock = ti.getLockInfo() != null ? new VisorThreadLockInfo(ti.getLockInfo()) : null;
        lockName =ti.getLockName();
        lockOwnerId = ti.getLockOwnerId();
        lockOwnerName = ti.getLockOwnerName();
        inNative = ti.isInNative();
        suspended = ti.isSuspended();
        waitedCnt = ti.getWaitedCount();
        waitedTime = ti.getWaitedTime();
        blockedCnt = ti.getBlockedCount();
        blockedTime = ti.getBlockedTime();
        stackTrace = ti.getStackTrace();

        locks = ti.getLockedSynchronizers() != null ?
            new VisorThreadLockInfo[ti.getLockedSynchronizers().length] : null;

        if (ti.getLockedSynchronizers() != null)
            for (int i = 0; i < ti.getLockedSynchronizers().length; i++)
                locks[i] = new VisorThreadLockInfo(ti.getLockedSynchronizers()[i]);

        lockedMonitors = ti.getLockedMonitors() != null ?
            new VisorThreadMonitorInfo[ti.getLockedMonitors().length] : null;

        if (ti.getLockedMonitors() != null)
            for (int i = 0; i < ti.getLockedMonitors().length; i++)
                lockedMonitors[i] = new VisorThreadMonitorInfo(ti.getLockedMonitors()[i]);
    }

    /**
     * @return Thread name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Thread ID.
     */
    public Long getId() {
        return id;
    }

    /**
     * @return Thread state.
     */
    public Thread.State getState() {
        return state;
    }

    /**
     * @return Lock information.
     */
    public VisorThreadLockInfo getLock() {
        return lock;
    }

    /**
     * @return Lock name.
     */
    public String getLockName() {
        return lockName;
    }

    /**
     * @return Lock owner thread ID.
     */
    public Long getLockOwnerId() {
        return lockOwnerId;
    }

    /**
     * @return Lock owner name.
     */
    public String getLockOwnerName() {
        return lockOwnerName;
    }

    /**
     * @return Thread executing native code.
     */
    public Boolean isInNative() {
        return inNative;
    }

    /**
     * @return Thread is suspended.
     */
    public Boolean isSuspended() {
        return suspended;
    }

    /**
     * @return Waited count.
     */
    public Long getWaitedCount() {
        return waitedCnt;
    }

    /**
     * @return Waited time.
     */
    public Long getWaitedTime() {
        return waitedTime;
    }

    /**
     * @return Blocked count.
     */
    public Long getBlockedCount() {
        return blockedCnt;
    }

    /**
     * @return Blocked time.
     */
    public Long getBlockedTime() {
        return blockedTime;
    }

    /**
     * @return Stack trace.
     */
    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }

    /**
     * @return Locks info.
     */
    public VisorThreadLockInfo[] getLocks() {
        return locks;
    }

    /**
     * @return Locked monitors.
     */
    public VisorThreadMonitorInfo[] getLockedMonitors() {
        return lockedMonitors;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        StringBuilder sb = new StringBuilder("\"" + name + "\"" + " Id=" + id + " " + state);

        if (lockName != null)
            sb.append(" on ").append(lockName);

        if (lockOwnerName != null)
            sb.append(" owned by \"").append(lockOwnerName).append("\" Id=").append(lockOwnerId);

        if (suspended)
            sb.append(" (suspended)");

        if (inNative)
            sb.append(" (in native)");

        sb.append('\n');

        int maxFrames = Math.min(stackTrace.length, MAX_FRAMES);

        for (int i = 0; i < maxFrames; i++) {
            StackTraceElement ste = stackTrace[i];

            sb.append("\tat ").append(ste.toString()).append('\n');

            if (i == 0 && lock != null) {
                switch (state) {
                    case BLOCKED:
                        sb.append("\t-  blocked on ").append(lock).append('\n');
                        break;

                    case WAITING:
                        sb.append("\t-  waiting on ").append(lock).append('\n');
                        break;

                    case TIMED_WAITING:
                        sb.append("\t-  waiting on ").append(lock).append('\n');
                        break;

                    default:
                }
            }

            for (VisorThreadMonitorInfo mi : lockedMonitors) {
                if (mi.getStackDepth() == i)
                    sb.append("\t-  locked ").append(mi).append('\n');
            }
        }

        if (maxFrames < stackTrace.length)
            sb.append("\t...").append('\n');

        if (locks.length > 0) {
            sb.append("\n\tNumber of locked synchronizers = ").append(locks.length).append('\n');

            for (VisorThreadLockInfo li : locks)
                sb.append("\t- ").append(li).append('\n');
        }

        sb.append('\n');

        return sb.toString();
    }
}
