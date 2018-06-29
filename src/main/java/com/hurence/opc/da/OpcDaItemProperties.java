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

/**
 * OPC-DA property indexes.
 *
 * @author amarziali
 */
@SuppressWarnings("unused")
public interface OpcDaItemProperties {
    /**
     * tem Canonical DataType
     * (VARTYPE stored in an I2)
     */
    int MANDATORY_DATA_TYPE = 1;
    /**
     * "Item Value" (VARIANT)
     * Note the type of value returned is as indicated by the "Item Canonical DataType above and depends on the item.
     * This will behave like a read from DEVICE.
     */
    int MANDATORY_ITEM_VALUE = 2;
    /**
     * Item Quality
     * (OPCQUALITY stored in an I2). This will behave like a read from DEVICE.
     */
    int MANDATORY_ITEM_QUALITY = 3;
    /**
     * Item Timestamp
     * (will be converted from FILETIME). This will behave like a read from DEVICE.
     */
    int MANDATORY_ITEM_TIMESTAMP = 4;
    /**
     * Item Access Rights
     * (OPCACCESSRIGHTS stored in an I4)
     */
    int MANDATORY_ITEM_ACCESS_RIGHTS = 5;
    /**
     * Server Scan Rate in Milliseconds.
     * This represents the fastest rate at which the server could obtain data from the underlying data source.
     * <p>
     * The nature of this source is not defined but is typically a DCS system, a SCADA system, a PLC via a COMM port or network,
     * a Device Network, etc. This value generally represents the 'best case' fastest RequestedUpdateRate which could be used
     * if this item were added to an OPCGroup.
     * <p>
     * The accuracy of this value (the ability of the server to attain 'best case' performance) can be greatly affected by system load and other factors.
     */
    int MANDATORY_SERVER_SCAN_RATE = 6;

    /**
     * "EU Units"e.g.
     * "DEGC" or "GALLONS"
     */
    int RECOMMENDED_EU_UNITS = 100;
    /**
     * The item description.
     */
    int RECOMMENDED_ITEM_DESCRIPTION = 101;
    /**
     * "High EU"
     * Present only for 'analog' data. This represents the highest value likely to be obtained in normal operation
     * and is intended for such use as automatically scaling a bargraph display. e.g. 1400.0
     */
    int RECOMMENDED_HIGH_EU = 102;
    /**
     * "Low EU"
     * Present only for 'analog' data. This represents the lowest value likely to be obtained in normal operation and
     * is intended for such use as automatically scaling a bargraph display.
     * e.g. -200.0
     */
    int RECOMMENDED_LOW_EU = 103;
    /**
     * "High Instrument Range"
     * Present only for 'analog' data. This represents the highest value that can be returned by the instrument.
     * e.g. 9999.9
     */
    int RECOMMENDED_HIGH_INSTRUMENT_RANGE = 104;
    /**
     * "Low Instrument Range"
     * Present only for 'analog' data. This represents the lowest value that can be returned by the instrument.
     * e.g. -9999.9
     */
    int RECOMMENDED_LOW_INSTRUMENT_RANGE = 105;

    /**
     * Access rights read bitmask.
     */
    int OPC_ACCESS_RIGHTS_READABLE = 0x1;
    /**
     * Access rights write bitmask.
     */
    int OPC_ACCESS_RIGHTS_WRITABLE = 0x2;

}
