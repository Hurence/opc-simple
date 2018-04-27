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
import java.util.HashSet;
import java.util.Set;

public class OpcTagInfo {

    private String name;
    private String group;
    private Type type;
    private Set<OpcTagProperty> properties = new HashSet<>();


    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public Type getType() {
        return type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Set<OpcTagProperty> getProperties() {
        return properties;
    }

    public void setProperties(Set<OpcTagProperty> properties) {
        this.properties = properties;
    }

    public OpcTagInfo addProperty(OpcTagProperty property) {
        properties.add(property);
        return this;
    }

    public OpcTagInfo withName(String name) {
        setName(name);
        return this;
    }

    public OpcTagInfo withGroup(String group) {
        setGroup(group);
        return this;
    }

    public OpcTagInfo withType(Type type) {
        setType(type);
        return this;
    }

    @Override
    public String toString() {
        return "OpcTagInfo{" +
                "name='" + name + '\'' +
                ", group='" + group + '\'' +
                ", type=" + type +
                ", properties=" + properties +
                '}';
    }
}
