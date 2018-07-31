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
import java.util.Optional;

/**
 * Basic information (metadata) of an object (can be any item folder or item).
 *
 * @author amarziali
 */
public abstract class OpcObjectInfo<T extends OpcObjectInfo<T>> {

    /**
     * Construct a new instance.
     *
     * @param id the unique id.
     */
    public OpcObjectInfo(String id) {
        this.id = id;
    }

    /**
     * The item id.
     */
    private final String id;
    /**
     * The item name.
     */
    private String name;
    /**
     * The item description (if available).
     */
    private Optional<String> description = Optional.empty();


    /**
     * Get the item id.
     *
     * @return the item unique id.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the description. The field is optional but never null.
     *
     * @return a optional description.
     */
    public Optional<String> getDescription() {
        return description;
    }


    /**
     * Set the optionally empty description.
     *
     * @param description a never null {@link Optional} description.
     */
    public void setDescription(Optional<String> description) {
        this.description = description;
    }

    /**
     * Gets the item name
     *
     * @return the never null item name.
     */
    public String getName() {
        return name;
    }


    /**
     * Sets the item name.
     *
     * @param name the never null item name.
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Sets the item name.
     *
     * @param name the never null item name.
     * @return itself
     */
    public T withName(String name) {
        setName(name);
        return (T) this;
    }


    /**
     * Set the optionally empty description.
     *
     * @param description a never null {@link Optional} description.
     * @return itself
     */
    public T withDescription(String description) {
        setDescription(Optional.ofNullable(description));
        return (T) this;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpcObjectInfo that = (OpcObjectInfo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


    @Override
    public String toString() {
        return "OpcObjectInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description=" + description +
                '}';
    }
}
