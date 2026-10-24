package com.fastasyncworldedit.core.configuration;

import javax.annotation.Nullable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Maps configuration records to and from YAML.
 * <p>
 * A configuration record may declare components of type {@code boolean}, {@code int}, {@code long}, {@link String}, an
 * enum, {@code List<String>}, another configuration record (a section) or {@code Map<String, R>} with {@code R} a
 * configuration record (named groups sharing one layout). A component named {@code parallelThreads} is stored under
 * {@code parallel-threads}, in declaration order, below the lines of its {@link Comment}.
 *
 * @since 3.0.0
 */
final class ConfigMapper {

    private static final ClassValue<Schema> SCHEMAS = new ClassValue<>() {
        @Override
        protected Schema computeValue(Class<?> type) {
            return Schema.of(type.asSubclass(Record.class));
        }
    };

    private ConfigMapper() {
    }

    /**
     * Creates a record from the {@link Default} of its options.
     *
     * @param computed the defaults of the options that have no {@link Default} because their value is only known at
     *                 runtime, by dotted path. The options of named groups are addressed without the name of a group,
     *                 e.g. {@code limits.max-radius}, and apply to each of them
     * @throws IllegalArgumentException if an option has neither kind of default or a computed default has no option
     */
    static <R extends Record> R defaults(Class<R> type, Map<String, Object> computed) {
        Set<String> unused = new LinkedHashSet<>(computed.keySet());
        R defaults = type.cast(defaults(type, "", computed, unused));
        if (!unused.isEmpty()) {
            throw new IllegalArgumentException("No options without a @Default at " + unused);
        }
        return defaults;
    }

    @SuppressWarnings("unchecked")
    private static Record defaults(Class<?> type, String path, Map<String, Object> computed, Set<String> unused) {
        Schema schema = SCHEMAS.get(type);
        Object[] values = new Object[schema.entries.size()];
        for (int i = 0; i < values.length; i++) {
            Entry entry = schema.entries.get(i);
            String entryPath = path.isEmpty() ? entry.key : path + "." + entry.key;
            values[i] = switch (entry.kind) {
                case SECTION -> defaults(entry.type, entryPath, computed, unused);
                case GROUPS -> {
                    if (entry.defaultValue == null) {
                        throw new IllegalArgumentException(entryPath + " needs a @Default naming its groups");
                    }
                    Map<String, Record> groups = new LinkedHashMap<>();
                    for (String name : (List<String>) entry.defaultValue) {
                        groups.put(name, defaults(entry.type, entryPath, computed, unused));
                    }
                    yield Collections.unmodifiableMap(groups);
                }
                default -> {
                    if (entry.defaultValue != null) {
                        yield entry.defaultValue;
                    }
                    Object value = coerce(entry, computed.get(entryPath));
                    if (value == null) {
                        throw new IllegalArgumentException(entryPath + " has no @Default and no usable computed default");
                    }
                    unused.remove(entryPath);
                    yield value;
                }
            };
        }
        return schema.create(values);
    }

    /**
     * Creates a record from a parsed file. Options that are missing or hold a value of the wrong type keep the value
     * they have in {@code defaults}.
     *
     * @param tree     the parsed file, see {@link ConfigYaml#read}
     * @param defaults values for everything the file does not set. A group of a {@code Map<String, R>} component that
     *                 the file adds takes its missing options from the first group of the defaults
     * @param problems receives a line for each value that was rejected and each option that is not known
     */
    static <R extends Record> R bind(Map<String, Object> tree, R defaults, List<String> problems) {
        return bind(tree, defaults, "", problems);
    }

    @SuppressWarnings("unchecked")
    private static <R extends Record> R bind(@Nullable Object node, R defaults, String path, List<String> problems) {
        Schema schema = SCHEMAS.get(defaults.getClass());
        Map<?, ?> section = Map.of();
        if (node instanceof Map<?, ?> map) {
            section = map;
        } else if (node != null) {
            problems.add(path + ": expected a section but found " + describe(node));
        }
        Set<Object> unknown = new LinkedHashSet<>(section.keySet());
        Object[] values = new Object[schema.entries.size()];
        for (int i = 0; i < values.length; i++) {
            Entry entry = schema.entries.get(i);
            String entryPath = path.isEmpty() ? entry.key : path + "." + entry.key;
            Object fallback = entry.read(defaults);
            Object value = section.get(entry.key);
            unknown.remove(entry.key);
            values[i] = switch (entry.kind) {
                case SECTION -> bind(value, (Record) fallback, entryPath, problems);
                case GROUPS -> bindGroups(value, (Map<String, Record>) fallback, entryPath, problems);
                default -> {
                    if (!section.containsKey(entry.key)) {
                        yield fallback;
                    }
                    Object coerced = coerce(entry, value);
                    if (coerced == null) {
                        problems.add(entryPath + ": expected " + entry.expected() + " but found " + describe(value)
                                + ", using " + describe(fallback));
                        yield fallback;
                    }
                    yield coerced;
                }
            };
        }
        for (Object key : unknown) {
            problems.add((path.isEmpty() ? "" : path + ".") + key + ": unknown option, it will be removed");
        }
        return (R) schema.create(values);
    }

    private static Map<String, Record> bindGroups(
            @Nullable Object node,
            Map<String, Record> defaults,
            String path,
            List<String> problems
    ) {
        if (!(node instanceof Map<?, ?> groups)) {
            if (node != null) {
                problems.add(path + ": expected a section but found " + describe(node));
            }
            return defaults;
        }
        if (groups.isEmpty()) {
            return defaults;
        }
        Record template = defaults.values().iterator().next();
        Map<String, Record> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> group : groups.entrySet()) {
            String name = String.valueOf(group.getKey());
            result.put(name, bind(group.getValue(), template, path + "." + name, problems));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Converts a parsed value to the type of the entry.
     *
     * @return the converted value or null if the value does not fit
     */
    @Nullable
    private static Object coerce(Entry entry, @Nullable Object value) {
        return switch (entry.kind) {
            case BOOLEAN -> value instanceof Boolean ? value : null;
            case INT -> value instanceof Integer ? value : null;
            case LONG -> value instanceof Integer || value instanceof Long ? (Long) ((Number) value).longValue() : null;
            case STRING -> isText(value) ? value.toString() : null;
            case ENUM -> {
                if (value instanceof String name) {
                    for (Object constant : entry.type.getEnumConstants()) {
                        if (((Enum<?>) constant).name().equalsIgnoreCase(name)) {
                            yield constant;
                        }
                    }
                }
                yield entry.type.isInstance(value) ? value : null;
            }
            case STRINGS -> {
                if (!(value instanceof List<?> list)) {
                    yield null;
                }
                String[] strings = new String[list.size()];
                for (int i = 0; i < strings.length; i++) {
                    Object element = list.get(i);
                    if (!isText(element)) {
                        yield null;
                    }
                    strings[i] = element.toString();
                }
                yield List.of(strings);
            }
            case SECTION, GROUPS -> throw new IllegalStateException(entry.key + " is not a value");
        };
    }

    private static boolean isText(@Nullable Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean;
    }

    private static String describe(@Nullable Object value) {
        if (value instanceof Map<?, ?>) {
            return "a section";
        }
        return value instanceof String ? '"' + value.toString() + '"' : String.valueOf(value);
    }

    /**
     * Copies a record with one option replaced.
     *
     * @param path the dotted path of the option as written in the file, e.g. {@code queue.parallel-threads}
     * @throws IllegalArgumentException if there is no such option or the value does not fit it
     */
    static <R extends Record> R with(R record, String path, Object value) {
        return with(record, path, 0, value);
    }

    /**
     * Finds the dotted path of the option an accessor reference names.
     *
     * @param root   the record of the whole configuration
     * @param option a reference to the accessor of the option, e.g. {@code Settings.Queue::parallelThreads}
     * @throws IllegalArgumentException if the argument is not a reference to an accessor of an option, or the option is
     *                                  in a named group
     */
    static String pathOf(Class<? extends Record> root, Option<?> option) {
        String path = null;
        try {
            // A serializable method reference can describe itself, which is the only way to learn what it refers to
            Method writeReplace = option.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda reference = (SerializedLambda) writeReplace.invoke(option);
            path = pathOf(root, reference.getImplClass(), reference.getImplMethodName(), "");
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Not a method reference, reported below
        }
        if (path == null) {
            throw new IllegalArgumentException("An option is named by a reference to its accessor, such as "
                    + "Settings.Queue::parallelThreads, and cannot be in a named group");
        }
        return path;
    }

    @Nullable
    private static String pathOf(Class<?> type, String internalName, String accessor, String path) {
        boolean declares = type.getName().replace('.', '/').equals(internalName);
        for (Entry entry : SCHEMAS.get(type).entries) {
            String entryPath = path.isEmpty() ? entry.key : path + "." + entry.key;
            if (entry.kind == Kind.SECTION) {
                String found = pathOf(entry.type, internalName, accessor, entryPath);
                if (found != null) {
                    return found;
                }
            } else if (declares && entry.kind != Kind.GROUPS && entry.component.getName().equals(accessor)) {
                return entryPath;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <R extends Record> R with(R record, String path, int start, Object value) {
        Schema schema = SCHEMAS.get(record.getClass());
        int end = path.indexOf('.', start);
        String key = end < 0 ? path.substring(start) : path.substring(start, end);
        Object[] values = new Object[schema.entries.size()];
        boolean found = false;
        for (int i = 0; i < values.length; i++) {
            Entry entry = schema.entries.get(i);
            values[i] = entry.read(record);
            if (!entry.key.equals(key)) {
                continue;
            }
            found = true;
            values[i] = switch (entry.kind) {
                case SECTION -> end < 0 ? null : with((Record) values[i], path, end + 1, value);
                case GROUPS -> {
                    int groupEnd = end < 0 ? -1 : path.indexOf('.', end + 1);
                    Map<String, Record> groups = new LinkedHashMap<>((Map<String, Record>) values[i]);
                    Record group = groupEnd < 0 ? null : groups.get(path.substring(end + 1, groupEnd));
                    if (group == null) {
                        yield null;
                    }
                    groups.put(path.substring(end + 1, groupEnd), with(group, path, groupEnd + 1, value));
                    yield Collections.unmodifiableMap(groups);
                }
                default -> end < 0 ? coerce(entry, value) : null;
            };
            if (values[i] == null) {
                throw new IllegalArgumentException("Cannot set " + path + " to " + describe(value));
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Unknown option " + path);
        }
        return (R) schema.create(values);
    }

    /**
     * Writes the entries of a record as the top level of a file.
     *
     * @param spaced if an empty line should be left above each section directly inside this one
     */
    static void write(Record record, boolean spaced, ConfigYaml.Writer out) {
        write(record, spaced, out, 0);
    }

    @SuppressWarnings("unchecked")
    private static void write(Record record, boolean spaced, ConfigYaml.Writer out, int indent) {
        for (Entry entry : SCHEMAS.get(record.getClass()).entries) {
            Object value = entry.read(record);
            if (spaced && (entry.kind == Kind.SECTION || entry.kind == Kind.GROUPS)) {
                out.blankLine();
            }
            out.comments(indent, entry.comments).key(indent, entry.key);
            switch (entry.kind) {
                case SECTION -> writeSection((Record) value, out, indent + 2);
                case GROUPS -> {
                    Map<String, Record> groups = (Map<String, Record>) value;
                    out.section(groups.isEmpty());
                    groups.forEach((name, group) -> writeSection(group, out.key(indent + 2, name), indent + 4));
                }
                case STRINGS -> out.list(indent, (List<String>) value);
                case ENUM -> out.scalar(((Enum<?>) value).name());
                default -> out.scalar(value);
            }
        }
    }

    private static void writeSection(Record record, ConfigYaml.Writer out, int indent) {
        out.section(SCHEMAS.get(record.getClass()).entries.isEmpty());
        write(record, false, out, indent);
    }

    private enum Kind {
        BOOLEAN("true or false"),
        INT("a whole number"),
        LONG("a whole number"),
        STRING("text"),
        ENUM("one of "),
        STRINGS("a list of text"),
        SECTION("a section"),
        GROUPS("a section");

        private final String expected;

        Kind(String expected) {
            this.expected = expected;
        }
    }

    /**
     * An option or section of a configuration record.
     *
     * @param type         the enum of an {@link Kind#ENUM} entry or the record of a {@link Kind#SECTION} or
     *                     {@link Kind#GROUPS} entry, otherwise null
     * @param defaultValue the value of the component's {@link Default}, which for a {@link Kind#GROUPS} entry is the list
     *                     of group names, or null if it has none
     */
    private record Entry(
            String key,
            RecordComponent component,
            Kind kind,
            @Nullable Class<?> type,
            List<String> comments,
            @Nullable Object defaultValue
    ) {

        static Entry of(RecordComponent component) {
            Class<?> type = component.getType();
            Kind kind;
            Class<?> detail = null;
            if (type == boolean.class) {
                kind = Kind.BOOLEAN;
            } else if (type == int.class) {
                kind = Kind.INT;
            } else if (type == long.class) {
                kind = Kind.LONG;
            } else if (type == String.class) {
                kind = Kind.STRING;
            } else if (type.isEnum()) {
                kind = Kind.ENUM;
                detail = type;
            } else if (type.isRecord()) {
                kind = Kind.SECTION;
                detail = type;
            } else if (type == List.class && typeArgument(component, 0) == String.class) {
                kind = Kind.STRINGS;
            } else if (type == Map.class && typeArgument(component, 0) == String.class
                    && typeArgument(component, 1) instanceof Class<?> group && group.isRecord()) {
                kind = Kind.GROUPS;
                detail = group;
            } else {
                throw new IllegalArgumentException("Unsupported configuration type " + component.getGenericType() + " of "
                        + component.getDeclaringRecord().getName() + "." + component.getName());
            }
            List<String> comments = new ArrayList<>();
            addComments(comments, component.getAnnotation(Comment.class));
            if (kind == Kind.SECTION || kind == Kind.GROUPS) {
                addComments(comments, detail.getAnnotation(Comment.class));
            }
            component.getAccessor().setAccessible(true);
            Entry entry = new Entry(keyOf(component.getName()), component, kind, detail, List.copyOf(comments), null);
            return new Entry(entry.key, component, kind, detail, entry.comments, entry.declaredDefault());
        }

        /**
         * Reads the {@link Default} annotation of the component.
         *
         * @return the default or null if the component has none
         * @throws IllegalArgumentException if the default does not suit the type of the component
         */
        @Nullable
        private Object declaredDefault() {
            Default declared = component.getAnnotation(Default.class);
            if (declared == null) {
                return null;
            }
            String[] values = declared.value();
            if (kind == Kind.STRINGS || kind == Kind.GROUPS) {
                return List.of(values);
            }
            Object value = kind == Kind.SECTION || values.length != 1 ? null : switch (kind) {
                case BOOLEAN -> switch (values[0]) {
                    case "true" -> true;
                    case "false" -> false;
                    default -> null;
                };
                case INT -> parse(values[0], Integer::parseInt);
                case LONG -> parse(values[0], Long::parseLong);
                default -> coerce(this, values[0]);
            };
            if (value == null) {
                String suited = kind == Kind.SECTION ? "is a section, which takes no @Default" : "takes " + expected();
                throw new IllegalArgumentException(component.getDeclaringRecord().getName() + "." + component.getName() + " "
                        + suited + " as its @Default, not " + Arrays.toString(values));
            }
            return value;
        }

        /**
         * Parses a whole number, which may have underscores between digits as in Java, e.g. {@code 50_000_000}.
         *
         * @return the number or null if the text is not one
         */
        @Nullable
        private static Object parse(String text, Function<String, Object> parser) {
            if (text.startsWith("_") || text.endsWith("_") || text.contains("-_")) {
                return null;
            }
            try {
                return parser.apply(text.replace("_", ""));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Nullable
        private static Type typeArgument(RecordComponent component, int index) {
            return component.getGenericType() instanceof ParameterizedType type ? type.getActualTypeArguments()[index] : null;
        }

        private static void addComments(List<String> comments, @Nullable Comment comment) {
            if (comment != null) {
                comments.addAll(Arrays.asList(comment.value()));
            }
        }

        private static String keyOf(String name) {
            StringBuilder key = new StringBuilder(name.length() + 4);
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (Character.isUpperCase(c)) {
                    key.append('-');
                }
                key.append(c);
            }
            return key.toString().toLowerCase(Locale.ROOT);
        }

        Object read(Record owner) {
            try {
                return component.getAccessor().invoke(owner);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot read " + key + " of " + owner.getClass().getName(), e);
            }
        }

        String expected() {
            return kind == Kind.ENUM ? kind.expected + Arrays.toString(type.getEnumConstants()) : kind.expected;
        }

    }

    private record Schema(List<Entry> entries, Constructor<?> constructor) {

        static Schema of(Class<? extends Record> type) {
            RecordComponent[] components = type.getRecordComponents();
            List<Entry> entries = new ArrayList<>(components.length);
            Class<?>[] parameters = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                entries.add(Entry.of(components[i]));
                parameters[i] = components[i].getType();
            }
            try {
                Constructor<?> constructor = type.getDeclaredConstructor(parameters);
                constructor.setAccessible(true);
                return new Schema(List.copyOf(entries), constructor);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }

        Record create(Object[] values) {
            try {
                return (Record) constructor.newInstance(values);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("Cannot create " + constructor.getDeclaringClass().getName(), e.getCause());
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot create " + constructor.getDeclaringClass().getName(), e);
            }
        }

    }

}
