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
import java.util.concurrent.ScheduledExecutorService;

/**
 * Factory class to allocate concurrent resources for {@link java.util.concurrent.ExecutorService} and {@link java.util.concurrent.ScheduledExecutorService}
 */
public interface ExecutorServiceFactory {

    /**
     * Create an executor to run slave tasks.
     *
     * @return an {@link ExecutorService} instance.
     */
    ExecutorService createWorker();

    /**
     * Create a executor to schedule tasks.
     *
     * @return a {@link ScheduledExecutorService} instance.
     */
    ScheduledExecutorService createScheduler();
}
