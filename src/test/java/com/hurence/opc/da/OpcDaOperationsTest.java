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
import com.hurence.opc.OpcSession;
import com.hurence.opc.OpcTagInfo;
import com.hurence.opc.auth.UsernamePasswordCredentials;
import com.hurence.opc.util.AutoReconnectOpcOperations;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

/**
 * E2E test. You can run by spawning an OPC-DA test server and changing connection parameters to target it.
 * Currently the test is ignored during the build.
 *
 * @author amarziali
 */
@Ignore
public class OpcDaOperationsTest {

    private final Logger logger = LoggerFactory.getLogger(OpcDaOperationsTest.class);


    private OpcDaOperations opcDaOperations;
    private OpcDaConnectionProfile connectionProfile;


    @Before
    public void init() throws Exception {
        opcDaOperations = new OpcDaOperations();
        connectionProfile = new OpcDaConnectionProfile()
                .withComClsId("F8582CF2-88FB-11D0-B850-00C0F0104305")
                .withDomain("OPC-9167C0D9342")
                .withCredentials(new UsernamePasswordCredentials()
                        .withUser("OPC")
                        .withPassword("opc"))
                .withConnectionUri(new URI("opc.da://192.168.99.100:135"))
                .withSocketTimeout(Duration.of(1, ChronoUnit.SECONDS));

        opcDaOperations.connect(connectionProfile);
        if (!opcDaOperations.awaitConnected()) {
            throw new IllegalStateException("Unable to connect");
        }
    }

    @After
    public void done() throws Exception {
        opcDaOperations.disconnect();
        opcDaOperations.awaitDisconnected();
    }


    @Test
    public void testBrowseTags() {
        logger.info("Received following tags {}", opcDaOperations.browseTags());
    }

    @Test
    public void listenToTags() throws Exception {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshPeriod(Duration.ofMillis(300));

        try (OpcSession session = opcDaOperations.createSession(sessionProfile)) {
            session.stream("Read Error.Int4", "Square Waves.Real8", "Random.ArrayOfString")
                    .limit(20)
                    .forEach(System.out::println);

        }
    }

    @Test
    public void testReadError() {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshPeriod(Duration.ofMillis(300));
        OpcDaSession session = null;

        try {
            session = opcDaOperations.createSession(sessionProfile);
            OpcData<String> result = null;

            int lastQuality = 0;
            while (lastQuality < 100) {
                result = session.read("Read Error.String").stream().findFirst().get();
                System.out.println(result);
                lastQuality = result.getQuality();
            }
            Assert.assertTrue(result.getErrorCode().isPresent());
            System.out.println("Received error : " + result.getErrorCode().get());

        } finally {
            opcDaOperations.releaseSession(session);
        }
    }


    @Test
    public void listenToArray() {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshPeriod(Duration.ofMillis(300));
        OpcDaSession session = null;

        try {
            session = opcDaOperations.createSession(sessionProfile);
            session.stream("Random.ArrayOfString")
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
                .withRefreshPeriod(Duration.ofMillis(300));
        OpcDaSession session = null;

        try {
            session = opcDaOperations.createSession(sessionProfile);
            session.stream(opcDaOperations.browseTags().stream().map(OpcTagInfo::getId).toArray(a -> new String[a]))
                    .limit(100)
                    .forEach(System.out::println);
        } finally {
            opcDaOperations.releaseSession(session);
        }
    }

    @Test
    public void testWriteValues() {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshPeriod(Duration.ofMillis(300));
        OpcDaSession session = null;

        try {
            session = opcDaOperations.createSession(sessionProfile);
            Assert.assertTrue(session.write(new OpcData("Square Waves.Real8", Instant.now(), 120, 123.31)));
        } finally {
            opcDaOperations.releaseSession(session);
        }
    }

    @Test
    public void testWriteValuesFails() {
        OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
                .withDirectRead(false)
                .withRefreshPeriod(Duration.ofMillis(300));
        OpcDaSession session = null;

        try {
            session = opcDaOperations.createSession(sessionProfile);
            Assert.assertFalse(session.write(new OpcData("Square Waves.Real8", Instant.now(), 120, "I'm not a number")));
        } finally {
            opcDaOperations.releaseSession(session);
        }
    }

    @Test
    public void testAutoReconnect() throws Exception {
        AutoReconnectOpcOperations autoReconnectOpcOperations = new AutoReconnectOpcOperations(opcDaOperations);
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
    }


}
