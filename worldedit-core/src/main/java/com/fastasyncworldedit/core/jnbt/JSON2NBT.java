package com.fastasyncworldedit.core.jnbt;

import org.enginehub.linbus.tree.LinByteTag;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinDoubleTag;
import org.enginehub.linbus.tree.LinFloatTag;
import org.enginehub.linbus.tree.LinIntArrayTag;
import org.enginehub.linbus.tree.LinIntTag;
import org.enginehub.linbus.tree.LinListTag;
import org.enginehub.linbus.tree.LinLongTag;
import org.enginehub.linbus.tree.LinShortTag;
import org.enginehub.linbus.tree.LinStringTag;
import org.enginehub.linbus.tree.LinTag;
import org.enginehub.linbus.tree.LinTagType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class JSON2NBT {

    private static final Pattern INT_ARRAY_MATCHER = Pattern.compile("\\[[-+\\d|,\\s]+\\]");

    private JSON2NBT() {
    }

    public static LinCompoundTag getTagFromJson(String jsonString) throws NBTException {
        if (jsonString == null) {
            throw new NBTException("jsonString must not be null");
        }
        jsonString = jsonString.trim();
        if (!jsonString.startsWith("{")) {
            throw new NBTException("Invalid tag encountered, expected '{' as first char.");
        } else if (topTagsCount(jsonString) != 1) {
            throw new NBTException("Encountered multiple top tags, only one expected");
        } else {
            return (LinCompoundTag) nameValueToNBT("tag", jsonString).parse();
        }
    }

    public static int topTagsCount(String str) throws NBTException {
        int count = 0;
        boolean inQuotes = false;
        Deque<Character> stack = new ArrayDeque<>();

        for (int j = 0; j < str.length(); ++j) {
            char c0 = str.charAt(j);
            if (c0 == '"') {
                if (isCharEscaped(str, j)) {
                    if (!inQuotes) {
                        throw new NBTException("Illegal use of \\\": " + str);
                    }
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (!inQuotes) {
                if (c0 != '{' && c0 != '[') {
                    if (c0 == '}' && (stack.isEmpty() || stack.pop() != '{')) {
                        throw new NBTException("Unbalanced curly brackets {}: " + str);
                    }

                    if (c0 == ']' && (stack.isEmpty() || stack.pop() != '[')) {
                        throw new NBTException("Unbalanced square brackets []: " + str);
                    }
                } else {
                    if (stack.isEmpty()) {
                        ++count;
                    }

                    stack.push(c0);
                }
            }
        }

        if (inQuotes) {
            throw new NBTException("Unbalanced quotation: " + str);
        } else if (!stack.isEmpty()) {
            throw new NBTException("Unbalanced brackets: " + str);
        } else {
            if (count == 0 && !str.isEmpty()) {
                count = 1;
            }

            return count;
        }
    }

    private static Any joinStrToNBT(String... args) throws NBTException {
        return nameValueToNBT(args[0], args[1]);
    }

    private static Any nameValueToNBT(String key, String value) throws NBTException {
        value = value.trim();
        String s;
        char c01;
        if (value.startsWith("{")) {
            value = value.substring(1, value.length() - 1);

            Compound compound;
            for (compound = new Compound(key); !value.isEmpty(); value = value.substring(s.length() + 1)) {
                s = nextNameValuePair(value, true);
                if (!s.isEmpty()) {
                    compound.tagList.add(getTagFromNameValue(s, false));
                }

                if (value.length() < s.length() + 1) {
                    break;
                }

                c01 = value.charAt(s.length());
                if (c01 != ',' && c01 != '{' && c01 != '}' && c01 != '[' && c01 != ']') {
                    throw new NBTException("Unexpected token '" + c01 + "' at: " + value.substring(s.length()));
                }
            }

            return compound;
        } else if (value.startsWith("[") && !INT_ARRAY_MATCHER.matcher(value).matches()) {
            value = value.substring(1, value.length() - 1);

            JSON2NBT.List list;
            for (list = new JSON2NBT.List(key); !value.isEmpty(); value = value.substring(s.length() + 1)) {
                s = nextNameValuePair(value, false);
                if (!s.isEmpty()) {
                    list.tagList.add(getTagFromNameValue(s, true));
                }

                if (value.length() < s.length() + 1) {
                    break;
                }

                c01 = value.charAt(s.length());
                if (c01 != ',' && c01 != '{' && c01 != '}' && c01 != '[' && c01 != ']') {
                    throw new NBTException("Unexpected token '" + c01 + "' at: " + value.substring(s.length()));
                }
            }

            return list;
        } else {
            return new Primitive(key, value);
        }
    }

    private static Any getTagFromNameValue(String str, boolean isArray) throws NBTException {
        String name = locateName(str, isArray);
        String value = locateValue(str, isArray);
        return joinStrToNBT(name, value);
    }

    private static String nextNameValuePair(String str, boolean isCompound) throws NBTException {
        int colonIndex = getNextCharIndex(str, ':');
        int commaIndex = getNextCharIndex(str, ',');
        if (isCompound) {
            if (colonIndex == -1) {
                throw new NBTException("Unable to locate name/value separator for string: " + str);
            }

            if (commaIndex != -1 && commaIndex < colonIndex) {
                throw new NBTException("Name error at: " + str);
            }
        } else if (colonIndex == -1 || colonIndex > commaIndex) {
            colonIndex = -1;
        }

        return locateValueAt(str, colonIndex);
    }

    private static String locateValueAt(String str, int index) throws NBTException {
        Deque<Character> stack = new ArrayDeque<>();
        int i = index + 1;
        boolean inQuotes = false;
        boolean sawQuotedValue = false;
        boolean sawNonWhitespace = false;

        for (int quoteEnd = 0; i < str.length(); ++i) {
            char c0 = str.charAt(i);
            if (c0 == '"') {
                if (isCharEscaped(str, i)) {
                    if (!inQuotes) {
                        throw new NBTException("Illegal use of \\\": " + str);
                    }
                } else {
                    inQuotes = !inQuotes;
                    if (inQuotes && !sawNonWhitespace) {
                        sawQuotedValue = true;
                    }

                    if (!inQuotes) {
                        quoteEnd = i;
                    }
                }
            } else if (!inQuotes) {
                if (c0 != '{' && c0 != '[') {
                    if (c0 == '}' && (stack.isEmpty() || stack.pop() != '{')) {
                        throw new NBTException("Unbalanced curly brackets {}: " + str);
                    }

                    if (c0 == ']' && (stack.isEmpty() || stack.pop() != '[')) {
                        throw new NBTException("Unbalanced square brackets []: " + str);
                    }

                    if (c0 == ',' && stack.isEmpty()) {
                        return str.substring(0, i);
                    }
                } else {
                    stack.push(c0);
                }
            }

            if (!Character.isWhitespace(c0)) {
                if (!inQuotes && sawQuotedValue && quoteEnd != i) {
                    return str.substring(0, quoteEnd + 1);
                }

                sawNonWhitespace = true;
            }
        }

        return str.substring(0, i);
    }

    private static String locateName(String str, boolean isArray) throws NBTException {
        if (isArray) {
            str = str.trim();
            if (str.startsWith("{") || str.startsWith("[")) {
                return "";
            }
        }

        int i = getNextCharIndex(str, ':');
        if (i == -1) {
            if (isArray) {
                return "";
            } else {
                throw new NBTException("Unable to locate name/value separator for string: " + str);
            }
        } else {
            return str.substring(0, i).trim();
        }
    }

    private static String locateValue(String str, boolean isArray) throws NBTException {
        if (isArray) {
            str = str.trim();
            if (str.startsWith("{") || str.startsWith("[")) {
                return str;
            }
        }

        int i = getNextCharIndex(str, ':');
        if (i == -1) {
            if (isArray) {
                return str;
            } else {
                throw new NBTException("Unable to locate name/value separator for string: " + str);
            }
        } else {
            return str.substring(i + 1).trim();
        }
    }

    private static int getNextCharIndex(String str, char targetChar) {
        boolean outsideQuotes = true;

        for (int i = 0; i < str.length(); ++i) {
            char c0 = str.charAt(i);
            if (c0 == '"') {
                if (!isCharEscaped(str, i)) {
                    outsideQuotes = !outsideQuotes;
                }
            } else if (outsideQuotes) {
                if (c0 == targetChar) {
                    return i;
                }

                if (c0 == '{' || c0 == '[') {
                    return -1;
                }
            }
        }

        return -1;
    }

    private static boolean isCharEscaped(String str, int index) {
        int backslashes = 0;
        while (index - 1 - backslashes >= 0 && str.charAt(index - 1 - backslashes) == '\\') {
            ++backslashes;
        }
        return (backslashes & 1) == 1;
    }

    private static class Primitive extends Any {

        private static final Pattern DOUBLE = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+[d|D]");
        private static final Pattern FLOAT = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+[f|F]");
        private static final Pattern BYTE = Pattern.compile("[-+]?[0-9]+[b|B]");
        private static final Pattern LONG = Pattern.compile("[-+]?[0-9]+[l|L]");
        private static final Pattern SHORT = Pattern.compile("[-+]?[0-9]+[s|S]");
        private static final Pattern INTEGER = Pattern.compile("[-+]?[0-9]+");
        private static final Pattern DOUBLE_UNTYPED = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+");

        protected String jsonValue;

        Primitive(String jsonIn, String valueIn) {
            this.json = jsonIn;
            this.jsonValue = valueIn;
        }

        @Override
        public LinTag<?> parse() throws NBTException {
            try {
                if (DOUBLE.matcher(this.jsonValue).matches()) {
                    return LinDoubleTag.of(Double.parseDouble(this.jsonValue.substring(0, this.jsonValue.length() - 1)));
                }

                if (FLOAT.matcher(this.jsonValue).matches()) {
                    return LinFloatTag.of(Float.parseFloat(this.jsonValue.substring(0, this.jsonValue.length() - 1)));
                }

                if (BYTE.matcher(this.jsonValue).matches()) {
                    return LinByteTag.of(Byte.parseByte(this.jsonValue.substring(0, this.jsonValue.length() - 1)));
                }

                if (LONG.matcher(this.jsonValue).matches()) {
                    return LinLongTag.of(Long.parseLong(this.jsonValue.substring(0, this.jsonValue.length() - 1)));
                }

                if (SHORT.matcher(this.jsonValue).matches()) {
                    return LinShortTag.of(Short.parseShort(this.jsonValue.substring(0, this.jsonValue.length() - 1)));
                }

                if (INTEGER.matcher(this.jsonValue).matches()) {
                    return LinIntTag.of(Integer.parseInt(this.jsonValue));
                }

                if (DOUBLE_UNTYPED.matcher(this.jsonValue).matches()) {
                    return LinDoubleTag.of(Double.parseDouble(this.jsonValue));
                }

                if ("true".equalsIgnoreCase(this.jsonValue)) {
                    return LinByteTag.of((byte)1);
                }

                if ("false".equalsIgnoreCase(this.jsonValue)) {
                    return LinByteTag.of((byte)0);
                }
            } catch (NumberFormatException e) {
                this.jsonValue = this.jsonValue.replace("\\\"", "\"");
                return LinStringTag.of(this.jsonValue);
            }

            if (this.jsonValue.startsWith("[") && this.jsonValue.endsWith("]")) {
                String inner = this.jsonValue.substring(1, this.jsonValue.length() - 1);
                String[] parts = Arrays.stream(inner.split(",", -1))
                        .filter(part -> !part.isEmpty())
                        .toArray(String[]::new);

                try {
                    int[] values = new int[parts.length];

                    for (int j = 0; j < parts.length; ++j) {
                        values[j] = Integer.parseInt(parts[j].trim());
                    }

                    return LinIntArrayTag.of(values);
                } catch (NumberFormatException e) {
                    return LinStringTag.of(this.jsonValue);
                }
            } else {
                if (this.jsonValue.startsWith("\"") && this.jsonValue.endsWith("\"")) {
                    this.jsonValue = this.jsonValue.substring(1, this.jsonValue.length() - 1);
                }

                this.jsonValue = this.jsonValue.replace("\\\"", "\"");
                StringBuilder stringBuilder = new StringBuilder();

                for (int i = 0; i < this.jsonValue.length(); ++i) {
                    if (i < this.jsonValue.length() - 1 && this.jsonValue.charAt(i) == '\\' && this.jsonValue.charAt(i + 1) == '\\') {
                        stringBuilder.append('\\');
                        ++i;
                    } else {
                        stringBuilder.append(this.jsonValue.charAt(i));
                    }
                }

                return LinStringTag.of(stringBuilder.toString());
            }
        }

    }

    private static class List extends Any {

        protected java.util.List<Any> tagList = new ArrayList<>();

        List(String json) {
            this.json = json;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public LinListTag parse() throws NBTException {
            java.util.List<LinTag<?>> list = new ArrayList<>();

            for (Any any : this.tagList) {
                list.add(any.parse());
            }
            LinTagType tagType = list.isEmpty() ? LinTagType.compoundTag() : list.getFirst().type();
            return LinListTag.of(tagType, list);
        }

    }

    private static class Compound extends Any {

        protected java.util.List<Any> tagList = new ArrayList<>();

        Compound(String jsonIn) {
            this.json = jsonIn;
        }

        @Override
        public LinCompoundTag parse() throws NBTException {
            Map<String, LinTag<?>> map = new LinkedHashMap<>();

            for (Any any : this.tagList) {
                map.put(any.json, any.parse());
            }

            return LinCompoundTag.of(map);
        }

    }

    private abstract static class Any {

        protected String json;

        Any() {
        }

        public abstract LinTag<?> parse() throws NBTException;

    }

}
