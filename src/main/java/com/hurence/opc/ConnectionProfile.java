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

/**
 * Base class to describe opc server related properties.
 *
 * @author amarziali
 */
public abstract class ConnectionProfile<T extends ConnectionProfile<T>> {

    /**
     * The host for the connection.
     */
    private String host;
    /**
     * The port to connect to
     */
    private Integer port;


    /**
     * The timeout used to read/write.
     */
    private Duration socketTimeout;


    /**
     * Set the host and return itself.
     *
     * @param host the host.
     * @return
     */
    public final T withHost(String host) {
        setHost(host);
        return (T) this;
    }

    public final T withPort(int port) {
        setPort(port);
        return (T) this;
    }

    public final T withSocketTimeout(Duration socketTimeout) {
        setSocketTimeout(socketTimeout);
        return (T) this;
    }


    public final Duration getSocketTimeout() {
        return socketTimeout;
    }

    public final void setSocketTimeout(Duration socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    /**
     * Get the hostname.
     *
     * @return
     */
    public final String getHost() {
        return host;
    }

    /**
     * Sets the host
     *
     * @param host the host
     */
    public final void setHost(String host) {
        this.host = host;
    }

    /**
     * Get the port used for the connection.
     *
     * @return
     */
    public final Integer getPort() {
        return port;
    }


    /**
     * Set the port used for the connection.
     *
     * @param port
     */
    public final void setPort(Integer port) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port value must any valid  unsigned 16 bit integer");
        }
        this.port = port;
    }

    @Override
    public String toString() {
        return "ConnectionProfile{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", socketTimeout=" + socketTimeout +
                '}';
    }
}
