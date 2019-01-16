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

import com.hurence.opc.ConnectionProfile;
import com.hurence.opc.auth.X509Credentials;

/**
 * Connection profile for OPC-UA.
 *
 * @author amarziali
 */
public class OpcUaConnectionProfile extends ConnectionProfile<OpcUaConnectionProfile> {


    /**
     * THe client ID URI.
     * <p>
     * If not provided and the certificate contains an alternative name URI within the extensions (ASN 2.5.29.18)
     * the client id will be automatically taken from it.
     * <p>
     * Defaults to 'urn:hurence:opc:simple'
     */
    private String clientIdUri = "urn:hurence:opc:simple";

    /**
     * The client name (Defaults to 'OPC-SIMPLE').
     */
    private String clientName = "OPC-SIMPLE";

    /**
     * In case the connection is through a secure layer,
     * a X509 credentials (both Certificate and keyPairs) have to be provided.
     */
    private X509Credentials secureChannelEncryption;


    /**
     * Get the client URI.
     * <p>
     * Defaults to 'urn:hurence:opc:simple'
     *
     * @return a valid client resource identifier.
     */
    public String getClientIdUri() {
        return clientIdUri;
    }

    /**
     * Set the client URI.
     *
     * @param clientIdUri a valid client resource identifier.
     */
    public void setClientIdUri(String clientIdUri) {
        this.clientIdUri = clientIdUri;
    }

    /**
     * Gets the client name.
     *
     * @return the client name (Defaults to 'OPC-SIMPLE').
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * Sets the client name
     *
     * @param clientName the client name.
     */
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    /**
     * In case the communication should go though a secure channel, the user should set a valid X509 Credentials.
     *
     * @return the X509 Credentials or null if communication is insecure.
     */
    public X509Credentials getSecureChannelEncryption() {
        return secureChannelEncryption;
    }

    /**
     * In case the communication should go though a secure channel, the user should set a valid X509 Credentials.
     *
     * @param secureChannelEncryption The {@link X509Credentials} or null to go insecurely.
     */
    public void setSecureChannelEncryption(X509Credentials secureChannelEncryption) {
        this.secureChannelEncryption = secureChannelEncryption;
    }

    /**
     * Set the client URI.
     *
     * @param clientIdUri a valid client resource identifier.
     * @return itself.
     */
    public OpcUaConnectionProfile withClientIdUri(String clientIdUri) {
        setClientIdUri(clientIdUri);
        return this;
    }

    /**
     * Set the client name.
     *
     * @param clientName the client name
     * @return itself.
     */
    public OpcUaConnectionProfile withClientName(String clientName) {
        setClientName(clientName);
        return this;
    }

    /**
     * In case the communication should go though a secure channel, the user should set a valid X509 Credentials.
     *
     * @param secureChannelEncryption The {@link X509Credentials} or null to go insecurely.
     * @return itself.
     */
    public OpcUaConnectionProfile withSecureChannelEncryption(X509Credentials secureChannelEncryption) {
        setSecureChannelEncryption(secureChannelEncryption);
        return this;
    }

    @Override
    public String toString() {
        return "OpcUaConnectionProfile{" +
                "clientIdUri='" + clientIdUri + '\'' +
                ", clientName='" + clientName + '\'' +
                ", secureChannelEncryption=" + secureChannelEncryption +
                "} " + super.toString();
    }
}
