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

package com.hurence.opc;

import com.hurence.opc.exception.OpcException;
import com.hurence.opc.util.SchedulerFactory;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

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
    private final BehaviorSubject<ConnectionState> connectionState = BehaviorSubject.createDefault(ConnectionState.DISCONNECTED);

    /**
     * The scheduler factory.
     */
    protected final SchedulerFactory schedulerFactory;

    /**
     * Construct an instance with an {@link SchedulerFactory}
     *
     * @param schedulerFactory the scheduler factory.
     */
    protected AbstractOpcOperations(SchedulerFactory schedulerFactory) {
        this.schedulerFactory = schedulerFactory;
    }


    /**
     * Atomically check a state and set next state.
     *
     * @param next of empty won't set anything.
     * @return the connection state.
     */
    protected synchronized ConnectionState getStateAndSet(Optional<ConnectionState> next) {
        ConnectionState ret = connectionState.getValue();
        next.ifPresent(connectionState::onNext);
        return ret;
    }


    @Override
    public final Observable<ConnectionState> getConnectionState() {
        return connectionState;
    }


    @Override
    public boolean isChannelSecured() {
        return false;
    }

    /**
     * Wait until connection is released.
     *
     * @return a {@link Completable} task.
     */
    protected Completable waitUntilDisconnected() {
        return getConnectionState()
                .takeWhile(connectionState -> connectionState != ConnectionState.DISCONNECTED)
                .filter(connectionState -> connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.DISCONNECTED)
                .switchMapCompletable(connectionState -> {
                    switch (connectionState) {
                        case CONNECTED:
                            return Completable.error(new OpcException("Client still in connected state"));
                        default:
                            return Completable.complete();
                    }
                });
    }

    /**
     * Wait until connection is established.
     *
     * @return a {@link Completable} task.
     */
    protected Completable waitUntilConnected() {
        return getConnectionState()
                .takeWhile(connectionState -> connectionState != ConnectionState.CONNECTED)
                .filter(connectionState -> connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.DISCONNECTED)
                .switchMapCompletable(connectionState -> {
                    switch (connectionState) {
                        case DISCONNECTED:
                            return Completable.error(new OpcException("Client disconnected while waiting for connection handshake"));
                        default:
                            return Completable.complete();
                    }
                });
    }
}
