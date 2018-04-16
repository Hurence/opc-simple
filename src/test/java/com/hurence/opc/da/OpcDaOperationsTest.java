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
import com.hurence.opc.util.AutoReconnectOpcOperations;
import org.jinterop.dcom.core.JIVariant;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * E2E test. You can run by spawning an OPC-DA test server and changing connection parameters to target it.
 * Currently the test is ignored during the build.
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
                .withUser("OPC")
                .withPassword("opc")
                .withRefreshPeriodMillis(50)
                .withDirectRead(true)
                .withHost("192.168.56.101")
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
    public void listenToTags() {
        opcDaOperations.stream("Read Error.Int4", "Square Waves.Real8").forEach(System.out::println);
    }

    @Test
    public void listenToAll() {
        opcDaOperations.stream(opcDaOperations.browseTags().stream().toArray(a -> new String[a])).forEach(System.out::println);
    }

    @Test
    public void testWriteValues() {
        Assert.assertTrue(opcDaOperations.write(new OpcData("Square Waves.Real8", Instant.now(), 120, JIVariant.makeVariant(123.31))));
    }

    @Test
    public void testWriteValuesFails() {
        Assert.assertFalse(opcDaOperations.write(new OpcData("Square Waves.Real8", Instant.now(), 120, JIVariant.makeVariant("I'm not a number"))));
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
