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

/**
 * The connection state machine.
 *
 * @author amarziali
 */
public enum ConnectionState {

    /**
     * The connection is handshaking but not yet established.
     */
    CONNECTING,
    /**
     * Connection is in place.
     */
    CONNECTED,
    /**
     * The client is disconnecting.
     */
    DISCONNECTING,
    /**
     * The client has disconnected.
     */
    DISCONNECTED
}
