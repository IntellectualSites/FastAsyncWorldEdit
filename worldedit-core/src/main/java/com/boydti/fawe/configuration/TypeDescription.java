/**
 * Copyright (c) 2008, http://www.snakeyaml.org
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.yaml.snakeyaml;

import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.nodes.Tag;

/**
 * Provides additional runtime information necessary to create a custom Java
 * instance.
 */
public final class TypeDescription {
    private final Class<? extends Object> type;
    private Tag tag;
    private Map<String, Class<? extends Object>> listProperties;
    private Map<String, Class<? extends Object>> keyProperties;
    private Map<String, Class<? extends Object>> valueProperties;

    public TypeDescription(Class<? extends Object> clazz, Tag tag) {
        this.type = clazz;
        this.tag = tag;
        listProperties = new HashMap<String, Class<? extends Object>>();
        keyProperties = new HashMap<String, Class<? extends Object>>();
        valueProperties = new HashMap<String, Class<? extends Object>>();
    }

    public TypeDescription(Class<? extends Object> clazz, String tag) {
        this(clazz, new Tag(tag));
    }

    public TypeDescription(Class<? extends Object> clazz) {
        this(clazz, (Tag) null);
    }

    /**
     * Get tag which shall be used to load or dump the type (class).
     *
     * @return tag to be used. It may be a tag for Language-Independent Types
     *         (http://www.yaml.org/type/)
     */
    public Tag getTag() {
        return tag;
    }

    /**
     * Set tag to be used to load or dump the type (class).
     *
     * @param tag
     *            local or global tag
     */
    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public void setTag(String tag) {
        setTag(new Tag(tag));
    }

    /**
     * Get represented type (class)
     *
     * @return type (class) to be described.
     */
    public Class<? extends Object> getType() {
        return type;
    }

    /**
     * Specify that the property is a type-safe <code>List</code>.
     *
     * @param property
     *            name of the JavaBean property
     * @param type
     *            class of List values
     */
    public void putListPropertyType(String property, Class<? extends Object> type) {
        listProperties.put(property, type);
    }

    /**
     * Get class of List values for provided JavaBean property.
     *
     * @param property
     *            property name
     * @return class of List values
     */
    public Class<? extends Object> getListPropertyType(String property) {
        return listProperties.get(property);
    }

    /**
     * Specify that the property is a type-safe <code>Map</code>.
     *
     * @param property
     *            property name of this JavaBean
     * @param key
     *            class of keys in Map
     * @param value
     *            class of values in Map
     */
    public void putMapPropertyType(String property, Class<? extends Object> key,
                                   Class<? extends Object> value) {
        keyProperties.put(property, key);
        valueProperties.put(property, value);
    }

    /**
     * Get keys type info for this JavaBean
     *
     * @param property
     *            property name of this JavaBean
     * @return class of keys in the Map
     */
    public Class<? extends Object> getMapKeyType(String property) {
        return keyProperties.get(property);
    }

    /**
     * Get values type info for this JavaBean
     *
     * @param property
     *            property name of this JavaBean
     * @return class of values in the Map
     */
    public Class<? extends Object> getMapValueType(String property) {
        return valueProperties.get(property);
    }

    @Override
    public String toString() {
        return "TypeDescription for " + getType() + " (tag='" + getTag() + "')";
    }
}
