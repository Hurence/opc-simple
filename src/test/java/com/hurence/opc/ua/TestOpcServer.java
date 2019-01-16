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

import com.google.common.collect.ImmutableList;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.CompositeValidator;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator;
import org.eclipse.milo.opcua.sdk.server.util.HostnameUtil;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.application.InsecureCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.core.util.CryptoRestrictions;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateBuilder;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.google.common.collect.Lists.newArrayList;

public class TestOpcServer implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(TestOpcServer.class);

    static {
        CryptoRestrictions.remove();
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) throws Exception {
        final TestOpcServer server = new TestOpcServer(InetAddress.getLoopbackAddress(), null);
        server.getInstance().startup()
                .thenAccept(opcUaServer -> logger.info("Started server on {}", server.getBindEndpoint())).get();
        final CompletableFuture<Void> future = new CompletableFuture<>();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> future.complete(null)));
        future.get();


    }


    private final OpcUaServer instance;

    public TestOpcServer(InetAddress bindAddress, Integer port) throws Exception {
        UsernameIdentityValidator identityValidator = new UsernameIdentityValidator(
                true,
                authChallenge -> {
                    String username = authChallenge.getUsername();
                    String password = authChallenge.getPassword();

                    boolean userOk = "user".equals(username) && "password1".equals(password);
                    boolean adminOk = "admin".equals(username) && "password2".equals(password);

                    return userOk || adminOk;
                }
        );
        X509IdentityValidator x509IdentityValidator = new X509IdentityValidator(c -> true);
        String ba = bindAddress.getHostAddress();
        List<String> bindAddresses = newArrayList();
        bindAddresses.add(ba);

        List<String> endpointAddresses = newArrayList();
        endpointAddresses.addAll(HostnameUtil.getHostnames(ba));
        int bindPort = port != null ? port : findFreePort(bindAddress);

        // The configured application URI must match the one in the certificate(s)
        String applicationUri = "urn:hurence:opc:test-server:" + UUID.randomUUID();
        KeyPair keyPair = SelfSignedCertificateGenerator.generateRsaKeyPair(2048);

        OpcUaServerConfig serverConfig = OpcUaServerConfig.builder()
                .setApplicationUri(applicationUri)
                .setApplicationName(LocalizedText.english("Hurence OPC UA Test Server"))
                .setBindPort(bindPort)
                .setBindAddresses(bindAddresses)
                .setEndpointAddresses(endpointAddresses)
                .setBuildInfo(
                        new BuildInfo(
                                "urn:hurence:opc:test-server",
                                "eclipse",
                                "Hurence opc UA test server",
                                OpcUaServer.SDK_VERSION,
                                "", DateTime.now()))
                .setCertificateManager(new DefaultCertificateManager(keyPair, generateCertificate(keyPair, applicationUri)))
                .setCertificateValidator(new InsecureCertificateValidator())
                .setIdentityValidator(new CompositeValidator(identityValidator, x509IdentityValidator))
                .setProductUri("urn:hurence:opc:test-server")
                .setServerName("test")
                .setSecurityPolicies(
                        EnumSet.of(
                                SecurityPolicy.None,
                                SecurityPolicy.Basic128Rsa15,
                                SecurityPolicy.Basic256,
                                SecurityPolicy.Basic256Sha256,
                                SecurityPolicy.Aes128_Sha256_RsaOaep,
                                SecurityPolicy.Aes256_Sha256_RsaPss))
                .setUserTokenPolicies(
                        ImmutableList.of(
                                OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS,
                                OpcUaServerConfig.USER_TOKEN_POLICY_USERNAME,
                                OpcUaServerConfig.USER_TOKEN_POLICY_X509))
                .build();

        instance = new OpcUaServer(serverConfig);
        registerSampleObjects();
        logger.info("Created OPC-UA server running on opc.tcp://{}:{}", bindAddress, bindPort);
    }


    public String getBindEndpoint() {
        return instance.getEndpointDescriptions()[0].getEndpointUrl();
    }

    public static X509Certificate generateCertificate(KeyPair keyPair, String appUri) throws Exception {
        SelfSignedCertificateBuilder builder = new SelfSignedCertificateBuilder(keyPair)
                .setCommonName("Hurence test")
                .setOrganization("Hurence")
                .setOrganizationalUnit("dev")
                .setLocalityName("Lyon")
                .setCountryCode("FR")
                .setApplicationUri(appUri);
        return builder.build();

    }

    private int findFreePort(InetAddress address) throws Exception {
        try (ServerSocket s = new ServerSocket(0, -1, address)) {
            return s.getLocalPort();
        }

    }


    private void registerSampleObjects() throws Exception {

        instance.getNamespaceManager().registerAndAdd(TestNamespace.URI, uShort -> new TestNamespace(uShort, instance));


    }


    public OpcUaServer getInstance() {
        return instance;
    }

    @Override
    public void close() throws Exception {
        if (instance != null) {
            instance.shutdown().get();
        }
    }
}
