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

package com.hurence.opc.ua;

import com.hurence.opc.OperationStatus;
import com.hurence.opc.Quality;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;

import java.util.Optional;

/**
 * Opc UA quality decoder.
 *
 * @author amarziali
 */
public class OpcUaQualityExtractor {

    /**
     * Convert quality from opc-ua to common format.
     *
     * @param statusCode the UA status code.
     * @return the decoded {@link Quality}
     */
    public static final Quality quality(final StatusCode statusCode) {
        Quality ret = Quality.Unknown;
        if (statusCode.isGood()) {
            ret = Quality.Good;
        } else if (statusCode.isUncertain() || statusCode.isOverflowSet()) {
            ret = Quality.Uncertain;
        } else if (statusCode.isBad()) {
            ret = Quality.Bad;
        }
        return ret;
    }

    /**
     * Translates the ua status code to the {@link OperationStatus}
     *
     * @param statusCode the ua status code.
     * @return the resulting {@link OperationStatus}
     */
    public static final OperationStatus operationStatus(final StatusCode statusCode) {
        OperationStatus.Level level = OperationStatus.Level.WARNING;
        if (statusCode.isBad()) {
            level = OperationStatus.Level.ERROR;
        } else if (statusCode.isGood()) {
            level = OperationStatus.Level.INFO;
        }
        Optional<String[]> lookup = StatusCodes.lookup(statusCode.getValue());
        return new OperationStatus(level, statusCode.getValue(),
                Optional.ofNullable(lookup.orElseGet(() -> new String[2])[1]));
    }
}
