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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration bean for a subscription.
 *
 * @author amarziali
 */
public class SubscriptionConfiguration {

    /**
     * The sampling configuration per tag.
     */
    private final Map<String, Duration> samplingIntervalByTag = new HashMap<>();

    /**
     * The default tag sampling interval. (Defaults to 10 seconds).
     */
    private Duration defaultSamplingInterval = Duration.ofSeconds(10);


    /**
     * Get the default tag sampling interval.
     *
     * @return a {@link Duration}
     */
    public Duration getDefaultSamplingInterval() {
        return defaultSamplingInterval;
    }

    /**
     * Set the default tag sampling interval.
     *
     * @param defaultSamplingInterval any non null {@link Duration}
     */
    public void setDefaultSamplingInterval(Duration defaultSamplingInterval) {
        if (defaultSamplingInterval == null) {
            throw new IllegalArgumentException("Parameter is expected not to be null");
        }
        this.defaultSamplingInterval = defaultSamplingInterval;
    }

    /**
     * Set the default tag sampling interval.
     *
     * @param defaultSamplingInterval any non null {@link Duration}
     * @return itself
     */
    public SubscriptionConfiguration withDefaultSamplingInterval(Duration defaultSamplingInterval) {
        setDefaultSamplingInterval(defaultSamplingInterval);
        return this;
    }

    /**
     * Gets the sampling interval for a certain tag.
     *
     * @param tagId the tag id.
     * @return the tag sampling interval if defined or the {@link SubscriptionConfiguration#defaultSamplingInterval} otherwise.
     */
    public Duration samplingIntervalForTag(String tagId) {
        return samplingIntervalByTag.getOrDefault(tagId, defaultSamplingInterval);
    }

    /**
     * Set the specific sampling interval for a certain tag.
     *
     * @param tagId            the tag id
     * @param samplingInterval any non null {@link Duration}
     * @return itself
     */
    public SubscriptionConfiguration withTagSamplingIntervalForTag(String tagId, Duration samplingInterval) {
        if (samplingInterval == null) {
            throw new IllegalArgumentException("Parameter samplingInterval is expected not to be null");
        }
        samplingIntervalByTag.put(tagId, samplingInterval);
        return this;
    }

    @Override
    public String toString() {
        return "SubscriptionConfiguration{" +
                "samplingIntervalByTag=" + samplingIntervalByTag +
                ", defaultSamplingInterval=" + defaultSamplingInterval +
                '}';
    }
}
