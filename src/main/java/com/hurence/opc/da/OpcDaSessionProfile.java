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

import com.hurence.opc.SessionProfile;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@link SessionProfile} with OPC-DA customizations.
 *
 * @author amarziali
 */
public class OpcDaSessionProfile extends SessionProfile<OpcDaSessionProfile> {

    /**
     * If set, server cache will be ignored and the value will be read directly from the device. Defaults to false.
     */
    private boolean directRead;


    /**
     * The item requested refresh interval. The server may not support as low as you set.
     * In this case the value will be refreshed at the server rate.
     * If you need a very low refresh delay, please consider use direct read mode.
     * Defaults to 1 second.
     */
    private Duration refreshInterval = Duration.ofSeconds(1);

    /**
     * The client can negotiate with the server which data type should be used for a tag.
     */
    private Map<String, Short> dataTypeOverrideMap = new HashMap<>();

    /**
     * Forces a datatype for a tag.
     *
     * @param tagId    the tag id.
     * @param dataType the data type code (See {@link org.jinterop.dcom.core.JIVariant}
     * @return itself
     */
    public OpcDaSessionProfile withDataTypeForTag(String tagId, short dataType) {
        dataTypeOverrideMap.put(tagId, dataType);
        return this;
    }

    /**
     * Get the data type for a certain tag.
     *
     * @param tagId the tag id
     * @return the forced data type if set. Otherwise an empty {@link Optional}
     */
    public Optional<Short> dataTypeForTag(String tagId) {
        return Optional.ofNullable(dataTypeOverrideMap.get(tagId));
    }

    /**
     * Get the whole data override mapping.
     *
     * @return an unmodifiable map.
     */
    public Map<String, Short> getDataTypeOverrideMap() {
        return Collections.unmodifiableMap(dataTypeOverrideMap);
    }

    /**
     * Gets the refresh interval.
     *
     * @return a never null {@link Duration}
     */
    public final Duration getRefreshInterval() {
        return refreshInterval;
    }

    /**
     * Sets the refresh interval.
     *
     * @param refreshInterval the refresh interval (non null).
     */
    public final void setRefreshInterval(Duration refreshInterval) {
        if (refreshInterval == null) {
            throw new IllegalArgumentException("The refresh interval must be any non null valid value.");
        }
        this.refreshInterval = refreshInterval;
    }

    /**
     * Sets the refresh interval.
     *
     * @param refreshInterval the refresh interval (non null).
     * @return itself.
     */
    public final OpcDaSessionProfile withRefreshInterval(Duration refreshInterval) {
        setRefreshInterval(refreshInterval);
        return this;
    }

    public boolean isDirectRead() {
        return directRead;
    }

    public void setDirectRead(boolean directRead) {
        this.directRead = directRead;
    }

    public OpcDaSessionProfile withDirectRead(boolean directRead) {
        setDirectRead(directRead);
        return this;
    }

    @Override
    public String toString() {
        return "OpcDaSessionProfile{" +
                "directRead=" + directRead +
                ", refreshInterval=" + refreshInterval +
                ", dataTypeOverrideMap=" + dataTypeOverrideMap +
                "} " + super.toString();
    }
}
