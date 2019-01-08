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

import javax.annotation.Nonnull;

/**
 * Factory class to use custom {@link Scheduler} depending on the operations to be done.
 *
 * @author amarziali
 */
public interface SchedulerFactory {

    /**
     * Scheduler to be used for slow blocking operations (e.g. IO wait).
     *
     * @return a valid {@link Scheduler}
     */
    @Nonnull
    Scheduler forBlocking();

    /**
     * Scheduler to be used for computation (time consuming but not blocking) operations.
     *
     * @return a valid {@link Scheduler}
     */
    @Nonnull
    Scheduler forComputation();

}
