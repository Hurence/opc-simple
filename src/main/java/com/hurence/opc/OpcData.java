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

import java.time.Instant;

/**
 * OPC data model.
 *
 * @author amarziali
 */
public class OpcData<T> {


    /**
     * The tag (item) id.
     */
    private String tag;
    /**
     * The timestamp of last data change. Can virtually track changes up to nanoseconds.
     */
    private Instant timestamp;
    /**
     * The quality of data. Value is server dependent. It's meaningful if you read data not directly from a device but
     * rather from the server cache (default mode).
     */
    private Quality quality;
    /**
     * The value of the data.
     */
    private T value;

    /**
     * The status of the operation that generated this data.
     */
    private OperationStatus operationStatus;

    /**
     * Default ctor.
     */
    public OpcData() {

    }


    /**
     * Construct an object with parameters (useful for write).
     *
     * @param tag       the tag (item) id.
     * @param timestamp the timestamp of last data change.
     * @param value     the value.
     */
    public OpcData(String tag, Instant timestamp, T value) {
        this.tag = tag;
        this.timestamp = timestamp;
        this.value = value;
    }


    /**
     * Construct an object with parameters.
     *
     * @param tag             the tag (item) id.
     * @param timestamp       the timestamp of last data change.
     * @param quality         the quality of the data (set by the server).
     * @param value           the value.
     * @param operationStatus the status of the operation that generated the data.
     */
    public OpcData(String tag, Instant timestamp, Quality quality, T value, OperationStatus operationStatus) {
        this.tag = tag;
        this.timestamp = timestamp;
        this.quality = quality;
        this.value = value;
        this.operationStatus = operationStatus;
    }


    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public Quality getQuality() {
        return quality;
    }

    public void setQuality(Quality quality) {
        this.quality = quality;
    }

    public OperationStatus getOperationStatus() {
        return operationStatus;
    }

    public void setOperationStatus(OperationStatus operationStatus) {
        this.operationStatus = operationStatus;
    }

    @Override
    public String toString() {
        return "OpcData{" +
                "tag='" + tag + '\'' +
                ", timestamp=" + timestamp +
                ", quality=" + quality +
                ", value=" + value +
                ", operationStatus=" + operationStatus +
                '}';
    }
}
