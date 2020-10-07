package org.yaml.snakeyaml;

import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.constructor.BaseConstructor;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.emitter.Emitable;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.parser.Parser;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;
import org.yaml.snakeyaml.serializer.Serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Public YAML interface. Each Thread must have its own instance.
 */
public class Yaml {
    protected final Resolver resolver;
    protected BaseConstructor constructor;
    protected Representer representer;
    protected DumperOptions dumperOptions;
    private String name;

    /**
     * Create Yaml instance. It is safe to create a few instances and use them
     * in different Threads.
     */
    public Yaml() {
        this(new Constructor(), new Representer(), new DumperOptions(), new Resolver());
    }

    /**
     * Create Yaml instance.
     *
     * @param dumperOptions DumperOptions to configure outgoing objects
     */
    public Yaml(DumperOptions dumperOptions) {
        this(new Constructor(), new Representer(), dumperOptions);
    }

    /**
     * Create Yaml instance. It is safe to create a few instances and use them
     * in different Threads.
     *
     * @param representer Representer to emit outgoing objects
     */
    public Yaml(Representer representer) {
        this(new Constructor(), representer);
    }

    /**
     * Create Yaml instance. It is safe to create a few instances and use them
     * in different Threads.
     *
     * @param constructor BaseConstructor to construct incoming documents
     */
    public Yaml(BaseConstructor constructor) {
        this(constructor, new Representer());
    }

    /**
     * Create Yaml instance. It is safe to create a few instances and use them
     * in different Threads.
     *
     * @param constructor BaseConstructor to construct incoming documents
     * @param representer Representer to emit outgoing objects
     */
    public Yaml(BaseConstructor constructor, Representer representer) {
        this(constructor, representer, new DumperOptions());
    }

    /**
     * Create Yaml instance. It is safe to create a few instances and use them
     * in different Threads.
     *
     * @param representer Representer to emit outgoing objects
     * @param dumperOptions DumperOptions to configure outgoing objects
     */
    public Yaml(Representer representer, DumperOptions dumperOptions) {
        this(new Constructor(), representer, dumperOptions, new Resolver());
    }

    /**
     * Create Yaml instance. It is safe to create a few instances and use them
     * in different Threads.
     *
     * @param constructor BaseConstructor to construct incoming documents
     * @param representer Representer to emit outgoing objects
     * @param dumperOptions DumperOptions to configure outgoing objects
     */
    public Yaml(BaseConstructor constructor, Representer representer, DumperOptions dumperOptions) {
        this(constructor, representer, dumperOptions, new Resolver());
    }

    /**
     * Create Yaml instance. It is safe to create a few instances and use them
     * in different Threads.
     *
     * @param constructor BaseConstructor to construct incoming documents
     * @param representer Representer to emit outgoing objects
     * @param dumperOptions DumperOptions to configure outgoing objects
     * @param resolver Resolver to detect implicit type
     */
    public Yaml(BaseConstructor constructor, Representer representer, DumperOptions dumperOptions, Resolver resolver) {
        if (!constructor.isExplicitPropertyUtils()) {
            constructor.setPropertyUtils(representer.getPropertyUtils());
        } else if (!representer.isExplicitPropertyUtils()) {
            representer.setPropertyUtils(constructor.getPropertyUtils());
        }
        this.constructor = constructor;
        representer.setDefaultFlowStyle(dumperOptions.getDefaultFlowStyle());
        representer.setDefaultScalarStyle(dumperOptions.getDefaultScalarStyle());
        representer.getPropertyUtils().setAllowReadOnlyProperties(dumperOptions.isAllowReadOnlyProperties());
        representer.setTimeZone(dumperOptions.getTimeZone());
        this.representer = representer;
        this.dumperOptions = dumperOptions;
        this.resolver = resolver;
        this.name = "Yaml:" + System.identityHashCode(this);
    }

    /**
     * Serialize a Java object into a YAML String.
     *
     * @param data Java object to be Serialized to YAML
     * @return YAML String
     */
    public String dump(Object data) {
        List<Object> list = new ArrayList<>(1);
        list.add(data);
        return dumpAll(list.iterator());
    }

    /**
     * Produce the corresponding representation tree for a given Object.
     *
     * @param data instance to build the representation tree for
     * @return representation tree
     * @see <a href="http://yaml.org/spec/1.1/#id859333">Figure 3.1. Processing
     * Overview</a>
     */
    public Node represent(Object data) {
        return representer.represent(data);
    }

    /**
     * Serialize a sequence of Java objects into a YAML String.
     *
     * @param data Iterator with Objects
     * @return YAML String with all the objects in proper sequence
     */
    public String dumpAll(Iterator<? extends Object> data) {
        StringWriter buffer = new StringWriter();
        dumpAll(data, buffer, null);
        return buffer.toString();
    }

    /**
     * Serialize a Java object into a YAML stream.
     *
     * @param data Java object to be serialized to YAML
     * @param output stream to write to
     */
    public void dump(Object data, Writer output) {
        List<Object> list = new ArrayList<>(1);
        list.add(data);
        dumpAll(list.iterator(), output, null);
    }

    /**
     * Serialize a sequence of Java objects into a YAML stream.
     *
     * @param data Iterator with Objects
     * @param output stream to write to
     */
    public void dumpAll(Iterator<? extends Object> data, Writer output) {
        dumpAll(data, output, null);
    }

    private void dumpAll(Iterator<? extends Object> data, Writer output, Tag rootTag) {
        Serializer serializer = new Serializer(new Emitter(output, dumperOptions), resolver, dumperOptions, rootTag);
        try {
            serializer.open();
            while (data.hasNext()) {
                Node node = representer.represent(data.next());
                serializer.serialize(node);
            }
            serializer.close();
        } catch (IOException e) {
            throw new YAMLException(e);
        }
    }

    /**
     * <p>
     * Serialize a Java object into a YAML string. Override the default root tag
     * with {@code rootTag}.
     * </p>
     *
     * <p>
     * This method is similar to {@code Yaml.dump(data)} except that the
     * root tag for the whole document is replaced with the given tag. This has
     * two main uses.
     * </p>
     *
     * <p>
     * First, if the root tag is replaced with a standard YAML tag, such as
     * {@code Tag.MAP}, then the object will be dumped as a map. The root
     * tag will appear as {@code !!map}, or blank (implicit !!map).
     * </p>
     *
     * <p>
     * Second, if the root tag is replaced by a different custom tag, then the
     * document appears to be a different type when loaded. For example, if an
     * instance of MyClass is dumped with the tag !!YourClass, then it will be
     * handled as an instance of YourClass when loaded.
     * </p>
     *
     * @param data Java object to be serialized to YAML
     * @param rootTag the tag for the whole YAML document. The tag should be Tag.MAP
     * for a JavaBean to make the tag disappear (to use implicit tag
     * !!map). If {@code null} is provided then the standard tag
     * with the full class name is used.
     * @param flowStyle flow style for the whole document. See Chapter 10. Collection
     * Styles http://yaml.org/spec/1.1/#id930798. If
     * {@code null} is provided then the flow style from
     * DumperOptions is used.
     * @return YAML String
     */
    public String dumpAs(Object data, Tag rootTag, FlowStyle flowStyle) {
        FlowStyle oldStyle = representer.getDefaultFlowStyle();
        if (flowStyle != null) {
            representer.setDefaultFlowStyle(flowStyle);
        }
        List<Object> list = new ArrayList<>(1);
        list.add(data);
        StringWriter buffer = new StringWriter();
        dumpAll(list.iterator(), buffer, rootTag);
        representer.setDefaultFlowStyle(oldStyle);
        return buffer.toString();
    }

    /**
     * <p>
     * Serialize a Java object into a YAML string. Override the default root tag
     * with {@code Tag.MAP}.
     * </p>
     * <p>
     * This method is similar to {@code Yaml.dump(data)} except that the
     * root tag for the whole document is replaced with {@code Tag.MAP} tag
     * (implicit !!map).
     * </p>
     * <p>
     * Block Mapping is used as the collection style. See 10.2.2. Block Mappings
     * (http://yaml.org/spec/1.1/#id934537)
     * </p>
     *
     * @param data Java object to be serialized to YAML
     * @return YAML String
     */
    public String dumpAsMap(Object data) {
        return dumpAs(data, Tag.MAP, FlowStyle.BLOCK);
    }

    /**
     * Serialize the representation tree into Events.
     *
     * @param data representation tree
     * @return Event list
     * @see <a href="http://yaml.org/spec/1.1/#id859333">Processing Overview</a>
     */
    public List<Event> serialize(Node data) {
        SilentEmitter emitter = new SilentEmitter();
        Serializer serializer = new Serializer(emitter, resolver, dumperOptions, null);
        try {
            serializer.open();
            serializer.serialize(data);
            serializer.close();
        } catch (IOException e) {
            throw new YAMLException(e);
        }
        return emitter.getEvents();
    }

    /**
     * Parse the only YAML document in a String and produce the corresponding
     * Java object. (Because the encoding in known BOM is not respected.)
     *
     * @param yaml YAML data to load from (BOM must not be present)
     * @return parsed object
     */
    public Object load(String yaml) {
        return loadFromReader(new StreamReader(yaml), Object.class);
    }

    /**
     * Parse the only YAML document in a stream and produce the corresponding
     * Java object.
     *
     * @param io data to load from (BOM is respected and removed)
     * @return parsed object
     */
    public Object load(InputStream io) {
        return loadFromReader(new StreamReader(new UnicodeReader(io)), Object.class);
    }

    /**
     * Parse the only YAML document in a stream and produce the corresponding
     * Java object.
     *
     * @param io data to load from (BOM must not be present)
     * @return parsed object
     */
    public Object load(Reader io) {
        return loadFromReader(new StreamReader(io), Object.class);
    }

    /**
     * Parse the only YAML document in a stream and produce the corresponding
     * Java object.
     *
     * @param <T> Class is defined by the second argument
     * @param io data to load from (BOM must not be present)
     * @param type Class of the object to be created
     * @return parsed object
     */
    @SuppressWarnings("unchecked")
    public <T> T loadAs(Reader io, Class<T> type) {
        return (T) loadFromReader(new StreamReader(io), type);
    }

    /**
     * Parse the only YAML document in a String and produce the corresponding
     * Java object. (Because the encoding in known BOM is not respected.)
     *
     * @param <T> Class is defined by the second argument
     * @param yaml YAML data to load from (BOM must not be present)
     * @param type Class of the object to be created
     * @return parsed object
     */
    @SuppressWarnings("unchecked")
    public <T> T loadAs(String yaml, Class<T> type) {
        return (T) loadFromReader(new StreamReader(yaml), type);
    }

    /**
     * Parse the only YAML document in a stream and produce the corresponding
     * Java object.
     *
     * @param <T> Class is defined by the second argument
     * @param input data to load from (BOM is respected and removed)
     * @param type Class of the object to be created
     * @return parsed object
     */
    @SuppressWarnings("unchecked")
    public <T> T loadAs(InputStream input, Class<T> type) {
        return (T) loadFromReader(new StreamReader(new UnicodeReader(input)), type);
    }

    private Object loadFromReader(StreamReader sreader, Class<?> type) {
        Composer composer = new Composer(new ParserImpl(sreader), resolver);
        constructor.setComposer(composer);
        return constructor.getSingleData(type);
    }

    /**
     * Parse all YAML documents in a String and produce corresponding Java
     * objects. The documents are parsed only when the iterator is invoked.
     *
     * @param yaml YAML data to load from (BOM must not be present)
     * @return an iterator over the parsed Java objects in this String in proper
     * sequence
     */
    public Iterable<Object> loadAll(Reader yaml) {
        Composer composer = new Composer(new ParserImpl(new StreamReader(yaml)), resolver);
        constructor.setComposer(composer);
        Iterator<Object> result = new Iterator<Object>() {
            public boolean hasNext() {
                return constructor.checkData();
            }

            public Object next() {
                return constructor.getData();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        return new YamlIterable(result);
    }

    /**
     * Parse all YAML documents in a String and produce corresponding Java
     * objects. (Because the encoding in known BOM is not respected.) The
     * documents are parsed only when the iterator is invoked.
     *
     * @param yaml YAML data to load from (BOM must not be present)
     * @return an iterator over the parsed Java objects in this String in proper
     * sequence
     */
    public Iterable<Object> loadAll(String yaml) {
        return loadAll(new StringReader(yaml));
    }

    /**
     * Parse all YAML documents in a stream and produce corresponding Java
     * objects. The documents are parsed only when the iterator is invoked.
     *
     * @param yaml YAML data to load from (BOM is respected and ignored)
     * @return an iterator over the parsed Java objects in this stream in proper
     * sequence
     */
    public Iterable<Object> loadAll(InputStream yaml) {
        return loadAll(new UnicodeReader(yaml));
    }

    /**
     * Parse the first YAML document in a stream and produce the corresponding
     * representation tree. (This is the opposite of the represent() method)
     *
     * @param yaml YAML document
     * @return parsed root Node for the specified YAML document
     * @see <a href="http://yaml.org/spec/1.1/#id859333">Figure 3.1. Processing
     * Overview</a>
     */
    public Node compose(Reader yaml) {
        Composer composer = new Composer(new ParserImpl(new StreamReader(yaml)), resolver);
        constructor.setComposer(composer);
        return composer.getSingleNode();
    }

    /**
     * Parse all YAML documents in a stream and produce corresponding
     * representation trees.
     *
     * @param yaml stream of YAML documents
     * @return parsed root Nodes for all the specified YAML documents
     * @see <a href="http://yaml.org/spec/1.1/#id859333">Processing Overview</a>
     */
    public Iterable<Node> composeAll(Reader yaml) {
        final Composer composer = new Composer(new ParserImpl(new StreamReader(yaml)), resolver);
        constructor.setComposer(composer);
        Iterator<Node> result = new Iterator<Node>() {
            public boolean hasNext() {
                return composer.checkNode();
            }

            public Node next() {
                return composer.getNode();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        return new NodeIterable(result);
    }

    /**
     * Add an implicit scalar detector. If an implicit scalar value matches the
     * given regexp, the corresponding tag is assigned to the scalar.
     *
     * @param tag tag to assign to the node
     * @param regexp regular expression to match against
     * @param first a sequence of possible initial characters or null (which means any).
     */
    public void addImplicitResolver(Tag tag, Pattern regexp, String first) {
        resolver.addImplicitResolver(tag, regexp, first);
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Get a meaningful name. It simplifies debugging in a multi-threaded
     * environment. If nothing is set explicitly the address of the instance is
     * returned.
     *
     * @return human readable name
     */
    public String getName() {
        return name;
    }

    /**
     * Set a meaningful name to be shown in {@code toString()}.
     *
     * @param name human readable name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Parse a YAML stream and produce parsing events.
     *
     * @param yaml YAML document(s)
     * @return parsed events
     * @see <a href="http://yaml.org/spec/1.1/#id859333">Processing Overview</a>
     */
    public Iterable<Event> parse(Reader yaml) {
        final Parser parser = new ParserImpl(new StreamReader(yaml));
        Iterator<Event> result = new Iterator<Event>() {
            public boolean hasNext() {
                return parser.peekEvent() != null;
            }

            public Event next() {
                return parser.getEvent();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        return new EventIterable(result);
    }

    public void setBeanAccess(BeanAccess beanAccess) {
        constructor.getPropertyUtils().setBeanAccess(beanAccess);
        representer.getPropertyUtils().setBeanAccess(beanAccess);
    }


    private static class SilentEmitter implements Emitable {
        private List<Event> events = new ArrayList<>(100);

        public List<Event> getEvents() {
            return events;
        }

        public void emit(Event event) throws IOException {
            events.add(event);
        }
    }


    private static class YamlIterable implements Iterable<Object> {
        private Iterator<Object> iterator;

        public YamlIterable(Iterator<Object> iterator) {
            this.iterator = iterator;
        }

        public Iterator<Object> iterator() {
            return iterator;
        }
    }


    private static class NodeIterable implements Iterable<Node> {
        private Iterator<Node> iterator;

        public NodeIterable(Iterator<Node> iterator) {
            this.iterator = iterator;
        }

        public Iterator<Node> iterator() {
            return iterator;
        }
    }


    private static class EventIterable implements Iterable<Event> {
        private Iterator<Event> iterator;

        public EventIterable(Iterator<Event> iterator) {
            this.iterator = iterator;
        }

        public Iterator<Event> iterator() {
            return iterator;
        }
    }

}
