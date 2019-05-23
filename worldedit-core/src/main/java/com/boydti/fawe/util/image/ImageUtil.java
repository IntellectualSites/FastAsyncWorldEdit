package com.boydti.fawe.util.image;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.command.FawePrimitiveBinding;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.util.command.parametric.ParameterException;

import javax.annotation.Nullable;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class ImageUtil {
    public static BufferedImage getScaledInstance(BufferedImage img,
                                                  int targetWidth,
                                                  int targetHeight,
                                                  Object hint,
                                                  boolean higherQuality)
    {
        if (img.getHeight() == targetHeight && img.getWidth() == targetWidth) {
            return img;
        }
        int type = (img.getTransparency() == Transparency.OPAQUE) ?
                BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = img;
        int width, height;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            width = ret.getWidth();
            height = ret.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            width = targetWidth;
            height = targetHeight;
        }

        do {
            if (higherQuality && width > targetWidth) {
                width /= 2;
                if (width < targetWidth) {
                    width = targetWidth;
                }
            } else if (width < targetWidth) width = targetWidth;

            if (higherQuality && height > targetHeight) {
                height /= 2;
                if (height < targetHeight) {
                    height = targetHeight;
                }
            } else if (height < targetHeight) height = targetHeight;

            BufferedImage tmp = new BufferedImage(width, height, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            g2.drawImage(ret, 0, 0, width, height, null);
            g2.dispose();

            ret = tmp;
        } while (width != targetWidth || height != targetHeight);

        return ret;
    }

    public static void fadeAlpha(BufferedImage image) {
        int[] raw = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        int width = image.getWidth();
        int height = image.getHeight();
        int centerX = width / 2;
        int centerZ = height / 2;

        float invRadiusX = 1f / centerX;
        float invRadiusZ = 1f / centerZ;

        float[] sqrX = new float[width];
        float[] sqrZ = new float[height];
        for (int x = 0; x < width; x++) {
            float distance = Math.abs(x - centerX) * invRadiusX;
            sqrX[x] = distance * distance;
        }
        for (int z = 0; z < height; z++) {
            float distance = Math.abs(z - centerZ) * invRadiusZ;
            sqrZ[z] = distance * distance;
        }

        for (int z = 0, index = 0; z < height; z++) {
            float dz2 = sqrZ[z];
            for (int x = 0; x < width; x++, index++) {
                int color = raw[index];
                int alpha = (color >> 24) & 0xFF;
                if (alpha != 0) {
                    float dx2 = sqrX[x];
                    float distSqr = dz2 + dx2;
                    if (distSqr > 1) raw[index] = 0;
                    else {
                        alpha = (int) (alpha * (1 - distSqr));
                        raw[index] = (color & 0x00FFFFFF) + (alpha << 24);
                    }
                }
            }
        }
    }

    public static void scaleAlpha(BufferedImage image, double alphaScale) {
        int[] raw = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        int defined = (MathMan.clamp((int) (255 * alphaScale), 0, 255)) << 24;
        for (int i = 0; i < raw.length; i++) {
            int color = raw[i];
            int alpha = ((color >> 24) & 0xFF);
            switch (alpha) {
                case 0:
                    continue;
                case 255:
                    raw[i] = (color & 0x00FFFFFF) + defined;
                    continue;
                default:
                    alpha = MathMan.clamp((int) (alpha * alphaScale), 0, 255);
                    raw[i] = (color & 0x00FFFFFF) + (alpha << 24);
                    continue;
            }
        }
    }

    public static int getColor(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        long totalRed = 0;
        long totalGreen = 0;
        long totalBlue = 0;
        long totalAlpha = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = image.getRGB(x, y);
                totalRed += (color >> 16) & 0xFF;
                totalGreen += (color >> 8) & 0xFF;
                totalBlue += (color >> 0) & 0xFF;
                totalAlpha += (color >> 24) & 0xFF;
            }
        }
        int a = width * height;
        int red = (int) (totalRed / a);
        int green = (int) (totalGreen / a);
        int blue = (int) (totalBlue / a);
        int alpha = (int) (totalAlpha / a);
        return (alpha << 24) + (red << 16) + (green << 8) + (blue << 0);
    }

    public static BufferedImage load(@Nullable FawePrimitiveBinding.ImageUri uri) throws ParameterException {
        return uri == null ? null : uri.load();
    }

    public static BufferedImage load(URI uri) throws ParameterException {
        try {
            return MainUtil.readImage(getInputStream(uri));
        } catch (IOException e) {
            throw new ParameterException(e);
        }
    }

    public static InputStream getInputStream(URI uri) throws ParameterException {
        try {
            String uriStr = uri.toString();
            if (uriStr.startsWith("file:/")) {
                File file = new File(uri.getPath());
                return new FileInputStream(file);
            }
            return new URL(uriStr).openStream();
        } catch (IOException e) {
            throw new ParameterException(e);
        }
    }

    public static BufferedImage getImage(String arg) throws ParameterException {
        try {
            if (arg.startsWith("http")) {
                if (arg.contains("imgur.com") && !arg.contains("i.imgur.com")) {
                    arg = "https://i.imgur.com/" + arg.split("imgur.com/")[1] + ".png";
                }
                URL url = new URL(arg);
                BufferedImage img = MainUtil.readImage(url);
                if (img == null) {
                    throw new IOException("Failed to read " + url + ", please try again later");
                }
                return img;
            } else if (arg.startsWith("file:/")) {
                arg = arg.replaceFirst("file:/+", "");
                File file = MainUtil.getFile(MainUtil.getFile(Fawe.imp().getDirectory(), com.boydti.fawe.config.Settings.IMP.PATHS.HEIGHTMAP), arg);
                return MainUtil.readImage(file);
            } else {
                throw new ParameterException("Invalid image " + arg);
            }
        } catch (IOException e) {
            throw new ParameterException(e);
        }
    }

    public static URI getImageURI(String arg) throws ParameterException {
        try {
            if (arg.startsWith("http")) {
                if (arg.contains("imgur.com") && !arg.contains("i.imgur.com")) {
                    arg = "https://i.imgur.com/" + arg.split("imgur.com/")[1] + ".png";
                }
                return new URL(arg).toURI();
            } else if (arg.startsWith("file:/")) {
                arg = arg.replaceFirst("file:/+", "");
                File file = MainUtil.getFile(MainUtil.getFile(Fawe.imp().getDirectory(), com.boydti.fawe.config.Settings.IMP.PATHS.HEIGHTMAP), arg);
                if (!file.exists()) {
                    throw new ParameterException("File not found " + file);
                }
                if (file.isDirectory()) {
                    throw new ParameterException("File is a directory " + file);
                }
                return file.toURI();
            } else {
                throw new ParameterException("Invalid image " + arg);
            }
        } catch (IOException e) {
            throw new ParameterException(e);
        } catch (URISyntaxException e) {
            throw new ParameterException(e);
        }
    }
}
