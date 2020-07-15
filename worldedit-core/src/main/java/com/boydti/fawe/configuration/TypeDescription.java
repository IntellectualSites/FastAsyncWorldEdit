package org.yaml.snakeyaml;

import org.yaml.snakeyaml.nodes.Tag;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides additional runtime information necessary to create a custom Java
 * instance.
 */
public final class TypeDescription {
    private final Class<?> type;
    private Tag tag;
    private Map<String, Class<?>> listProperties;
    private Map<String, Class<?>> keyProperties;
    private Map<String, Class<?>> valueProperties;

    public TypeDescription(Class<?> clazz, Tag tag) {
        this.type = clazz;
        this.tag = tag;
        listProperties = new HashMap<>();
        keyProperties = new HashMap<>();
        valueProperties = new HashMap<>();
    }

    public TypeDescription(Class<?> clazz, String tag) {
        this(clazz, new Tag(tag));
    }

    public TypeDescription(Class<?> clazz) {
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
     * @param tag local or global tag
     */
    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public void setTag(String tag) {
        setTag(new Tag(tag));
    }

    /**
     * Get represented type (class).
     *
     * @return type (class) to be described.
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * Specify that the property is a type-safe <code>List</code>.
     *
     * @param property name of the JavaBean property
     * @param type class of List values
     */
    public void putListPropertyType(String property, Class<?> type) {
        listProperties.put(property, type);
    }

    /**
     * Get class of List values for provided JavaBean property.
     *
     * @param property property name
     * @return class of List values
     */
    public Class<?> getListPropertyType(String property) {
        return listProperties.get(property);
    }

    /**
     * Specify that the property is a type-safe <code>Map</code>.
     *
     * @param property property name of this JavaBean
     * @param key class of keys in Map
     * @param value class of values in Map
     */
    public void putMapPropertyType(String property, Class<?> key,
                                   Class<?> value) {
        keyProperties.put(property, key);
        valueProperties.put(property, value);
    }

    /**
     * Get keys type info for this JavaBean.
     *
     * @param property property name of this JavaBean
     * @return class of keys in the Map
     */
    public Class<?> getMapKeyType(String property) {
        return keyProperties.get(property);
    }

    /**
     * Get values type info for this JavaBean.
     *
     * @param property property name of this JavaBean
     * @return class of values in the Map
     */
    public Class<?> getMapValueType(String property) {
        return valueProperties.get(property);
    }

    @Override
    public String toString() {
        return "TypeDescription for " + getType() + " (tag='" + getTag() + "')";
    }
}
