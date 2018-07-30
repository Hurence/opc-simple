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

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Holds metadata about a tag
 *
 * @author amarziali
 */
public class OpcTagInfo extends OpcObjectInfo<OpcTagInfo> {

    public OpcTagInfo(String id) {
        super(id);
    }

    /**
     * The java {@link Type} corresponding to the item data type.
     */
    private Type type;

    /**
     * The server scan rate (if available)
     */
    private Optional<Duration> scanRate = Optional.empty();

    /**
     * The access rights (always non null).
     */
    private final OpcTagAccessRights accessRights = new OpcTagAccessRights();
    /**
     * The item properties if any. See {@link OpcTagProperty} for further details.
     */
    private Set<OpcTagProperty> properties = new HashSet<>();

    /**
     * Sets the value data type.
     *
     * @param type the Java {@link Type} linked to the value.
     *             Use {@link Void} if the item does not carry any value.
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * The item properties.
     *
     * @return a {@link Set} of {@link OpcTagProperty}
     */
    public Set<OpcTagProperty> getProperties() {
        return properties;
    }

    /**
     * Sets the item properties.
     *
     * @param properties a never null {@link Set} of {@link OpcTagProperty}
     */
    public void setProperties(Set<OpcTagProperty> properties) {
        this.properties = properties;
    }

    /**
     * Gets the optional scan rate.
     *
     * @return the optionally empty (but never null) scan rate {@link Duration}
     */
    public Optional<Duration> getScanRate() {
        return scanRate;
    }

    /**
     * Gets the item data type.
     *
     * @return the Java {@link Type} of the item value.
     */
    public Type getType() {
        return type;
    }

    /**
     * Sets the scan rate.
     *
     * @param scanRate the {@link Optional} never null {@link Duration}
     */
    public void setScanRate(Optional<Duration> scanRate) {
        this.scanRate = scanRate;
    }


    /**
     * Adds a property to this item.
     *
     * @param property the not null {@link OpcTagProperty}
     * @return itself.
     */
    public synchronized OpcObjectInfo addProperty(OpcTagProperty property) {
        properties.add(property);
        return this;
    }

    /**
     * Sets the value data type.
     *
     * @param type the Java {@link Type} linked to the value.
     *             Use {@link Void} if the item does not carry any value.
     * @return itself
     */
    public OpcTagInfo withType(Type type) {
        setType(type);
        return this;
    }

    /**
     * Sets the scan rate.
     *
     * @param scanRate the {@link Optional} never null {@link Duration}
     * @return itself
     */
    public OpcTagInfo withScanRate(Duration scanRate) {
        setScanRate(Optional.ofNullable(scanRate));
        return this;
    }

    /**
     * Gets the never null access rights.
     *
     * @return the {@link OpcTagAccessRights}
     */
    public OpcTagAccessRights getAccessRights() {
        return accessRights;
    }

    /**
     * Sets the read access rights.
     *
     * @param readable true if item value can be read.
     * @return itself
     */
    public OpcTagInfo withReadAccessRights(boolean readable) {
        getAccessRights().setReadable(readable);
        return this;
    }

    /**
     * Sets the write access rights.
     *
     * @param writable true if item value can be written.
     * @return itself
     */
    public OpcTagInfo withWriteAccessRights(boolean writable) {
        getAccessRights().setWritable(writable);
        return this;
    }

    @Override
    public String toString() {
        return "OpcTagInfo{" +
                "type=" + type +
                ", scanRate=" + scanRate +
                ", accessRights=" + accessRights +
                ", properties=" + properties +
                "} " + super.toString();
    }


}
