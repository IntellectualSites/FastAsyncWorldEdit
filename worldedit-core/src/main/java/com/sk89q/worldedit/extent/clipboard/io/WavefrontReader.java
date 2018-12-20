package com.sk89q.worldedit.extent.clipboard.io;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.collection.SoftHashMap;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.imageio.ImageIO;

public class WavefrontReader implements ClipboardReader {
    private final InputStream inputStream;
    private final File root;

    private final Map<String, BufferedImage> textures = new SoftHashMap<>();
    private final Map<String, Map<String, Material>> materialFiles = new HashMap<>();
    private final Map<String, Material> materials = new HashMap<>();

    public WavefrontReader(File file) throws FileNotFoundException {
        this.inputStream = new BufferedInputStream(new FileInputStream(file));
        File parent = file.getParentFile();
        this.root = parent == null ? new File(".") : parent;
    }

    private final static double parse(String s) {
        int len = s.length();
        int index;
        int numIndex = 1;

        double neg;

        if (s.charAt(0) == '-') {
            neg = -1;
            index = 1;
        } else {
            index = 0;
            neg = 1;
        }
        double val = 0;
        outer:
        for (; index < len; index++) {
            char c = s.charAt(index);
            switch (c) {
                case ' ': break outer;
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
                    val = val * 10 + (c - 48);
                    continue;
                case '.': {
                    double factor = 0.1;
                    for (; index < len; index++) {
                        c = s.charAt(index);
                        switch (c) {
                            case ' ': break outer;
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
                                val += ((c - 48) * factor);
                                factor *= 0.1;
                        }
                    }
                }
                break;
            }
        }
        return val * neg;
    }

    @Override
    public Clipboard read() throws IOException {
        try (InputStream finalStream = inputStream) {
            load(finalStream);
        }
        return null;
    }

    private final BufferedImage getTexture(String file) throws IOException {
        BufferedImage texture = textures.get(file);
        if (texture == null) {
            texture = ImageIO.read(new File(root, file));
            textures.put(file, texture);
        }
        return texture;
    }

    private void readLines(InputStream stream, Consumer<String> onEachLine, boolean nullTerminate) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                onEachLine.accept(line);
            }
            if (nullTerminate) onEachLine.accept(null);
        }
    }

    private final int toIntColor(float color) {
        return (int) (color * 256 + 0.5);
    }

    private String getFileName(String arg) {
        String[] pathSplit = arg.split("[/|\\\\]");
        return pathSplit[pathSplit.length - 1];
    }

    @Override
    public void close() throws IOException {

    }

    private class Material {
        private double dissolve = 1;
        private int color = Integer.MIN_VALUE;
        private String texture;
    }

    private final void loadMaterials(String fileName) throws IOException {
        File file = new File(root, fileName);
        if (!file.exists()) {
            Fawe.debug(".mtl not found: " + fileName);
            return;
        }
        Map<String, Material> mtl = materialFiles.get(fileName);
        if (mtl == null) {
            final Map<String, Material> tmp = mtl = new HashMap<>();
            materialFiles.put(fileName, tmp);
            readLines(new FileInputStream(file), new Consumer<String>() {

                private String name;
                private Material material;
                private int index;

                private void add() {
                    if (material != null) {
                        if (material.color == Integer.MIN_VALUE) {
                            material.color = -1;
                        }
                        tmp.put(name, material);
                        material = null;
                        name = null;
                    }
                }

                @Override
                public void accept(String s) {
                    if (s == null) {
                        add();
                        return;
                    }
                    String[] args = s.split("[ ]+");
                    switch (args[0]) {
                        // Name
                        case "newmtl": {
                            add();
                            material = new Material();
                            name = args[1];
                            break;
                        }
                        // Color
                        case "Ka":
                            if (material.color != Integer.MIN_VALUE) break;
                        case "Kd": {
                            float r = Float.parseFloat(args[1]);
                            float g = Float.parseFloat(args[2]);
                            float b = Float.parseFloat(args[3]);
                            material.color = (toIntColor(r) << 16) + (toIntColor(g) << 8) + toIntColor(b);
                            break;
                        }
                        // Density
                        case "d": {
                            material.dissolve = Double.parseDouble(args[1]);
                            break;
                        }
                        case "Tr": {
                            material.dissolve = 1.0 - Double.parseDouble(args[1]);
                            break;
                        }
                        case "map_Ka":
                            if (material.texture != null) break;
                        case "map_Kd": {
                            material.texture = getFileName(args[1]);
                            break;
                        }

                    }
                }
            }, true);
        }
        materials.putAll(mtl);
    }

    private final Material getMaterial(String name) {
        Material mtl = materials.get(name);
        return mtl != null ? mtl : new Material();
    }

    private void load(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (line.isEmpty()) continue;
                char char0 = line.charAt(0);
                switch (char0) {
                    case '#': continue;
                    case 'v':
                        switch (line.charAt(1)) {
                            case ' ':
                            case 'n': {
                                Double.parseDouble("");
                                break;
                            }
                            case 't': {

                            }
                        }
                        break;
                    case 'f': {

                        break;
                    }
                    case 'l':
                    case 's':
                    case 'o':
                    case 'g':
                        // Ignore
                        break;
                    default:
                        String[] args = line.split(" ");
                        switch (args[0]) {
                            case "mtllib": {
                                String[] pathSplit = args[1].split("[/|\\\\]");
                                String fileName = pathSplit[pathSplit.length - 1];
                                loadMaterials(fileName);
                                break;
                            }
                        }
                }
            }
        }

//
//        final File directory = file.getParentFile();
//        final Map<String, SimpleMaterial> materials = new HashMap<String, SimpleMaterial>();
//        final Map<Face, BufferedImage> textures = new HashMap<Face, BufferedImage>();
//        final Map<Face, Color> colors = new HashMap<Face, Color>();
//        final List<Vertex> v = new LinkedList<Vertex>();
//        final List<VertexTexture> vt = new LinkedList<VertexTexture>();
//        final List<Vertex> vn = new LinkedList<Vertex>();
//        final List<Face> f = new LinkedList<Face>();
//        final List<String[]> obj = new LinkedList<String[]>();
//        for (final String[] entry : obj) {
//            if (entry[0].equals("v") || entry[0].equals("vn")) {
//                if (entry.length == 1) {
//                    VLogger.log("[ERROR] Invalid vertex or vertex normal entry found (no data)");
//                    return null;
//                }
//                double x;
//                double y;
//                double z;
//                try {
//                    x = Double.parseDouble(entry[1]);
//                    y = Double.parseDouble(entry[2]);
//                    z = Double.parseDouble(entry[3]);
//                }
//                catch (NumberFormatException | ArrayIndexOutOfBoundsException ex8) {
//                    final RuntimeException ex5;
//                    final RuntimeException ex = ex5;
//                    VLogger.log("[ERROR] Invalid vertex or vertex normal entry found (not parseable data)");
//                    return null;
//                }
//                if (entry[0].equals("v")) {
//                    if (entry.length >= 6) {
//                        try {
//                            final float r = Float.parseFloat(entry[4]);
//                            final float g = Float.parseFloat(entry[5]);
//                            final float b = Float.parseFloat(entry[6]);
//                            final Color c = new Color(r, g, b);
//                            v.add(new Vertex(x, y, z, v.size() + 1, c));
//                            continue;
//                        }
//                        catch (NumberFormatException | ArrayIndexOutOfBoundsException ex9) {
//                            final RuntimeException ex6;
//                            final RuntimeException ex = ex6;
//                            VLogger.log("[ERROR] Invalid vertex color (not parseable data)");
//                            return null;
//                        }
//                    }
//                    v.add(new Vertex(x, y, z, v.size() + 1));
//                }
//                else {
//                    vn.add(new Vertex(x, y, z, vn.size() + 1));
//                }
//            }
//            else {
//                if (!entry[0].equals("vt")) {
//                    continue;
//                }
//                if (entry.length == 1) {
//                    VLogger.log("[ERROR] Invalid vertex texture entry found (no data)");
//                    return null;
//                }
//                double vt_u;
//                double vt_v;
//                try {
//                    vt_u = Double.parseDouble(entry[1]);
//                    vt_v = Double.parseDouble(entry[2]);
//                    if (vt_u < 0.0 || vt_v < 0.0 || vt_u > 1.0 || vt_v > 1.0) {
//                        VLogger.log("[ERROR] UV of vertex texture out of bounds");
//                        return null;
//                    }
//                }
//                catch (NumberFormatException | ArrayIndexOutOfBoundsException ex10) {
//                    final RuntimeException ex7;
//                    final RuntimeException ex2 = ex7;
//                    VLogger.log("[ERROR] Invalid vertex texture entry found (not parseable data)");
//                    return null;
//                }
//                vt.add(new VertexTexture(vt_u, vt_v, vn.size() + 1));
//            }
//        }
//        VLogger.log("[IMPORT] Loaded " + v.size() + " vertexes");
//        VLogger.log("[IMPORT] Loaded " + vt.size() + " vertex textures");
//        VLogger.log("[IMPORT] Loaded " + vn.size() + " vertex normals");
//        BufferedImage usemtl_texture = null;
//        Color usemtl_color = null;
//        for (final String[] entry2 : obj) {
//            if (entry2[0].equals("usemtl")) {
//                if (entry2.length == 1) {
//                    VLogger.log("[ERROR] Invalid usemtl entry");
//                    return null;
//                }
//                SimpleMaterial material = (SimpleMaterial)materials.get(entry2[1]);
//                if (material == null) {
//                    VLogger.log("[WARN] Material '" + entry2[1] + "' does not exist");
//                    material = getFallbackMaterial();
//                    VLogger.log("[WARN] Replacing with fallback material");
//                }
//                usemtl_texture = material.texture;
//                usemtl_color = material.color;
//                VLogger.log("[IMPORT] Now using material '" + entry2[1] + "'");
//            }
//            else {
//                if (!entry2[0].equals("f")) {
//                    continue;
//                }
//                if (entry2.length == 1) {
//                    VLogger.log("[ERROR] Invalid face entry (no arguments)");
//                    return null;
//                }
//                if (usemtl_texture == null && usemtl_color == null) {
//                    VLogger.log("[WARN] Current Material has neither a texture nor a color");
//                    usemtl_color = Colors.getGray(1.0f);
//                    VLogger.log("[WARN] Using fallback color");
//                }
//                final Collection<FacePoint> points = new LinkedList<FacePoint>();
//                for (int i = 1; i < entry2.length; ++i) {
//                    final String[] comp = entry2[i].split("/");
//                    Integer comp_v;
//                    Integer comp_vt;
//                    Integer comp_vn;
//                    try {
//                        comp_v = Integer.parseInt(comp[0]);
//                        comp_vt = ((comp.length <= 1 || comp[1].isEmpty()) ? null : Integer.parseInt(comp[1]));
//                        comp_vn = ((comp.length <= 2 || comp[2].isEmpty()) ? null : Integer.parseInt(comp[2]));
//                    }
//                    catch (NumberFormatException ex3) {
//                        final StringBuilder debug = new StringBuilder();
//                        String[] array;
//                        for (int length = (array = comp).length, j = 0; j < length; ++j) {
//                            final String segment = array[j];
//                            debug.append(segment);
//                            debug.append("/");
//                        }
//                        VLogger.log("[ERROR] Face point failed to load (" + (Object)debug + ")");
//                        return null;
//                    }
//                    try {
//                        final FacePoint point = new FacePoint();
//                        point.v = v.get((int)comp_v - 1);
//                        point.vt = ((comp_vt == null) ? null : ((VertexTexture)vt.get((int)comp_vt - 1)));
//                        point.vn = ((comp_vn == null) ? null : ((Vertex)vn.get((int)comp_vn - 1)));
//                        points.add(point);
//                    }
//                    catch (IndexOutOfBoundsException ex4) {
//                        VLogger.log("[ERROR] Face point reference to missing vertex");
//                        return null;
//                    }
//                }
//                final Face face = new Face(points);
//                f.add(face);
//                if (usemtl_texture != null) {
//                    textures.put(face, usemtl_texture);
//                }
//                if (usemtl_color == null) {
//                    continue;
//                }
//                colors.put(face, usemtl_color);
//            }
//        }
//        double minX = Double.MAX_VALUE;
//        double minY = Double.MAX_VALUE;
//        double minZ = Double.MAX_VALUE;
//        double maxX = -1.7976931348623157E308;
//        double maxY = -1.7976931348623157E308;
//        double maxZ = -1.7976931348623157E308;
//        for (final Face face2 : f) {
//            for (final FacePoint point2 : face2.points) {
//                final double x2 = point2.v.getX();
//                final double y2 = point2.v.getY();
//                final double z2 = point2.v.getZ();
//                if (x2 < minX) {
//                    minX = x2;
//                }
//                else if (x2 > maxX) {
//                    maxX = x2;
//                }
//                if (y2 < minY) {
//                    minY = y2;
//                }
//                else if (y2 > maxY) {
//                    maxY = y2;
//                }
//                if (z2 < minZ) {
//                    minZ = z2;
//                }
//                else {
//                    if (z2 <= maxZ) {
//                        continue;
//                    }
//                    maxZ = z2;
//                }
//            }
//        }
//        final double size = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
//        final double scale = size / (resolution - 1.0);
//        final List<Face> polygons = new LinkedList<Face>();
//        for (final Face face3 : f) {
//            polygons.addAll(shatterFace(face3, colors, textures));
//        }
//        VLogger.log("[IMPORT] " + f.size() + " faces -> " + polygons.size() + " polygons");
//        final Map<Position3D, Color> colormap = new HashMap<Position3D, Color>();
//        for (final Face poly : polygons) {
//            final FacePoint a = (FacePoint)poly.points.get(0);
//            final FacePoint b2 = (FacePoint)poly.points.get(1);
//            final FacePoint c2 = (FacePoint)poly.points.get(2);
//            final Vector3D vAB = new Vector3D((Point3D)a.v, (Point3D)b2.v);
//            final Vector3D vAC = new Vector3D((Point3D)a.v, (Point3D)c2.v);
//            final float lAB = (float)vAB.length();
//            final float lAC = (float)vAC.length();
//            double[] array3;
//            if (a.vt == null || b2.vt == null) {
//                final double[] array2 = array3 = new double[2];
//                array2[1] = (array2[0] = 0.0);
//            }
//            else {
//                final double[] array4 = array3 = new double[2];
//                array4[0] = b2.vt.u - a.vt.u;
//                array4[1] = b2.vt.v - a.vt.v;
//            }
//            final double[] uvAB = array3;
//            double[] array6;
//            if (a.vt == null || c2.vt == null) {
//                final double[] array5 = array6 = new double[2];
//                array5[1] = (array5[0] = 0.0);
//            }
//            else {
//                final double[] array7 = array6 = new double[2];
//                array7[0] = c2.vt.u - a.vt.u;
//                array7[1] = c2.vt.v - a.vt.v;
//            }
//            final double[] uvAC = array6;
//            double[] array9;
//            if (a.vt == null) {
//                final double[] array8 = array9 = new double[2];
//                array8[1] = (array8[0] = 0.0);
//            }
//            else {
//                final double[] array10 = array9 = new double[2];
//                array10[0] = a.vt.u;
//                array10[1] = a.vt.v;
//            }
//            final double[] uvA = array9;
//            final Vector3D i2 = vAB.clone();
//            i2.normalize();
//            i2.multiply(scale);
//            final Vector3D i3 = vAC.clone();
//            i3.normalize();
//            i3.multiply(scale);
//            final BufferedImage texture = (BufferedImage)textures.get(poly);
//            final Color poly_color = (Color)colors.get(poly);
//            final int maxW = (texture == null) ? 0 : (texture.getWidth() - 1);
//            final int maxH = (texture == null) ? 0 : (texture.getHeight() - 1);
//            final double l = scale / 2.0;
//            for (float aloop = 0.0f; aloop < lAB; aloop += l) {
//                for (float bloop = 0.0f; bloop < lAC; bloop += l) {
//                    final float ratio1 = aloop / lAB;
//                    final float ratio2 = bloop / lAC;
//                    if (ratio1 + ratio2 > 1.0f) {
//                        break;
//                    }
//                    final Point3D point3 = a.v.clone();
//                    point3.add(vAB.clone().multiply((double)ratio1));
//                    point3.add(vAC.clone().multiply((double)ratio2));
//                    final double colorU = uvA[0] + uvAB[0] * ratio1 + uvAC[0] * ratio2;
//                    final double colorV = uvA[1] + uvAB[1] * ratio1 + uvAC[1] * ratio2;
//                    Color pointcolor = null;
//                    if (texture == null) {
//                        if (poly.hasVertexColors()) {
//                            final WeightedColor cA = new WeightedColor(a.v.getColor().getRGB(), 1.0f - ratio1 - ratio2);
//                            final WeightedColor cB = new WeightedColor(b2.v.getColor().getRGB(), ratio1);
//                            final WeightedColor cC = new WeightedColor(c2.v.getColor().getRGB(), ratio2);
//                            pointcolor = Colors.blendColors(new WeightedColor[] { cA, cB, cC });
//                        }
//                        else {
//                            pointcolor = poly_color;
//                        }
//                    }
//                    else {
//                        pointcolor = new Color(texture.getRGB((int)Math.floor((double)maxW * colorU), (int)Math.floor((double)maxH - maxH * colorV)), true);
//                    }
//                    if (pointcolor.getAlpha() != 0) {
//                        point3.divide(scale);
//                        colormap.put(point3.toPositionRound(), pointcolor);
//                        colormap.put(point3.toPositionFloor(), pointcolor);
//                    }
//                }
//            }
//        }
//        VLogger.log("[IMPORT] Import complete, loaded " + f.size() + " faces");
//        VLogger.log("[IMPORT] Import complete, created " + colormap.size() + " voxels");
//        return new VoxelBox(colormap);
    }


}
