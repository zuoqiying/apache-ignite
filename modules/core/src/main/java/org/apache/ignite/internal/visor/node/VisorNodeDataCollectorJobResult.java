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

package org.apache.ignite.internal.visor.node;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.internal.visor.VisorDataTransferObject;
import org.apache.ignite.internal.visor.cache.VisorCache;
import org.apache.ignite.internal.visor.event.VisorGridEvent;
import org.apache.ignite.internal.visor.igfs.VisorIgfs;
import org.apache.ignite.internal.visor.igfs.VisorIgfsEndpoint;

/**
 * Data collector job result.
 */
public class VisorNodeDataCollectorJobResult extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Grid name. */
    private String gridName;

    /** Node topology version. */
    private long topVer;

    /** Task monitoring state collected from node. */
    private boolean taskMonitoringEnabled;

    /** Node events. */
    private Collection<VisorGridEvent> evts = new ArrayList<>();

    /** Exception while collecting node events. */
    private Throwable evtsEx;

    /** Node caches. */
    private Collection<VisorCache> caches = new ArrayList<>();

    /** Exception while collecting node caches. */
    private Throwable cachesEx;

    /** Node IGFSs. */
    private Collection<VisorIgfs> igfss = new ArrayList<>();

    /** All IGFS endpoints collected from nodes. */
    private Collection<VisorIgfsEndpoint> igfsEndpoints = new ArrayList<>();

    /** Exception while collecting node IGFSs. */
    private Throwable igfssEx;

    /** Errors count. */
    private long errCnt;

    /**
     * Default constructor.
     */
    public VisorNodeDataCollectorJobResult() {
        // No-op.
    }

    /**
     * @return Grid name.
     */
    public String gridName() {
        return gridName;
    }

    /**
     * @param gridName New grid name value.
     */
    public void gridName(String gridName) {
        this.gridName = gridName;
    }

    /**
     * @return Current topology version.
     */
    public long topologyVersion() {
        return topVer;
    }

    /**
     * @param topVer New topology version value.
     */
    public void topologyVersion(long topVer) {
        this.topVer = topVer;
    }

    /**
     * @return Current task monitoring state.
     */
    public boolean taskMonitoringEnabled() {
        return taskMonitoringEnabled;
    }

    /**
     * @param taskMonitoringEnabled New value of task monitoring state.
     */
    public void taskMonitoringEnabled(boolean taskMonitoringEnabled) {
        this.taskMonitoringEnabled = taskMonitoringEnabled;
    }

    /**
     * @return Collection of collected events.
     */
    public Collection<VisorGridEvent> events() {
        return evts;
    }

    /**
     * @return Exception caught during collecting events.
     */
    public Throwable eventsEx() {
        return evtsEx;
    }

    /**
     * @param evtsEx Exception caught during collecting events.
     */
    public void eventsEx(Throwable evtsEx) {
        this.evtsEx = evtsEx;
    }

    /**
     * @return Collected cache metrics.
     */
    public Collection<VisorCache> caches() {
        return caches;
    }

    /**
     * @return Exception caught during collecting caches metrics.
     */
    public Throwable cachesEx() {
        return cachesEx;
    }

    /**
     * @param cachesEx Exception caught during collecting caches metrics.
     */
    public void cachesEx(Throwable cachesEx) {
        this.cachesEx = cachesEx;
    }

    /**
     * @return Collected IGFSs metrics.
     */
    public Collection<VisorIgfs> igfss() {
        return igfss;
    }

    /**
     * @return Collected IGFSs endpoints.
     */
    public Collection<VisorIgfsEndpoint> igfsEndpoints() {
        return igfsEndpoints;
    }

    /**
     * @return Exception caught during collecting IGFSs metrics.
     */
    public Throwable igfssEx() {
        return igfssEx;
    }

    /**
     * @param igfssEx Exception caught during collecting IGFSs metrics.
     */
    public void igfssEx(Throwable igfssEx) {
        this.igfssEx = igfssEx;
    }

    /**
     * @return Errors count.
     */
    public long errorCount() {
        return errCnt;
    }

    /**
     * @param errCnt Errors count.
     */
    public void errorCount(long errCnt) {
        this.errCnt = errCnt;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        U.writeString(out, gridName);
        out.writeLong(topVer);
        out.writeBoolean(taskMonitoringEnabled);
        U.writeCollection(out, evts);
        out.writeObject(evtsEx);
        U.writeCollection(out, caches);
        out.writeObject(cachesEx);
        U.writeCollection(out, igfss);
        U.writeCollection(out, igfsEndpoints);
        out.writeObject(igfssEx);
        out.writeLong(errCnt);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(ObjectInput in) throws IOException, ClassNotFoundException {
        gridName = U.readString(in);
        topVer = in.readLong();
        taskMonitoringEnabled = in.readBoolean();
        evts = U.readCollection(in);
        evtsEx = (Throwable)in.readObject();
        caches = U.readCollection(in);
        cachesEx = (Throwable)in.readObject();
        igfss = U.readCollection(in);
        igfsEndpoints = U.readCollection(in);
        igfssEx = (Throwable)in.readObject();
        errCnt = in.readLong();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorNodeDataCollectorJobResult.class, this);
    }
}
