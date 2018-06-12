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
import java.util.Objects;
import java.util.Optional;

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
    private int quality;
    /**
     * The value of the data.
     */
    private T value;

    /**
     * Read/Write error code. Codes are generated server side.
     */
    private Optional<Integer> errorCode = Optional.empty();

    /**
     * Default ctor.
     */
    public OpcData() {

    }


    /**
     * Construct an object with parameters.
     *
     * @param tag       the tag (item) id.
     * @param timestamp the timestamp of last data change.
     * @param quality   the quality of the data (set by the server depending on its caching policies.
     * @param value     the value.
     */
    public OpcData(String tag, Instant timestamp, int quality, T value) {
        this.tag = tag;
        this.timestamp = timestamp;
        this.quality = quality;
        this.value = value;
    }

    /**
     * Construct an object with parameters.
     *
     * @param tag       the tag (item) id.
     * @param timestamp the timestamp of last data change.
     * @param quality   the quality of the data (set by the server depending on its caching policies.
     * @param value     the value.
     * @param errorCode the optional error code. Can be null.
     */
    public OpcData(String tag, Instant timestamp, int quality, T value, Integer errorCode) {
        this.tag = tag;
        this.timestamp = timestamp;
        this.quality = quality;
        this.value = value;
        this.errorCode = Optional.ofNullable(errorCode);
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

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public Optional<Integer> getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Optional<Integer> errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpcData<?> opcData = (OpcData<?>) o;
        return quality == opcData.quality &&
                Objects.equals(tag, opcData.tag) &&
                Objects.equals(timestamp, opcData.timestamp) &&
                Objects.equals(value, opcData.value) &&
                Objects.equals(errorCode, opcData.errorCode);
    }

    @Override
    public int hashCode() {

        return Objects.hash(tag, timestamp, quality, value, errorCode);
    }

    @Override
    public String toString() {
        return "OpcData{" +
                "tag='" + tag + '\'' +
                ", timestamp=" + timestamp +
                ", quality=" + quality +
                ", value=" + value +
                ", errorCode=" + errorCode +
                '}';
    }
}
