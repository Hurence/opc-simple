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

public abstract class SessionProfile<T extends SessionProfile> {

    /**
     * The item requested refresh period in milliseconds. The server may not support as low as you set.
     * In this case the value will be refreshed at the server rate.
     * If you need a very low refresh delay, please consider use direct read mode.
     * Defaults to 1 second.
     */
    private long refreshPeriodMillis = 1000;

    public final long getRefreshPeriodMillis() {
        return refreshPeriodMillis;
    }

    public final void setRefreshPeriodMillis(long refreshPeriodMillis) {
        if (refreshPeriodMillis <= 0) {
            throw new IllegalArgumentException("refreshPeriodMillis must be any non-negative value ");
        }
        this.refreshPeriodMillis = refreshPeriodMillis;
    }

    public final T withRefreshPeriodMillis(long refreshPeriodMillis) {
        setRefreshPeriodMillis(refreshPeriodMillis);
        return (T) this;
    }

    @Override
    public String toString() {
        return "SessionProfile{" +
                "refreshPeriodMillis=" + refreshPeriodMillis +
                '}';
    }
}
