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

package com.hurence.opc.ua;

import com.hurence.opc.SessionProfile;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * OPC-UA {@link SessionProfile}
 *
 * @author amarziali
 */
public class OpcUaSessionProfile extends SessionProfile<OpcUaSessionProfile> {

    /**
     * A map holding polling information about every tag that should be read.
     */
    private final Map<String, Duration> pollingMap = new HashMap<>();

    /**
     * The default polling interval.
     */
    private Duration defaultPollingInterval;

    /**
     * Get the mapping between a tag and it's polling interval.
     *
     * @return a {@link Map}
     */
    public Map<String, Duration> getPollingMap() {
        return Collections.unmodifiableMap(pollingMap);
    }

    /**
     * Sets the polling map with the value of the (non-nullable) pollingMap.
     *
     * @param pollingMap the {@link Map} containing the mapping to set.
     * @return itself.
     */
    public OpcUaSessionProfile withPollingMapFrom(Map<String, Duration> pollingMap) {
        pollingMap.putAll(pollingMap);
        return this;
    }

    /**
     * Add a mapping to the polling map.
     *
     * @param tag             the tag to map
     * @param pollingInterval the polling interval to be linked to the tag
     * @return itself.
     */
    public OpcUaSessionProfile addToPollingMap(String tag, Duration pollingInterval) {
        pollingMap.put(tag, pollingInterval);
        return this;
    }

    /**
     * Gets the default polling interval.
     *
     * @return a {@link Duration}. Can be null.
     */
    public Duration getDefaultPollingInterval() {
        return defaultPollingInterval;
    }


    /**
     * Set the default polling interval.
     * <p>
     * This value will be used in case a tag has no specific mapping in the polling table.
     * If the value is not set, the base refresh period {@link SessionProfile#refreshPeriod} will be used.
     *
     * @param defaultPollingInterval the default polling interval
     */
    public void setDefaultPollingInterval(Duration defaultPollingInterval) {
        this.defaultPollingInterval = defaultPollingInterval;
    }

    /**
     * Set the default polling interval.
     * <p>
     * This value will be used in case a tag has no specific mapping in the polling table.
     * If the value is not set, the base refresh period {@link SessionProfile#refreshPeriod} will be used.
     *
     * @param defaultPollingInterval the default polling interval
     * @return itself.
     */
    public OpcUaSessionProfile withDefaultPollingInterval(Duration defaultPollingInterval) {
        setDefaultPollingInterval(defaultPollingInterval);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpcUaSessionProfile that = (OpcUaSessionProfile) o;
        return Objects.equals(pollingMap, that.pollingMap) &&
                Objects.equals(defaultPollingInterval, that.defaultPollingInterval);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pollingMap, defaultPollingInterval);
    }

    @Override
    public String toString() {
        return "OpcUaSessionProfile{" +
                "pollingMap=" + pollingMap +
                ", defaultPollingInterval=" + defaultPollingInterval +
                "} " + super.toString();
    }
}
