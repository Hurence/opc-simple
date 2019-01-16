/*
 *  Copyright (C) 2019 Hurence (support@hurence.com)
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
import com.hurence.opc.OperationStatus;
import com.hurence.opc.exception.OpcException;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JIVariant;
import org.openscada.opc.dcom.common.KeyedResult;
import org.openscada.opc.dcom.common.KeyedResultSet;
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
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;


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
    private OPCSyncIO syncIO;
    private OPCItemMgt opcItemMgt;
    private OPCDATASOURCE datasource;
    private final WeakReference<OpcDaTemplate> creatingOperations;
    private final Map<String, Short> dataTypeMap;
    private final Map<String, AtomicLong> refcountMap = Collections.synchronizedMap(new HashMap<>());
    private final Flowable<OpcData> masterFlowable;

    private OpcDaSession(OpcDaTemplate creatingOperations, OPCGroupStateMgt group, OPCDATASOURCE datasource,
                         Map<String, Short> dataTypeMap)
            throws JIException {
        this.group = group;
        this.opcItemMgt = group.getItemManagement();
        this.syncIO = group.getSyncIO();
        this.datasource = datasource;
        this.creatingOperations = new WeakReference<>(creatingOperations);
        this.dataTypeMap = dataTypeMap;
        try {
            long refreshRate = group.getState().getUpdateRate();
            logger.info("Using revised session refresh rate: {} milliseconds", refreshRate);
            //start emitting hot flowable.
            masterFlowable = Flowable.interval(refreshRate, TimeUnit.MILLISECONDS)
                    .takeWhile(ignored -> this.group != null)
                    .filter(ignored -> !refcountMap.isEmpty())
                    .flatMap(ignored -> read(refcountMap.keySet().toArray(new String[refcountMap.size()]))
                            .flattenAsFlowable(opcData -> opcData)
                    ).share();

        } catch (JIException e) {
            throw new OpcException("Unable to get revised refresh interval", e);
        }
    }

    static OpcDaSession create(OPCServer server, OpcDaSessionProfile sessionProfile, OpcDaTemplate creatingOperations) {
        try {
            return new OpcDaSession(creatingOperations,
                    server.addGroup(null, true,
                            (int) sessionProfile.getRefreshInterval().toMillis(), clientHandleCounter.incrementAndGet(),
                            null, null, 0),
                    sessionProfile.isDirectRead() ? OPCDATASOURCE.OPC_DS_DEVICE : OPCDATASOURCE.OPC_DS_CACHE,
                    sessionProfile.getDataTypeOverrideMap());
        } catch (Exception e) {
            throw new OpcException("Unable to create an OPC-DA session", e);
        }
    }

    /**
     * @param opcServer
     */
    public void cleanup(OPCServer opcServer) {
        logger.info("Cleaning session");
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
    public Single<List<OpcData>> read(String... tags) {
        return Single.fromCallable(() -> {
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
                                        OpcDaQualityExtractor.quality(value.getQuality()),
                                        JIVariantMarshaller.toJavaType(value.getValue()),
                                        OpcDaQualityExtractor.operationStatus(value.getQuality()));
                            } catch (JIException e) {
                                throw new OpcException("Unable to read tag " + value, e);
                            }
                        }).collect(Collectors.toList());

            } catch (JIException e) {
                throw new OpcException("Unable to read tags", e);
            }
        });
    }


    @Override
    public Single<List<OperationStatus>> write(OpcData... data) {
        return Single.fromCallable(() -> {
            if (group == null) {
                throw new OpcException("Unable to write tags. Session has been detached!");
            }
            try {
                ResultSet<WriteRequest> result = syncIO.write(Arrays.stream(data)
                        .map(d -> new WriteRequest(resolveItemHandles(d.getTag()).getKey(), JIVariant.makeVariant(d.getValue())))
                        .toArray(a -> new WriteRequest[a]));
                return result.stream()
                        .map(OpcDaQualityExtractor::operationStatus)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                throw new OpcException("Unable to write data", e);
            }
        });
    }


    private void incrementRefCount(String tagId) {
        if (refcountMap != null) {
            refcountMap.compute(tagId, (s, atomicLong) -> {
                if (atomicLong == null) {
                    atomicLong = new AtomicLong();
                }
                atomicLong.incrementAndGet();
                return atomicLong;
            });
        }
    }

    private void decrementRefCount(String tagId) {
        if (refcountMap != null) {
            refcountMap.compute(tagId, (s, atomicLong) -> {
                if (atomicLong != null && atomicLong.decrementAndGet() <= 0) {
                    atomicLong = null;
                }
                return atomicLong;
            });
        }
    }

    @Override
    public Flowable<OpcData> stream(String tagId, Duration samplingInterval) {
        if (masterFlowable == null) {
            return Flowable.error(new OpcException("Unable to read tags. Session has been detached!"));
        }
        //validate tag
        return Single.fromCallable(() -> resolveItemHandles(tagId))
                .ignoreElement()
                .andThen(masterFlowable)
                .filter(opcData -> opcData.getTag().equals(tagId))
                .distinctUntilChanged()
                .throttleLatest(samplingInterval.toNanos(), TimeUnit.NANOSECONDS)
                .doOnSubscribe(ignored -> incrementRefCount(tagId))
                .doOnTerminate(() -> decrementRefCount(tagId));
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
            opcitemdef.setRequestedDataType(dataTypeMap.getOrDefault(tag, (short) JIVariant.VT_EMPTY));
            try {
                Integer serverHandle = opcItemMgt.add(opcitemdef).get(0).getValue().getServerHandle();
                if (serverHandle == null || serverHandle == 0) {
                    throw new OpcException("Received invalid handle from OPC server.");
                }
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
                creatingOperations.get().releaseSession(this).blockingAwait();
            } finally {
                creatingOperations.clear();
            }
        }
    }
}
