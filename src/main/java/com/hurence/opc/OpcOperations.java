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
import java.util.stream.Stream;

/**
 * Base Interface to describe OPC releated operations
 *
 * @author amarziali
 */
public interface OpcOperations<T extends ConnectionProfile<T>, U extends OpcData> {

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
    Collection<String> browseTags();

    /**
     * Synchronously reads a list of tags and return as soon as possible.
     * May throw {@link com.hurence.opc.exception.OpcException} in case of issues.
     *
     * @param tags the list of tags.
     * @return the values that have been read.
     */
    Collection<U> read(String... tags);

    /**
     * Synchronously writes a list of tags and return as soon as possible.
     * May throw {@link com.hurence.opc.exception.OpcException} in case of issues.
     *
     * @param data the data to be written.
     * @return true if operation is successful, false otherwise
     */
    boolean write(U... data);

    /**
     * Continuously read a stream of tags.
     * May throw {@link com.hurence.opc.exception.OpcException} in case of issues. In this case the streaming will be interrupted.
     *
     * @param tags the tags to be read.
     * @return a java {@link Stream}.
     */
    Stream<U> stream(String... tags);

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
