package com.boydti.fawe.util;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.Locale;

public class ColorUtil {
    private static final int PARSE_COMPONENT = 0; // percent, or clamped to [0,255] => [0,1]
    private static final int PARSE_PERCENT = 1; // clamped to [0,100]% => [0,1]
    private static final int PARSE_ANGLE = 2; // clamped to [0,360]
    private static final int PARSE_ALPHA = 3; // clamped to [0f,1f]

    private static float parseComponent(String color, int off, int end, int type) {
        color = color.substring(off, end).trim();
        if (color.endsWith("%")) {
            if (type > PARSE_PERCENT) {
                throw new IllegalArgumentException("Invalid color specification");
            }
            type = PARSE_PERCENT;
            color = color.substring(0, color.length()-1).trim();
        } else if (type == PARSE_PERCENT) {
            throw new IllegalArgumentException("Invalid color specification");
        }
        float c = ((type == PARSE_COMPONENT)
                ? Integer.parseInt(color)
                : Float.parseFloat(color));
        switch (type) {
            case PARSE_ALPHA:
                return (c < 0f) ? 0f : (Math.min(c, 1f));
            case PARSE_PERCENT:
                return (c <= 0f) ? 0f : ((c >= 100f) ? 1f : (c / 100f));
            case PARSE_COMPONENT:
                return (c <= 0f) ? 0f : ((c >= 255f) ? 1f : (c / 255f));
            case PARSE_ANGLE:
                return ((c < 0f)
                        ? ((c % 360f) + 360f)
                        : ((c > 360f)
                        ? (c % 360f)
                        : c));
        }

        throw new IllegalArgumentException("Invalid color specification");
    }

    private static Color parseRGBColor(String color, int roff)
    {
        try {
            int rend = color.indexOf(',', roff);
            int gend = rend < 0 ? -1 : color.indexOf(',', rend+1);
            int bend = gend < 0 ? -1 : color.indexOf(gend+1);
            float r = parseComponent(color, roff, rend, PARSE_COMPONENT);
            float g = parseComponent(color, rend+1, gend, PARSE_COMPONENT);
            float b = parseComponent(color, gend+1, bend, PARSE_COMPONENT);
            return new Color(r, g, b);
        } catch (NumberFormatException nfe) {}

        throw new IllegalArgumentException("Invalid color specification");
    }

    private static Color parseHSLColor(String color, int hoff)
    {
        try {
            int hend = color.indexOf(',', hoff);
            int send = hend < 0 ? -1 : color.indexOf(',', hend+1);
            int lend = send < 0 ? -1 : color.indexOf(send+1);
            float h = parseComponent(color, hoff, hend, PARSE_ANGLE);
            float s = parseComponent(color, hend+1, send, PARSE_PERCENT);
            float l = parseComponent(color, send+1, lend, PARSE_PERCENT);
            return Color.getHSBColor(h, s, l);
        } catch (NumberFormatException nfe) {}

        throw new IllegalArgumentException("Invalid color specification");
    }

    public static Color parseColor(String colorString) {
        if (colorString == null) {
            throw new NullPointerException(
                    "The color components or name must be specified");
        }
        if (colorString.isEmpty()) {
            throw new IllegalArgumentException("Invalid color specification");
        }

        String color = colorString.toLowerCase(Locale.ROOT);

        if (color.startsWith("#")) {
            color = color.substring(1);
        } else if (color.startsWith("0x")) {
            color = color.substring(2);
        } else if (color.startsWith("rgb")) {
            if (color.startsWith("(", 3)) {
                return parseRGBColor(color, 4);
            } else if (color.startsWith("a(", 3)) {
                return parseRGBColor(color, 5);
            }
        } else if (color.startsWith("hsl")) {
            if (color.startsWith("(", 3)) {
                return parseHSLColor(color, 4);
            } else if (color.startsWith("a(", 3)) {
                return parseHSLColor(color, 5);
            }
        } else {
            Color col = null;
            try {
                Field field = java.awt.Color.class.getField(color.toLowerCase());
                col = (Color) field.get(null);
            } catch (Throwable ignore) {}
            if (col != null) {
                return col;
            }
        }

        int len = color.length();

        try {
            int r;
            int g;
            int b;
            int a;

            if (len == 3) {
                r = Integer.parseInt(color.substring(0, 1), 16);
                g = Integer.parseInt(color.substring(1, 2), 16);
                b = Integer.parseInt(color.substring(2, 3), 16);
                return new Color(r / 15f, g / 15f, b / 15f);
            } else if (len == 4) {
                r = Integer.parseInt(color.substring(0, 1), 16);
                g = Integer.parseInt(color.substring(1, 2), 16);
                b = Integer.parseInt(color.substring(2, 3), 16);
                return new Color(r / 15f, g / 15f, b / 15f);
            } else if (len == 6) {
                r = Integer.parseInt(color.substring(0, 2), 16);
                g = Integer.parseInt(color.substring(2, 4), 16);
                b = Integer.parseInt(color.substring(4, 6), 16);
                return new Color(r, g, b);
            } else if (len == 8) {
                r = Integer.parseInt(color.substring(0, 2), 16);
                g = Integer.parseInt(color.substring(2, 4), 16);
                b = Integer.parseInt(color.substring(4, 6), 16);
                return new Color(r, g, b);
            }
        } catch (NumberFormatException nfe) {}

        throw new IllegalArgumentException("Invalid color specification");
    }
}
