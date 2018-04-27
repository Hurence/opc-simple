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

import java.util.Objects;

/**
 * Represents an opc tag property (metadata).
 *
 * @param <T> The data type.
 * @author amarziali
 */
public class OpcTagProperty<T> {

    /**
     * The property key (or id).
     */
    private final String key;
    /**
     * The description.
     */
    private final String description;
    /**
     * The value.
     */
    private final T value;

    /**
     * Construct an immutable {@link OpcTagProperty}
     *
     * @param key
     * @param description
     * @param value
     */
    public OpcTagProperty(String key, String description, T value) {
        this.key = key;
        this.description = description;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public T getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpcTagProperty<?> that = (OpcTagProperty<?>) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {

        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "OpcTagProperty{" +
                "key='" + key + '\'' +
                ", description='" + description + '\'' +
                ", value=" + value +
                '}';
    }
}
