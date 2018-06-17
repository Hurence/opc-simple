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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hurence.opc.OpcData;
import com.hurence.opc.OpcTagInfo;
import com.hurence.opc.OperationStatus;
import com.hurence.opc.auth.Credentials;
import com.hurence.opc.auth.UsernamePasswordCredentials;
import com.hurence.opc.auth.X509Credentials;
import com.hurence.opc.exception.OpcException;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator;
import org.junit.*;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
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

/**
 * {@link OpcUaOperations} tests.
 * This suite spawns a fake OPC-UA test on localhost on any free port.
 *
 * @author amarziali
 */
public class OpcUaOperationsTest {

    private static final Logger logger = LoggerFactory.getLogger(OpcUaOperationsTest.class);

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
        try (OpcUaOperations opcUaOperations = new OpcUaOperations()) {
            opcUaOperations.connect(createConnectionProfile()
                    .withCredentials(new UsernamePasswordCredentials()
                            .withUser("user")
                            .withPassword("password1"))
            );
            Assert.assertTrue(opcUaOperations.awaitConnected());
        }
    }

    @Test
    public void connectionAnonymousSuccessTest() throws Exception {
        try (OpcUaOperations opcUaOperations = new OpcUaOperations()) {
            opcUaOperations.connect(createConnectionProfile()
                    .withCredentials(Credentials.ANONYMOUS_CREDENTIALS));
            Assert.assertTrue(opcUaOperations.awaitConnected());
            Assert.assertFalse(opcUaOperations.isChannelSecured());
        }
    }

    @Test
    public void connectionOnSecuredChannelSuccessTest() throws Exception {
        try (OpcUaOperations opcUaOperations = new OpcUaOperations()) {
            OpcUaConnectionProfile connectionProfile = createConnectionProfile();
            opcUaOperations.connect(connectionProfile
                    .withCredentials(Credentials.ANONYMOUS_CREDENTIALS)
                    .withSecureChannelEncryption(createX509Credentials(connectionProfile.getClientIdUri())));
            Assert.assertTrue(opcUaOperations.awaitConnected());
            Assert.assertTrue(opcUaOperations.isChannelSecured());

        }
    }


    @Test(expected = OpcException.class)
    public void connectionUserPasswordFails() throws Exception {
        try (OpcUaOperations opcUaOperations = new OpcUaOperations()) {
            opcUaOperations.connect(createConnectionProfile()
                    .withCredentials(new UsernamePasswordCredentials()
                            .withUser("user")
                            .withPassword("badpassword"))
            );
        }
    }

    @Test
    public void connectionX509dSuccessTest() throws Exception {
        try (OpcUaOperations opcUaOperations = new OpcUaOperations()) {
            OpcUaConnectionProfile connectionProfile = createConnectionProfile();
            opcUaOperations.connect(connectionProfile
                    .withCredentials(createX509Credentials(connectionProfile.getClientIdUri())));
            Assert.assertTrue(opcUaOperations.awaitConnected());
        }
    }

    @Test
    public void testBrowse() throws Exception {
        try (OpcUaOperations opcUaOperations = new OpcUaOperations()) {
            opcUaOperations.connect(createConnectionProfile());
            Collection<OpcTagInfo> ret = opcUaOperations.browseTags();
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            logger.info("{}", mapper.writeValueAsString(ret));
            Optional<OpcTagInfo> sint = ret.stream().filter(t -> "SinT".equals(t.getName()) &&
                    "Objects.TestFolder".equals(t.getGroup()))
                    .findFirst();
            Assert.assertTrue(sint.isPresent());
            Assert.assertFalse(ret.isEmpty());


        }
    }

    @Test
    public void testRead() throws Exception {
        try (OpcUaOperations opcUaOperations = new OpcUaOperations()) {
            opcUaOperations.connect(createConnectionProfile());
            OpcUaSession session = opcUaOperations.createSession(new OpcUaSessionProfile()
                    .withRefreshPeriod(Duration.ofMillis(10)));
            logger.info("Read tag {}", session.read("ns=2;s=sint"));
        }
    }

    @Test
    public void testWrite() throws Exception {
        try (OpcUaOperations opcUaOperations = new OpcUaOperations()) {
            opcUaOperations.connect(createConnectionProfile());
            OpcUaSession session = opcUaOperations.createSession(new OpcUaSessionProfile()
                    .withRefreshPeriod(Duration.ofMillis(10)));
            List<OperationStatus> result = session.write(
                    new OpcData("ns=2;s=HelloWorld/Dynamic/Double", Instant.now(), 3.1415d),
                    new OpcData("ns=2;s=sint", Instant.now(), true)
            );
            logger.info("Write result: {}", result);
            Assert.assertFalse(result.isEmpty());
            Assert.assertEquals(2, result.size());
            Assert.assertEquals(OperationStatus.Level.INFO, result.get(0).getLevel());
            Assert.assertEquals(OperationStatus.Level.ERROR, result.get(1).getLevel());
        }
    }

    @Test
    public void testStream() throws Exception {
        try (OpcUaOperations opcUaOperations = new OpcUaOperations()) {
            opcUaOperations.connect(createConnectionProfile());
            try (OpcUaSession session = opcUaOperations.createSession(new OpcUaSessionProfile()
                    .withDefaultPollingInterval(Duration.ofMillis(1))
                    .withRefreshPeriod(Duration.ofMillis(100)))) {
                final List<OpcData<Double>> values = new ArrayList<>();
                session.stream("ns=2;s=sint").limit(1000).forEach(values::add);
                logger.info("Received {} items", values.size());
                values.stream().map(OpcData::getValue).forEach(System.err::println);
                logger.info("Stream result: {}", values);
            }
        }
    }

    @Test
    public void testStreamWithGraph() throws Exception {
        try (OpcUaOperations opcUaOperations = new OpcUaOperations()) {
            opcUaOperations.connect(createProsysConnectionProfile());
            try (OpcUaSession session = opcUaOperations.createSession(new OpcUaSessionProfile()
                    .withDefaultPollingInterval(Duration.ofMillis(10))
                    .withRefreshPeriod(Duration.ofMillis(100)))) {

                final long startTime = System.currentTimeMillis();
                final List<OpcData<Double>> items = new ArrayList<>();


                session.stream("ns=5;s=Sawtooth1").limit(200).forEach(items::add);
                System.err.println(items.size());
                // Create Chart
                final XYChart chart = QuickChart.getChart("Simple XChart Real-time Demo", "Time", "Sine (f=1second)", "sine",
                        items.stream().mapToDouble(d -> d.getTimestamp().toEpochMilli() - startTime).toArray(),
                        items.stream().mapToDouble(OpcData::getValue).toArray());
                // Show it
                final SwingWrapper<XYChart> sw = new SwingWrapper<>(chart);
                sw.displayChart();
                Thread.sleep(200000);

            }
        }
    }

    @Test
    @Ignore
    public void testStreamFromProsys() throws Exception {
        try (OpcUaOperations opcUaOperations = new OpcUaOperations()) {
            opcUaOperations.connect(createProsysConnectionProfile());
            try (OpcUaSession session = opcUaOperations.createSession(new OpcUaSessionProfile()
                    .withDefaultPollingInterval(Duration.ofMillis(10))
                    .withRefreshPeriod(Duration.ofMillis(100)))) {
                final List<OpcData<Double>> values = new ArrayList<>();
                session.stream("ns=5;s=Sawtooth1").limit(1000).forEach(values::add);

                logger.info("Stream result: {}", values);
                values.stream().map(OpcData::getTimestamp).forEach(System.err::println);

            }
        }
    }


    @Ignore
    @Test
    public void testReadFromProsys() throws Exception {
        try (OpcUaOperations opcUaOperations = new OpcUaOperations()) {
            opcUaOperations.connect(createProsysConnectionProfile());
            OpcUaSession session = opcUaOperations.createSession(new OpcUaSessionProfile()
                    .withRefreshPeriod(Duration.ofMillis(10)));
            logger.info("Read tag {}", session.read("ns=5;s=Sawtooth1"));
        }
    }


}
