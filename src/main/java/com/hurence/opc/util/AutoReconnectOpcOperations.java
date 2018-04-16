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

package com.hurence.opc.util;

import com.hurence.opc.ConnectionProfile;
import com.hurence.opc.ConnectionState;
import com.hurence.opc.OpcData;
import com.hurence.opc.OpcOperations;
import com.hurence.opc.exception.OpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * Utility class delegating operations to a standard {@link OpcOperations} implementation but automatically reconnecting
 * if the underlying connection is lost.
 *
 * @author amarziali
 */
public class AutoReconnectOpcOperations<S extends ConnectionProfile<S>, T extends OpcData> implements OpcOperations<S, T> {

    private static final Logger logger = LoggerFactory.getLogger(AutoReconnectOpcOperations.class);


    /**
     * Our threadpool.
     */
    private ExecutorService executorService;

    /**
     * Monitors running state of connection keepalive polling thread.
     */
    private volatile boolean shouldKeepAlive = false;

    /**
     * Our delegate.
     */
    private final OpcOperations<S, T> delegate;

    /**
     * Construct an instance.
     *
     * @param delegate the delegate
     */
    public AutoReconnectOpcOperations(OpcOperations<S, T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void connect(S connectionProfile) {
        delegate.connect(connectionProfile);
        //if no error we'll run our keepalive loop.
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
            shouldKeepAlive = true;
            executorService.execute(() -> {
                while (awaitDisconnected()) {
                    if (shouldKeepAlive) {
                        logger.info("Detected disconnection. Triggering reconnection");
                        try {
                            connect(connectionProfile);
                        }  catch (OpcException e) {
                            logger.warn("Error while reconnecting. Retrying in 1 second", e);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ie) {
                                //abort here
                                break;
                            }
                        }
                    } else {
                        logger.info("Connection keepalive done since user asked for disconnection.");
                        break;
                    }
                }

            });
        }

    }

    @Override
    public void disconnect() {
        shouldKeepAlive = false;
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
        delegate.disconnect();
    }

    @Override
    public ConnectionState getConnectionState() {
        return delegate.getConnectionState();
    }

    @Override
    public Collection<String> browseTags() {
        return delegate.browseTags();
    }

    @Override
    public Collection<T> read(String... tags) {
        return delegate.read(tags);
    }

    @Override
    public boolean write(T... data) {
        return delegate.write(data);
    }

    @Override
    public Stream<T> stream(String... tags) {
        return delegate.stream(tags);
    }

    @Override
    public boolean awaitConnected() {
        return delegate.awaitConnected();
    }

    @Override
    public boolean awaitDisconnected() {
        return delegate.awaitDisconnected();
    }
}
