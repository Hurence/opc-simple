/*
 *  Copyright (C) 2018 Hurence (support@hurence.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hurence.opc.da;

import com.hurence.opc.ConnectionState;
import com.hurence.opc.OpcData;
import com.hurence.opc.OpcOperations;
import com.hurence.opc.exception.OpcException;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.*;
import org.openscada.opc.dcom.common.KeyedResult;
import org.openscada.opc.dcom.common.KeyedResultSet;
import org.openscada.opc.dcom.common.Result;
import org.openscada.opc.dcom.common.ResultSet;
import org.openscada.opc.dcom.common.impl.EnumString;
import org.openscada.opc.dcom.da.*;
import org.openscada.opc.dcom.da.impl.OPCGroupStateMgt;
import org.openscada.opc.dcom.da.impl.OPCItemMgt;
import org.openscada.opc.dcom.da.impl.OPCServer;
import org.openscada.opc.dcom.da.impl.OPCSyncIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * OPC-DA implementation for {@link OpcOperations}
 *
 * @author amarziali
 */
public class OpcDaOperations implements OpcOperations<OpcDaConnectionProfile, OpcData> {

    private static final Logger logger = LoggerFactory.getLogger(OpcDaOperations.class);

    private JISession session;
    private JIComServer comServer;
    private OPCServer opcServer;
    private OPCGroupStateMgt group;
    private ScheduledExecutorService scheduler = null;
    private volatile ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private Map<String, Map.Entry<Integer, Integer>> handlesMap = new HashMap<>();
    private AtomicInteger clientHandleCounter = new AtomicInteger();
    private long refreshPeriodMillis;
    private OPCSyncIO syncIO;
    private OPCItemMgt opcItemMgt;
    private OPCDATASOURCE datasource;

    /**
     * Atomically check a state and set next state.
     *
     * @param next of empty won't set anything.
     * @return
     */
    private synchronized ConnectionState getStateAndSet(Optional<ConnectionState> next) {
        ConnectionState ret = connectionState;
        if (next.isPresent()) {
            connectionState = next.get();
        }
        return ret;
    }


    /**
     * Check if the underlying connection to the com server is still alive.
     * Reconnects as well in case the autoreconnect has been set to true
     */
    private synchronized void checkAlive() {
        ConnectionState connectionState = getConnectionState();
        if (opcServer != null && (connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.CONNECTED)) {
            boolean inError = false;
            try {
                OPCSERVERSTATUS status = opcServer.getStatus();

                if (status == null || status.getServerState() == null || !status.getServerState().equals(OPCSERVERSTATE.OPC_STATUS_RUNNING)) {
                    logger.warn("Server is no more running but rather is in state {}", status != null ? status.getServerState() : null);
                    inError = true;
                }
            } catch (JIException e) {
                logger.error("Unable to read server state. Marking as disconnected", e);
                inError = true;
            }

            if (inError) {
                disconnect();
            } else {
                getStateAndSet(Optional.of(ConnectionState.CONNECTED));
            }

        }
    }


    @Override
    public void connect(OpcDaConnectionProfile connectionProfile) {
        if (connectionProfile == null) {
            throw new OpcException("Please provide any valid non null connection profile");
        }

        ConnectionState cs = getConnectionState();
        if (cs != ConnectionState.DISCONNECTED) {
            throw new OpcException("There is already an active connection. Please disconnect first");
        }

        this.datasource = connectionProfile.isDirectRead() ? OPCDATASOURCE.OPC_DS_DEVICE : OPCDATASOURCE.OPC_DS_CACHE;
        this.refreshPeriodMillis = connectionProfile.getRefreshPeriodMillis();
        try {
            getStateAndSet(Optional.of(ConnectionState.CONNECTING));
            if (connectionProfile.getComClsId() != null) {
                this.session = JISession.createSession(connectionProfile.getDomain(),
                        connectionProfile.getUser(), connectionProfile.getPassword());
                if (connectionProfile.getSocketTimeout() != null) {
                    this.session.setGlobalSocketTimeout((int) connectionProfile.getSocketTimeout().toMillis());
                }
                this.comServer = new JIComServer(JIClsid.valueOf(connectionProfile.getComClsId()),
                        connectionProfile.getHost(), this.session);
            } else if (connectionProfile.getComProgId() != null) {
                this.session = JISession.createSession(connectionProfile.getDomain(),
                        connectionProfile.getUser(), connectionProfile.getPassword());
                if (connectionProfile.getSocketTimeout() != null) {
                    this.session.setGlobalSocketTimeout((int) connectionProfile.getSocketTimeout().toMillis());
                }
                this.comServer = new JIComServer(JIProgId.valueOf(connectionProfile.getComProgId()),
                        connectionProfile.getHost(), this.session);
            } else {
                throw new IllegalArgumentException("Neither clsid nor progid is valid!");
            }


            opcServer = new OPCServer(comServer.createInstance());

            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleWithFixedDelay(() -> {
                checkAlive();

            }, 0, 1, TimeUnit.SECONDS);


            group = opcServer.addGroup(null, true, (int) connectionProfile.getRefreshPeriodMillis(),
                    0, null, null, 0);
            opcItemMgt = group.getItemManagement();
            syncIO = group.getSyncIO();
        } catch (Exception e) {
            try {
                disconnect();
            } finally {
                throw new OpcException("Unexpected exception occurred while connecting", e);
            }

        }

    }

    @Override
    public void disconnect() {
        try {
            getStateAndSet(Optional.of(ConnectionState.DISCONNECTING));
            cleanup();
        } catch (Exception e) {
            throw new OpcException("Unable to properly disconnect", e);
        } finally {
            getStateAndSet(Optional.of(ConnectionState.DISCONNECTED));
        }
    }

    private void cleanup() {
        logger.info("Destroying DCOM session");
        if (scheduler != null) {
            scheduler.shutdown();
        }
        try {
            JISession.destroySession(session);
            logger.info("Session properly cleaned up");

        } catch (JIException e) {
            throw new OpcException("Unable to properly destroy dcom session", e);
        } finally {
            comServer = null;
            session = null;
            opcServer = null;
            if (handlesMap != null) {
                handlesMap.clear();
            }
            handlesMap = null;
            group = null;
            opcItemMgt = null;
            scheduler = null;
            syncIO = null;
        }
    }

    @Override
    public ConnectionState getConnectionState() {
        return getStateAndSet(Optional.empty());
    }

    @Override
    public Collection<String> browseTags() {
        if (getConnectionState() != ConnectionState.CONNECTED) {
            throw new OpcException("Unable to browse tags. Not connected!");
        }
        try {
            EnumString res = opcServer.getBrowser().browse(OPCBROWSETYPE.OPC_FLAT, "", 0, JIVariant.VT_EMPTY);
            if (res != null) {
                return res.asCollection();
            }
        } catch (Exception e) {
            throw new OpcException("Unable to browse tags", e);
        }

        return Collections.emptyList();
    }

    @Override
    public Collection<OpcData> read(String... tags) {
        if (getConnectionState() != ConnectionState.CONNECTED) {
            throw new OpcException("Unable to read tags. Not connected!");
        }
        Map<String, Map.Entry<Integer, Integer>> tagsHandles =
                Arrays.stream(tags).collect(Collectors.toMap(Function.identity(), this::resolveItemHandles));
        Map<Integer, String> mapsToClientHandles = tagsHandles.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getValue().getValue(), e -> e.getKey()));
        KeyedResultSet<Integer, OPCITEMSTATE> result;
        try {
            result = syncIO.read(datasource, tagsHandles.values().stream().map(Map.Entry::getKey).toArray(a -> new Integer[a]));
        } catch (JIException e) {
            throw new OpcException("Unable to read tags", e);
        }


        return result.stream()
                .map(KeyedResult::getValue)
                .map(value -> new OpcData(mapsToClientHandles.get(value.getClientHandle()),
                        value.getTimestamp().asBigDecimalCalendar().toInstant(),
                        value.getQuality(), value.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean write(OpcData... data) {
        try {
            ResultSet<WriteRequest> result = syncIO.write(Arrays.stream(data)
                    .map(d -> new WriteRequest(resolveItemHandles(d.getTag()).getKey(), d.getValue()))
                    .toArray(a -> new WriteRequest[a]));
            if (result != null) {
                boolean failed = result.stream().anyMatch(Result::isFailed);
                if (failed) {
                    result.stream().filter(Result::isFailed).forEach(f -> logger.warn("Error {} writing tag handle {}", f.getErrorCode(), f.getValue().getServerHandle()));
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            throw new OpcException("Unable to write data", e);
        }

    }

    @Override
    public Stream<OpcData> stream(String... tags) {
        final ArrayList<OpcData> readItems = new ArrayList<>();
        final long lastTimeStamp[] = {0L};
        return Stream.generate(() -> {
            if (readItems.isEmpty()) {
                try {

                    long toSleep = refreshPeriodMillis - (System.currentTimeMillis() - lastTimeStamp[0]);
                    if (toSleep > 0) {
                        Thread.sleep(toSleep);
                    }
                    readItems.addAll(read(tags));
                    lastTimeStamp[0] = System.currentTimeMillis();

                } catch (InterruptedException ie) {
                    throw new OpcException("Stream interrupted", ie);
                } catch (Exception e) {
                    throw new OpcException("EOF reading tags. Disconnected", e);
                }
            }
            return readItems.size() > 0 ? readItems.remove(0) : null;
        });
    }

    @Override
    public boolean awaitConnected() {
        while (getConnectionState() != ConnectionState.CONNECTED) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean awaitDisconnected() {
        while (getConnectionState() != ConnectionState.DISCONNECTED){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Resolve tag names into server/client couple of Integer handles looking in a local cache to avoid resolving several time the same object.
     *
     * @param tag the tag to resolve.
     * @return a couple of Integers. First is the server handle. Second is the client handle.
     */
    private synchronized Map.Entry<Integer, Integer> resolveItemHandles(String tag) {
        Map.Entry<Integer, Integer> handles = handlesMap.get(tag);
        if (handles == null) {
            OPCITEMDEF opcitemdef = new OPCITEMDEF();
            opcitemdef.setActive(true);
            opcitemdef.setClientHandle(clientHandleCounter.incrementAndGet());
            opcitemdef.setItemID(tag);
            try {
                Integer serverHandle = opcItemMgt.add(opcitemdef).get(0).getValue().getServerHandle();
                handles = new AbstractMap.SimpleEntry<>(serverHandle, opcitemdef.getClientHandle());
            } catch (Exception e) {
                throw new OpcException("Unable to add item " + tag, e);
            }

            handlesMap.put(tag, handles);
        }
        return handles;
    }


}
