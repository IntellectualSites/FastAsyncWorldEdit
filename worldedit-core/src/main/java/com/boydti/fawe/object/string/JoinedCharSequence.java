package com.boydti.fawe.object.string;

public class JoinedCharSequence implements CharSequence {
    private char join;
    private int len2;
    private int len1;
    private int length;
    private String a;
    private String b;

    public JoinedCharSequence init(String a, char join, String b) {
        this.len1 = a.length();
        this.len2 = b.length();
        this.length = len1 + len2 + 1;
        this.join = join;
        this.a = a;
        this.b = b;
        return this;
    }
    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        if (index < len1) {
            return a.charAt(index);
        }
        if (index == len1) {
            return join;
        }
        return b.charAt(index - len1 - 1);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        char[] chars = new char[end - start];
        for (int i = start, j = 0; i < end; i++, j++) {
            chars[j] = charAt(i);
        }
        return new String(chars);
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int i = 0; i < length; i++) {
            h = 31 * h + charAt(i);
        }
        return h;
    }

    @Override
    public String toString() {
        return (String) subSequence(0, length);
    }

    @Override
    public boolean equals(Object obj) {
        CharSequence anotherString = (CharSequence) obj;
        if (length == anotherString.length()) {
            for (int i = length - 1; i >= 0; i--) {
                if (charAt(i) != anotherString.charAt(i)) return false;
            }
            return true;
        }
        return false;
    }
}
