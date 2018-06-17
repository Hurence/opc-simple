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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Basic information (metadata) of a tag.
 *
 * @author amarziali
 */
public class OpcTagInfo {

    /**
     * Construct a new instance.
     *
     * @param id the unique id.
     */
    public OpcTagInfo(String id) {
        this.id = id;
    }

    /**
     * The tag id.
     */
    private final String id;
    /**
     * The tag name.
     */
    private String name;
    /**
     * The item description (if available).
     */
    private Optional<String> description = Optional.empty();
    /**
     * The Group (hierarchically separated by a dot '.' character
     */
    private String group;
    /**
     * The java {@link Type} corresponding to the tag data type.
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
     * The tag properties if any. See {@link OpcTagProperty} for further details.
     */
    private Set<OpcTagProperty> properties = new HashSet<>();

    /**
     * Get the tag id.
     *
     * @return the tag unique id.
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

    /**
     * Set the optionally empty description.
     *
     * @param description a never null {@link Optional} description.
     */
    public void setDescription(Optional<String> description) {
        this.description = description;
    }

    /**
     * Gets the tag name
     *
     * @return the never null tag name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the tag group (hierarchically separated by 'dot')
     *
     * @return the never null (empty for the root) tag group.
     */
    public String getGroup() {
        return group;
    }

    /**
     * Gets the tag data type.
     *
     * @return the Java {@link Type} of the tag value.
     */
    public Type getType() {
        return type;
    }

    /**
     * Sets the tag name.
     *
     * @param name the never null tag name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the tag group.
     *
     * @param group the never null (can be empty) group name.
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * Sets the value data type.
     *
     * @param type the Java {@link Type} linked to the value.
     *             Use {@link Void} if the tag does not carry any value.
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * The tag properties.
     *
     * @return a {@link Set} of {@link OpcTagProperty}
     */
    public Set<OpcTagProperty> getProperties() {
        return properties;
    }

    /**
     * Sets the tag properties.
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
     * Sets the scan rate.
     *
     * @param scanRate the {@link Optional} never null {@link Duration}
     */
    public void setScanRate(Optional<Duration> scanRate) {
        this.scanRate = scanRate;
    }


    /**
     * Adds a property to this tag.
     *
     * @param property the not null {@link OpcTagProperty}
     * @return itself.
     */
    public synchronized OpcTagInfo addProperty(OpcTagProperty property) {
        properties.add(property);
        return this;
    }

    /**
     * Sets the tag name.
     *
     * @param name the never null tag name.
     * @return itself
     */
    public OpcTagInfo withName(String name) {
        setName(name);
        return this;
    }

    /**
     * Sets the tag group.
     *
     * @param group the never null (can be empty) group name.
     * @return itself
     */
    public OpcTagInfo withGroup(String group) {
        setGroup(group);
        return this;
    }

    /**
     * Sets the value data type.
     *
     * @param type the Java {@link Type} linked to the value.
     *             Use {@link Void} if the tag does not carry any value.
     * @return itself
     */
    public OpcTagInfo withType(Type type) {
        setType(type);
        return this;
    }

    /**
     * Set the optionally empty description.
     *
     * @param description a never null {@link Optional} description.
     * @return itself
     */
    public OpcTagInfo withDescription(String description) {
        setDescription(Optional.ofNullable(description));
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpcTagInfo that = (OpcTagInfo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


    @Override
    public String toString() {
        return "OpcTagInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description=" + description +
                ", group='" + group + '\'' +
                ", type=" + type +
                ", scanRate=" + scanRate +
                ", accessRights=" + accessRights +
                ", properties=" + properties +
                '}';
    }
}
