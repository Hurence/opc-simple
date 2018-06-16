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

package com.hurence.opc.ua;

import com.hurence.opc.OpcData;
import com.hurence.opc.OpcSession;
import com.hurence.opc.OperationStatus;
import com.hurence.opc.exception.OpcException;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpcUaSession implements OpcSession {

    private static final Logger logger = LoggerFactory.getLogger(OpcUaSession.class);


    private static final AtomicInteger clientHandleCounter = new AtomicInteger();
    private final long refreshPeriodMillis;
    private final WeakReference<OpcUaClient> client;
    private final WeakReference<OpcUaOperations> creatingOperations;
    private UaSubscription subscription;


    private OpcUaSession(OpcUaOperations creatingOperations, OpcUaClient client, long refreshPeriodMillis) throws Exception {
        this.refreshPeriodMillis = refreshPeriodMillis;
        this.client = new WeakReference<>(client);
        this.creatingOperations = new WeakReference<>(creatingOperations);
    }


    static OpcUaSession create(OpcUaClient client, long refreshPeriodMillis, OpcUaOperations creatingOperations) {
        try {
            return new OpcUaSession(creatingOperations, client, refreshPeriodMillis);
        } catch (Exception e) {
            throw new OpcException("Unable to create an OPC-UA session", e);
        }
    }

    private synchronized void subscribeTo(String... tags) {
        try {
            if (subscription == null && client.get() != null) {
                subscription = client.get().getSubscriptionManager().createSubscription(refreshPeriodMillis).get();
            }
        } catch (Exception e) {
            throw new OpcException("Unable to create subscription", e);
        }
    }


    public void cleanup() {
        logger.info("Destroying UA session");
        try {
            if (client.get() != null) {
                if (subscription != null) {
                    client.get().getSubscriptionManager().deleteSubscription(subscription.getSubscriptionId()).get();
                    logger.info("Released subscription {}", subscription.getSubscriptionId());
                }

            }
        } catch (Exception e) {
            logger.warn("Unable to properly clear subscription " + subscription.getSubscriptionId(), e);
        } finally {
            subscription = null;
            client.clear();
        }
    }

    private OpcUaClient fetchValidClient() {
        if (client.get() == null) {
            throw new OpcException("Unable to read items. OPC-UA Client has been garbage collected. Please use a fresher instance");
        }
        return client.get();
    }


    private OpcData<?> opcData(String tag, DataValue dataValue) {
        Instant instant = Instant.now();
        DateTime dt = null;
        double picos = 0.0;
        if (dataValue.getSourceTime() != null) {
            dt = dataValue.getSourceTime();
            if (dataValue.getSourcePicoseconds() != null) {
                picos = dataValue.getSourcePicoseconds().doubleValue();
            }
        } else if (dataValue.getServerTime() != null) {
            dt = dataValue.getServerTime();
            if (dataValue.getServerPicoseconds() != null) {
                picos = dataValue.getServerPicoseconds().doubleValue();
            }
        }
        if (dt != null) {
            instant = dt.getJavaDate().toInstant()
                    .plusNanos(Math.round(picos / 1.0e3));
        }
        return new OpcData<>(tag,
                instant,
                OpcUaQualityExtractor.quality(dataValue.getStatusCode()),
                UaVariantMarshaller.toJavaType(dataValue.getValue()),
                OpcUaQualityExtractor.operationStatus(dataValue.getStatusCode()));
    }


    @Override
    public Collection<OpcData> read(String... tags) {
        OpcUaClient c = fetchValidClient();

        try {
            return c.readValues(0.0, TimestampsToReturn.Both, Arrays.stream(tags).map(NodeId::parseSafe)
                    .map(Optional::get).collect(Collectors.toList()))
                    .thenApply(dataValues -> {
                        if (dataValues.size() != tags.length) {
                            throw new OpcException("Input tags does not match received tags. Aborting");
                        }
                        List<OpcData> ret = new ArrayList<>();
                        for (int i = 0; i < dataValues.size(); i++) {
                            try {
                                ret.add(opcData(tags[i], dataValues.get(i)));

                            } catch (Exception e) {
                                logger.warn("Unable to properly map tag " + tags[i] + ". Skipping!", e);
                            }
                        }
                        return ret;
                    }).get();
        } catch (Exception e) {
            throw new OpcException("Unable to successfully read tags", e);
        }
    }


    @Override
    public Collection<OperationStatus> write(OpcData... data) {
        //client.get().write
        return Collections.emptyList();

    }

    @Override
    public Stream<OpcData> stream(String... tags) {
        return null;
    }


    @Override
    public void close() throws Exception {
        if (creatingOperations.get() != null) {
            try {
                creatingOperations.get().releaseSession(this);
            } finally {
                creatingOperations.clear();
            }
        }
    }
}
