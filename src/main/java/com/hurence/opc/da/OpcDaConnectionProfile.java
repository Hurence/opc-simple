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
    /**
     * The domain name.
     */
    private String domain;
    /**
     * The item requested refresh period in milliseconds. The server may not support as low as you set.
     * In this case the value will be refreshed at the server rate.
     * If you need a very low refresh delay, please consider use direct read mode.
     */
    private long refreshPeriodMillis = 1000; // defaults to 1 second
    /**
     * The user name.
     */
    private String user;
    /**
     * The password to authenticate the user on the provided domain.
     */
    private String password;
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

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

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

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public long getRefreshPeriodMillis() {
        return refreshPeriodMillis;
    }

    public void setRefreshPeriodMillis(long refreshPeriodMillis) {
        if (refreshPeriodMillis <= 0) {
            throw new IllegalArgumentException("refreshPeriodMillis must be any non-negative value ");
        }
        this.refreshPeriodMillis = refreshPeriodMillis;
    }

    public OpcDaConnectionProfile withComClsId(String comClsId) {
        setComClsId(comClsId);
        return this;
    }

    public OpcDaConnectionProfile withComProgId(String comProgId) {
        setComProgId(comProgId);
        return this;
    }

    public OpcDaConnectionProfile withDomain(String domain) {
        setDomain(domain);
        return this;
    }

    public OpcDaConnectionProfile withRefreshPeriodMillis(long refreshPeriodMillis) {
        setRefreshPeriodMillis(refreshPeriodMillis);
        return this;
    }

    public OpcDaConnectionProfile withUser(String user) {
        setUser(user);
        return this;
    }

    public OpcDaConnectionProfile withPassword(String password) {
        setPassword(password);
        return this;
    }

    public OpcDaConnectionProfile withDirectRead(boolean directRead) {
        setDirectRead(directRead);
        return this;
    }

    @Override
    public String toString() {
        return "OpcDaConnectionProfile{" +
                "comClsId='" + comClsId + '\'' +
                ", comProgId='" + comProgId + '\'' +
                ", domain='" + domain + '\'' +
                ", refreshPeriodMillis=" + refreshPeriodMillis +
                ", user='" + user + '\'' +
                ", password='**********'" +
                ", directRead='" + directRead + '\'' +
                '}';
    }
}
