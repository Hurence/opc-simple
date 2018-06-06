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

import java.util.Objects;

/**
 * Username/Passaword {@link Credentials} information.
 *
 * @author amarziali
 */
public class UsernamePasswordCredentials implements Credentials {

    /**
     * The user name.
     */
    private String user;
    /**
     * The password to authenticate the user on the provided domain.
     */
    private String password;

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

    public UsernamePasswordCredentials withUser(String user) {
        setUser(user);
        return this;
    }

    public UsernamePasswordCredentials withPassword(String password) {
        setPassword(password);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsernamePasswordCredentials that = (UsernamePasswordCredentials) o;
        return Objects.equals(user, that.user) &&
                Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {

        return Objects.hash(user, password);
    }

    @Override
    public String toString() {
        return "UsernamePasswordCredentials{" +
                "user='" + user + '\'' +
                ", password='***********'" +
                '}';
    }
}
