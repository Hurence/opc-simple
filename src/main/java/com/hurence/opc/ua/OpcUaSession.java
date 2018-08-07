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
import com.hurence.opc.SubscriptionConfiguration;
import com.hurence.opc.exception.OpcException;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The OCP-UA Session.
 * This object should be used to read/write/stream data from/to an UA server.
 *
 * @author amarziali
 */
public class OpcUaSession implements OpcSession {

    private static final Logger logger = LoggerFactory.getLogger(OpcUaSession.class);


    private static final AtomicInteger clientHandleCounter = new AtomicInteger();
    private final Duration publicationInterval;
    private final WeakReference<OpcUaClient> client;
    private final WeakReference<OpcUaTemplate> creatingOperations;
    private UaSubscription subscription;


    private OpcUaSession(OpcUaTemplate creatingOperations,
                         OpcUaClient client,
                         Duration publicationInterval) {
        this.client = new WeakReference<>(client);
        this.creatingOperations = new WeakReference<>(creatingOperations);
        this.publicationInterval = publicationInterval;
    }


    static OpcUaSession create(OpcUaTemplate creatingOperations,
                               OpcUaClient client,
                               OpcUaSessionProfile sessionProfile) {
        try {
            return new OpcUaSession(creatingOperations, client, sessionProfile.getDefaultPublicationInterval());

        } catch (Exception e) {
            throw new OpcException("Unable to create an OPC-UA session", e);
        }
    }

    private synchronized UaSubscription subscription() {
        try {
            if (subscription == null && client.get() != null) {
                subscription = client.get().getSubscriptionManager().createSubscription(Math.round(publicationInterval.toNanos() / 1.0e6)).get();
            }
        } catch (Exception e) {
            throw new OpcException("Unable to create subscription", e);
        }
        return this.subscription;
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
    public List<OpcData> read(String... tags) {
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
    public List<OperationStatus> write(OpcData... data) {
        try {
            return fetchValidClient().writeValues(
                    Arrays.stream(data)
                            .map(OpcData::getTag)
                            .map(NodeId::parse)
                            .collect(Collectors.toList()),
                    Arrays.stream(data)
                            .map(OpcData::getValue)
                            .map(Variant::new)
                            .map(DataValue::valueOnly)
                            .collect(Collectors.toList())
            ).thenApply(statusCodes -> statusCodes.stream()
                    .map(OpcUaQualityExtractor::operationStatus)
                    .collect(Collectors.toList())
            ).get();
        } catch (Exception e) {
            throw new OpcException("Unable to successfully read tags", e);
        }

    }

    @Override
    public Stream<OpcData> stream(SubscriptionConfiguration subscriptionConfiguration, String... tags) {
        BlockingQueue<OpcData> transferQueue = new SynchronousQueue<>();
        final List<UaMonitoredItem> results = new ArrayList<>();
        final Map<String, Integer> handles = Arrays.stream(tags).collect(Collectors.toMap(Function.identity(), tag -> clientHandleCounter.incrementAndGet()));
        final Map<Integer, String> reverseHandles = handles.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        try {
            //attach the consumer
            final BiConsumer<UaMonitoredItem, DataValue> callback = (uaMonitoredItem1, dataValue) -> {
                String tag = reverseHandles.get(uaMonitoredItem1.getClientHandle().intValue());
                try {
                    transferQueue.put(opcData(tag, dataValue));
                } catch (Exception e) {
                    logger.warn("Unable to properly map value for item " + tag, e);
                }
            };
            results.addAll(subscription().createMonitoredItems(TimestampsToReturn.Both,
                    Arrays.stream(tags).map(tag ->
                            new MonitoredItemCreateRequest(
                                    new ReadValueId(NodeId.parse(tag), AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE),
                                    MonitoringMode.Reporting,
                                    new MonitoringParameters(UInteger.valueOf(handles.get(tag)),
                                            (double) subscriptionConfiguration.samplingIntervalForTag(tag).toNanos() / 1.0e6,
                                            null,
                                            UInteger.valueOf(Math.round(Math.ceil((double) publicationInterval.toNanos() /
                                                    (double) subscriptionConfiguration.samplingIntervalForTag(tag).toNanos())))
                                            , true)
                            )).collect(Collectors.toList()),
                    ((uaMonitoredItem, integer) -> {
                        logger.info("Subscription for item {} with revised polling time {}",
                                reverseHandles.get(uaMonitoredItem.getClientHandle().intValue()),
                                uaMonitoredItem.getRevisedSamplingInterval());
                        uaMonitoredItem.setValueConsumer(callback);
                    })).get());

            // check for any subscription failure.
            results.stream()
                    .filter(uaMonitoredItem -> !uaMonitoredItem.getStatusCode().isGood())
                    .findFirst()
                    .ifPresent(uaMonitoredItem -> {
                        throw new OpcException(String.format("Failure trying to monitoring item %s : %s",
                                reverseHandles.get(uaMonitoredItem.getClientHandle().intValue()),
                                Arrays.toString(StatusCodes.lookup(uaMonitoredItem.getStatusCode().getValue()).orElse(null))));
                    });


            return Stream.generate(() -> {
                try {
                    OpcData ret = null;
                    while ((ret = transferQueue.poll(publicationInterval.toNanos(), TimeUnit.NANOSECONDS)) == null) {
                        if (subscription == null) {
                            throw new OpcException("EOF reading tags. Disconnected");
                        }
                    }
                    return ret;
                } catch (InterruptedException ie) {
                    throw new OpcException("Stream interrupted", ie);
                }
            }).onClose(() -> removeSubscriptions(results));

        } catch (Exception e) {
            try {
                removeSubscriptions(results);
            } finally {
                throw new OpcException("Unable to stream requested tags", e);
            }
        }

    }

    private void removeSubscriptions(List<UaMonitoredItem> results) {
        try {
            List<StatusCode> removeResult = subscription.deleteMonitoredItems(results).get();
            for (int i = 0; i < removeResult.size(); i++) {
                if (!removeResult.get(i).isGood()) {
                    logger.warn("Unable to properly unsubscribe for item {}: {}",
                            results.get(i).getReadValueId().getNodeId().toParseableString(),
                            StatusCodes.lookup(removeResult.get(i).getValue()));
                }
            }
        } catch (Exception e) {
            logger.error("Unable to properly removed monitored items", e);
        }
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
