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
import com.hurence.opc.OpcSession;
import com.hurence.opc.OpcTagInfo;
import com.hurence.opc.OperationStatus;
import com.hurence.opc.auth.NtlmCredentials;
import com.hurence.opc.exception.OpcException;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.schedulers.Timed;
import io.reactivex.subscribers.TestSubscriber;
import org.jinterop.dcom.core.JIVariant;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * E2E test. You can run by spawning an OPC-DA test server and changing connection parameters to target it.
 * Currently the test is ignored during the build.
 *
 * @author amarziali
 */
@Ignore
public class OpcDaTemplateTest {

    private final Logger logger = LoggerFactory.getLogger(OpcDaTemplateTest.class);


    private OpcDaOperations opcDaOperations;
    private OpcDaConnectionProfile connectionProfile;


    @Before
    public void init() throws Exception {
        opcDaOperations = new OpcDaTemplate();
        connectionProfile = new OpcDaConnectionProfile()
                .withComClsId("F8582CF2-88FB-11D0-B850-00C0F0104305")
                .withCredentials(new NtlmCredentials()
                        .withDomain("OPC-9167C0D9342")
                        .withUser("OPC")
                        .withPassword("opc"))
                .withConnectionUri(new URI("opc.da://192.168.99.100:135"))
                .withKeepAliveInterval(Duration.ofSeconds(5))
                .withSocketTimeout(Duration.of(1, ChronoUnit.SECONDS));

        opcDaOperations.connect(connectionProfile).blockingAwait();

    }

    @After
    public void done() throws Exception {
        opcDaOperations.disconnect().blockingAwait();
    }


    @Test
    public void testBrowseTags() {
        logger.info("Received following tags {}", opcDaOperations.browseTags().toList().blockingGet());
    }

    @Test
    public void testFetchLeaves() {
        opcDaOperations.fetchNextTreeLevel("Square Waves")
                .forEach(System.out::println);
    }

    @Test
    public void testFetchMetadata() {
        opcDaOperations.fetchMetadata("Random.Real8")
                .forEach(System.out::println);
    }


    @Test
    public void testSampling_Subscribe() throws Exception {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshInterval(Duration.ofMillis(300));

        try (OpcSession session = opcDaOperations.createSession(sessionProfile).blockingGet()) {

            List<Instant> received = session
                    .stream("Square Waves.Real8", Duration.ofMillis(10))
                    .sample(1, TimeUnit.SECONDS)
                    .limit(5)
                    .map(a -> {
                        Instant now = Instant.now();
                        System.out.println(a);
                        return now;
                    }).toList().blockingGet();

            for (int i = 1; i < received.size(); i++) {
                Assert.assertTrue(received.get(i).toEpochMilli() - received.get(i - 1).toEpochMilli() >= 900);
            }

        }
    }

    @Test
    public void testSampling_Poll() throws Exception {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshInterval(Duration.ofMillis(300));

        List<Timed<OpcData>> received = Flowable.combineLatest(
                Flowable.interval(10, TimeUnit.MILLISECONDS),
                opcDaOperations.createSession(sessionProfile).toFlowable()
                        .flatMap(session -> session.stream("Square Waves.Real8", Duration.ofMillis(10))
                                .doFinally(session::close)

                        ), (a, b) -> b)
                .sample(10, TimeUnit.MILLISECONDS)
                .timeInterval()
                .limit(100)
                .toList().blockingGet();

        received.forEach(opcDataTimed -> logger.info("Received {}", opcDataTimed));


        for (int i = 1; i < received.size(); i++) {
            Assert.assertTrue(received.get(i).time(TimeUnit.MILLISECONDS) - received.get(i - 1).time(TimeUnit.MILLISECONDS) < 15);
        }

        Assert.assertTrue(received.stream().map(a -> a.value().getValue())
                .distinct().count() > 1);

    }

    @Test
    public void testStaticValues() throws Exception {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshInterval(Duration.ofMillis(300));

        try (OpcDaSession writeSession = opcDaOperations.createSession(sessionProfile).blockingGet()) {

            //create a first stream to regularly write to a tag
            Flowable<List<OperationStatus>> writer = Flowable.interval(2, TimeUnit.SECONDS)
                    .flatMap(ignored -> writeSession.write(new OpcData("Bucket Brigade.Real8",
                            Instant.now(), new Random().nextDouble())).toFlowable());


            Flowable<OpcData> flowable = opcDaOperations.createSession(sessionProfile)
                    .toFlowable()
                    .flatMap(session ->
                            session.stream("Bucket Brigade.Real8", Duration.ofMillis(300))
                                    .doFinally(session::close)
                    )
                    .doOnNext(opcData -> logger.info("{}", opcData))
                    .subscribeOn(Schedulers.newThread());
            TestSubscriber<OpcData> s1 = new TestSubscriber<>();
            TestSubscriber<Object> s2 = new TestSubscriber<>();
            writer.skipWhile(ignored -> !s2.hasSubscription()).limit(2).subscribe(s2);
            flowable.limit(2).subscribe(s1);
            s2.await();
            s1.await()
                    .assertComplete()
                    .assertValueCount(2);
        }
    }


    @Test
    public void listenToTags() throws Exception {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshInterval(Duration.ofMillis(300));

        try (OpcSession session = opcDaOperations.createSession(sessionProfile).blockingGet()) {
            TestSubscriber<OpcData> subscriber = new TestSubscriber<>();
            Flowable.merge(Flowable.fromArray(new String[]{"Read Error.Int4", "Square Waves.Real8", "Random.ArrayOfString"})
                    .map(tagId -> session.stream(tagId, Duration.ofMillis(100))))
                    .limit(20)
                    .subscribeOn(Schedulers.io())
                    .subscribe(subscriber);

            subscriber
                    .await()
                    .assertComplete()
                    .assertValueCount(20);


            Assert.assertEquals(3, subscriber.values().stream().map(OpcData::getTag).distinct().count());
        }
    }


    @Test
    public void testReadError() throws Exception {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshInterval(Duration.ofMillis(300));

        TestSubscriber<OpcData> testSubscriber = new TestSubscriber<>();

        opcDaOperations.createSession(sessionProfile)
                .flatMap(s ->
                        s.read("Read Error.String")
                                .flattenAsFlowable(a -> a)
                                .takeUntil(data -> data.getOperationStatus().getLevel() == OperationStatus.Level.INFO)
                                .firstOrError()
                                .doFinally(s::close)
                )
                .toFlowable()
                .doOnNext(opcData -> logger.info("Received {}", opcData))
                .subscribe(testSubscriber);


        testSubscriber.await();
        testSubscriber.assertComplete();
        testSubscriber.assertValueCount(1);


    }


    @Test
    public void listenToArray() throws Exception {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshInterval(Duration.ofMillis(300));

        TestSubscriber<OpcData> subscriber = new TestSubscriber<>();

        opcDaOperations.createSession(sessionProfile)
                .toFlowable()
                .flatMap(session -> session.stream("Random.ArrayOfString", Duration.ofMillis(500)))
                .limit(10)
                .doOnNext(System.out::println)
                .subscribe(subscriber);

        subscriber.await()
                .assertComplete()
                .assertValueCount(10);


    }


    @Test
    public void listenToAll() throws Exception {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshInterval(Duration.ofMillis(10));

        TestSubscriber<OpcData> subscriber = new TestSubscriber<>();
        try (OpcDaSession session = opcDaOperations.createSession(sessionProfile).blockingGet()) {
            List<OpcTagInfo> tagList = opcDaOperations.browseTags().toList().blockingGet();
            Flowable.merge(Flowable.fromArray(tagList.toArray(new OpcTagInfo[tagList.size()]))
                    .map(tagInfo -> session.stream(tagInfo.getId(), Duration.ofMillis(100)))
            )
                    .doOnNext(data -> logger.info("{}", data))
                    .limit(1000)
                    .subscribeOn(Schedulers.io())
                    .subscribe(subscriber);


            subscriber.await();
            subscriber.assertComplete();
        }

    }


    @Test
    public void forceDataTypeTest() throws Exception {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshInterval(Duration.ofSeconds(1))
                .withDataTypeForTag("Bucket Brigade.Int4", (short) JIVariant.VT_R8);

        try (OpcDaSession session = opcDaOperations.createSession(sessionProfile).blockingGet()) {
            OpcData data = session.read("Bucket Brigade.Int4").blockingGet().get(0);
            System.out.println(data);
            Assert.assertNotNull(data);
            Assert.assertTrue(data.getValue() instanceof Double);
        }

    }


    @Test(expected = OpcException.class)
    public void testTagNotFound() throws Exception {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshInterval(Duration.ofMillis(300));

        try (OpcSession session = opcDaOperations.createSession(sessionProfile).blockingGet()) {
            session.stream("I do not exist", Duration.ofMillis(500))
                    .blockingForEach(System.out::println);
        }
    }

    @Test
    public void testWriteValues() {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshInterval(Duration.ofMillis(300));
        OpcDaSession session = null;

        try {
            session = opcDaOperations.createSession(sessionProfile).blockingGet();
            Collection<OperationStatus> result =
                    session.write(new OpcData<>("Square Waves.Real8", Instant.now(), 123.31)).blockingGet();
            logger.info("Write result: {}", result);
            Assert.assertTrue(result
                    .stream().noneMatch(operationStatus -> operationStatus.getLevel() != OperationStatus.Level.INFO));


        } finally {
            opcDaOperations.releaseSession(session);
        }
    }

    @Test
    public void testWriteValuesFails() {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshInterval(Duration.ofMillis(300));
        OpcDaSession session = null;

        try {
            session = opcDaOperations.createSession(sessionProfile).blockingGet();
            Collection<OperationStatus> result = session.write(new OpcData("Square Waves.Real8", Instant.now(), "I'm not a number")).blockingGet();
            logger.info("Write result: {}", result);
            Assert.assertFalse(result.stream()
                    .noneMatch(operationStatus -> operationStatus.getLevel() != OperationStatus.Level.INFO));
        } finally {
            opcDaOperations.releaseSession(session);
        }
    }

    @Test
    public void testAutoReconnect() throws Exception {

        final OpcDaTemplate daTemplate = new OpcDaTemplate();
        final OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshInterval(Duration.ofMillis(100));
        final TestSubscriber<OpcData> subscriber = new TestSubscriber<>();


        Flowable<OpcData> flowable = daTemplate
                //establish a connection
                .connect(connectionProfile)
                //log connection errors
                .doOnError(t -> logger.warn("Unable to connect. Retrying...: {}", t.getMessage()))
                .andThen(daTemplate.createSession(sessionProfile))
                //when ready create a subscription and start streaming some data
                .toFlowable().flatMap(session ->
                        session.stream("Saw-toothed Waves.UInt4", Duration.ofMillis(100))
                                .subscribeOn(Schedulers.io())
                                .doFinally(session::close))
                //retry anything in case something failed failed
                .doOnError(throwable -> logger.warn("An error occurred. Retrying: " + throwable.getMessage()))
                .retryWhen(throwable -> throwable.delay(1, TimeUnit.SECONDS))
                //do not forget to close connections
                .doFinally(() -> daTemplate.close())
                //create an hot flowable
                .publish()
                .autoConnect();

        //create a deferred stream to simulate a disconnection
        flowable
                .limit(20)
                .doOnComplete(() -> daTemplate.disconnect().blockingAwait())
                //and attach it
                .subscribe();

        //attach now the real consumer
        flowable
                //look just for 300 values
                .limit(50)
                .doOnNext(data -> logger.info("Received {}", data))
                .subscribe(subscriber);


        subscriber.await();
        subscriber.assertComplete();
        subscriber.assertValueCount(50);

    }


}
