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

/**
 * Windows logon credentials.
 *
 * @author amarziali
 */
public class NtlmCredentials extends UsernamePasswordCredentials {

    /**
     * The logon domain.
     */
    private String domain;

    /**
     * Get the logon domain
     *
     * @return the domain.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Sets the logon domain.
     *
     * @param domain the domain.
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * Sets the logon domain.
     *
     * @param domain the domain.
     * @return itself
     */
    public NtlmCredentials withDomain(String domain) {
        setDomain(domain);
        return this;
    }

    @Override
    public String toString() {
        return "NtlmCredentials{" +
                "domain='" + domain + '\'' +
                "} " + super.toString();
    }
}
