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
import com.hurence.opc.OpcTagInfo;
import com.hurence.opc.auth.Credentials;
import com.hurence.opc.auth.UsernamePasswordCredentials;
import com.hurence.opc.auth.X509Credentials;
import com.hurence.opc.exception.OpcException;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collection;
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

    @Ignore
    @Test
    public void testReamdFromProsys() throws Exception {
        try (OpcUaOperations opcUaOperations = new OpcUaOperations()) {
            opcUaOperations.connect(createProsysConnectionProfile());
            OpcUaSession session = opcUaOperations.createSession(new OpcUaSessionProfile()
                    .withRefreshPeriod(Duration.ofMillis(10)));
            logger.info("Read tag {}", session.read("ns=5;s=Sawtooth1"));
        }
    }

}
