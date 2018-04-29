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

import com.hurence.opc.SessionProfile;

/**
 * {@link SessionProfile} with OPC-DA customizations.
 *
 * @author amarziali
 */
public class OpcDaSessionProfile extends SessionProfile<OpcDaSessionProfile> {

    /**
     * If set, server cache will be ignored and the value will be read directly from the device. Defaults to false.
     */
    private boolean directRead;


    public boolean isDirectRead() {
        return directRead;
    }

    public void setDirectRead(boolean directRead) {
        this.directRead = directRead;
    }

    public OpcDaSessionProfile withDirectRead(boolean directRead) {
        setDirectRead(directRead);
        return this;
    }

    @Override
    public String toString() {
        return "OpcDaSessionProfile{" +
                "directRead=" + directRead +
                "} " + super.toString();
    }
}
