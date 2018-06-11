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

package com.hurence.opc.auth;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * X509 (Private key + Certificate) {@link Credentials} information.
 * Used to authenticate securely without exchanging passwords.
 *
 * @author amarziali
 */
public class X509Credentials implements Credentials {

    /**
     * The issued public X509 {@link X509Certificate}.
     */
    private X509Certificate certificate;

    /**
     * The {@link PrivateKey} private key.
     */
    private PrivateKey privateKey;


    /**
     * Get the issued public X509 {@link X509Certificate}.
     *
     * @return the {@link X509Certificate}
     */
    public X509Certificate getCertificate() {
        return certificate;
    }

    /**
     * Set the issued public X509 {@link Certificate}.
     *
     * @param certificate the X509 {@link Certificate} to set
     */
    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    /**
     * Get the {@link PrivateKey} private key.
     *
     * @return the private key.
     */
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * Set the {@link PrivateKey} private key.
     *
     * @param privateKey the private key to set.
     */
    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    /**
     * Set the issued public X509 {@link X509Certificate}.
     *
     * @param certificate the X509 {@link X509Certificate} to set
     * @return itself.
     */
    public X509Credentials withCertificate(X509Certificate certificate) {
        setCertificate(certificate);
        return this;
    }

    /**
     * Set the {@link PrivateKey}  private key.
     *
     * @param privateKey the private key to set.
     * @return itself.
     */
    public X509Credentials withPrivateKey(PrivateKey privateKey) {
        setPrivateKey(privateKey);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        X509Credentials that = (X509Credentials) o;
        return Objects.equals(certificate, that.certificate) &&
                Objects.equals(privateKey, that.privateKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(certificate, privateKey);
    }

    @Override
    public String toString() {
        return "X509Credentials{" +
                "certificate=" + certificate +
                ", privateKey='****hidden****'" +
                '}';
    }
}
