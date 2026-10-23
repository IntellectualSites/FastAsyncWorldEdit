package com.fastasyncworldedit.core.configuration;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMapperTest {

    private enum Mode {
        FAST,
        SAFE
    }

    @Comment("A section")
    private record Inner(@Comment("How many") int count, long total) {

    }

    private record Outer(
            boolean enabled,
            @Comment({"First line", "Second line"}) String someName,
            Mode mode,
            List<String> names,
            Inner inner,
            Map<String, Inner> groups
    ) {

    }

    private record Unsupported(double ratio) {

    }

    private record Annotated(
            @Default("true") boolean enabled,
            @Default("1_000") int count,
            @Default("-5") long total,
            @Default("") String empty,
            @Default("safe") Mode mode,
            @Default({"a", "b"}) List<String> names,
            @Default({}) List<String> none,
            int computed,
            AnnotatedInner inner,
            @Default({"first", "second"}) Map<String, AnnotatedInner> groups
    ) {

    }

    private record AnnotatedInner(@Default("text") String name, int computed) {

    }

    private record BadDefault(@Default("1") boolean enabled) {

    }

    private record BadNumber(@Default("1_000_") int count) {

    }

    private record TwoValues(@Default({"a", "b"}) String name) {

    }

    private record NoDefault(int count) {

    }

    private static final Outer DEFAULTS = new Outer(
            true,
            "name",
            Mode.FAST,
            List.of("a"),
            new Inner(1, 2),
            Map.of("first", new Inner(3, 4))
    );

    private static String normalized(String yaml) {
        return yaml.replace("\r\n", "\n");
    }

    private static String yaml(Record record) {
        ConfigYaml.Writer out = new ConfigYaml.Writer();
        ConfigMapper.write(record, true, out);
        return normalized(out.toString());
    }

    @Test
    void recordsAreWrittenWithTheirCommentsInDeclarationOrder() {
        assertEquals("""
                enabled: true
                # First line
                # Second line
                some-name: "name"
                mode: "FAST"
                names:
                - "a"

                # A section
                inner:
                  # How many
                  count: 1
                  total: 2

                # A section
                groups:
                  first:
                    # How many
                    count: 3
                    total: 4
                """, yaml(DEFAULTS));
    }

    @Test
    void emptyListsAndGroupsAreWrittenInFlowStyle() {
        Outer empty = new Outer(false, "", Mode.SAFE, List.of(), new Inner(0, 0), Map.of());

        assertEquals("""
                enabled: false
                # First line
                # Second line
                some-name: ""
                mode: "SAFE"
                names: []

                # A section
                inner:
                  # How many
                  count: 0
                  total: 0

                # A section
                groups: {}
                """, yaml(empty));
    }

    @Test
    void commentsAreWrittenAsUtf8() {
        ConfigYaml.Writer out = new ConfigYaml.Writer();
        out.comments(2, List.of("café ☃ 😀", ""));

        assertEquals("  # café ☃ 😀\n  #\n", normalized(out.toString()));
    }

    @Test
    void anyTextIsReadBackAsWritten() {
        List<String> samples = new ArrayList<>(List.of(
                "", " ", "a: b", "- c", "# d", "\"quoted\"", "back\\slash", "tab\there", "two\nlines", "cr\rlf\r\n",
                "\u0000\u0007\u001b\u007f", "\u0085   ﻿", "café 日本 😀", "\ud800 lone",
                "true", "null", "123", "~", "&anchor", "*alias", "!tag", "%directive", "@at", "`tick`", "{}", "[]"
        ));
        Random random = new Random(42);
        for (int i = 0; i < 2000; i++) {
            StringBuilder text = new StringBuilder();
            for (int length = random.nextInt(12); length > 0; length--) {
                // Mostly ASCII, with control characters, Latin-1 and other planes mixed in
                int bound = switch (random.nextInt(4)) {
                    case 0 -> 0x20;
                    case 1, 2 -> 0x80;
                    default -> 0x110000;
                };
                text.appendCodePoint(random.nextInt(bound));
            }
            samples.add(text.toString());
        }
        for (String sample : samples) {
            ConfigYaml.Writer out = new ConfigYaml.Writer();
            out.key(0, sample).scalar(sample);
            out.key(0, "list").list(0, List.of(sample));
            Map<String, Object> read = new Yaml(new SafeConstructor(new LoaderOptions())).load(out.toString());
            String key = read.keySet().iterator().next();
            assertEquals(sample, key, () -> "key " + out);
            assertEquals(sample, read.get(key), () -> "value " + out);
            assertEquals(List.of(sample), read.get("list"), () -> "list " + out);
        }
    }

    @Test
    void valuesAreTakenFromTheTreeAndDefaultsFillTheRest() {
        Map<String, Object> tree = new LinkedHashMap<>();
        tree.put("enabled", false);
        tree.put("mode", "safe");
        tree.put("names", List.of("x", 5, true));
        tree.put("inner", Map.of("total", 9));
        tree.put("groups", Map.of("second", Map.of("count", 7)));
        List<String> problems = new ArrayList<>();

        Outer bound = ConfigMapper.bind(tree, DEFAULTS, problems);

        assertEquals(
                new Outer(false, "name", Mode.SAFE, List.of("x", "5", "true"), new Inner(1, 9), Map.of("second", new Inner(7, 4))),
                bound
        );
        assertEquals(List.of(), problems);
    }

    @Test
    void everyRejectedValueAndUnknownOptionIsReported() {
        Map<String, Object> tree = new LinkedHashMap<>();
        tree.put("enabled", "yes please");
        tree.put("mode", "slow");
        tree.put("names", "a");
        tree.put("inner", Map.of("count", 1L << 40, "extra", 1));
        tree.put("groups", 5);
        tree.put("gone", true);
        List<String> problems = new ArrayList<>();

        Outer bound = ConfigMapper.bind(tree, DEFAULTS, problems);

        assertEquals(DEFAULTS, bound);
        assertEquals(7, problems.size(), problems::toString);
        assertTrue(problems.contains("enabled: expected true or false but found \"yes please\", using true"), problems::toString);
        assertTrue(problems.contains("mode: expected one of [FAST, SAFE] but found \"slow\", using FAST"), problems::toString);
        assertTrue(problems.contains("inner.extra: unknown option, it will be removed"), problems::toString);
        assertTrue(problems.contains("gone: unknown option, it will be removed"), problems::toString);
    }

    @Test
    void defaultsComeFromTheAnnotationsAndTheComputedValues() {
        Annotated defaults = ConfigMapper.defaults(
                Annotated.class,
                Map.of("computed", 7, "inner.computed", 8, "groups.computed", 9)
        );

        assertEquals(
                new Annotated(
                        true,
                        1000,
                        -5,
                        "",
                        Mode.SAFE,
                        List.of("a", "b"),
                        List.of(),
                        7,
                        new AnnotatedInner("text", 8),
                        Map.of("first", new AnnotatedInner("text", 9), "second", new AnnotatedInner("text", 9))
                ),
                defaults
        );
        assertEquals(List.of("first", "second"), List.copyOf(defaults.groups().keySet()));
    }

    @Test
    void defaultsThatCannotBeUsedAreRejected() {
        // A value that does not suit the type of its option
        assertThrows(IllegalArgumentException.class, () -> ConfigMapper.defaults(BadDefault.class, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> ConfigMapper.defaults(BadNumber.class, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> ConfigMapper.defaults(TwoValues.class, Map.of()));
        // An option with no default at all
        assertThrows(IllegalArgumentException.class, () -> ConfigMapper.defaults(NoDefault.class, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> ConfigMapper.defaults(NoDefault.class, Map.of("count", "one")));
        // A computed default for an option that does not exist or already has a default
        assertThrows(
                IllegalArgumentException.class,
                () -> ConfigMapper.defaults(NoDefault.class, Map.of("count", 1, "gone", 2))
        );
        assertEquals(new NoDefault(1), ConfigMapper.defaults(NoDefault.class, Map.of("count", 1)));
    }

    @Test
    void unsupportedComponentTypesAreRejected() {
        Unsupported unsupported = new Unsupported(1);

        assertThrows(IllegalArgumentException.class, () -> ConfigMapper.write(unsupported, false, new ConfigYaml.Writer()));
    }

}
