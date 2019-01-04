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

import com.hurence.opc.*;
import com.hurence.opc.auth.Credentials;
import com.hurence.opc.auth.UsernamePasswordCredentials;
import com.hurence.opc.auth.X509Credentials;
import com.hurence.opc.exception.OpcException;
import io.reactivex.Flowable;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * {@link OpcUaTemplate} tests.
 * This suite spawns a fake OPC-UA test on localhost on any free port.
 *
 * @author amarziali
 */
public class OpcUaTemplateTest {

    private static final Logger logger = LoggerFactory.getLogger(OpcUaTemplateTest.class);

    private static TestOpcServer server;

    @BeforeClass
    public static void initServer() throws Exception {
        server = new TestOpcServer(InetAddress.getLoopbackAddress(), null);
        server.getInstance().startup().get();
    }

    @AfterClass
    public static void teardownServer() {
        try {
            server.close();
        } catch (Exception e) {
            //nothing to do here
        }
    }

    private X509Credentials createX509Credentials(String clientIdUri) {
        try {
            KeyPair keyPair = SelfSignedCertificateGenerator.generateRsaKeyPair(2048);
            X509Certificate cert = TestOpcServer.generateCertificate(keyPair, clientIdUri);
            return new X509Credentials()
                    .withCertificate(cert)
                    .withPrivateKey(keyPair.getPrivate());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private OpcUaConnectionProfile createConnectionProfile() {
        return (new OpcUaConnectionProfile()
                .withConnectionUri(URI.create(server.getBindEndpoint()))
                .withClientIdUri("hurence:opc-simple:client:test")
                .withClientName("Simple OPC test client")
                .withSocketTimeout(Duration.ofSeconds(5))
        );
    }

    private OpcUaConnectionProfile createProsysConnectionProfile() {
        return (new OpcUaConnectionProfile()
                .withConnectionUri(URI.create("opc.tcp://localhost:53530/OPCUA/SimulationServer"))
                .withClientIdUri("hurence:opc-simple:client:test")
                .withClientName("Simple OPC test client")
                .withSocketTimeout(Duration.ofSeconds(5))
        );
    }

    @Test
    public void connectionUserPasswordSuccessTest() throws Exception {
        try (OpcUaTemplate opcUaTemplate = new OpcUaTemplate()) {
            opcUaTemplate.connect(createConnectionProfile()
                    .withCredentials(new UsernamePasswordCredentials()
                            .withUser("user")
                            .withPassword("password1"))
            ).ignoreElement().blockingAwait();
        }
    }

    @Test
    public void connectionAnonymousSuccessTest() throws Exception {
        try (OpcUaTemplate opcUaTemplate = new OpcUaTemplate()) {
            opcUaTemplate.connect(createConnectionProfile()
                    .withCredentials(Credentials.ANONYMOUS_CREDENTIALS))
                    .ignoreElement().blockingAwait();
            Assert.assertFalse(opcUaTemplate.isChannelSecured());
        }
    }

    @Test
    public void connectionOnSecuredChannelSuccessTest() throws Exception {
        try (OpcUaTemplate opcUaTemplate = new OpcUaTemplate()) {
            OpcUaConnectionProfile connectionProfile = createConnectionProfile();
            opcUaTemplate.connect(connectionProfile
                    .withCredentials(Credentials.ANONYMOUS_CREDENTIALS)
                    .withSecureChannelEncryption(createX509Credentials(connectionProfile.getClientIdUri())))
                    .ignoreElement()
                    .blockingAwait();
            Assert.assertTrue(opcUaTemplate.isChannelSecured());

        }
    }


    @Test(expected = OpcException.class)
    public void connectionUserPasswordFails() throws Exception {
        try (OpcUaTemplate opcUaTemplate = new OpcUaTemplate()) {
            opcUaTemplate.connect(createConnectionProfile()
                    .withCredentials(new UsernamePasswordCredentials()
                            .withUser("user")
                            .withPassword("badpassword"))
            )
                    .ignoreElement()
                    .blockingAwait();
        }
    }

    @Test
    public void connectionX509dSuccessTest() throws Exception {
        try (OpcUaTemplate opcUaTemplate = new OpcUaTemplate()) {
            OpcUaConnectionProfile connectionProfile = createConnectionProfile();
            opcUaTemplate.connect(connectionProfile
                    .withCredentials(createX509Credentials(connectionProfile.getClientIdUri())))
                    .ignoreElement()
                    .blockingAwait();
        }
    }

    @Test
    public void testReactiveBrowse() throws Exception {
        final OpcUaTemplate opcUaTemplate = new OpcUaTemplate();
        TestSubscriber<OpcTagInfo> subscriber = new TestSubscriber<>();
        opcUaTemplate.connect(createConnectionProfile())
                .toFlowable()
                .flatMap(client -> client.browseTags()
                        .doFinally(client::close))
                .subscribe(subscriber);

        subscriber.assertComplete();
        subscriber.assertValueCount(229);
        subscriber.dispose();
        Assert.assertEquals(ConnectionState.DISCONNECTED, opcUaTemplate.getConnectionState().blockingFirst());

    }

    @Test
    public void testBrowse() throws Exception {

        try (OpcUaTemplate opcUaTemplate = new OpcUaTemplate()) {
            Collection<OpcTagInfo> ret =
                    opcUaTemplate.connect(createConnectionProfile())
                            .toFlowable()
                            .flatMap(client -> client.browseTags())
                            .toList().blockingGet();


            logger.info("{}", ret);
            Optional<OpcTagInfo> sint = ret.stream().filter(t -> "SinT".equals(t.getName()))
                    .findFirst();
            Assert.assertTrue(sint.isPresent());
            Assert.assertFalse(ret.isEmpty());


        }
    }

    @Test
    public void testFetchMetadata() throws Exception {
        try (OpcUaTemplate opcUaTemplate = new OpcUaTemplate()) {

            Collection<OpcTagInfo> ret =
                    opcUaTemplate.connect(createConnectionProfile())
                            .toFlowable()
                            .flatMap(client -> client.fetchMetadata("ns=2;s=sint"))
                            .toList().blockingGet();

            logger.info("Metadata: {}", ret);
            Assert.assertEquals(1, ret.size());

        }
    }

    @Test
    public void testRead() throws Exception {
        try (OpcUaTemplate opcUaTemplate = new OpcUaTemplate()) {
            opcUaTemplate.connect(createConnectionProfile())
                    .ignoreElement()
                    .andThen(opcUaTemplate.createSession(new OpcUaSessionProfile()))
                    .flatMap(opcUaSession -> opcUaSession.read("ns=2;s=sint")
                            .doFinally(opcUaSession::close))
                    .doOnSuccess(items -> logger.info("Read tag {}", items))
                    .blockingGet();
        }
    }

    @Test
    public void testWrite() throws Exception {
        try (OpcUaTemplate opcUaTemplate = new OpcUaTemplate()) {
            opcUaTemplate.connect(createConnectionProfile())
                    .ignoreElement()
                    .blockingAwait();
            OpcUaSession session = opcUaTemplate.createSession(new OpcUaSessionProfile()
            ).blockingGet();
            List<OperationStatus> result = session.write(
                    new OpcData("ns=2;s=HelloWorld/Dynamic/Double", Instant.now(), 3.1415d),
                    new OpcData("ns=2;s=sint", Instant.now(), true)
            ).blockingGet();
            logger.info("Write result: {}", result);
            Assert.assertFalse(result.isEmpty());
            Assert.assertEquals(2, result.size());
            Assert.assertEquals(OperationStatus.Level.INFO, result.get(0).getLevel());
            Assert.assertEquals(OperationStatus.Level.ERROR, result.get(1).getLevel());
        }
    }

    @Test
    public void testStream() throws Exception {
        final OpcUaTemplate opcUaTemplate = new OpcUaTemplate();
        final TestSubscriber<OpcData> subscriber = new TestSubscriber<>();
        opcUaTemplate.connect(createConnectionProfile())
                .subscribeOn(Schedulers.newThread())
                .flatMap(client -> client.createSession(new OpcUaSessionProfile()
                        .withPublicationInterval(Duration.ofMillis(100))
                ))
                .toFlowable()
                .flatMap(opcUaSession -> opcUaSession.stream("ns=2;s=sint", Duration.ofMillis(1))
                        .doFinally(opcUaSession::close)
                        .onBackpressureBuffer()
                ).limit(10000)
                .doFinally(opcUaTemplate::close)
                .subscribe(subscriber);

        subscriber
                .await()
                .assertComplete()
                .assertValueCount(10000);
    }


    @Test
    public void testfetchNextTreeLevel() throws Exception {
        try (OpcUaTemplate opcUaTemplate = new OpcUaTemplate()) {
            TestSubscriber<OpcObjectInfo> subscriber1 = new TestSubscriber<>();
            TestSubscriber<OpcObjectInfo> subscriber2 = new TestSubscriber<>();

            ConnectableFlowable<OpcUaOperations> cf = opcUaTemplate.connect(createConnectionProfile())
                    .toFlowable().publish();
            cf.flatMap(client -> client.fetchNextTreeLevel("ns=0;i=84"))
                    .doOnNext(item -> logger.info(item.toString()))
                    .subscribe(subscriber1);

            cf.flatMap(client -> client.fetchNextTreeLevel("ns=2;s=sint"))
                    .subscribe(subscriber2);

            cf.connect();

            subscriber1.await()
                    .assertComplete()
                    .assertValueCount(3);
            subscriber2.await()
                    .assertComplete()
                    .assertValueCount(0);
        }
    }


    @Test
    @Ignore
    public void testStreamFromProsys() throws Exception {
        try (OpcUaTemplate opcUaTemplate = new OpcUaTemplate()) {
            opcUaTemplate.connect(createProsysConnectionProfile()).ignoreElement().blockingAwait();
            try (OpcUaSession session = opcUaTemplate.createSession(new OpcUaSessionProfile()
                    .withPublicationInterval(Duration.ofMillis(1000))).blockingGet()) {
                final List<OpcData<Double>> values = new ArrayList<>();
                session.stream("ns=5;s=Sawtooth1", Duration.ofMillis(10))
                        .limit(1000).map(a -> {
                    System.out.println(a);
                    return a;
                }).blockingForEach(values::add);

                logger.info("Stream result: {}", values);
                values.stream().map(OpcData::getTimestamp).forEach(System.err::println);

            }
        }

    }


    @Test
    public void testAutoReconnect() throws Exception {

        //start a new dedicated server
        TestOpcServer uaServer = new TestOpcServer(InetAddress.getLoopbackAddress(), null);
        try {
            uaServer.getInstance().startup().get();

            TestSubscriber<OpcData> subscriber = new TestSubscriber<>();

            //now build our stream
            Flowable<OpcData> flowable = new OpcUaTemplate()
                    //establish a connection
                    .connect(new OpcUaConnectionProfile()
                            .withConnectionUri(URI.create(uaServer.getBindEndpoint()))
                            .withSocketTimeout(Duration.ofSeconds(3))
                            .withKeepAliveInterval(Duration.ofSeconds(1)))
                    //log connection errors
                    .doOnError(t -> logger.warn("Unable to connect. Retrying...: {}", t.getMessage()))
                    .toFlowable()
                    .flatMap(client -> client.createSession(new OpcUaSessionProfile()
                            .withPublicationInterval(Duration.ofMillis(100)))
                            //when ready create a subscription and start streaming some data
                            .toFlowable()
                            .doOnNext(opcUaSession -> logger.info("Created new OPC UA session"))
                            .flatMap(session ->
                                    session.stream("ns=2;s=sint", Duration.ofMillis(100))
                                            //do not forget to close connections
                                            .doFinally(session::close)

                            )
                            .doFinally(client::close)
                    )
                    //retry anything in case something failed failed
                    .doOnError(throwable -> logger.warn("An error occurred. Reconnecting: " + throwable.getMessage()))
                    .retryWhen(throwable -> throwable.delay(1, TimeUnit.SECONDS))
                    .subscribeOn(Schedulers.io())
                    .takeWhile(ignored -> !subscriber.isTerminated())
                    //create an hot flowable
                    .publish()
                    .autoConnect(2);


            //create a deferred stream to simulate a disconnection
            flowable.take(20)
                    .subscribeOn(Schedulers.newThread())
                    .doOnComplete(() ->
                            new Thread(() -> {
                                try {
                                    //get server down
                                    uaServer.close();
                                    Thread.sleep(5000);
                                    //now bring server back up
                                    uaServer.getInstance().startup().get();
                                } catch (Exception e) {
                                    //nothing we can do here
                                }

                            }).start()
                    )
                    //and attach it
                    .subscribe();

            //attach now the real consumer
            flowable
                    //look just for 300 values
                    .take(50)
                    .doOnNext(data -> logger.info("Received {}", data))
                    .subscribe(subscriber);


            subscriber.await();
            subscriber.assertComplete();
            subscriber.assertValueCount(50);
            subscriber.dispose();
        } finally {
            uaServer.close();
        }

    }

    @Ignore
    @Test
    public void testReadFromProsys() throws Exception {
        try (OpcUaTemplate opcUaTemplate = new OpcUaTemplate()) {
            opcUaTemplate.connect(createProsysConnectionProfile()).ignoreElement().blockingAwait();
            OpcUaSession session = opcUaTemplate.createSession(new OpcUaSessionProfile()
            ).blockingGet();
            logger.info("Read tag {}", session.read("ns=5;s=Sawtooth1"));
        }
    }


}
