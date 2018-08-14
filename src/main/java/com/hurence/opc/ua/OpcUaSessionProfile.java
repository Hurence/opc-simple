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

/**
 * OPC-UA {@link SessionProfile}
 *
 * @author amarziali
 */
public class OpcUaSessionProfile extends SessionProfile<OpcUaSessionProfile> {

    /**
     * The data publication interval (we ask the server to publish at this rate).
     */
    private Duration defaultPublicationInterval = Duration.ofSeconds(1);

    /**
     * Get The data publication interval (we ask the server to publish at this rate).
     *
     * @return a {@link Duration}
     */
    public Duration getDefaultPublicationInterval() {
        return defaultPublicationInterval;
    }

    /**
     * Set data publication interval (we ask the server to publish at this rate).
     *
     * @param defaultPublicationInterval the never null publication interval.
     */
    public void setDefaultPublicationInterval(Duration defaultPublicationInterval) {
        this.defaultPublicationInterval = defaultPublicationInterval;
    }

    /**
     * Set data publication interval (we ask the server to publish at this rate).
     *
     * @param defaultPublicationInterval the never null publication interval.
     * @return itself.
     */
    public OpcUaSessionProfile withDefaultPublicationInterval(Duration defaultPublicationInterval) {
        setDefaultPublicationInterval(defaultPublicationInterval);
        return this;
    }

    @Override
    public String toString() {
        return "OpcUaSessionProfile{" +
                "defaultPublicationInterval=" + defaultPublicationInterval +
                "} " + super.toString();
    }
}
