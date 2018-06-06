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

import com.hurence.opc.auth.Credentials;

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
     * The authentication credentials.
     */
    private Credentials credentials;


    /**
     * Set the host and return itself.
     *
     * @param host the host.
     * @return itself.
     */
    public final T withHost(String host) {
        setHost(host);
        return (T) this;
    }

    /**
     * Set the port and return itself.
     *
     * @param port the port number.
     * @return itself.
     */
    public final T withPort(int port) {
        setPort(port);
        return (T) this;
    }


    /**
     * Set the socket timeout and return itselg.
     *
     * @param socketTimeout the socket timeout.
     * @return itself.
     */
    public final T withSocketTimeout(Duration socketTimeout) {
        setSocketTimeout(socketTimeout);
        return (T) this;
    }

    /**
     * Set the {@link Credentials} to use for authentication.
     * It can be any subclass. The connector must however support the related authentication method.
     *
     * @param credentials the credentials.
     * @return itself.
     */
    public final T withCredentials(Credentials credentials) {
        setCredentials(credentials);
        return (T) this;
    }


    /**
     * Get the global socket timeout.
     *
     * @return the socket timeout.
     */
    public final Duration getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * Set the global socket timeout.
     *
     * @param socketTimeout the max allowed timeout.
     */
    public final void setSocketTimeout(Duration socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    /**
     * Get the hostname.
     *
     * @return the host.
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
     * @return the port number if set.
     */
    public final Integer getPort() {
        return port;
    }

    /**
     * Get the credentials to authenticate with.
     *
     * @return the {@link Credentials}
     */
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Set the {@link Credentials} to use for authentication.
     * It can be any subclass. The connector must however support the related authentication method.
     *
     * @param credentials the credentials.
     */
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    /**
     * Set the port used for the connection.
     *
     * @param port the port number (cannot be null and must be in the interval [1, 65535].
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
