package com.boydti.fawe.config;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.configuration.MemorySection;
import com.boydti.fawe.configuration.file.YamlConfiguration;
import com.boydti.fawe.util.StringMan;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {

    public Config() {
        save(new PrintWriter(new ByteArrayOutputStream(0)), getClass(), this, 0);
    }

    /**
     * Get the value for a node<br>
     * Probably throws some error if you try to get a non existent key
     *
     * @param key
     * @param <T>
     * @return
     */
    private <T> T get(String key, Class root) {
        String[] split = key.split("\\.");
        Object instance = getInstance(split, root);
        if (instance != null) {
            Field field = getField(split, instance);
            if (field != null) {
                try {
                    return (T) field.get(instance);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        Fawe.debug("Failed to get config option: " + key);
        return null;
    }

    /**
     * Set the value of a specific node<br>
     * Probably throws some error if you supply non existing keys or invalid values
     *
     * @param key   config node
     * @param value value
     */
    private void set(String key, Object value, Class root) {
        String[] split = key.split("\\.");
        Object instance = getInstance(split, root);
        if (instance != null) {
            Field field = getField(split, instance);
            if (field != null) {
                try {
                    if (field.getAnnotation(Final.class) != null) {
                        return;
                    }
                    if (field.getType() == String.class && !(value instanceof String)) {
                        value = value + "";
                    }
                    // TODO FIXME parsing using bindings
                    field.set(instance, value);
                    return;
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        Fawe.debug("Failed to set config option: " + key + ": " + value + " | " + instance + " | " + root.getSimpleName() + ".yml");
    }

    public boolean load(File file) {
        if (!file.exists()) {
            return false;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(true)) {
            Object value = yml.get(key);
            if (value instanceof MemorySection) {
                continue;
            }
            set(key, value, getClass());
        }
        return true;
    }

    /**
     * Set all values in the file (load first to avoid overwriting)
     *
     * @param file
     */
    public void save(File file) {
        Class<? extends Config> root = getClass();
        try {
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (parent != null) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
            }
            PrintWriter writer = new PrintWriter(file);
            Object instance = this;
            save(writer, getClass(), instance, 0);
            writer.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Indicates that a field should be instantiated / created
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface Create {
    }

    /**
     * Indicates that a field cannot be modified
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface Final {
    }

    /**
     * Creates a comment
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface Comment {
        String[] value();
    }

    /**
     * The names of any default blocks
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface BlockName {
        String[] value();
    }

    /**
     * Any field or class with is not part of the config
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface Ignore {
    }

    @Ignore // This is not part of the config
    public static class ConfigBlock<T> {

        private HashMap<String, T> INSTANCES = new HashMap<>();

        public T remove(String key) {
            return INSTANCES.remove(key);
        }

        public T get(String key) {
            return INSTANCES.get(key);
        }

        public void put(String key, T value) {
            INSTANCES.put(key, value);
        }

        public Collection<T> getInstances() {
            return INSTANCES.values();
        }

        public Collection<String> getSections() {
            return INSTANCES.keySet();
        }

        private Map<String, T> getRaw() {
            return INSTANCES;
        }
    }

    /**
     * Get the static fields in a section
     *
     * @param clazz
     * @return
     */
    private Map<String, Object> getFields(Class clazz) {
        HashMap<String, Object> map = new HashMap<>();
        for (Field field : clazz.getFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                try {
                    map.put(toNodeName(field.getName()), field.get(null));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return map;
    }

    private String toYamlString(Object value, String spacing) {
        if (value instanceof List) {
            Collection<?> listValue = (Collection<?>) value;
            if (listValue.isEmpty()) {
                return "[]";
            }
            StringBuilder m = new StringBuilder();
            for (Object obj : listValue) {
                m.append(System.lineSeparator() + spacing + "- " + toYamlString(obj, spacing));
            }
            return m.toString();
        }
        if (value instanceof String) {
            String stringValue = (String) value;
            if (stringValue.isEmpty()) {
                return "''";
            }
            return "\"" + stringValue + "\"";
        }
        return value != null ? value.toString() : "null";
    }

    private void save(PrintWriter writer, Class clazz, final Object instance, int indent) {
        try {
            String CTRF = System.lineSeparator();
            String spacing = StringMan.repeat(" ", indent);
            HashMap<Class, Object> instances = new HashMap<>();
            for (Field field : clazz.getFields()) {
                if (field.getAnnotation(Ignore.class) != null) {
                    continue;
                }
                Class<?> current = field.getType();
                if (field.getAnnotation(Ignore.class) != null) {
                    continue;
                }
                Comment comment = field.getAnnotation(Comment.class);
                if (comment != null) {
                    for (String commentLine : comment.value()) {
                        writer.write(spacing + "# " + commentLine + CTRF);
                    }
                }
                if (current == ConfigBlock.class) {
                    current = (Class<?>) ((ParameterizedType) (field.getGenericType())).getActualTypeArguments()[0];
                    comment = current.getAnnotation(Comment.class);
                    if (comment != null) {
                        for (String commentLine : comment.value()) {
                            writer.write(spacing + "# " + commentLine + CTRF);
                        }
                    }
                    BlockName blockNames = current.getAnnotation(BlockName.class);
                    if (blockNames != null) {
                        writer.write(spacing + toNodeName(current.getSimpleName()) + ":" + CTRF);
                        ConfigBlock configBlock = (ConfigBlock) field.get(instance);
                        if (configBlock == null || configBlock.getInstances().isEmpty()) {
                            configBlock = new ConfigBlock();
                            field.set(instance, configBlock);
                            for (String blockName : blockNames.value()) {
                                configBlock.put(blockName, current.newInstance());
                            }
                        }
                        // Save each instance
                        for (Map.Entry<String, Object> entry : ((Map<String, Object>) configBlock.getRaw()).entrySet()) {
                            String key = entry.getKey();
                            writer.write(spacing + "  " + toNodeName(key) + ":" + CTRF);
                            save(writer, current, entry.getValue(), indent + 4);
                        }
                    }
                    continue;
                }
                Create create = field.getAnnotation(Create.class);
                if (create != null) {
                    Object value = field.get(instance);
                    setAccessible(field);
                    if (indent == 0) {
                        writer.write(CTRF);
                    }
                    comment = current.getAnnotation(Comment.class);
                    if (comment != null) {
                        for (String commentLine : comment.value()) {
                            writer.write(spacing + "# " + commentLine + CTRF);
                        }
                    }
                    writer.write(spacing + toNodeName(current.getSimpleName()) + ":" + CTRF);
                    if (value == null) {
                        field.set(instance, value = current.newInstance());
                        instances.put(current, value);
                    }
                    save(writer, current, value, indent + 2);
                    continue;
                } else {
                    writer.write(spacing + toNodeName(field.getName() + ": ") + toYamlString(field.get(instance), spacing) + CTRF);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the field for a specific config node
     *
     * @param split the node (split by period)
     * @return
     */
    private Field getField(String[] split, Class root) {
        Object instance = getInstance(split, root);
        if (instance == null) {
            return null;
        }
        return getField(split, instance);
    }

    /**
     * Get the field for a specific config node and instance<br>
     * Note: As expiry can have multiple blocks there will be multiple instances
     *
     * @param split    the node (split by period)
     * @param instance the instance
     * @return
     */
    private Field getField(String[] split, Object instance) {
        try {
            Field field = instance.getClass().getField(toFieldName(split[split.length - 1]));
            setAccessible(field);
            return field;
        } catch (Throwable e) {
            Fawe.debug("Invalid config field: " + StringMan.join(split, ".") + " for " + toNodeName(instance.getClass().getSimpleName()));
            return null;
        }
    }

    private Object getInstance(Object instance, Class clazz) throws IllegalAccessException, InstantiationException {
        try {
            Field instanceField = clazz.getDeclaredField(clazz.getSimpleName());
        } catch (Throwable ignore) {
        }
        return clazz.newInstance();
    }

    /**
     * Get the instance for a specific config node
     *
     * @param split the node (split by period)
     * @return The instance or null
     */
    private Object getInstance(String[] split, Class root) {
        try {
            Class<?> clazz = root == null ? MethodHandles.lookup().lookupClass() : root;
            Object instance = this;
            while (split.length > 0) {
                switch (split.length) {
                    case 1:
                        return instance;
                    default:
                        Class found = null;
                        Class<?>[] classes = clazz.getDeclaredClasses();
                        for (Class current : classes) {
                            if (StringMan.isEqual(current.getSimpleName(), toFieldName(split[0]))) {
                                found = current;
                                break;
                            }
                        }
                        try {
                            Field instanceField = clazz.getDeclaredField(toFieldName(split[0]));
                            setAccessible(instanceField);
                            if (instanceField.getType() != ConfigBlock.class) {
                                Object value = instanceField.get(instance);
                                if (value == null) {
                                    value = found.newInstance();
                                    instanceField.set(instance, value);
                                }
                                clazz = found;
                                instance = value;
                                split = Arrays.copyOfRange(split, 1, split.length);
                                continue;
                            }
                            ConfigBlock value = (ConfigBlock) instanceField.get(instance);
                            if (value == null) {
                                value = new ConfigBlock();
                                instanceField.set(instance, value);
                            }
                            instance = value.get(split[1]);
                            if (instance == null) {
                                instance = found.newInstance();
                                value.put(split[1], instance);
                            }
                            clazz = found;
                            split = Arrays.copyOfRange(split, 2, split.length);
                            continue;
                        } catch (NoSuchFieldException ignore) {
                        }
                        if (found != null) {
                            split = Arrays.copyOfRange(split, 1, split.length);
                            clazz = found;
                            instance = clazz.newInstance();
                            continue;
                        }
                        return null;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Translate a node to a java field name
     *
     * @param node
     * @return
     */
    private String toFieldName(String node) {
        return node.toUpperCase().replaceAll("-", "_");
    }

    /**
     * Translate a field to a config node
     *
     * @param field
     * @return
     */
    private String toNodeName(String field) {
        return field.toLowerCase().replace("_", "-");
    }

    /**
     * Set some field to be accesible
     *
     * @param field
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private void setAccessible(Field field) throws NoSuchFieldException, IllegalAccessException {
        field.setAccessible(true);
        if (Modifier.isFinal(field.getModifiers())) {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        }
    }
}
