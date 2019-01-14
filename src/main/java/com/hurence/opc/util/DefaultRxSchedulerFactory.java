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

package com.hurence.opc.util;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

import javax.annotation.Nonnull;

/**
 * Default {@link SchedulerFactory} using RX standard pools.
 */
public class DefaultRxSchedulerFactory implements SchedulerFactory {

    /**
     * Hidden constructor.
     */
    private DefaultRxSchedulerFactory() {
    }

    /**
     * Instance getter.
     *
     * @return the singleton instance.
     */
    @Nonnull
    public static DefaultRxSchedulerFactory get() {
        return SingletonHolder.INSTANCE;
    }

    @Nonnull
    @Override
    public Scheduler forBlocking() {
        return Schedulers.io();
    }

    @Nonnull
    @Override
    public Scheduler forComputation() {
        return Schedulers.computation();
    }

    /**
     * Singleton lazy initializer
     */
    private static class SingletonHolder {

        private static final DefaultRxSchedulerFactory INSTANCE = new DefaultRxSchedulerFactory();
    }
}
