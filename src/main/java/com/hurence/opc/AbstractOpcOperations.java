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

import com.hurence.opc.util.ExecutorServiceFactory;

import java.util.Optional;

/**
 * Abstract base class for {@link OpcOperations}
 *
 * @author amarziali
 */
public abstract class AbstractOpcOperations<T extends ConnectionProfile, U extends SessionProfile, V extends OpcSession> implements OpcOperations<T, U, V> {


    /**
     * The connection state
     */
    private volatile ConnectionState connectionState = ConnectionState.DISCONNECTED;

    /**
     * The thread factory.
     */
    protected final ExecutorServiceFactory executorServiceFactory;

    /**
     * Construct an instance with an {@link ExecutorServiceFactory}
     *
     * @param executorServiceFactory the executor thread factory.
     */
    protected AbstractOpcOperations(ExecutorServiceFactory executorServiceFactory) {
        this.executorServiceFactory = executorServiceFactory;
    }


    /**
     * Atomically check a state and set next state.
     *
     * @param next of empty won't set anything.
     * @return
     */
    protected synchronized ConnectionState getStateAndSet(Optional<ConnectionState> next) {
        ConnectionState ret = connectionState;
        if (next.isPresent()) {
            connectionState = next.get();
        }
        return ret;
    }


    @Override
    public final ConnectionState getConnectionState() {
        return getStateAndSet(Optional.empty());
    }

    @Override
    public final boolean awaitConnected() {
        while (getConnectionState() != ConnectionState.CONNECTED) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final boolean awaitDisconnected() {
        while (getConnectionState() != ConnectionState.DISCONNECTED) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isChannelSecured() {
        return false;
    }
}
