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

import io.reactivex.Flowable;
import io.reactivex.Single;

import java.time.Duration;
import java.util.List;

/**
 * Represents a session to manipulate a group of tags.
 * Multiple sessions may share a single connection (multiplexing).
 *
 * @author amarziali
 */
public interface OpcSession extends AutoCloseable {

    /**
     * Synchronously reads a list of tags and return as soon as possible.
     * May throw {@link com.hurence.opc.exception.OpcException} in case of issues.
     *
     * @param tags the list of tags.
     * @return the values that have been read.
     */
    Single<List<OpcData>> read(String... tags);

    /**
     * Synchronously writes a list of tags and return as soon as possible.
     * May throw {@link com.hurence.opc.exception.OpcException} in case of issues.
     *
     * @param data the data to be written.
     * @return the status of each write operation.
     */
    Single<List<OperationStatus>> write(OpcData... data);

    /**
     * Continuously read a stream of data for a tag.
     * When stream is requested, a subscription is done and values are output only in case they change.
     * May throw {@link com.hurence.opc.exception.OpcException} in case of issues. In this case the streaming will be interrupted.
     *
     * @param tagId            the tas to be read.
     * @param samplingInterval the sampling interval.
     * @return a {@link Flowable} stream of {@link OpcData}
     */
    Flowable<OpcData> stream(String tagId, Duration samplingInterval);
}
