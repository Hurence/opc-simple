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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Singleton single threaded implementation of {@link ExecutorServiceFactory}.
 *
 * @author amarziali
 */
public class SingleThreadedExecutorServiceFactory implements ExecutorServiceFactory {

    /**
     * Lazy loading singleton.
     */
    private static final class SingletonHolder {
        private static final SingleThreadedExecutorServiceFactory instance = new SingleThreadedExecutorServiceFactory();
    }

    /**
     * Hide constructor.
     */
    private SingleThreadedExecutorServiceFactory() {
    }

    public static SingleThreadedExecutorServiceFactory instance() {
        return SingletonHolder.instance;
    }


    @Override
    public ExecutorService createWorker() {
        return Executors.newSingleThreadExecutor();
    }

    @Override
    public ScheduledExecutorService createScheduler() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}
