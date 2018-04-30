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

import com.hurence.opc.OpcData;
import com.hurence.opc.OpcOperations;
import com.hurence.opc.OpcSession;
import com.hurence.opc.exception.OpcException;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JIVariant;
import org.openscada.opc.dcom.common.KeyedResult;
import org.openscada.opc.dcom.common.KeyedResultSet;
import org.openscada.opc.dcom.common.Result;
import org.openscada.opc.dcom.common.ResultSet;
import org.openscada.opc.dcom.da.OPCDATASOURCE;
import org.openscada.opc.dcom.da.OPCITEMDEF;
import org.openscada.opc.dcom.da.OPCITEMSTATE;
import org.openscada.opc.dcom.da.WriteRequest;
import org.openscada.opc.dcom.da.impl.OPCGroupStateMgt;
import org.openscada.opc.dcom.da.impl.OPCItemMgt;
import org.openscada.opc.dcom.da.impl.OPCServer;
import org.openscada.opc.dcom.da.impl.OPCSyncIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * OPC-DA implementation for {@link OpcOperations}
 *
 * @author amarziali
 */
public class OpcDaSession implements OpcSession {

    private static final Logger logger = LoggerFactory.getLogger(OpcDaSession.class);

    private OPCGroupStateMgt group;
    private Map<String, Map.Entry<Integer, Integer>> handlesMap = new HashMap<>();
    private static final AtomicInteger clientHandleCounter = new AtomicInteger();
    private final long refreshPeriodMillis;
    private OPCSyncIO syncIO;
    private OPCItemMgt opcItemMgt;
    private OPCDATASOURCE datasource;
    private final WeakReference<OpcDaOperations> creatingOperations;

    private OpcDaSession(OpcDaOperations creatingOperations, OPCGroupStateMgt group, OPCDATASOURCE datasource,
                         long refreshPeriodMillis) throws JIException {
        this.group = group;
        this.opcItemMgt = group.getItemManagement();
        this.syncIO = group.getSyncIO();
        this.datasource = datasource;
        this.refreshPeriodMillis = refreshPeriodMillis;
        this.creatingOperations = new WeakReference<>(creatingOperations);
    }

    static OpcDaSession create(OPCServer server, long refreshPeriodMillis, boolean directRead, OpcDaOperations creatingOperations) {
        try {
            return new OpcDaSession(creatingOperations,
                    server.addGroup(null, true,
                            (int) refreshPeriodMillis, clientHandleCounter.incrementAndGet(), null, null, 0),
                    directRead ? OPCDATASOURCE.OPC_DS_DEVICE : OPCDATASOURCE.OPC_DS_CACHE,
                    refreshPeriodMillis);
        } catch (Exception e) {
            throw new OpcException("Unable to create an OPC-DA session", e);
        }
    }

    /**
     * @param opcServer
     */
    public void cleanup(OPCServer opcServer) {
        logger.info("Destroying DCOM session");

        try {
            opcServer.removeGroup(group, true);
        } catch (JIException e) {
            logger.warn("Unable to properly remove group from opc server", e);
            if (handlesMap != null) {
                handlesMap.clear();
            }
            handlesMap = null;
            group = null;
            opcItemMgt = null;
            syncIO = null;
        }
    }


    @Override
    public Collection<OpcData> read(String... tags) {
        if (group == null) {
            throw new OpcException("Unable to read tags. Session has been detached!");
        }
        Map<String, Map.Entry<Integer, Integer>> tagsHandles =
                Arrays.stream(tags).collect(Collectors.toMap(Function.identity(), this::resolveItemHandles));
        Map<Integer, String> mapsToClientHandles = tagsHandles.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getValue().getValue(), e -> e.getKey()));
        KeyedResultSet<Integer, OPCITEMSTATE> result;
        try {
            result = syncIO.read(datasource, tagsHandles.values().stream().map(Map.Entry::getKey).toArray(a -> new Integer[a]));
            return result.stream()
                    .map(KeyedResult::getValue)
                    .filter(value -> mapsToClientHandles.containsKey(value.getClientHandle()))
                    .map(value -> {
                        try {
                            return new OpcData<>(mapsToClientHandles.get(value.getClientHandle()),
                                    value.getTimestamp().asBigDecimalCalendar().toInstant(),
                                    value.getQuality(), JIVariantMarshaller.toJavaType(value.getValue()),
                                    JIVariantMarshaller.extractError(value.getValue()));
                        } catch (JIException e) {
                            throw new OpcException("Unable to read tag " + value, e);
                        }
                    }).collect(Collectors.toList());

        } catch (JIException e) {
            throw new OpcException("Unable to read tags", e);
        }
    }


    @Override
    public boolean write(OpcData... data) {
        if (group == null) {
            throw new OpcException("Unable to write tags. Session has been detached!");
        }
        try {
            ResultSet<WriteRequest> result = syncIO.write(Arrays.stream(data)
                    .map(d -> new WriteRequest(resolveItemHandles(d.getTag()).getKey(), JIVariant.makeVariant(d.getValue())))
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

    @Override
    public void close() {
        if (creatingOperations != null && creatingOperations.get() != null) {
            try {
                creatingOperations.get().releaseSession(this);
            } finally {
                creatingOperations.clear();
            }
        }
    }
}
