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

import com.hurence.opc.AbstractOpcOperations;
import com.hurence.opc.ConnectionState;
import com.hurence.opc.OpcTagInfo;
import com.hurence.opc.OpcTagProperty;
import com.hurence.opc.auth.Credentials;
import com.hurence.opc.auth.UsernamePasswordCredentials;
import com.hurence.opc.auth.X509Credentials;
import com.hurence.opc.exception.OpcException;
import com.hurence.opc.util.ExecutorServiceFactory;
import com.hurence.opc.util.SingleThreadedExecutorServiceFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.X509IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;
import org.eclipse.milo.opcua.sdk.client.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.client.api.nodes.VariableTypeNode;
import org.eclipse.milo.opcua.sdk.client.model.nodes.objects.ServerNode;
import org.eclipse.milo.opcua.sdk.client.model.nodes.variables.ServerStatusNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.ServerState;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.util.CertificateUtil;
import org.eclipse.milo.opcua.stack.core.util.CryptoRestrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * OPC-UA implementation of {@link com.hurence.opc.OpcOperations}
 *
 * @author amarziali
 */
public class OpcUaOperations extends AbstractOpcOperations<OpcUaConnectionProfile, OpcUaSessionProfile, OpcUaSession> {

    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(OpcUaOperations.class);

    static {
        CryptoRestrictions.remove();
        Security.addProvider(new BouncyCastleProvider());
    }


    /**
     * Milo UA client.
     */
    private OpcUaClient client;
    /**
     * The scheduler service.
     */
    private ScheduledExecutorService scheduler;

    /**
     * Construct an instance with an {@link ExecutorServiceFactory}
     *
     * @param executorServiceFactory the executor thread factory.
     */
    public OpcUaOperations(ExecutorServiceFactory executorServiceFactory) {
        super(executorServiceFactory);
    }

    /**
     * Standard constructor. Uses a single threaded worker.
     */
    public OpcUaOperations() {
        this(SingleThreadedExecutorServiceFactory.instance());
    }

    /**
     * Check if the underlying connection to the com server is still alive.
     * Reconnects as well in case the autoreconnect has been set to true
     */
    private synchronized void checkAlive() {
        ConnectionState connectionState = getConnectionState();
        if (client != null && (connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.CONNECTED)) {
            boolean inError = false;
            try {
                // Get a typed reference to the Server object: ServerNode
                ServerState serverState = client.getAddressSpace().
                        getObjectNode(Identifiers.Server, ServerNode.class)
                        .thenCompose(ServerNode::getServerStatusNode)
                        .thenCompose(ServerStatusNode::getState)
                        .get();

                if (serverState == null || !ServerState.Running.equals(serverState)) {
                    logger.warn("Server is no more running but rather is in state {}", serverState);
                    inError = true;
                }
            } catch (Exception e) {
                logger.error("Unable to read server state. Marking as disconnected", e);
                inError = true;
            }

            if (inError) {
                disconnect();
            } else {
                getStateAndSet(Optional.of(ConnectionState.CONNECTED));
            }

        }
    }

    /**
     * Build an opc ua configuration from a connection profile.
     *
     * @param connectionProfile the connection profile.
     * @return the OPC-UA client.
     */
    private OpcUaClientConfigBuilder clientConfig(OpcUaConnectionProfile connectionProfile) {
        OpcUaClientConfigBuilder ret = new OpcUaClientConfigBuilder()
                .setApplicationName(LocalizedText.english(connectionProfile.getClientName()))
                .setApplicationUri(connectionProfile.getClientIdUri())
                .setEndpoint(findMatchingEndpoint(
                        discoverEndpoints(connectionProfile.getConnectionUri().toString(), connectionProfile.getSocketTimeout()),
                        connectionProfile.getSecureChannelEncryption() != null ? null : SecurityPolicy.None)
                        .orElseThrow(() -> new OpcException("Unable to find a matching endpoint. Please check server requirements")))
                .setRequestTimeout(UInteger.valueOf(connectionProfile.getSocketTimeout().toMillis()))
                .setIdentityProvider(resolveIdentityProvider(connectionProfile.getCredentials())
                        .orElseThrow(() -> new OpcException("Unrecognised Credentials " + connectionProfile.getCredentials())));

        // Set secure layer certificates if required
        if (connectionProfile.getSecureChannelEncryption() != null) {
            X509Credentials x509 = connectionProfile.getSecureChannelEncryption();
            ret.setCertificate(x509.getCertificate());
            ret.setKeyPair(new KeyPair(x509.getCertificate().getPublicKey(), x509.getPrivateKey()));
        }

        return ret;
    }

    /**
     * Select the {@link IdentityProvider} to use according the provided {@link Credentials}
     *
     * @param credentials the credentials.
     * @return the identity provider matching the credentials or empty if the mapping is not possible.
     */
    private Optional<IdentityProvider> resolveIdentityProvider(Credentials credentials) {
        IdentityProvider ret = null;
        if (credentials == null || credentials == Credentials.ANONYMOUS_CREDENTIALS) {
            ret = new AnonymousProvider();
        } else if (credentials instanceof UsernamePasswordCredentials) {
            ret = new UsernameProvider(((UsernamePasswordCredentials) credentials).getUser(),
                    ((UsernamePasswordCredentials) credentials).getPassword());
        } else if (credentials instanceof X509Credentials) {
            ret = new X509IdentityProvider(((X509Credentials) credentials).getCertificate(),
                    ((X509Credentials) credentials).getPrivateKey());
        }
        return Optional.ofNullable(ret);
    }

    /**
     * Find the best matching endpoint according to the security level requirements.
     *
     * @param endpoints    the list of available endpoints.
     * @param targetPolicy the exact match. (can be null)
     * @return an exact match with targetPolicy if not null or the best secured one if targetPolicy is null
     */
    private Optional<EndpointDescription> findMatchingEndpoint(Collection<EndpointDescription> endpoints,
                                                               SecurityPolicy targetPolicy) {
        return endpoints.stream()
                .filter(endpoint -> targetPolicy == null ||
                        targetPolicy.equals(SecurityPolicy.fromUriSafe(endpoint.getSecurityPolicyUri()).orElse(null)))
                .sorted(Comparator.comparing(EndpointDescription::getSecurityLevel).reversed())
                .findFirst();
    }

    /**
     * Discovers endpoints from an OPC-UA server
     *
     * @param serverUrl the server url
     * @param timeout   the request timeout
     * @return a never null list of {@link EndpointDescription}. Empty in case of issues.
     */
    private Collection<EndpointDescription> discoverEndpoints(String serverUrl, Duration timeout) {
        Collection<EndpointDescription> ret = Collections.emptyList();
        try {
            logger.info("Discovering OCP-UA endpoints from {}", serverUrl);
            EndpointDescription[] response = UaTcpStackClient.getEndpoints(serverUrl).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (response == null || response.length == 0) {
                logger.warn("Received empty endpoint descriptions from {}", serverUrl);
            } else {
                logger.warn("Received {} endpoint descriptions from {}", response.length, serverUrl);
                ret = Arrays.asList(response);
            }
        } catch (Exception e) {
            logger.error("Unexpected error while discovering OPC-UA endpoints from " + serverUrl, e);
        }
        return ret;
    }

    private String beautifyEndpoint(EndpointDescription endpointDescription) {
        X509Certificate serverCertificate = null;
        if (endpointDescription.getServerCertificate().isNotNull()) {
            try {
                serverCertificate = CertificateUtil.decodeCertificate(endpointDescription.getServerCertificate().bytes());
            } catch (UaException e) {
                logger.warn("Unable to decode server certificate", e);
            }
        }
        return String.format("Server: %s\n" +
                        "Url: %s\n" +
                        "Security policy: %s\n" +
                        "Server identity: %s",
                endpointDescription.getServer(),
                endpointDescription.getEndpointUrl(),
                endpointDescription.getSecurityPolicyUri(),
                serverCertificate);
    }

    @Override
    public boolean isChannelSecured() {
        if (client == null || !getConnectionState().equals(ConnectionState.CONNECTED)) {
            throw new OpcException("Cannot state security on non established link. Please connect first");
        }
        if (client.getStackClient().getEndpoint().isPresent()) {
            return !SecurityPolicy.None.equals(SecurityPolicy.fromUriSafe(
                    client.getStackClient().getEndpoint().get().getSecurityPolicyUri()).orElse(null));
        }
        return false;
    }

    @Override
    public void connect(OpcUaConnectionProfile connectionProfile) {
        if (connectionProfile == null || connectionProfile.getCredentials() == null ||
                connectionProfile.getConnectionUri() == null) {
            throw new OpcException("Please provide any valid non null connection profile with valid credentials");
        }

        ConnectionState cs = getConnectionState();
        if (cs != ConnectionState.DISCONNECTED) {
            throw new OpcException("There is already an active connection. Please disconnect first");
        }
        try {
            getStateAndSet(Optional.of(ConnectionState.CONNECTING));
            OpcUaClientConfig config = clientConfig(connectionProfile).build();
            logger.info("Connecting to OPC-UA endpoint\n{}", beautifyEndpoint(config.getEndpoint().get()));
            client = new OpcUaClient(config);
            //block until connected
            client.connect().get();
            scheduler = executorServiceFactory.createScheduler();
            scheduler.scheduleWithFixedDelay(this::checkAlive, 0, 1, TimeUnit.SECONDS);
            getStateAndSet(Optional.of(ConnectionState.CONNECTED));
        } catch (Exception e) {
            try {
                disconnect();
            } finally {
                throw new OpcException("Unexpected exception occurred while connecting", e);
            }
        }
    }

    @Override
    public void disconnect() {
        try {
            getStateAndSet(Optional.of(ConnectionState.DISCONNECTING));
            scheduler.shutdown();
            //cleanup here
            client.disconnect().get();
        } catch (Exception e) {
            throw new OpcException("Unable to properly disconnect", e);
        } finally {
            getStateAndSet(Optional.of(ConnectionState.DISCONNECTED));
            scheduler = null;
            client = null;
        }

    }

    /**
     * Internally browse the whole tag tree with a DFS algorithm.
     * It retains only {@link VariableNode} objects.
     *
     * @param path        the root path.
     * @param prevTagInfo the previous found tag (only if coming from -1 level.) This is used to decorate
     *                    a variable node tag with properties found in level +1.
     * @param result      the result collection container.
     * @throws Exception in case of any issue.
     */
    private void browse(Stack<Node> path, OpcTagInfo prevTagInfo, Collection<OpcTagInfo> result) throws Exception {
        Node n = path.isEmpty() ? null : path.peek();
        NodeId current = n == null ? Identifiers.RootFolder : n.getNodeId().get();

        OpcTagInfo currentTagInfo = null;

        if (n != null && n instanceof VariableNode) {

            UaVariableNode vn = (UaVariableNode) n;

            final NodeId nodeId = vn.getNodeId().get();
            VariableTypeNode vtn;

            try {
                vtn = vn.getTypeDefinition().get();
            } catch (Exception e) {
                logger.warn("Unable to resolve property type for {}. Defaulting to BaseDataVariableType", nodeId);
                vtn = client.getAddressSpace().createVariableTypeNode(Identifiers.BaseDataVariableType);
            }


            if (prevTagInfo != null && Identifiers.PropertyType.equals(vtn.getNodeId().get())) {
                OpcTagInfo info = fillOpcTagInformation(new OpcTagInfo(nodeId.toParseableString()), vn);

                prevTagInfo.addProperty(new OpcTagProperty<>(info.getId(),
                        info.getDescription().orElse(info.getName()),
                        vn.getValue().exceptionally(e -> null).thenApply(UaVariantMarshaller::toJavaType).get()));


            } else {
                Optional<Class<?>> cls = UaVariantMarshaller.findJavaClass(client, n.getNodeId().get());
                if (cls.isPresent()) {
                    currentTagInfo = new OpcTagInfo(nodeId.toParseableString())
                            .withGroup(path.stream().limit(path.size() - 1)
                                    .map(theNode -> {
                                        try {
                                            return theNode.getBrowseName().get().getName();
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    }).collect(Collectors.joining(".")))
                            .withName(path.peek().getBrowseName().get().getName())
                            .withType(cls.get());
                    result.add(fillOpcTagInformation(currentTagInfo, vn));
                }

            }

        }

        //browse next

        List<Node> nodes = client.getAddressSpace().browse(current).get();
        if (nodes != null && !nodes.isEmpty())

        {
            for (Node child : nodes) {
                path.push(child);
                browse(path, currentTagInfo, result);
                path.pop();
            }
        }

    }


    @Override
    public Collection<OpcTagInfo> browseTags() {
        Set<OpcTagInfo> ret = new LinkedHashSet<>();
        try {
            browse(new Stack<>(), null, ret);
        } catch (Exception e) {
            throw new OpcException("Unable to browse tags", e);
        }
        return ret;
    }

    /**
     * Introspects a variable node and return structured information.
     *
     * @param info the opc tag container.
     * @param vn   the {@link VariableNode} to introspect.
     * @return the info value.
     */
    private OpcTagInfo fillOpcTagInformation(OpcTagInfo info, VariableNode vn) {
        try {
            //final VariableNode vn = client.getAddressSpace().createVariableNode(ref.getNodeId().local().get());

            CompletableFuture.allOf(
                    vn.getMinimumSamplingInterval().exceptionally(e -> null).whenCompleteAsync((d, e) -> {
                        info.setScanRate(Optional.ofNullable(
                                d != null && d > 0.0 ? Duration.ofNanos(Math.round(d * 1e6)) : null));
                        if (d != null) {
                            info.addProperty(new OpcTagProperty<>(Integer.toString(AttributeId.MinimumSamplingInterval.id()),
                                    AttributeId.MinimumSamplingInterval.toString(),
                                    d));
                        }

                    }),
                    vn.getHistorizing().exceptionally(e -> null).whenCompleteAsync((historizing, e) -> info.addProperty(new OpcTagProperty<>(
                            Integer.toString(AttributeId.Historizing.id()), AttributeId.Historizing.toString(), historizing == null ? false : historizing))),
                    vn.getDescription().exceptionally(e -> null).whenCompleteAsync((description, e1) -> {
                        try {
                            LocalizedText displayName = vn.getDescription().exceptionally(e -> null).get();
                            String toSet = null;
                            if (description != null && description.getText() != null) {
                                toSet = description.getText();
                                info.addProperty(new OpcTagProperty<>(Integer.toString(AttributeId.Description.id()),
                                        AttributeId.Description.toString(),
                                        description.getText()));
                            }
                            if (displayName != null && displayName.getText() != null) {
                                info.addProperty(new OpcTagProperty<>(Integer.toString(AttributeId.DisplayName.id()),
                                        AttributeId.DisplayName.toString(),
                                        displayName.getText()));
                                if (toSet == null) {
                                    toSet = displayName.getText();
                                }
                            }
                            info.setDescription(Optional.ofNullable(toSet));

                        } catch (Exception e2) {
                            //just swallow
                        }
                    }),
                    vn.getAccessLevel().exceptionally(e -> null).whenCompleteAsync((accessLevel, e) -> {
                        if (accessLevel != null) {
                            EnumSet<AccessLevel> levels = AccessLevel.fromMask(accessLevel);
                            info.withReadAccessRights(levels.contains(AccessLevel.CurrentRead));
                            info.withWriteAccessRights(levels.contains(AccessLevel.CurrentWrite));
                            //set the mask for more advanced usages
                            info.addProperty(new OpcTagProperty<>(Integer.toString(AttributeId.AccessLevel.id()), AttributeId.AccessLevel.toString(), accessLevel.intValue()));
                        }
                    })
            ).get();

        } catch (Exception e) {
            logger.warn("Unable to properly fill information for tag " + vn, e);
        }
        return info;
    }


    @Override
    public OpcUaSession createSession(OpcUaSessionProfile sessionProfile) {
        return null;
    }

    @Override
    public void releaseSession(OpcUaSession session) {

    }


    @Override
    public void close() throws Exception {

    }
}