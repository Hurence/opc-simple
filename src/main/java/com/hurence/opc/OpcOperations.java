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

package com.hurence.opc;

import java.util.Collection;

/**
 * Base Interface to describe OPC releated operations
 *
 * @author amarziali
 */
public interface OpcOperations<T extends ConnectionProfile, U extends SessionProfile, V extends OpcSession> {

    /**
     * Establish a connection to an OPC server.
     * May throw {@link com.hurence.opc.exception.OpcException} in case of issues.
     *
     * @param connectionProfile the connection information
     */
    void connect(T connectionProfile);

    /**
     * Disconnects from the OPC server.
     */
    void disconnect();

    /**
     * Retrieves the state of the current connection.
     *
     * @return the {@link ConnectionState}
     */
    ConnectionState getConnectionState();

    /**
     * Retrieves the list of tags (or Items).
     * May throw {@link com.hurence.opc.exception.OpcException} in case of issues.
     *
     * @return a never null {@link Collection}
     */
    Collection<OpcTagInfo> browseTags();


    /**
     * Create a new {@link OpcSession} and attach to the current connection.
     * The session needs then to be released. See {@link OpcOperations#releaseSession(OpcSession)}
     *
     * @param sessionProfile the information about the session to be created.
     * @return the session.
     */
    V createSession(U sessionProfile);


    /**
     * Clear up the session and detatch it from the current session.
     *
     * @param session the session to be destroyed.
     */
    void releaseSession(V session);


    /**
     * Wait until the connection has been established.
     *
     * @return true if the connect operation was successful.
     */
    boolean awaitConnected();


    /**
     * Wait until the connection has been disconnected.
     *
     * @return true if the disconnect operation was successful.
     */
    boolean awaitDisconnected();


}
