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

package com.hurence.opc.ua;

import com.hurence.opc.OpcData;
import com.hurence.opc.OpcSession;
import com.hurence.opc.OperationStatus;
import com.hurence.opc.exception.OpcException;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.CompletableSubject;
import io.reactivex.subjects.UnicastSubject;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    private final CompletableSubject terminationSignal = CompletableSubject.create();


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
            return new OpcUaSession(creatingOperations, client, sessionProfile.getPublicationInterval());

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
                    client.get().getSubscriptionManager().deleteSubscription(subscription.getSubscriptionId())
                            .get(client.get().getConfig().getRequestTimeout().longValue(), TimeUnit.MILLISECONDS);
                    logger.info("Released subscription {}", subscription.getSubscriptionId());
                }

            }
        } catch (Exception e) {
            logger.warn("Unable to properly clear subscription " + subscription.getSubscriptionId(), e);
        } finally {
            subscription = null;
            client.clear();
            terminationSignal.onComplete();
        }
    }

    private Single<OpcUaClient> fetchValidClient() {
        if (client.get() == null) {
            return Single.error(new OpcException("Unable to read items. OPC-UA Client has been garbage collected. Please use a fresher instance"));
        }
        return Single.just(client.get());
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
    public Single<List<OpcData>> read(String... tags) {
        return fetchValidClient()
                .flatMap(c -> Single.fromFuture(
                        c.readValues(0.0, TimestampsToReturn.Both, Arrays.stream(tags).map(NodeId::parseSafe)
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
                                })));
    }


    @Override
    public Single<List<OperationStatus>> write(OpcData... data) {
        return fetchValidClient()
                .flatMap(c -> Single.fromFuture(c.writeValues(
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
                        .collect(Collectors.toList()))));


    }

    @Override
    public Flowable<OpcData> stream(String tagId, Duration duration) {
        logger.info("Creating monitored item for tag {}", tagId);
        return Single.fromFuture(subscription().createMonitoredItems(TimestampsToReturn.Both,
                Collections.singletonList(new MonitoredItemCreateRequest(
                        new ReadValueId(NodeId.parse(tagId), AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE),
                        MonitoringMode.Reporting,
                        new MonitoringParameters(UInteger.valueOf(clientHandleCounter.incrementAndGet()),
                                (double) duration.toMillis(),
                                null,
                                UInteger.valueOf(Math.round(Math.ceil((double) publicationInterval.toNanos() /
                                        (double) duration.toNanos())))
                                , true)))
        ).toCompletableFuture())
                .map(uaMonitoredItems -> uaMonitoredItems.stream()
                        .findFirst()
                        .orElseThrow(() -> new OpcException("Received empty response for subscription to tag " + tagId)))
                .toFlowable()
                .flatMap(uaMonitoredItem -> {
                    logger.info("Subscription for item {} with revised polling time {}",
                            uaMonitoredItem.getReadValueId().getNodeId().toParseableString(),
                            uaMonitoredItem.getRevisedSamplingInterval());

                    UnicastSubject<OpcData> ret = UnicastSubject.create();
                    uaMonitoredItem.setValueConsumer((uaMonitoredItem1, dataValue) ->
                            ret.onNext(opcData(uaMonitoredItem1.getReadValueId().getNodeId().toParseableString(), dataValue)));
                    final Disposable disposable = terminationSignal.subscribe(() ->
                            ret.onError(new OpcException("EOF reading from the stream. Client closed unexpectedly")));
                    return ret.toFlowable(BackpressureStrategy.MISSING)
                            .doOnComplete(() -> {
                                logger.info("Clearing subscription for item {}", uaMonitoredItem);
                                removeSubscriptions(Collections.singletonList(uaMonitoredItem));
                            }).doFinally(() -> disposable.dispose());

                }).takeWhile(ignored -> !terminationSignal.hasComplete());
    }


    private void removeSubscriptions(List<UaMonitoredItem> results) {
        if (subscription != null) {
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
    }


    @Override
    public void close() throws Exception {
        if (creatingOperations.get() != null) {
            try {
                creatingOperations.get().releaseSession(this).blockingAwait();
            } finally {
                creatingOperations.clear();
            }
        }
    }
}
