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

package com.hurence.opc.ua;

import com.hurence.opc.SessionProfile;

import javax.annotation.Nonnull;
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
    private Duration publicationInterval = Duration.ofSeconds(1);

    /**
     * Get The data publication interval (we ask the server to publish at this rate).
     *
     * @return a {@link Duration}
     */
    public Duration getPublicationInterval() {
        return publicationInterval;
    }

    /**
     * Set data publication interval (we ask the server to publish at this rate).
     *
     * @param publicationInterval the never null publication interval.
     */
    public void setPublicationInterval(@Nonnull Duration publicationInterval) {
        this.publicationInterval = publicationInterval;
    }

    /**
     * Set data publication interval (we ask the server to publish at this rate).
     *
     * @param publicationInterval the never null publication interval.
     * @return itself.
     */
    public OpcUaSessionProfile withPublicationInterval(@Nonnull Duration publicationInterval) {
        setPublicationInterval(publicationInterval);
        return this;
    }

    @Override
    public String toString() {
        return "OpcUaSessionProfile{" +
                "publicationInterval=" + publicationInterval +
                "} " + super.toString();
    }
}
