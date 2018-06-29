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

package com.hurence.opc.da;

import com.hurence.opc.OperationStatus;
import com.hurence.opc.Quality;
import org.openscada.opc.dcom.common.Result;

import java.util.Optional;

/**
 * Opc-DA status decoder.
 *
 * @author amarziali
 */
public class OpcDaQualityExtractor {

    public static final int OPC_QUALITY_MASK = 0xC0;
    public static final int OPC_STATUS_MASK = 0xFC;
    public static final int OPC_LIMIT_MASK = 0x03;
    // Values for QUALITY_MASK bit field
    public static final int OPC_QUALITY_UNCERTAIN = 0x40;
    public static final int OPC_QUALITY_GOOD = 0xC0;
    public static final int OPC_QUALITY_BAD = 0x0;

    // STATUS_MASK Values for Quality = BAD
    public static final int OPC_QUALITY_CONFIG_ERROR_CODE = 0x04;
    public static final int OPC_QUALITY_NOT_CONNECTED_ERROR_CODE = 0x08;
    public static final int OPC_QUALITY_DEVICE_FAILURE_CODE = 0x0c;
    public static final int OPC_QUALITY_SENSOR_FAILURE_CODE = 0x10;
    public static final int OPC_QUALITY_LAST_KNOWN_CODE = 0x14;
    public static final int OPC_QUALITY_COMM_FAILURE_CODE = 0x18;
    public static final int OPC_QUALITY_OUT_OF_SERVICE_CODE = 0x1C;
    public static final int OPC_QUALITY_WAITING_FOR_INITIAL_DATA_CODE = 0x20;
    // STATUS_MASK Values for Quality = UNCERTAIN
    public static final int OPC_QUALITY_UNCERTAIN_LAST_USABLE_VALUE_CODE = 0x44;
    public static final int OPC_QUALITY_SENSOR_CAL_CODE = 0x50;
    public static final int OPC_QUALITY_EGU_EXCEEDED_CODE = 0x54;
    public static final int OPC_QUALITY_UNCERTAIN_SUBNORMAL_CODE = 0x58;
    // STATUS_MASK Values for Quality = GOOD
    public static final int OPC_QUALITY_LOCAL_OVERRIDE_CODE = 0xD8;

    public static final String OPC_QUALITY_CONFIG_ERROR_DESC = "There is some server specific problem with the configuration.";
    public static final String OPC_QUALITY_NOT_CONNECTED_ERROR_DESC = "The input is required to be logically connected to something but is not.";
    public static final String OPC_QUALITY_DEVICE_FAILURE_DESC = "A device failure has been detected.";
    public static final String OPC_QUALITY_SENSOR_FAILURE_DESC = "A sensor failure had been detected";
    public static final String OPC_QUALITY_LAST_KNOWN_DESC = "Communications have failed. However, the last known value is available";
    public static final String OPC_QUALITY_COMM_FAILURE_DESC = "Communications have failed. There is no last known value is available.";
    public static final String OPC_QUALITY_OUT_OF_SERVICE_DESC = "The block is off scan or otherwise locked ";
    public static final String OPC_QUALITY_WAITING_FOR_INITIAL_DATA_DESC = "After Items are added to a group, it may take some time for the server to actually obtain values for these items.";

    public static final String OPC_QUALITY_UNCERTAIN_LAST_USABLE_VALUE_DESC = "Whatever was writing this value has stopped doing so. The returned value should be regarded as 'stale'.";
    public static final String OPC_QUALITY_SENSOR_CAL_DESC = "The sensor is known to be out of calibration.";
    public static final String OPC_QUALITY_EGU_EXCEEDED_DESC = "The returned value is outside the limits defined for this parameter";
    public static final String OPC_QUALITY_UNCERTAIN_SUBNORMAL_DESC = "The value is derived from multiple sources and has less than the required number of good sources.";

    public static final String OPC_QUALITY_LOCAL_OVERRIDE_DESC = "The value has been Overridden.";

    /**
     * Extracts status from the raw value.
     *
     * @param value the raw value returned by the OPC-DA server (only first 32bit matters).
     * @return the {@link Quality}. Defaults to {@link Quality#Unknown}
     */
    public static Quality quality(long value) {
        Quality ret;
        int extracted = (int) value & OPC_QUALITY_MASK;
        switch (extracted) {
            case OPC_QUALITY_GOOD:
                ret = Quality.Good;
                break;
            case OPC_QUALITY_UNCERTAIN:
                ret = Quality.Uncertain;
                break;
            case OPC_QUALITY_BAD:
                ret = Quality.Bad;
                break;
            default:
                ret = Quality.Unknown;
                break;
        }
        return ret;
    }

    /**
     * Extracts the information from a {@link Result}.
     *
     * @param result operation result.
     * @return the {@link OperationStatus} linked to the result.
     */
    public static OperationStatus operationStatus(Result<?> result) {

        if (result.isFailed()) {
            return operationStatus(result.getErrorCode());
        }
        return new OperationStatus(OperationStatus.Level.INFO, result.getErrorCode(), Optional.empty());
    }

    /**
     * Extracts the information from an encoded status value.
     *
     * @param value the encoded value
     * @return the {@link OperationStatus} linked to the value.
     */
    public static OperationStatus operationStatus(long value) {
        OperationStatus.Level level;
        switch (quality(value)) {
            case Good:
                level = OperationStatus.Level.INFO;
                break;
            case Bad:
                level = OperationStatus.Level.ERROR;
                break;
            default:
                level = OperationStatus.Level.WARNING;
                break;
        }
        int code = (int) value & OPC_STATUS_MASK;
        String desc;

        switch (code) {
            case OPC_QUALITY_CONFIG_ERROR_CODE:
                desc = OPC_QUALITY_CONFIG_ERROR_DESC;
                break;
            case OPC_QUALITY_NOT_CONNECTED_ERROR_CODE:
                desc = OPC_QUALITY_NOT_CONNECTED_ERROR_DESC;
                break;
            case OPC_QUALITY_DEVICE_FAILURE_CODE:
                desc = OPC_QUALITY_DEVICE_FAILURE_DESC;
                break;
            case OPC_QUALITY_SENSOR_FAILURE_CODE:
                desc = OPC_QUALITY_SENSOR_FAILURE_DESC;
                break;
            case OPC_QUALITY_LAST_KNOWN_CODE:
                desc = OPC_QUALITY_LAST_KNOWN_DESC;
                break;
            case OPC_QUALITY_COMM_FAILURE_CODE:
                desc = OPC_QUALITY_COMM_FAILURE_DESC;
                break;
            case OPC_QUALITY_OUT_OF_SERVICE_CODE:
                desc = OPC_QUALITY_OUT_OF_SERVICE_DESC;
                break;
            case OPC_QUALITY_WAITING_FOR_INITIAL_DATA_CODE:
                desc = OPC_QUALITY_WAITING_FOR_INITIAL_DATA_DESC;
                break;
            case OPC_QUALITY_UNCERTAIN_LAST_USABLE_VALUE_CODE:
                desc = OPC_QUALITY_UNCERTAIN_LAST_USABLE_VALUE_DESC;
                break;
            case OPC_QUALITY_SENSOR_CAL_CODE:
                desc = OPC_QUALITY_SENSOR_CAL_DESC;
                break;
            case OPC_QUALITY_EGU_EXCEEDED_CODE:
                desc = OPC_QUALITY_EGU_EXCEEDED_DESC;
                break;
            case OPC_QUALITY_UNCERTAIN_SUBNORMAL_CODE:
                desc = OPC_QUALITY_UNCERTAIN_SUBNORMAL_DESC;
                break;
            // STATUS_MASK Values for Quality = GOOD
            case OPC_QUALITY_LOCAL_OVERRIDE_CODE:
                desc = OPC_QUALITY_LOCAL_OVERRIDE_DESC;
                break;
            default:
                desc = null;
                break;
        }

        return new OperationStatus(level, code, Optional.ofNullable(desc));
    }
}
