package com.fastasyncworldedit.core.configuration;

import com.fastasyncworldedit.core.configuration.file.YamlConfiguration;
import com.fastasyncworldedit.core.util.StringMan;
import com.sk89q.util.StringUtil;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Config {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private final Map<String, Object> removedKeyVals = new HashMap<>();
    @Nullable
    private Map<String, FromNode> copyTo = new HashMap<>();
    private boolean performCopyTo = false;
    private List<String> existingMigrateNodes = null;

    public Config() {
        // This is now definitely required as the save -> load -> save order means the @CopiedFrom annotated fields work
        save(new PrintWriter(new ByteArrayOutputStream(0)), getClass(), this, 0, null);
        performCopyTo = true;
    }

    /**
     * Get the value for a node. Probably throws some error if you try to get a non-existent key.
     */
    private <T> T get(String key, Class<?> root) {
        String[] split = key.split("\\.");
        Object instance = getInstance(split, root);
        if (instance != null) {
            Field field = getField(split, instance);
            if (field != null) {
                try {
                    return (T) field.get(instance);
                } catch (IllegalAccessException e) {
                    LOGGER.error("Failed to get config option: {}", key, e);
                    return null;
                }
            }
        }
        LOGGER.error("Failed to get config option: {}", key);
        return null;
    }

    /**
     * Set the value of a specific node. Probably throws some error if you supply non existing keys or invalid values.
     * This should only be called during loading of a config file
     *
     * @param key   config node
     * @param value value
     */
    private void setLoadedNode(String key, Object value, Class<?> root) {
        String[] split = key.split("\\.");
        Object instance = getInstance(split, root);
        if (instance != null) {
            Field field = getField(split, instance);
            if (field != null) {
                try {
                    if (field.getAnnotation(Final.class) != null) {
                        return;
                    }
                    if (copyTo != null) {
                        copyTo.remove(key); // Remove if the config field is already written
                        final Object finalValue = value;
                        copyTo.replaceAll((copyToNode, fromNode) -> {
                            if (!key.equals(fromNode.node())) {
                                return fromNode;
                            }
                            return new FromNode(key, fromNode.computation, finalValue);
                        });
                    }
                    Migrate migrate = field.getAnnotation(Migrate.class);
                    if (migrate != null) {
                        existingMigrateNodes.add(migrate.value());
                    }
                    if (field.getType() == String.class && !(value instanceof String)) {
                        value = value + "";
                    }
                    // TODO FIXME parsing using bindings
                    field.set(instance, value);
                    return;
                } catch (Throwable e) {
                    LOGGER.error("Failed to set config option: {}", key);
                }
            }
        }
        removedKeyVals.put(key, value);
        LOGGER.warn(
                "Failed to set config option: {}: {} | {} | {}.yml. This is likely because it was removed or was set with an " +
                        "invalid value.",
                key,
                value,
                instance,
                root.getSimpleName()
        );
    }

    public boolean load(File file) {
        if (!file.exists()) {
            return false;
        }
        existingMigrateNodes = new ArrayList<>();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(true)) {
            Object value = yml.get(key);
            if (value instanceof MemorySection) {
                continue;
            }
            setLoadedNode(key, value, getClass());
        }
        for (String node : existingMigrateNodes) {
            removedKeyVals.remove(node);
        }
        existingMigrateNodes = null;
        return true;
    }

    /**
     * Set all values in the file (load first to avoid overwriting).
     */
    public void save(File file) {
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
            save(writer, getClass(), instance, 0, null);
            writer.close();
        } catch (Throwable e) {
            LOGGER.error("Failed to save config file: {}", file, e);
        }
    }

    /**
     * Indicates that a field should be instantiated / created.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface Create {

    }

    /**
     * Indicates that a field cannot be modified.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface Final {

    }

    /**
     * Creates a comment.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface Comment {

        String[] value();

    }

    /**
     * The names of any default blocks.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface BlockName {

        String[] value();

    }

    /**
     * Any field or class with is not part of the config.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface Ignore {

    }

    /**
     * Indicates that a field should be migrated from a node that is deleted
     *
     * @since 2.10.0
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface Migrate {

        String value();

    }

    /**
     * Indicates that a field's default value should match another input if the config is otherwise already generated
     *
     * @since 2.11.0
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface CopiedFrom {

        String value();

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    @interface ComputedFrom {

        String node();

        Class<? extends ConfigOptComputation<?>> computer();

    }

    @Ignore // This is not part of the config
    public static class ConfigBlock<T> {

        private final HashMap<String, T> INSTANCES = new HashMap<>();

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

    private String toYamlString(Object value, String spacing) {
        if (value instanceof List) {
            Collection<?> listValue = (Collection<?>) value;
            if (listValue.isEmpty()) {
                return "[]";
            }
            StringBuilder m = new StringBuilder();
            for (Object obj : listValue) {
                m.append(System.lineSeparator()).append(spacing).append("- ").append(toYamlString(obj, spacing));
            }
            return m.toString();
        }
        if (value instanceof String stringValue) {
            if (stringValue.isEmpty()) {
                return "''";
            }
            return "\"" + stringValue + "\"";
        }
        return value != null ? value.toString() : "null";
    }

    private void save(PrintWriter writer, Class<?> clazz, final Object instance, int indent, String parentNode) {
        try {
            String CTRF = System.lineSeparator();
            String spacing = StringMan.repeat(" ", indent);
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
                    handleConfigBlockSave(writer, instance, indent, field, spacing, CTRF, current, parentNode);
                    continue;
                } else if (!removedKeyVals.isEmpty()) {
                    Migrate migrate = field.getAnnotation(Migrate.class);
                    Object value;
                    if (migrate != null && (value = removedKeyVals.remove(migrate.value())) != null) {
                        field.set(instance, value);
                    }
                }
                CopiedFrom copiedFrom;
                if (copyTo != null && (copiedFrom = field.getAnnotation(CopiedFrom.class)) != null) {
                    String node = toNodeName(field.getName());
                    node = parentNode == null ? node : parentNode + "." + node;
                    FromNode entry = copyTo.remove(node);
                    Object copiedVal;
                    if (entry == null) {
                        copyTo.put(node, new FromNode(copiedFrom.value(), null, null));
                    } else if ((copiedVal = entry.val()) != null) {
                        field.set(instance, copiedVal);
                    }
                }
                ComputedFrom computedFrom;
                if (copyTo != null && (computedFrom = field.getAnnotation(ComputedFrom.class)) != null) {
                    String node = toNodeName(field.getName());
                    node = parentNode == null ? node : parentNode + "." + node;
                    FromNode entry = copyTo.remove(node);
                    Object copiedVal;
                    if (entry == null) {
                        copyTo.put(node, new FromNode(computedFrom.node(), computedFrom.computer(), null));
                    } else if ((copiedVal = entry.val()) != null) {
                        ConfigOptComputation<?> computer = computedFrom.computer().getDeclaredConstructor().newInstance();
                        field.set(instance, computer.apply(copiedVal));
                    }
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
                    String node = toNodeName(current.getSimpleName());
                    writer.write(spacing + node + ":" + CTRF);
                    if (value == null) {
                        field.set(instance, value = current.getDeclaredConstructor().newInstance());
                    }
                    save(writer, current, value, indent + 2, parentNode == null ? node : parentNode + "." + node);
                } else {
                    writer.write(spacing + toNodeName(field.getName() + ": ") + toYamlString(
                            field.get(instance),
                            spacing
                    ) + CTRF);
                }
            }
        } catch (Throwable e) {
            LOGGER.error("Failed to save config file", e);
        }
        if (parentNode == null && performCopyTo) {
            performCopyTo = false;
            copyTo = null;
        }
    }

    private <T> void handleConfigBlockSave(
            PrintWriter writer,
            Object instance,
            int indent,
            Field field,
            String spacing,
            String CTRF,
            Class<T> current,
            String parentNode
    ) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        Comment comment = current.getAnnotation(Comment.class);
        if (comment != null) {
            for (String commentLine : comment.value()) {
                writer.write(spacing + "# " + commentLine + CTRF);
            }
        }
        BlockName blockNames = current.getAnnotation(BlockName.class);
        if (blockNames != null) {
            String node = toNodeName(current.getSimpleName());
            writer.write(spacing + toNodeName(current.getSimpleName()) + ":" + CTRF);
            ConfigBlock<T> configBlock = (ConfigBlock<T>) field.get(instance);
            if (configBlock == null || configBlock.getInstances().isEmpty()) {
                configBlock = new ConfigBlock<>();
                field.set(instance, configBlock);
                for (String blockName : blockNames.value()) {
                    configBlock.put(blockName, current.getDeclaredConstructor().newInstance());
                }
            }
            // Save each instance
            for (Map.Entry<String, T> entry : configBlock.getRaw().entrySet()) {
                String key = entry.getKey();
                writer.write(spacing + "  " + toNodeName(key) + ":" + CTRF);
                save(writer, current, entry.getValue(), indent + 4, parentNode == null ? node : parentNode + "." + node);
            }
        }
    }

    /**
     * Get the field for a specific config node and instance.
     * <p>
     * As expiry can have multiple blocks there will be multiple instances
     *
     * @param split    the node (split by period)
     * @param instance the instance
     */
    private Field getField(String[] split, Object instance) {
        try {
            Field field = instance.getClass().getField(toFieldName(split[split.length - 1]));
            setAccessible(field);
            return field;
        } catch (Throwable ignored) {
            LOGGER.warn(
                    "Invalid config field: {} for {}. It is possible this is because it has been removed.",
                    StringMan.join(split, "."),
                    toNodeName(instance.getClass().getSimpleName())
            );
            return null;
        }
    }

    /**
     * Get the instance for a specific config node.
     *
     * @param split the node (split by period)
     * @return The instance or null
     */
    private Object getInstance(String[] split, Class<?> root) {
        try {
            Class<?> clazz = root == null ? MethodHandles.lookup().lookupClass() : root;
            Object instance = this;
            while (split.length > 0) {
                if (split.length == 1) {
                    return instance;
                }
                Class<?> found = null;
                Class<?>[] classes = clazz.getDeclaredClasses();
                for (Class<?> current : classes) {
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
                            value = found.getDeclaredConstructor().newInstance();
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
                        instance = found.getDeclaredConstructor().newInstance();
                        value.put(split[1], instance);
                    }
                    clazz = found;
                    split = Arrays.copyOfRange(split, 2, split.length);
                    continue;
                } catch (NoSuchFieldException ignored) {
                }
                if (found != null) {
                    split = Arrays.copyOfRange(split, 1, split.length);
                    clazz = found;
                    instance = clazz.getDeclaredConstructor().newInstance();
                    continue;
                }
                return null;
            }
        } catch (Throwable e) {
            LOGGER.error("Failed retrieving instance for config node: {}", StringUtil.joinString(split, "."), e);
        }
        return null;
    }

    /**
     * Translate a node to a java field name.
     */
    private String toFieldName(String node) {
        return node.toUpperCase(Locale.ROOT).replaceAll("-", "_");
    }

    /**
     * Translate a field to a config node.
     */
    private String toNodeName(String field) {
        return field.toLowerCase(Locale.ROOT).replace("_", "-");
    }

    /**
     * Set some field to be accessible.
     */
    private void setAccessible(Field field) throws NoSuchFieldException, IllegalAccessException {
        field.setAccessible(true);
        if (Modifier.isFinal(field.getModifiers())) {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        }
    }

    private record FromNode(String node, Class<? extends ConfigOptComputation<?>> computation, Object val) {

    }

}
