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

package com.hurence.opc;

import io.reactivex.Completable;
import io.reactivex.Observable;

import java.util.Collection;

/**
 * Base Interface to describe OPC releated operations
 *
 * @author amarziali
 */
public interface OpcOperations<T extends ConnectionProfile, U extends SessionProfile, V extends OpcSession>
        extends AutoCloseable {

    /**
     * Establish a connection to an OPC server.
     * May throw {@link com.hurence.opc.exception.OpcException} in case of issues.
     *
     * @param connectionProfile the connection information
     */
    Completable connect(T connectionProfile);

    /**
     * Disconnects from the OPC server.
     */
    Completable disconnect();


    /**
     * Check whenever the connection has been established under a secure layer (e.g. ssl).
     *
     * @return true if the connection transport layer can be considered as secure. False otherwise.
     */
    boolean isChannelSecured();

    /**
     * Retrieves observable connected to the state of the current connection.
     *
     * @return the {@link ConnectionState} as an {@link Observable}
     */
    Observable<ConnectionState> getConnectionState();

    /**
     * Retrieves the list of tags.
     * May throw {@link com.hurence.opc.exception.OpcException} in case of issues.
     *
     * @return a never null {@link Collection}
     */
    Collection<OpcTagInfo> browseTags();

    /**
     * Inspects the OPC tree starting from the provided tree (empty is the root) and returns only the next level.
     * May throw {@link com.hurence.opc.exception.OpcException} in case of issues.
     *
     * @param rootTagId the root tag to begin exploring from.
     *
     * @return a never null {@link Collection} of {@link OpcObjectInfo} (may also be {@link OpcTagInfo} in case is a leaf)
     */
    Collection<OpcObjectInfo> fetchNextTreeLevel(String rootTagId);

    /**
     * Fetch metadata of provided tags.
     * May throw {@link com.hurence.opc.exception.OpcException} in case of issues.
     *
     * @param tagIds the id of tags to fetch.
     * @return a never null {@link Collection}
     */
    Collection<OpcTagInfo> fetchMetadata(String... tagIds);

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



}
