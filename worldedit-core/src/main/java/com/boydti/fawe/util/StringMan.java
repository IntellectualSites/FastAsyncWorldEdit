package com.boydti.fawe.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

public class StringMan {
    public static String replaceFromMap(final String string, final Map<String, String> replacements) {
        final StringBuilder sb = new StringBuilder(string);
        int size = string.length();
        for (final Entry<String, String> entry : replacements.entrySet()) {
            if (size == 0) {
                break;
            }
            final String key = entry.getKey();
            final String value = entry.getValue();
            int start = sb.indexOf(key, 0);
            while (start > -1) {
                final int end = start + key.length();
                final int nextSearchStart = start + value.length();
                sb.replace(start, end, value);
                size -= end - start;
                start = sb.indexOf(key, nextSearchStart);
            }
        }
        return sb.toString();
    }

    public static boolean containsAny(CharSequence sequence, String any) {
        for (int i = 0; i < sequence.length(); i++) {
            if (any.indexOf(sequence.charAt(i)) != -1) return true;
        }
        return false;
    }

    public static int findMatchingBracket(CharSequence sequence, int index) {
        char startC = sequence.charAt(index);
        char lookC = getMatchingBracket(startC);
        if (lookC == startC) return -1;
        boolean forward = isBracketForwards(startC);
        int increment = forward ? 1 : -1;
        int end = forward ? sequence.length() : -1;
        int count = 0;
        for (int i = index + increment; i != end; i += increment) {
            char c = sequence.charAt(i);
            if (c == startC) {
                count++;
            } else if (c == lookC && count-- == 0) {
                return i;
            }
        }
        return -1;
    }

    public static String prettyFormat(double d) {
        if (d == Double.MIN_VALUE) return "-∞";
        if (d == Double.MAX_VALUE) return "∞";
        if(d == (long) d) return String.format("%d",(long)d);
        else return String.format("%s",d);
    }

    public static boolean isBracketForwards(char c) {
        switch (c) {
            case '[':
            case '(':
            case '{':
            case '<':
                return true;
            default: return false;
        }
    }

    public static char getMatchingBracket(char c) {
        switch (c) {
            case '[': return ']';
            case '(': return ')';
            case '{': return '}';
            case '<': return '>';
            case ']': return '[';
            case ')': return '(';
            case '}': return '{';
            case '>': return '<';
            default: return c;
        }
    }

    public static int parseInt(CharSequence string) {
        int val = 0;
        boolean neg = false;
        int numIndex = 1;
        int len = string.length();
        outer:
        for (int i = len - 1; i >= 0; i--) {
            char c = string.charAt(i);
            switch (c) {
                case '-':
                    val = -val;
                    break;
                default:
                    val = val + (c - 48) * numIndex;
                    numIndex *= 10;
                    break;
            }
        }
        return val;
    }

    public static String removeFromSet(final String string, final Collection<String> replacements) {
        final StringBuilder sb = new StringBuilder(string);
        int size = string.length();
        for (final String key : replacements) {
            if (size == 0) {
                break;
            }
            int start = sb.indexOf(key, 0);
            while (start > -1) {
                final int end = start + key.length();
                final int nextSearchStart = start + 0;
                sb.delete(start, end);
                size -= end - start;
                start = sb.indexOf(key, nextSearchStart);
            }
        }
        return sb.toString();
    }

    public static int indexOf(String input, int start, char... values) {
        for (int i = start; i < input.length(); i++) {
            for (char c : values) {
                if (c == input.charAt(i)) return i;
            }
        }
        return -1;
    }

    public static String toProperCase(String s) {
        return s.substring(0, 1).toUpperCase() +
                s.substring(1);
    }

    public static List<String> split(String input, char delim) {
        List<String> result = new ArrayList<String>();
        int start = 0;
        int bracket = 0;
        boolean inQuotes = false;
        for (int current = 0; current < input.length(); current++) {
            char currentChar = input.charAt(current);
            boolean atLastChar = (current == input.length() - 1);
            if (!atLastChar && (bracket > 0 || (currentChar == '{' && ++bracket > 0) || (current == '}' && --bracket <= 0)))
                continue;
            if (currentChar == '\"') inQuotes = !inQuotes; // toggle state
            if (atLastChar) result.add(input.substring(start));
            else if (currentChar == delim && !inQuotes) {
                String toAdd = input.substring(start, current);
                if (toAdd.startsWith("\"")) {
                    toAdd = toAdd.substring(1, toAdd.length() - 1);
                }
                result.add(toAdd);
                start = current + 1;
            }
        }
        return result;
    }

    public static int intersection(final Set<String> options, final String[] toCheck) {
        int count = 0;
        for (final String check : toCheck) {
            if (options.contains(check)) {
                count++;
            }
        }
        return count;
    }

    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }

    public static String padLeft(String s, int n) {
        return String.format("%1$" + n + "s", s);
    }

    public static String getString(final Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj.getClass() == String.class) {
            return (String) obj;
        }
        if (obj.getClass().isArray()) {
            String result = "";
            String prefix = "";

            for (int i = 0; i < Array.getLength(obj); i++) {
                result += prefix + getString(Array.get(obj, i));
                prefix = ",";
            }
            return "{ " + result + " }";
        } else if (obj instanceof Collection<?>) {
            String result = "";
            String prefix = "";
            for (final Object element : (Collection<?>) obj) {
                result += prefix + getString(element);
                prefix = ",";
            }
            return "( " + result + " )";
        } else {
            return obj.toString();
        }
    }

    public static String replaceFirst(final char c, final String s) {
        if (s == null) {
            return "";
        }
        if (s.isEmpty()) {
            return s;
        }
        char[] chars = s.toCharArray();
        final char[] newChars = new char[chars.length];
        int used = 0;
        boolean found = false;
        for (final char cc : chars) {
            if (!found && (c == cc)) {
                found = true;
            } else {
                newChars[used++] = cc;
            }
        }
        if (found) {
            chars = new char[newChars.length - 1];
            System.arraycopy(newChars, 0, chars, 0, chars.length);
            return String.valueOf(chars);
        }
        return s;
    }

    public static String replaceAll(final String string, final Object... pairs) {
        final StringBuilder sb = new StringBuilder(string);
        for (int i = 0; i < pairs.length; i += 2) {
            final String key = pairs[i] + "";
            final String value = pairs[i + 1] + "";
            int start = sb.indexOf(key, 0);
            while (start > -1) {
                final int end = start + key.length();
                final int nextSearchStart = start + value.length();
                sb.replace(start, end, value);
                start = sb.indexOf(key, nextSearchStart);
            }
        }
        return sb.toString();
    }

    public static boolean isAlphanumeric(final String str) {
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);
            if ((c < 0x30) || ((c >= 0x3a) && (c <= 0x40)) || ((c > 0x5a) && (c <= 0x60)) || (c > 0x7a)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAlphanumericUnd(final CharSequence str) {
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);
            if ((c < 0x30) || ((c >= 0x3a) && (c <= 0x40)) || ((c > 0x5a) && (c <= 0x60)) || (c > 0x7a) || (c == '_')) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAlpha(final String str) {
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);
            if ((c <= 0x40) || ((c > 0x5a) && (c <= 0x60)) || (c > 0x7a)) {
                return false;
            }
        }
        return true;
    }

    public static String join(final Collection<?> collection, final String delimiter) {
        return join(collection.toArray(), delimiter);
    }

    public static String joinOrdered(final Collection<?> collection, final String delimiter) {
        final Object[] array = collection.toArray();
        Arrays.sort(array, new Comparator<Object>() {
            @Override
            public int compare(final Object a, final Object b) {
                return a.hashCode() - b.hashCode();
            }

        });
        return join(array, delimiter);
    }

    public static String join(final Collection<?> collection, final char delimiter) {
        return join(collection.toArray(), delimiter + "");
    }

    public static boolean isAsciiPrintable(final char c) {
        return (c >= ' ') && (c < '');
    }

    public static boolean isAsciiPrintable(final String s) {
        for (final char c : s.toCharArray()) {
            if (!isAsciiPrintable(c)) {
                return false;
            }
        }
        return true;
    }

    public static int getLevenshteinDistance(String s, String t) {
        int n = s.length();
        int m = t.length();
        if (n == 0) {
            return m;
        } else if (m == 0) {
            return n;
        }
        if (n > m) {
            final String tmp = s;
            s = t;
            t = tmp;
            n = m;
            m = t.length();
        }
        int p[] = new int[n + 1];
        int d[] = new int[n + 1];
        int _d[];
        int i;
        int j;
        char t_j;
        int cost;
        for (i = 0; i <= n; i++) {
            p[i] = i;
        }
        for (j = 1; j <= m; j++) {
            t_j = t.charAt(j - 1);
            d[0] = j;

            for (i = 1; i <= n; i++) {
                cost = s.charAt(i - 1) == t_j ? 0 : 1;
                d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
            }
            _d = p;
            p = d;
            d = _d;
        }
        return p[n];
    }

    public static <T> String join(Collection<T> arr, final String delimiter, Function<T, String> funx) {
        final StringBuilder result = new StringBuilder();
        int i = 0;
        for (T obj : arr) {
            if (i > 0) {
                result.append(delimiter);
            }
            result.append(funx.apply(obj));
            i++;
        }
        return result.toString();
    }

    public static String join(final Object[] array, final String delimiter) {
        switch (array.length) {
            case 0:
                return "";
            case 1:
                return array[0].toString();
            default:
                final StringBuilder result = new StringBuilder();
                for (int i = 0, j = array.length; i < j; i++) {
                    if (i > 0) {
                        result.append(delimiter);
                    }
                    result.append(array[i]);
                }
                return result.toString();
        }
    }

    public static Integer toInteger(String string, int start, int end) {
        int value = 0;
        char char0 = string.charAt(0);
        boolean negative;
        if (char0 == '-') {
            negative = true;
            start++;
        }
        else negative = false;
        for (int i = start; i < end; i++) {
            char c = string.charAt(i);
            switch (c) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    value = value * 10 + c - '0';
                    break;
                default:
                    return null;
            }
        }
        return negative ? -value : value;
    }

    public static String join(final int[] array, final String delimiter) {
        final Integer[] wrapped = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            wrapped[i] = array[i];
        }
        return join(wrapped, delimiter);
    }

    public static boolean isEqualToAny(final String a, final String... args) {
        for (final String arg : args) {
            if (StringMan.isEqual(a, arg)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEqualIgnoreCaseToAny(final String a, final String... args) {
        for (final String arg : args) {
            if (StringMan.isEqualIgnoreCase(a, arg)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEqual(final String a, final String b) {
        return ((a == b) || ((a != null) && (b != null) && (a.length() == b.length()) && (a.hashCode() == b.hashCode()) && a.equals(b)));
    }

    public static boolean isEqualIgnoreCase(final String a, final String b) {
        return ((a == b) || ((a != null) && (b != null) && (a.length() == b.length()) && a.equalsIgnoreCase(b)));
    }

    public static String repeat(final String s, final int n) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
