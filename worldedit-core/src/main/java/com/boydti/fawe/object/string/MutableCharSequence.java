package com.boydti.fawe.object.string;

public class MutableCharSequence implements CharSequence {
    private String str;
    private int start, length;

    private static final ThreadLocal<MutableCharSequence> mutableChar = ThreadLocal.withInitial(MutableCharSequence::new);

    public static MutableCharSequence getTemporal() {
        return mutableChar.get();
    }

    public MutableCharSequence(String parent, int start, int length) {
        this.str = parent;
        this.start = start;
        this.length = length;
    }

    public MutableCharSequence() {}

    public void setSubstring(int start, int end) {
        this.start = start;
        this.length = end - start;
    }

    public void setString(String str) {
        this.str = str;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        return str.charAt(index + start);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new MutableCharSequence(str, this.start + start, end - start + 1);
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int i = 0; i < length; i++) {
            h = 31 * h + str.charAt(i + start);
        }
        return h;
    }

    @Override
    public String toString() {
        return str.substring(start, start + length);
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
