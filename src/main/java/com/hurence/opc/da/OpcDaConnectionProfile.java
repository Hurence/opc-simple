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

import com.hurence.opc.ConnectionProfile;

/**
 * OPC-DA specific connection information.
 * Please remember that either comClsId or comProgId are mandatory and must be set.
 *
 * @author amarziali
 */
public class OpcDaConnectionProfile extends ConnectionProfile<OpcDaConnectionProfile> {

    /**
     * The CLS UUID of the com application.
     */
    private String comClsId;
    /**
     * The com program id.
     */
    private String comProgId;


    public String getComClsId() {
        return comClsId;
    }

    public void setComClsId(String comClsId) {
        this.comClsId = comClsId;
    }

    public String getComProgId() {
        return comProgId;
    }

    public void setComProgId(String comProgId) {
        this.comProgId = comProgId;
    }


    public OpcDaConnectionProfile withComClsId(String comClsId) {
        setComClsId(comClsId);
        return this;
    }

    public OpcDaConnectionProfile withComProgId(String comProgId) {
        setComProgId(comProgId);
        return this;
    }


    @Override
    public String toString() {
        return "OpcDaConnectionProfile{" +
                "comClsId='" + comClsId + '\'' +
                ", comProgId='" + comProgId + '\'' +
                '}';
    }
}
