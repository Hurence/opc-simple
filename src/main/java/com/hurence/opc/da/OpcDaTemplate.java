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
import com.hurence.opc.auth.Credentials;
import com.hurence.opc.auth.NtlmCredentials;
import com.hurence.opc.exception.OpcException;
import com.hurence.opc.util.ExecutorServiceFactory;
import com.hurence.opc.util.SingleThreadedExecutorServiceFactory;
import io.reactivex.Completable;
import io.reactivex.subjects.CompletableSubject;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.*;
import org.openscada.opc.dcom.common.KeyedResult;
import org.openscada.opc.dcom.common.KeyedResultSet;
import org.openscada.opc.dcom.common.impl.EnumString;
import org.openscada.opc.dcom.da.*;
import org.openscada.opc.dcom.da.impl.OPCItemProperties;
import org.openscada.opc.dcom.da.impl.OPCServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * OPC-DA implementation for {@link OpcOperations}
 *
 * @author amarziali
 */
public class OpcDaTemplate extends AbstractOpcOperations<OpcDaConnectionProfile, OpcDaSessionProfile, OpcDaSession>
        implements OpcDaOperations {

    static {
        //enable surrogates auto-registration
        JISystem.setAutoRegisteration(true);
        JISystem.setJavaCoClassAutoCollection(false);
    }

    private static final Logger logger = LoggerFactory.getLogger(OpcDaTemplate.class);

    private JISession session;
    private JIComServer comServer;
    private OPCServer opcServer;
    private OPCItemProperties opcItemProperties;
    private ScheduledExecutorService scheduler = null;
    private final Set<OpcDaSession> sessions = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));

    /**
     * Construct an instance with an {@link ExecutorServiceFactory}
     *
     * @param executorServiceFactory the executor thread factory.
     */
    public OpcDaTemplate(ExecutorServiceFactory executorServiceFactory) {
        super(executorServiceFactory);
    }

    /**
     * Standard constructor. Uses a single threaded worker.
     */
    public OpcDaTemplate() {
        this(SingleThreadedExecutorServiceFactory.instance());
    }

    /**
     * Check if the underlying connection to the com server is still alive.
     * Reconnects as well in case the autoreconnect has been set to true
     */
    private synchronized void checkAlive() {
        ConnectionState connectionState = getConnectionState().blockingFirst();
        if (opcServer != null && (connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.CONNECTED)) {
            boolean inError = false;
            try {
                OPCSERVERSTATUS status = opcServer.getStatus();

                if (status == null || status.getServerState() == null || !status.getServerState().equals(OPCSERVERSTATE.OPC_STATUS_RUNNING)) {
                    logger.warn("Server is no more running but rather is in state {}", status != null ? status.getServerState() : null);
                    inError = true;
                }
            } catch (JIException e) {
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

    @Override
    public Completable connect(OpcDaConnectionProfile connectionProfile) {
        return CompletableSubject.fromAction(() -> doConnect(connectionProfile))
                .andThen(waitUntilConnected());
    }

    public void doConnect(OpcDaConnectionProfile connectionProfile) {
        if (connectionProfile == null || connectionProfile.getConnectionUri() == null) {
            throw new OpcException("Please provide any valid non null connection profile");
        }

        Credentials credentials = connectionProfile.getCredentials();

        if (credentials != null && !(credentials instanceof NtlmCredentials)) {
            throw new OpcException("Credentials " + credentials.getClass().getCanonicalName() +
                    " is not supported by OPC-DA connector. Please use " + NtlmCredentials.class.getCanonicalName());
        }

        String username = ((NtlmCredentials) credentials).getUser();
        String password = ((NtlmCredentials) credentials).getPassword();
        String domain = ((NtlmCredentials) credentials).getDomain();


        ConnectionState cs = getConnectionState().blockingFirst();
        if (cs != ConnectionState.DISCONNECTED) {
            throw new OpcException("There is already an active connection. Please disconnect first");
        }
        try {
            getStateAndSet(Optional.of(ConnectionState.CONNECTING));
            //ugly: custom port not supported by UtGard since hardcoded?
            String connectionString = connectionProfile.getConnectionUri().getHost();
            if (connectionProfile.getComClsId() != null) {
                this.session = JISession.createSession(domain, username, password);
                if (connectionProfile.getSocketTimeout() != null) {
                    this.session.setGlobalSocketTimeout((int) connectionProfile.getSocketTimeout().toMillis());
                }
                this.comServer = new JIComServer(JIClsid.valueOf(connectionProfile.getComClsId()),
                        connectionString, this.session);
            } else if (connectionProfile.getComProgId() != null) {
                this.session = JISession.createSession(domain, username, password);
                if (connectionProfile.getSocketTimeout() != null) {
                    this.session.setGlobalSocketTimeout((int) connectionProfile.getSocketTimeout().toMillis());
                }
                this.comServer = new JIComServer(JIProgId.valueOf(connectionProfile.getComProgId()),
                        connectionString, this.session);
            } else {
                throw new IllegalArgumentException("Neither clsid nor progid is valid!");
            }


            opcServer = new OPCServer(comServer.createInstance());

            opcItemProperties = opcServer.getItemPropertiesService();
            scheduler = executorServiceFactory.createScheduler();
            scheduler.scheduleWithFixedDelay(this::checkAlive, 0, connectionProfile.getKeepAliveInterval().toNanos(), TimeUnit.NANOSECONDS);
        } catch (Exception e) {
            try {
                disconnect();
            } finally {
                throw new OpcException("Unexpected exception occurred while connecting", e);
            }
        }

    }

    @Override
    public Completable disconnect() {
        return CompletableSubject.fromAction(() -> doDisconnect())
                .andThen(waitUntilDisconnected());
    }

    public synchronized void doDisconnect() {
        try {
            getStateAndSet(Optional.of(ConnectionState.DISCONNECTING));
            if (scheduler != null) {
                scheduler.shutdown();
            }
            destroySessions();

        } catch (Exception e) {
            throw new OpcException("Unable to properly disconnect", e);
        } finally {
            cleanup();
            getStateAndSet(Optional.of(ConnectionState.DISCONNECTED));
        }
    }


    private void destroySessions() {
        logger.info("Destroying DCOM sessions");
        while (!sessions.isEmpty()) {
            try {
                OpcDaSession s = sessions.stream().findFirst().get();
                sessions.remove(s);
                s.cleanup(opcServer);
            } catch (Exception e) {
                logger.warn("Group not properly released", e);
            }
        }
        try {
            JISession.destroySession(session);

        } catch (Exception e) {
            throw new OpcException("Unable to properly destroy dcom session", e);
        }
    }

    private void cleanup() {
        opcItemProperties = null;
        comServer = null;
        session = null;
        opcServer = null;
        scheduler = null;
    }


    private String nameFromId(String tag) {
        int idx = tag.lastIndexOf('.');
        if (idx > 0) {
            return tag.substring(idx + 1);
        }
        return tag;
    }

    private String toggleNullTermination(String orig) {
        if (orig.endsWith("\u0000")) {
            return orig.substring(0, orig.length() - 1);
        }
        return orig;
    }

    private String sanitize(String orig) {
        if (orig.endsWith(Character.toString((char) 165))) {
            return orig.substring(0, orig.length() - 1);
        }
        return orig;
    }

    private <S, T> T extractFromProperty(OpcTagProperty<S> property, Function<S, T> transformer) {
        if (property != null) {
            return transformer.apply(property.getValue());
        }
        return null;
    }

    @Override
    public Collection<OpcTagInfo> fetchMetadata(String... tagIds) {
        if (getConnectionState().blockingFirst() != ConnectionState.CONNECTED) {
            throw new OpcException("Unable to fetch metadata. Not connected!");
        }
        return Arrays.stream(tagIds).map(s -> {
            OpcTagInfo ret;
            try {
                Map<Integer, PropertyDescription> properties = opcItemProperties.queryAvailableProperties(s)
                        .stream().collect(Collectors.toMap(PropertyDescription::getId, Function.identity()));

                ret = new OpcTagInfo(s).withName(nameFromId(s));

                KeyedResultSet<Integer, JIVariant> rawProps = opcItemProperties.getItemProperties(s,
                        properties.keySet().stream().mapToInt(Integer::intValue).toArray());
                Map<Integer, OpcTagProperty> tagProps = new HashMap<>();
                for (KeyedResult<Integer, JIVariant> result : rawProps) {
                    tagProps.put(result.getKey(), new OpcTagProperty<>(result.getKey().toString(),
                            toggleNullTermination(properties.get(result.getKey()).getDescription()),
                            JIVariantMarshaller.toJavaType(result.getValue())));
                }
                ret.setProperties(new HashSet<>(tagProps.values()));
                //set common properties
                if (tagProps.containsKey(OpcDaItemProperties.MANDATORY_DATA_TYPE)) {
                    OpcTagProperty<Short> tmp = tagProps.get(OpcDaItemProperties.MANDATORY_DATA_TYPE);
                    ret.setType(JIVariantMarshaller.findJavaClass(tmp != null && tmp.getValue() != null ? tmp.getValue() : JIVariant.VT_EMPTY));
                }

                ret.setScanRate(Optional.ofNullable(extractFromProperty(
                        (OpcTagProperty<Float>) tagProps.get(OpcDaItemProperties.MANDATORY_SERVER_SCAN_RATE),
                        (rate -> Duration.ofMillis(Math.round(rate))))));
                ret.setDescription(Optional.ofNullable((extractFromProperty(
                        (OpcTagProperty<String>) tagProps.get(OpcDaItemProperties.RECOMMENDED_ITEM_DESCRIPTION),
                        Function.identity()))));
                //access rights part
                Integer accessRightsBits = extractFromProperty(
                        (OpcTagProperty<Integer>) tagProps.get(OpcDaItemProperties.MANDATORY_ITEM_ACCESS_RIGHTS),
                        Function.identity());
                if (accessRightsBits != null) {
                    ret.withWriteAccessRights((accessRightsBits & OpcDaItemProperties.OPC_ACCESS_RIGHTS_WRITABLE) != 0);
                    ret.withReadAccessRights((accessRightsBits & OpcDaItemProperties.OPC_ACCESS_RIGHTS_READABLE) != 0);

                }

            } catch (JIException e) {
                throw new OpcException("Unable to fetch metadata for tag " + s, e);
            }

            return ret;
        }).collect(Collectors.toList());
    }


    private String resolveItemId(String name) {
        try {
            return toggleNullTermination(sanitize(opcServer.getBrowser().getItemID(name)));
        } catch (JIException e) {
            throw new OpcException("Unable to resolve ID for item name " + name, e);
        }
    }

    @Override
    public Collection<OpcObjectInfo> fetchNextTreeLevel(String rootTagId) {
        if (getConnectionState().blockingFirst() != ConnectionState.CONNECTED) {
            throw new OpcException("Unable to fetch tags. Not connected!");
        }
        synchronized (opcServer) {
            try {
                opcServer.getBrowser().changePosition(rootTagId, OPCBROWSEDIRECTION.OPC_BROWSE_TO);

                return Stream.concat(
                        opcServer.getBrowser().browse(OPCBROWSETYPE.OPC_BRANCH, "", 0, JIVariant.VT_EMPTY).asCollection().stream()
                                .map(s -> new OpcContainerInfo((resolveItemId(s))).withName(s)),
                        opcServer.getBrowser().browse(OPCBROWSETYPE.OPC_LEAF, "", 0, JIVariant.VT_EMPTY).asCollection().stream()
                                .map(s -> new OpcTagInfo(resolveItemId(s)).withName(s)))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                throw new OpcException("Unable to hierarchically browse the access space", e);
            }
        }
    }


    @Override
    public Collection<OpcTagInfo> browseTags() {
        if (getConnectionState().blockingFirst() != ConnectionState.CONNECTED) {
            throw new OpcException("Unable to browse tags. Not connected!");
        }
        synchronized (opcServer) {
            try {
                opcServer.getBrowser().changePosition(null, OPCBROWSEDIRECTION.OPC_BROWSE_TO);
                EnumString res = opcServer.getBrowser().browse(OPCBROWSETYPE.OPC_FLAT, "", 0, JIVariant.VT_EMPTY);
                if (res != null) {
                    Collection<String> result = res.asCollection();
                    return fetchMetadata(result.toArray(new String[result.size()]));

                }
            } catch (Exception e) {
                throw new OpcException("Unable to browse tags", e);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public OpcDaSession createSession(OpcDaSessionProfile sessionProfile) {
        if (getConnectionState().blockingFirst() != ConnectionState.CONNECTED) {
            throw new OpcException("Unable to create a session. Not connected!");
        }
        OpcDaSession ret = OpcDaSession.create(opcServer, sessionProfile, this);
        sessions.add(ret);
        return ret;

    }

    @Override
    public void releaseSession(OpcDaSession session) {
        if (getConnectionState().blockingFirst() == ConnectionState.CONNECTED && session != null) {
            sessions.remove(session);
            session.cleanup(opcServer);
        }
    }


    @Override
    public void close() throws Exception {
        disconnect();
    }
}
