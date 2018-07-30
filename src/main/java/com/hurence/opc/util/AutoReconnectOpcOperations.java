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

import com.hurence.opc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

/**
 * Utility class delegating operations to a standard {@link OpcOperations} implementation but automatically reconnecting
 * if the underlying connection is lost.
 *
 * @author amarziali
 */
public class AutoReconnectOpcOperations<S extends ConnectionProfile<S>, T extends SessionProfile<T>, U extends OpcSession> implements OpcOperations<S, T, U> {

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
    private final OpcOperations<S, T, U> delegate;

    /**
     * Create a new dynamic proxy instance allowing for seamless automatic reconnection.
     *
     * @param delegate the {@link OpcOperations} to decorate.
     * @return the decorated instance.
     */
    @SuppressWarnings("unchecked")
    public static <O extends OpcOperations> O create(final O delegate) {
        final AutoReconnectOpcOperations toProxy = new AutoReconnectOpcOperations(delegate);
        return (O) Proxy.newProxyInstance(delegate.getClass().getClassLoader(),
                delegate.getClass().getInterfaces(),
                (proxy, method, args) -> {
                    try {
                        return toProxy.getClass().getMethod(method.getName(), method.getParameterTypes())
                                .invoke(toProxy, args);
                    } catch (NoSuchMethodException e) {
                        return delegate.getClass().getMethod(method.getName(), method.getParameterTypes())
                                .invoke(delegate, args);
                    }
                });
    }


    /**
     * Construct an instance.
     *
     * @param delegate the delegate
     */
    private AutoReconnectOpcOperations(OpcOperations<S, T, U> delegate) {
        this.delegate = delegate;

    }

    @Override
    public void connect(S connectionProfile) {
        delegate.connect(connectionProfile);
        //if no error we'll run our keepalive loop.
        if (executorService == null) {
            executorService = SingleThreadedExecutorServiceFactory.instance().createScheduler();
            shouldKeepAlive = true;
            executorService.execute(() -> {
                while (shouldKeepAlive) {
                    if (getConnectionState() == ConnectionState.CONNECTED) {
                        awaitDisconnected();
                    }
                    if (shouldKeepAlive) {
                        logger.info("Detected disconnection. Triggering reconnection");
                        try {
                            connect(connectionProfile);
                            awaitConnected();
                        } catch (Exception e) {
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
    public Collection<OpcTagInfo> fetchMetadata(String... tagIds) {
        return delegate.fetchMetadata(tagIds);
    }

    @Override
    public ConnectionState getConnectionState() {
        return delegate.getConnectionState();
    }

    @Override
    public Collection<OpcTagInfo> browseTags() {
        return delegate.browseTags();
    }

    @Override
    public Collection<OpcObjectInfo> fetchNextTreeLevel(String rootTagId) {
        return delegate.fetchNextTreeLevel(rootTagId);
    }

    @Override
    public U createSession(T sessionProfile) {
        return delegate.createSession(sessionProfile);
    }

    @Override
    public void releaseSession(U session) {
        delegate.releaseSession(session);
    }

    @Override
    public boolean awaitConnected() {
        return delegate.awaitConnected();
    }

    @Override
    public boolean awaitDisconnected() {
        return delegate.awaitDisconnected();
    }

    @Override
    public void close() throws Exception {
        delegate.close();
    }

    @Override
    public boolean isChannelSecured() {
        return delegate.isChannelSecured();
    }
}
