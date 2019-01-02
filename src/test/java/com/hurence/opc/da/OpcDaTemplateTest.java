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

import com.hurence.opc.*;
import com.hurence.opc.auth.NtlmCredentials;
import com.hurence.opc.exception.OpcException;
import org.jinterop.dcom.core.JIVariant;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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
    public void testSampling() throws Exception {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshInterval(Duration.ofMillis(300));

        try (OpcSession session = opcDaOperations.createSession(sessionProfile).blockingGet()) {
            List<Instant> received = session.stream(
                    new SubscriptionConfiguration().withDefaultSamplingInterval(Duration.ofMillis(10))
                            .withTagSamplingIntervalForTag("Square Waves.Real8", Duration.ofSeconds(1)),
                    "Square Waves.Real8")
                    .limit(5)
                    .map(a -> {
                        Instant now = Instant.now();
                        System.out.println(a);
                        return now;
                    }).collect(Collectors.toList());

            for (int i = 1; i < received.size(); i++) {
                Assert.assertTrue(received.get(i).toEpochMilli() - received.get(i - 1).toEpochMilli() >= 900);
            }

        }
    }

    @Test
    public void testStaticValues() throws Exception {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshInterval(Duration.ofMillis(300));


        try (OpcSession session = opcDaOperations.createSession(sessionProfile).blockingGet()) {
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    session.write(new OpcData("Bucket Brigade.Real8", Instant.now(), new Random().nextDouble()));

                } catch (Exception e) {
                    Assert.fail(e.getMessage());
                }
            }).start();

            List<OpcData> received = session.stream(
                    new SubscriptionConfiguration().withDefaultSamplingInterval(Duration.ofMillis(10)),
                    "Bucket Brigade.Real8")
                    .limit(2)
                    .map(a -> {
                        System.out.println(a);
                        return a;
                    }).collect(Collectors.toList());

            Assert.assertEquals(2, received.size());
            Assert.assertFalse(Objects.equals(received.get(0).getValue(), received.get(1).getValue()));

        }
    }

    @Test
    public void listenToTags() throws Exception {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshInterval(Duration.ofMillis(300));

        try (OpcSession session = opcDaOperations.createSession(sessionProfile).blockingGet()) {
            Assert.assertEquals(20, session.stream(new SubscriptionConfiguration().withDefaultSamplingInterval(Duration.ofMillis(10)),
                    "Read Error.Int4", "Square Waves.Real8", "Random.ArrayOfString")
                    .limit(20)
                    .map(a -> {
                        System.out.println(a);
                        return a;
                    }).count());

        }
    }


    @Test
    public void testReadError() {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshInterval(Duration.ofMillis(300));
        OpcDaSession session = null;

        try {
            session = opcDaOperations.createSession(sessionProfile).blockingGet();
            OpcData<String> result = null;

            OperationStatus lastQuality;
            do {
                result = session.read("Read Error.String").stream().findFirst().get();
                System.out.println(result);
                lastQuality = result.getOperationStatus();
            } while (lastQuality.getLevel() == OperationStatus.Level.INFO);
            System.out.println("Received error : " + result.getOperationStatus());

        } finally {
            opcDaOperations.releaseSession(session);
        }
    }


    @Test
    public void listenToArray() {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshInterval(Duration.ofMillis(300));
        OpcDaSession session = null;

        try {
            session = opcDaOperations.createSession(sessionProfile).blockingGet();
            session.stream(new SubscriptionConfiguration().withDefaultSamplingInterval(Duration.ofMillis(500)),
                    "Random.ArrayOfString")
                    .limit(20)
                    .map(a -> Arrays.toString((String[]) a.getValue()))
                    .forEach(System.out::println);

        } finally {
            opcDaOperations.releaseSession(session);
        }
    }

    @Test
    public void listenToAll() {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshInterval(Duration.ofMillis(10));

        OpcDaSession session = null;

        try {
            session = opcDaOperations.createSession(sessionProfile).blockingGet();
            session.stream(new SubscriptionConfiguration().withDefaultSamplingInterval(Duration.ofMillis(100)),
                    opcDaOperations.browseTags().toList().blockingGet().stream().map(OpcTagInfo::getId).toArray(a -> new String[a]))
                    .limit(10000)
                    .forEach(System.out::println);
        } finally {
            opcDaOperations.releaseSession(session);
        }
    }

    @Test
    public void forceDataTypeTest() throws Exception {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshInterval(Duration.ofSeconds(1))
                .withDataTypeForTag("Bucket Brigade.Int4", (short) JIVariant.VT_R8);

        try (OpcDaSession session = opcDaOperations.createSession(sessionProfile).blockingGet()) {
            OpcData data = session.read("Bucket Brigade.Int4").get(0);
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
            session.stream(new SubscriptionConfiguration().withDefaultSamplingInterval(Duration.ofMillis(500)),
                    "I do not exist")
                    .forEach(System.out::println);
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
                    session.write(new OpcData<>("Square Waves.Real8", Instant.now(), 123.31));
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
            Collection<OperationStatus> result = session.write(new OpcData("Square Waves.Real8", Instant.now(), "I'm not a number"));
            logger.info("Write result: {}", result);
            Assert.assertFalse(result.stream()
                    .noneMatch(operationStatus -> operationStatus.getLevel() != OperationStatus.Level.INFO));
        } finally {
            opcDaOperations.releaseSession(session);
        }
    }

    @Test
    public void testAutoReconnect() throws Exception {
        /*
        OpcDaOperations autoReconnectOpcOperations = AutoReconnectOpcOperations.create(opcDaOperations);
        opcDaOperations.disconnect();
        autoReconnectOpcOperations.connect(connectionProfile);
        Assert.assertTrue(autoReconnectOpcOperations.awaitConnected());
        //force disconnect
        opcDaOperations.disconnect();
        Assert.assertTrue(autoReconnectOpcOperations.awaitConnected());
        opcDaOperations.disconnect();
        Assert.assertTrue(autoReconnectOpcOperations.awaitConnected());
        Assert.assertFalse(autoReconnectOpcOperations.browseTags().isEmpty());
        autoReconnectOpcOperations.disconnect();
        Assert.assertTrue(autoReconnectOpcOperations.awaitDisconnected());
        */
    }


}
