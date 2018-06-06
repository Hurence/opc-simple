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

import java.security.KeyPair;
import java.security.cert.Certificate;
import java.util.Objects;

/**
 * X509 (Private key + Certificate) {@link Credentials} information.
 * Used to authenticate securely without exchanging passwords.
 *
 * @author amarziali
 */
public class X509Credentials implements Credentials {

    /**
     * The issued public X509 {@link Certificate}.
     */
    private Certificate certificate;

    /**
     * The {@link KeyPair} holding public and private keys.
     */
    private KeyPair keys;


    /**
     * Get the issued public X509 {@link Certificate}.
     *
     * @return the {@link Certificate}
     */
    public Certificate getCertificate() {
        return certificate;
    }

    /**
     * Set the issued public X509 {@link Certificate}.
     *
     * @param certificate the X509 {@link Certificate} to set
     */
    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    /**
     * Get the {@link KeyPair} holding public and private keys.
     *
     * @return the keys information.
     */
    public KeyPair getKeys() {
        return keys;
    }

    /**
     * Set the {@link KeyPair} holding public and private keys.
     *
     * @param keys the keys to set.
     */
    public void setKeys(KeyPair keys) {
        this.keys = keys;
    }

    /**
     * Set the issued public X509 {@link Certificate}.
     *
     * @param certificate the X509 {@link Certificate} to set
     * @return itself.
     */
    public X509Credentials withCertificate(Certificate certificate) {
        setCertificate(certificate);
        return this;
    }

    /**
     * Set the {@link KeyPair} holding public and private keys.
     *
     * @param keys the keys to set.
     * @return itself.
     */
    public X509Credentials withKeys(KeyPair keys) {
        setKeys(keys);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        X509Credentials that = (X509Credentials) o;
        return Objects.equals(certificate, that.certificate) &&
                Objects.equals(keys, that.keys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(certificate, keys);
    }

    @Override
    public String toString() {
        return "X509Credentials{" +
                "certificate=" + certificate +
                ", keys='****hidden****'" +
                '}';
    }
}
