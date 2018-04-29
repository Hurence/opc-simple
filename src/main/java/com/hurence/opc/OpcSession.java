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
 * Represents a session to manipulate a group of tags.
 * Multiple sessions may share a single connection (multiplexing).
 *
 * @author amarziali
 */
public interface OpcSession {

    /**
     * Synchronously reads a list of tags and return as soon as possible.
     * May throw {@link com.hurence.opc.exception.OpcException} in case of issues.
     *
     * @param tags the list of tags.
     * @return the values that have been read.
     */
    Collection<OpcData> read(String... tags);

    /**
     * Synchronously writes a list of tags and return as soon as possible.
     * May throw {@link com.hurence.opc.exception.OpcException} in case of issues.
     *
     * @param data the data to be written.
     * @return true if operation is successful, false otherwise
     */
    boolean write(OpcData... data);

    /**
     * Continuously read a stream of tags.
     * May throw {@link com.hurence.opc.exception.OpcException} in case of issues. In this case the streaming will be interrupted.
     *
     * @param tags the tags to be read.
     * @return a java {@link Stream}.
     */
    Stream<OpcData> stream(String... tags);
}
