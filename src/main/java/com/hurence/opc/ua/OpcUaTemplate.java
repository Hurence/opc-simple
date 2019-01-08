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
import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.CompletableSubject;
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
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.*;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.util.CertificateUtil;
import org.eclipse.milo.opcua.stack.core.util.CryptoRestrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * OPC-UA implementation of {@link com.hurence.opc.OpcOperations}
 *
 * @author amarziali
 */
public class OpcUaTemplate extends AbstractOpcOperations<OpcUaConnectionProfile, OpcUaSessionProfile, OpcUaSession>
        implements OpcUaOperations {

    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(OpcUaTemplate.class);

    /**
     * Map of held sessions.
     */
    private final Set<OpcUaSession> sessions = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));


    static {
        CryptoRestrictions.remove();
        Security.addProvider(new BouncyCastleProvider());
    }


    /**
     * Milo UA client.
     */
    private OpcUaClient client;
    /**
     * The watcher task disposable.
     */
    private Disposable watcherTaskDisposable = Disposables.disposed();


    /**
     * Check if the underlying connection to the com server is still alive.
     */
    private synchronized void checkAlive() {
        ConnectionState connectionState = getConnectionState().blockingFirst();
        if (client != null && (connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.CONNECTED)) {
            boolean inError = false;
            try {
                // Get a typed reference to the Server object: ServerNode
                final DataValue serverState = client.readValue(0.0,
                        TimestampsToReturn.Neither, Identifiers.Server_ServerStatus_State)
                        .get(client.getConfig().getRequestTimeout().longValue(), TimeUnit.MILLISECONDS);

                if (serverState == null ||
                        !serverState.getStatusCode().isGood() ||
                        serverState.getValue() == null ||
                        serverState.getValue().isNull() ||
                        !serverState.getValue().getValue().equals(ServerState.Running.getValue())) {
                    logger.warn("Server is no more running but rather is in state {}", serverState);
                    inError = true;
                }
            } catch (Exception e) {
                logger.error("Unable to read server state. Marking as disconnected", e);
                inError = true;
            }

            if (inError) {
                disconnect().blockingAwait();
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
                        discoverEndpoints(connectionProfile.getConnectionUri().toString(),
                                Optional.ofNullable(connectionProfile.getSocketTimeout())),
                        connectionProfile.getSecureChannelEncryption() != null ? null : SecurityPolicy.None)
                        .orElseThrow(() -> new OpcException("Unable to find a matching endpoint. Please check server requirements")))
                .setIdentityProvider(resolveIdentityProvider(connectionProfile.getCredentials())
                        .orElseThrow(() -> new OpcException("Unrecognised Credentials " + connectionProfile.getCredentials())));
        if (connectionProfile.getSocketTimeout() != null) {
            ret.setRequestTimeout(UInteger.valueOf(connectionProfile.getSocketTimeout().toMillis()));
        }
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
    private Collection<EndpointDescription> discoverEndpoints(String serverUrl, Optional<Duration> timeout) {
        Collection<EndpointDescription> ret = Collections.emptyList();
        try {
            logger.info("Discovering OCP-UA endpoints from {}", serverUrl);
            EndpointDescription[] response = UaTcpStackClient.getEndpoints(serverUrl).get(timeout.orElse(Duration.ofSeconds(30)).toMillis(), TimeUnit.MILLISECONDS);
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
        if (client == null || !getConnectionState().blockingFirst().equals(ConnectionState.CONNECTED)) {
            throw new OpcException("Cannot state security on non established link. Please connect first");
        }
        return client.getStackClient().getEndpoint().isPresent() &&
                !SecurityPolicy.None.equals(SecurityPolicy.fromUriSafe(client.getStackClient().getEndpoint().get().getSecurityPolicyUri()).orElse(null));
    }

    @Override
    public Single<OpcUaOperations> connect(@Nonnull OpcUaConnectionProfile connectionProfile) {
        return Completable
                .fromAction(() -> doConnect(connectionProfile))
                .andThen(waitUntilConnected())
                .andThen(Single.just(this));
    }

    private void doConnect(OpcUaConnectionProfile connectionProfile) {

        if (connectionProfile == null || connectionProfile.getCredentials() == null ||
                connectionProfile.getConnectionUri() == null) {
            throw new OpcException("Please provide any valid non null connection profile with valid credentials");
        }

        ConnectionState cs = getConnectionState().blockingFirst();
        if (cs != ConnectionState.DISCONNECTED) {
            throw new OpcException("There is already an active connection. Please disconnect first");
        }
        try {
            getStateAndSet(Optional.of(ConnectionState.CONNECTING));
            OpcUaClientConfig config = clientConfig(connectionProfile).build();
            logger.info("Connecting to OPC-UA endpoint\n{}", beautifyEndpoint(config.getEndpoint().get()));
            client = new OpcUaClient(config);
            //block until connected
            client.connect().get(client.getConfig().getRequestTimeout().longValue(), TimeUnit.MILLISECONDS);
            watcherTaskDisposable.dispose();
            watcherTaskDisposable = Schedulers.io().schedulePeriodicallyDirect(
                    this::checkAlive, 0, connectionProfile.getKeepAliveInterval().toNanos(), TimeUnit.NANOSECONDS);
            getStateAndSet(Optional.of(ConnectionState.CONNECTED));
        } catch (Exception e) {
            try {
                disconnect().blockingAwait(connectionProfile.getSocketTimeout().toMillis(), TimeUnit.MILLISECONDS);
            } finally {
                throw new OpcException("Unexpected exception occurred while connecting", e);
            }
        }
    }

    @Override
    public Completable disconnect() {
        return CompletableSubject.fromAction(this::doDisconnect)
                .andThen(waitUntilDisconnected());
    }

    private void doDisconnect() {
        logger.info("Disconnecting now");
        getStateAndSet(Optional.of(ConnectionState.DISCONNECTING));
        try {
            //cleanup here
            while (!sessions.isEmpty()) {
                try {
                    OpcUaSession session = sessions.stream().findFirst().orElse(null);
                    if (session != null) {
                        sessions.remove(session);
                        session.close();
                    }
                } catch (Exception e) {
                    logger.warn("Unable to properly close a session", e);
                }
            }
            watcherTaskDisposable.dispose();
            if (client != null) {
                client.getSubscriptionManager().clearSubscriptions();
                client.disconnect().get(client.getConfig().getRequestTimeout().longValue(), TimeUnit.MILLISECONDS);


            }
        } catch (Exception e) {
            try {
                client.getStackClient().disconnect().get();
            } catch (Exception e1) {
                logger.error("Unable to force the disconnection. Client may be in a bad shape", e1);
            } finally {
                throw new OpcException("Unable to properly disconnect", e);
            }
        } finally {
            getStateAndSet(Optional.of(ConnectionState.DISCONNECTED));
            watcherTaskDisposable = Disposables.disposed();
            client = null;
        }
    }

    @Override
    public Flowable<OpcTagInfo> fetchMetadata(@Nonnull String... tagIds) {
        return Flowable.fromArray(tagIds)
                .flatMap(t -> Flowable.create(emitter -> {
                    try {
                        NodeId target = NodeId.parse(t);
                        Node node = client.getAddressSpace().createNode(target).get();

                        if (!(node instanceof VariableNode)) {
                            emitter.onError(new IllegalArgumentException("Tag " + t + " is not a Variable node"));
                        }

                        final NodeId nodeId = node.getNodeId().get();
                        browse(nodeId, null, emitter);
                        emitter.onComplete();
                    } catch (Exception e) {
                        emitter.onError(new OpcException("Unable to fetch metadata for tag " + t, e));
                    }
                }, BackpressureStrategy.MISSING));
    }


    /**
     * Internally browse the whole tag tree with a DFS algorithm.
     * It retains only {@link VariableNode} objects.
     *
     * @param root        the root path.
     * @param prevTagInfo the previous found tag (only if coming from -1 level.) This is used to decorate
     *                    a variable node tag with properties found in level +1.
     * @param subscriber  the subject who subscribed for this data.
     * @throws Exception in case of any issue.
     */
    private void browse(NodeId root, OpcTagInfo prevTagInfo, Emitter<? super OpcTagInfo> subscriber) throws Exception {
        Node n;
        try {
            n = client.getAddressSpace().createNode(root).get();
        } catch (Exception e) {
            logger.warn("Unable to read after tag {} : {}", prevTagInfo, e.getMessage());
            return;
        }

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
                            .withName(n.getBrowseName().get().getName())
                            .withType(cls.get());
                    subscriber.onNext(fillOpcTagInformation(currentTagInfo, vn));
                }

            }

        }
        //browse next
        List<Node> nodes = client.getAddressSpace().browse(root).get();
        if (nodes != null && !nodes.isEmpty()) {
            for (Node child : nodes) {
                try {
                    browse(child.getNodeId().get(), currentTagInfo, subscriber);
                } catch (Exception e) {
                    logger.warn("Skipping node {} because of an unexpected error: {}", child, e.getMessage());
                }
            }
        }

    }

    private Flowable<BrowseResult> doBrowseAll(@Nonnull BrowseResult previous) {
        if (previous != null && previous.getContinuationPoint() != null && previous.getContinuationPoint().isNotNull()) {
            return Flowable.merge(Flowable.just(previous),
                    Flowable.fromFuture(client.browseNext(true, previous.getContinuationPoint()).toCompletableFuture())
            ).flatMap(this::doBrowseAll);
        }
        return Flowable.just(previous);
    }


    @Override
    public Flowable<OpcObjectInfo> fetchNextTreeLevel(@Nonnull String rootTagId) {
        return Flowable.fromFuture(client.browse(new BrowseDescription(NodeId.parse(rootTagId), BrowseDirection.Forward,
                Identifiers.HierarchicalReferences, true,
                UInteger.valueOf(NodeClass.Object.getValue() | NodeClass.Variable.getValue()),
                UInteger.valueOf(BrowseResultMask.All.getValue()))))
                .flatMap(this::doBrowseAll)
                .flatMap(browseResult -> browseResult.getReferences() != null ?
                        Flowable.fromArray(browseResult.getReferences()) : Flowable.empty())
                .filter(referenceDescription -> !referenceDescription.getTypeDefinition().isLocal() ||
                        !referenceDescription.getTypeDefinition().local().get().equals(Identifiers.PropertyType))
                .map(referenceDescription -> (NodeClass.Object.equals(referenceDescription.getNodeClass()) ?
                        new OpcContainerInfo(referenceDescription.getNodeId().local().get().toParseableString()) :
                        new OpcTagInfo(referenceDescription.getNodeId().local().get().toParseableString()))
                        .withDescription(referenceDescription.getDisplayName().getText())
                        .withName(referenceDescription.getBrowseName().getName()));


    }

    @Override
    public Flowable<OpcTagInfo> browseTags() {
        return Flowable.create(emitter -> {
            try {
                browse(Identifiers.RootFolder, null, emitter);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(new OpcException("Unexpected exception while browsing tags", e));
            }
        }, BackpressureStrategy.MISSING);
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
                    vn.readMinimumSamplingInterval().exceptionally(e -> null).whenCompleteAsync((raw, e) -> {
                        Number d = (Number) UaVariantMarshaller.toJavaType(raw);
                        info.setScanRate(Optional.ofNullable(
                                d != null && d.doubleValue() > 0.0 ? Duration.ofNanos(Math.round(d.doubleValue() * 1e6)) : null));
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
                    vn.getUserAccessLevel().exceptionally(e -> null).whenCompleteAsync((accessLevel, e) -> {
                        if (accessLevel != null) {
                            EnumSet<AccessLevel> levels = AccessLevel.fromMask(accessLevel);
                            info.withReadAccessRights(levels.contains(AccessLevel.CurrentRead));
                            info.withWriteAccessRights(levels.contains(AccessLevel.CurrentWrite));
                            //set the mask for more advanced usages
                            info.addProperty(new OpcTagProperty<>(Integer.toString(AttributeId.UserAccessLevel.id()), AttributeId.UserAccessLevel.toString(), accessLevel.intValue()));
                        }
                    })
            ).get();

        } catch (Exception e) {
            logger.warn("Unable to properly fill information for tag " + vn, e);
        }
        return info;
    }


    @Override
    public Single<OpcUaSession> createSession(@Nonnull OpcUaSessionProfile sessionProfile) {
        return Single.fromCallable(() -> OpcUaSession.create(this, client, sessionProfile))
                .doOnSuccess(sessions::add);
    }

    @Override
    public Completable releaseSession(@Nonnull OpcUaSession session) {
        return Completable.fromRunnable(() -> {
            sessions.remove(session);
            session.cleanup();
        });
    }


    @Override
    public void close() throws Exception {
        disconnect()
                .doOnError(throwable -> logger.warn("Unexpected error while closing UA client", throwable))
                .onErrorComplete()
                .blockingAwait();
    }
}
