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

import com.hurence.opc.ConnectionState;
import com.hurence.opc.OpcOperations;
import com.hurence.opc.OpcTagInfo;
import com.hurence.opc.OpcTagProperty;
import com.hurence.opc.exception.OpcException;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.*;
import org.openscada.opc.dcom.common.KeyedResult;
import org.openscada.opc.dcom.common.impl.EnumString;
import org.openscada.opc.dcom.da.OPCBROWSETYPE;
import org.openscada.opc.dcom.da.OPCSERVERSTATE;
import org.openscada.opc.dcom.da.OPCSERVERSTATUS;
import org.openscada.opc.dcom.da.PropertyDescription;
import org.openscada.opc.dcom.da.impl.OPCItemProperties;
import org.openscada.opc.dcom.da.impl.OPCServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * OPC-DA implementation for {@link OpcOperations}
 *
 * @author amarziali
 */
public class OpcDaOperations implements OpcOperations<OpcDaConnectionProfile, OpcDaSessionProfile, OpcDaSession> {

    private static final Logger logger = LoggerFactory.getLogger(OpcDaOperations.class);

    private JISession session;
    private JIComServer comServer;
    private OPCServer opcServer;
    private OPCItemProperties opcItemProperties;
    private ScheduledExecutorService scheduler = null;
    private volatile ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private final Set<OpcDaSession> sessions = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));


    /**
     * Atomically check a state and set next state.
     *
     * @param next of empty won't set anything.
     * @return
     */
    private synchronized ConnectionState getStateAndSet(Optional<ConnectionState> next) {
        ConnectionState ret = connectionState;
        if (next.isPresent()) {
            connectionState = next.get();
        }
        return ret;
    }


    /**
     * Check if the underlying connection to the com server is still alive.
     * Reconnects as well in case the autoreconnect has been set to true
     */
    private synchronized void checkAlive() {
        ConnectionState connectionState = getConnectionState();
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
    public void connect(OpcDaConnectionProfile connectionProfile) {
        if (connectionProfile == null) {
            throw new OpcException("Please provide any valid non null connection profile");
        }

        ConnectionState cs = getConnectionState();
        if (cs != ConnectionState.DISCONNECTED) {
            throw new OpcException("There is already an active connection. Please disconnect first");
        }
        try {
            getStateAndSet(Optional.of(ConnectionState.CONNECTING));
            if (connectionProfile.getComClsId() != null) {
                this.session = JISession.createSession(connectionProfile.getDomain(),
                        connectionProfile.getUser(), connectionProfile.getPassword());
                if (connectionProfile.getSocketTimeout() != null) {
                    this.session.setGlobalSocketTimeout((int) connectionProfile.getSocketTimeout().toMillis());
                }
                this.comServer = new JIComServer(JIClsid.valueOf(connectionProfile.getComClsId()),
                        connectionProfile.getHost(), this.session);
            } else if (connectionProfile.getComProgId() != null) {
                this.session = JISession.createSession(connectionProfile.getDomain(),
                        connectionProfile.getUser(), connectionProfile.getPassword());
                if (connectionProfile.getSocketTimeout() != null) {
                    this.session.setGlobalSocketTimeout((int) connectionProfile.getSocketTimeout().toMillis());
                }
                this.comServer = new JIComServer(JIProgId.valueOf(connectionProfile.getComProgId()),
                        connectionProfile.getHost(), this.session);
            } else {
                throw new IllegalArgumentException("Neither clsid nor progid is valid!");
            }


            opcServer = new OPCServer(comServer.createInstance());
            opcItemProperties = opcServer.getItemPropertiesService();
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleWithFixedDelay(() -> {
                checkAlive();

            }, 0, 1, TimeUnit.SECONDS);


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
            cleanup();
        } catch (Exception e) {
            throw new OpcException("Unable to properly disconnect", e);
        } finally {
            getStateAndSet(Optional.of(ConnectionState.DISCONNECTED));
        }
    }

    private void cleanup() {
        logger.info("Destroying DCOM session");
        if (scheduler != null) {
            scheduler.shutdown();
        }
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
            logger.info("Session properly cleaned up");

        } catch (JIException e) {
            throw new OpcException("Unable to properly destroy dcom session", e);
        } finally {
            opcItemProperties = null;
            comServer = null;
            session = null;
            opcServer = null;
            scheduler = null;
        }
    }

    @Override
    public ConnectionState getConnectionState() {
        return getStateAndSet(Optional.empty());
    }

    private String groupFromName(String tag) {
        int idx = tag.lastIndexOf('.');
        if (idx > 0) {
            return tag.substring(0, idx);
        }
        return "";
    }

    private String toggleNullTermination(String orig) {
        if (orig.endsWith("\u0000")) {
            return orig.substring(0, orig.length() - 1);
        }
        return orig;
    }

    @Override
    public Collection<OpcTagInfo> browseTags() {
        if (getConnectionState() != ConnectionState.CONNECTED) {
            throw new OpcException("Unable to browse tags. Not connected!");
        }
        try {
            EnumString res = opcServer.getBrowser().browse(OPCBROWSETYPE.OPC_FLAT, "", 0, JIVariant.VT_EMPTY);
            if (res != null) {
                return res.asCollection().stream()
                        .map(s -> {
                            OpcTagInfo ret;
                            try {
                                ret = new OpcTagInfo().withName(s).withGroup(groupFromName(s));

                                Map<Integer, PropertyDescription> properties = opcItemProperties.queryAvailableProperties(s)
                                        .stream().collect(Collectors.toMap(PropertyDescription::getId, Function.identity()));

                                for (KeyedResult<Integer, JIVariant> result : opcItemProperties.getItemProperties(s, properties.keySet().stream().mapToInt(Integer::intValue).toArray())) {
                                    if (result.getKey() == 1) {
                                        ret.setType(JIVariantMarshaller.findJavaClass(result.getValue().getObjectAsShort()));
                                    }
                                    ret.addProperty(new OpcTagProperty(result.getKey().toString(),
                                            toggleNullTermination(properties.get(result.getKey()).getDescription()),
                                            JIVariantMarshaller.toJavaType(result.getValue())));
                                }

                            } catch (JIException e) {
                                throw new OpcException("Unable to fetch metadata for tag " + s, e);
                            }
                            return ret;

                        }).collect(Collectors.toList());

            }
        } catch (Exception e) {
            throw new OpcException("Unable to browse tags", e);
        }

        return Collections.emptyList();
    }

    @Override
    public OpcDaSession createSession(OpcDaSessionProfile sessionProfile) {
        if (getConnectionState() != ConnectionState.CONNECTED) {
            throw new OpcException("Unable to create a session. Not connected!");
        }
        OpcDaSession ret = OpcDaSession.create(opcServer, sessionProfile.getRefreshPeriodMillis(), sessionProfile.isDirectRead());
        sessions.add(ret);
        return ret;

    }

    @Override
    public void releaseSession(OpcDaSession session) {
        if (getConnectionState() == ConnectionState.CONNECTED && session != null) {
            sessions.remove(session);
            session.cleanup(opcServer);
        }
    }


    @Override
    public boolean awaitConnected() {
        while (getConnectionState() != ConnectionState.CONNECTED) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean awaitDisconnected() {
        while (getConnectionState() != ConnectionState.DISCONNECTED) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return false;
            }
        }
        return true;
    }


}
