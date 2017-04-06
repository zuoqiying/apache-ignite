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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.internal.util.typedef.internal.U;

/**
 * Lock state structure is as follows:
 * <pre>
 *     +----------------+------+
 *     | WAIT CNT | LOCK FLAG |
 *     +----------------+-----+
 *     | 2 bytes |  2 bytes  |
 *     +----------------+----+
 * </pre>
 */
@SuppressWarnings({"NakedNotify", "SynchronizationOnLocalVariableOrMethodParameter", "CallToThreadYield", "WaitWhileNotSynced"})
public class NonFairLock {
    /**
     * TODO benchmark optimal spin count.
     */
    public static final int SPIN_CNT = IgniteSystemProperties.getInteger("IGNITE_NON_FAIR_LOCK_SPIN_COUNT", 32);

    /** Lock size. */
    public static final int LOCK_SIZE = 4;

    /** Maximum number of waiting threads, read or write. */
    public static final int MAX_WAITERS = 0xFFFF;

    /** */
    private final ReentrantLock[] locks;

    /** */
    private final Condition[] conditions;

    /** */
    private final AtomicInteger[] balancers;

    /** */
    private int monitorsMask;

    /**
     * @param concLvl Concurrency level, must be a power of two.
     */
    public NonFairLock(int concLvl) {
        if ((concLvl & concLvl - 1) != 0)
            throw new IllegalArgumentException("Concurrency level must be a power of 2: " + concLvl);

        monitorsMask = concLvl - 1;

        locks = new ReentrantLock[concLvl];
        conditions = new Condition[concLvl];
        balancers = new AtomicInteger[concLvl];

        for (int i = 0; i < locks.length; i++) {
            ReentrantLock lock = new ReentrantLock();

            locks[i] = lock;
            conditions[i] = lock.newCondition();
            balancers[i] = new AtomicInteger(0);
        }
    }

    /**
     * @param lock Lock address.
     */
    public boolean tryLock(Object obj, long lock) {
        int state = GridUnsafe.getIntVolatile(null, lock);

        return canLock(state) &&
            GridUnsafe.compareAndSwapInt(obj, lock, state, updateState(state, -1, 0));
    }

    /**
     * @param lock Lock address.
     */
    public boolean lock(Object obj, long lock) {
        for (int i = 0; i < SPIN_CNT; i++) {
            int state = GridUnsafe.getIntVolatile(obj, lock);

            //assert state != 0;

            if (canLock(state)) {
                if (GridUnsafe.compareAndSwapInt(obj, lock, state, updateState(state, -1, 0)))
                    return true;
                else
                    // Retry CAS, do not count as spin cycle.
                    i--;
            }
        }

        int idx = lockIndex(obj);

        ReentrantLock lockObj = locks[idx];

        lockObj.lock();

        try {
            updateWaitCount(obj, lock, lockObj, 1);

            return waitAcquireLock(obj, lock, idx);
        }
        finally {
            lockObj.unlock();
        }
    }

    /**
     * @param lock Lock to check.
     * @return {@code True} if write lock is held by any thread for the given offheap RW lock.
     */
    public boolean isLocked(Object obj, long lock) {
        return lockCount(GridUnsafe.getIntVolatile(obj, lock)) == -1;
    }

    /**
     * @param lock Lock address.
     */
    public void unlock(Object obj, long lock) {
        int updated;

        while (true) {
            int state = GridUnsafe.getIntVolatile(obj, lock);

            if (lockCount(state) != -1)
                throw new IllegalMonitorStateException("Attempted to release write lock while not holding it " +
                    "[lock=" + U.hexLong(lock) + ", state=" + U.hexInt(state));

            updated = release(state);

            //assert updated != 0;

            if (GridUnsafe.compareAndSwapInt(obj, lock, state, updated))
                break;
        }

        int waitCnt = waitCount(updated);

        if (waitCnt > 0) {
            int idx = lockIndex(obj);

            ReentrantLock lockObj = locks[idx];

            lockObj.lock();

            try {
                signalNextWaiter(idx);
            }
            finally {
                lockObj.unlock();
            }
        }
    }

    /**
     * @param idx Lock index.
     */
    private void signalNextWaiter(int idx) {
        Condition writeCond = conditions[idx];

        writeCond.signalAll();
    }

    /**
     * Acquires lock in waiting loop.
     *
     * @param lock Lock address.
     * @param lockIdx Lock index.
     * @return {@code True} if lock was acquired, {@code false} if tag validation failed.
     */
    private boolean waitAcquireLock(Object obj, long lock, int lockIdx) {
        ReentrantLock lockObj = locks[lockIdx];
        Condition waitCond = conditions[lockIdx];

        assert lockObj.isHeldByCurrentThread();

        boolean interrupted = false;

        try {
            while (true) {
                try {
                    int state = GridUnsafe.getIntVolatile(obj, lock);

                    if (canLock(state)) {
                        int updated = updateState(state, -1, -1);

                        if (GridUnsafe.compareAndSwapInt(obj, lock, state, updated))
                            return true;
                    }
                    else
                        waitCond.await();
                }
                catch (InterruptedException ignore) {
                    interrupted = true;
                }
            }
        }
        finally {
            if (interrupted)
                Thread.currentThread().interrupt();
        }
    }

    /**
     * @param obj object for lock.
     * @return Lock monitor object.
     */
    private int lockIndex(Object obj) {
        return U.safeAbs(U.hash(obj)) & monitorsMask;
    }

    /**
     * @param state Lock state.
     * @return {@code True} if lock not acquired.
     */
    private boolean canLock(int state) {
        return lockCount(state) == 0;
    }

    /**
     * @param state State.
     * @return Lock count.
     */
    private int lockCount(int state) {
        return (short)(state & 0xFFFF);
    }

    /**
     * @param state State.
     * @return wait count.
     */
    private int waitCount(int state) {
        return (int)((state >>> 16) & 0xFFFF);
    }

    /**
     * @param state State to update.
     * @param lockDelta Lock counter delta.
     * @param writersWaitDelta wait delta.
     * @return Modified state.
     */
    private int updateState(int state, int lockDelta, int writersWaitDelta) {
        int lock = lockCount(state);
        int wait = waitCount(state);

        lock += lockDelta;
        wait += writersWaitDelta;

        if (wait > MAX_WAITERS)
            throw new IllegalStateException("Failed to add write waiter (too many waiting threads): " + MAX_WAITERS);

        assert wait >= 0 : wait;
        assert lock >= -1;

        return buildState(wait, lock);
    }

    /**
     * @param state State to update.
     * @return Modified state.
     */
    private int release(int state) {
        int lock = lockCount(state);
        int wait = waitCount(state);

        lock += 1;

        assert wait >= 0 : wait;
        assert lock >= -1;

        return buildState(wait, lock);
    }

    /**
     * Creates state from counters.
     *
     * @param wait Wait count.
     * @param lock Lock count.
     * @return State.
     */
    private int buildState(int wait, int lock) {

        return (wait << 16) | (lock & 0xFFFF);
    }

    /**
     * Updates wait count.
     *
     * @param lock Lock to update.
     * @param delta Delta to update.
     */
    private void updateWaitCount(Object obj, long lock, ReentrantLock lockObj, int delta) {
        assert lockObj.isHeldByCurrentThread();

        while (true) {
            int state = GridUnsafe.getIntVolatile(obj, lock);

            int updated = updateState(state, 0, delta);

            if (GridUnsafe.compareAndSwapInt(obj, lock, state, updated))
                return;
        }
    }
}
