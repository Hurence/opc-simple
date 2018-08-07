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

import java.net.URI;
import java.time.Duration;

/**
 * Base class to describe opc server related properties.
 *
 * @author amarziali
 */
public abstract class ConnectionProfile<T extends ConnectionProfile<T>> {


    /**
     * The connection {@link URI}
     */
    private URI connectionUri;

    /**
     * The timeout used to read/write.
     */
    private Duration socketTimeout;

    /**
     * The interval used to ping the remote server and check for health. (defaults to 10 seconds)
     */
    private Duration keepAliveInterval = Duration.ofSeconds(10);

    /**
     * The authentication credentials.
     * <p>
     * Defaults to {@link Credentials#ANONYMOUS_CREDENTIALS}
     */
    private Credentials credentials = Credentials.ANONYMOUS_CREDENTIALS;


    /**
     * Set the connection URI and return itself.
     *
     * @param connectionUri the URI.
     * @return itself.
     */
    public final T withConnectionUri(URI connectionUri) {
        setConnectionUri(connectionUri);
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
     * Set the interval used to ping the remote server and check for health.
     *
     * @param keepAliveInterval a valid non null {@link Duration}
     * @return itself
     */
    public final T withKeepAliveInterval(Duration keepAliveInterval) {
        setKeepAliveInterval(keepAliveInterval);
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
     * Get The interval used to ping the remote server and check for health.
     *
     * @return a {@link Duration}
     */
    public Duration getKeepAliveInterval() {
        return keepAliveInterval;
    }

    /**
     * Set the interval used to ping the remote server and check for health. (defaults to 10 seconds)
     *
     * @param keepAliveInterval a never null {@link Duration}
     */
    public void setKeepAliveInterval(Duration keepAliveInterval) {
        if (keepAliveInterval == null) {
            throw new IllegalArgumentException("keepAliveInterval must be a valid non null duration");
        }
        this.keepAliveInterval = keepAliveInterval;
    }

    /**
     * Gets the connection URI.
     *
     * @return a valid {@link URI}
     */
    public URI getConnectionUri() {
        return connectionUri;
    }

    /**
     * Sets the connection URI
     *
     * @param connectionUri a valid {@link URI} (the scheme will depend to the underlying implementation.
     */
    public void setConnectionUri(URI connectionUri) {
        this.connectionUri = connectionUri;
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


    @Override
    public String toString() {
        return "ConnectionProfile{" +
                "connectionUri=" + connectionUri +
                ", socketTimeout=" + socketTimeout +
                ", keepAliveInterval=" + keepAliveInterval +
                ", credentials=" + credentials +
                '}';
    }
}
