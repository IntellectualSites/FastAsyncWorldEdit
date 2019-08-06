package com.boydti.fawe.util;

import static java.lang.System.arraycopy;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.changeset.FaweStreamChangeSet;
import com.boydti.fawe.object.io.AbstractDelegateOutputStream;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.util.Location;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import net.jpountz.lz4.LZ4InputStream;
import net.jpountz.lz4.LZ4Utils;

public class MainUtil {
    /*
     * Generic non plugin related utils
     *  e.g. sending messages
     */
    public static void sendMessage(final FawePlayer<?> player, String message) {
        if (player == null) {
            Fawe.debug(message);
        } else {
            player.sendMessage(message);
        }
    }

    public static void sendAdmin(final String s) {
        for (final FawePlayer<?> player : Fawe.get().getCachedPlayers()) {
            if (player.hasPermission("fawe.admin")) {
                player.sendMessage(s);
            }
        }
        Fawe.debug(s);
    }

    public static List<String> filter(String prefix, List<String> suggestions) {
        if (prefix.isEmpty()) {
            return suggestions;
        }
        if (suggestions.getClass() != ArrayList.class) {
            suggestions = new ArrayList<>(suggestions);
        }
        suggestions.removeIf(s -> !s.startsWith(prefix));
        return suggestions;
    }

    public static <T> T getOf(Object[] arr, Class<T> ofType) {
        for (Object a : arr) {
            if (a != null && a.getClass() == ofType) {
                return (T) a;
            }
        }
        return null;
    }

    public static long getTotalSize(Path path) {
        final AtomicLong size = new AtomicLong(0);
        traverse(path, new RunnableVal2<Path, BasicFileAttributes>() {
            @Override
            public void run(Path path, BasicFileAttributes attrs) {
                size.addAndGet(attrs.size());
            }
        });
        return size.get();
    }

    public static void traverse(Path path, final BiConsumer<Path, BasicFileAttributes> onEach) {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult
                visitFile(Path file, BasicFileAttributes attrs) {
                    onEach.accept(file, attrs);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult
                visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult
                postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
        }
    }

    public static File resolveRelative(File file) {
        if (!file.exists()) return new File(relativize(file.getPath()));
        return file;
    }

    public static String relativize(String path) {
        String[] split = path.split(Pattern.quote(File.separator));
        StringBuilder out = new StringBuilder();
        int skip = 0;
        int len = split.length - 1;
        for (int i = len; i >= 0; i--) {
            if (skip > 0) skip--;
            else {
                String arg = split[i];
                if (arg.equals("..")) skip++;
                else out.insert(0, arg + (i == len ? "" : File.separator));
            }
        }
        return out.toString();
    }

    public static void forEachFile(Path path, final RunnableVal2<Path, BasicFileAttributes> onEach, Comparator<File> comparator) {
        File dir = path.toFile();
        if (!dir.exists()) return;
        File[] files = path.toFile().listFiles();
        if (comparator != null) Arrays.sort(files, comparator);
        for (File file : files) {
            Path filePath = file.toPath();
            try {
                BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
                onEach.run(file.toPath(), attr);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static int getMaxFileId(File folder) {
        final int[] max = new int[1];
        folder.listFiles(pathname -> {
            String name = pathname.getName();
            Integer val = null;
            if (pathname.isDirectory()) {
                val = StringMan.toInteger(name, 0, name.length());
            } else {
                int i = name.lastIndexOf('.');
                if (i != -1) val = StringMan.toInteger(name, 0, i);
            }
            if (val != null && val > max[0]) max[0] = val;
            return false;
        });
        return max[0] + 1;
    }

    public static File getFile(File base, String path) {
        if (Paths.get(path).isAbsolute()) {
            return new File(path);
        }
        return new File(base, path);
    }

    public static File getFile(File base, String path, String extension) {
        return getFile(base, path.endsWith("." + extension) ? path : path + "." + extension);
    }

    public static long getSize(ChangeSet changeSet) {
        if (changeSet instanceof FaweStreamChangeSet) {
            FaweStreamChangeSet fscs = (FaweStreamChangeSet) changeSet;
            return fscs.getSizeOnDisk() + fscs.getSizeInMemory();
//        } else if (changeSet instanceof CPUOptimizedChangeSet) {
//            return changeSet.size() + 32;
        } else if (changeSet != null) {
            return changeSet.size() * 128;
        } else {
            return 0;
        }
    }

    public static FaweOutputStream getCompressedOS(OutputStream os, int amount) throws IOException {
        return getCompressedOS(os, amount, Settings.IMP.HISTORY.BUFFER_SIZE);
    }

    private static final LZ4Factory FACTORY = LZ4Factory.fastestInstance();
    private static final LZ4Compressor COMPRESSOR = FACTORY.fastCompressor();
    private static final LZ4FastDecompressor DECOMPRESSOR = FACTORY.fastDecompressor();

    public static int getMaxCompressedLength(int size) {
        return LZ4Utils.maxCompressedLength(size);
    }

    public static byte[] compress(byte[] bytes, byte[] buffer, Deflater deflate) {
        if (buffer == null) {
            buffer = new byte[8192];
        }
        if (deflate == null) {
            deflate = new Deflater(1, false);
        } else {
            deflate.reset();
        }
        deflate.setInput(bytes);
        deflate.finish();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (!deflate.finished()) {
            int n = deflate.deflate(buffer);
            if (n != 0) baos.write(buffer, 0, n);
        }
        return baos.toByteArray();
    }

    public static byte[] decompress(byte[] bytes, byte[] buffer, Inflater inflater) throws DataFormatException {
        if (buffer == null) {
            buffer = new byte[8192];
        }
        if (inflater == null) {
            inflater = new Inflater(false);
        } else {
            inflater.reset();
        }
        inflater.setInput(bytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (!inflater.finished()) {
            int n = inflater.inflate(buffer);
            if (n != 0) baos.write(buffer, 0, n);
        }
        return baos.toByteArray();
    }

    public static byte[] compress(byte[] bytes, byte[] buffer, int level) {
        if (level == 0) {
            return bytes;
        }
        LZ4Compressor compressor = level == 1 ? COMPRESSOR : FACTORY.highCompressor(level);
        int decompressedLength = bytes.length;
        if (buffer == null) {
            int maxCompressedLength = compressor.maxCompressedLength(decompressedLength);
            buffer = new byte[maxCompressedLength];
        }
        int compressLen = compressor.compress(bytes, 0, decompressedLength, buffer, 0, buffer.length);
        return Arrays.copyOf(buffer, compressLen);
    }

    public static byte[] decompress(byte[] bytes, byte[] buffer, int length, int level) {
        if (level == 0) {
            return bytes;
        }
        if (buffer == null) {
            buffer = new byte[length];
        }
        DECOMPRESSOR.decompress(bytes, buffer);
        return buffer;
    }

    public static FaweOutputStream getCompressedOS(OutputStream os, int amount, int buffer) throws IOException {
        os.write((byte) 10 + amount);
        os = new BufferedOutputStream(os, buffer);
        if (amount == 0) {
            return new FaweOutputStream(os);
        }
        int gzipAmount = amount > 6 ? 1 : 0;
        for (int i = 0; i < gzipAmount; i++) {
            os = new ZstdOutputStream(os, 22);
        }
        LZ4Factory factory = LZ4Factory.fastestInstance();
        int fastAmount = 1 + ((amount - 1) % 3);
        for (int i = 0; i < fastAmount; i++) {
            os = new LZ4BlockOutputStream(os, buffer, factory.fastCompressor());
        }
        int highAmount = amount > 3 ? 1 : 0;
        for (int i = 0; i < highAmount; i++) {
            if (amount == 9) {
                os = new LZ4BlockOutputStream(os, buffer, factory.highCompressor(17));
            } else {
                os = new LZ4BlockOutputStream(os, buffer, factory.highCompressor());
            }
        }
        os = new BufferedOutputStream(os, buffer);
        return new FaweOutputStream(os);
    }

    public static FaweInputStream getCompressedIS(InputStream is) throws IOException {
        return getCompressedIS(is, Settings.IMP.HISTORY.BUFFER_SIZE);
    }

    public static FaweInputStream getCompressedIS(InputStream is, int buffer) throws IOException {
        int mode = (byte) is.read();
        is = new BufferedInputStream(is, buffer);
        if (mode == 0) {
            return new FaweInputStream(is);
        }
        boolean legacy;
        if (mode >= 10) {
            legacy = false;
            mode = -mode + 10;
        } else {
            legacy = true;
        }
        if (mode == 0) {
            return new FaweInputStream(is);
        }
        int amountAbs = Math.abs(mode);
        if (amountAbs > 6) {
            if (mode > 0) {
                is = new BufferedInputStream(new GZIPInputStream(is, buffer));
            } else {
                is = new ZstdInputStream(is);
            }
        }
        amountAbs = (1 + ((amountAbs - 1) % 3)) + (amountAbs > 3 ? 1 : 0);
        for (int i = 0; i < amountAbs; i++) {
            if (legacy) {
                is = new LZ4InputStream(is);
            } else {
                is = new LZ4BlockInputStream(is);
            }
        }
        return new FaweInputStream(is);
    }

    public static URL upload(UUID uuid, String file, String extension, final RunnableVal<OutputStream> writeTask) {
        return upload(Settings.IMP.WEB.URL, uuid != null, uuid != null ? uuid.toString() : null, file, extension, writeTask);
    }

    public static URL upload(String urlStr, boolean save, String uuid, String file, String extension, final RunnableVal<OutputStream> writeTask) {
        if (writeTask == null) {
            Fawe.debug("&cWrite task cannot be null");
            return null;
        }
        String filename = (file == null ? "plot" : file) + (extension != null ? "." + extension : "");
        uuid = uuid == null ? UUID.randomUUID().toString() : uuid;
        final String website;
        if (!save) {
            website = urlStr + "upload.php?" + uuid;

        } else {
            website = urlStr + "save.php?" + uuid;
        }
        final URL url;
        try {
            url = new URL(urlStr + "?key=" + uuid + "&type=" + "" + extension);
            String boundary = Long.toHexString(System.currentTimeMillis());
            URLConnection con = new URL(website).openConnection();
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            try (OutputStream output = con.getOutputStream(); PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), true)) {
                String crlf = "\r\n";
                writer.append("--" + boundary).append(crlf);
                writer.append("Content-Disposition: form-data; name=\"param\"").append(crlf);
                writer.append("Content-Type: text/plain; charset=" + StandardCharsets.UTF_8.displayName()).append(crlf);
                String param = "value";
                writer.append(crlf).append(param).append(crlf).flush();
                writer.append("--" + boundary).append(crlf);
                writer.append("Content-Disposition: form-data; name=\"schematicFile\"; filename=\"" + filename + '"').append(crlf);
                writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(filename)).append(crlf);
                writer.append("Content-Transfer-Encoding: binary").append(crlf);
                writer.append(crlf).flush();
                OutputStream nonClosable = new AbstractDelegateOutputStream(new BufferedOutputStream(output)) {
                    @Override
                    public void close() {
                    } // Don't close
                };
                writeTask.value = nonClosable;
                writeTask.run();
                nonClosable.flush();
                writer.append(crlf).flush();
                writer.append("--" + boundary + "--").append(crlf).flush();
            }
            int responseCode = ((HttpURLConnection) con).getResponseCode();
            String content;
            try (Scanner scanner = new Scanner(con.getInputStream()).useDelimiter("\\A")) {
                content = scanner.next().trim();
            }
            if (!content.startsWith("<")) {
                Fawe.debug(content);
            }
            if (responseCode == 200) {
                return url;
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void setPosition(CompoundTag tag, int x, int y, int z) {
        Map<String, Tag> value = ReflectionUtils.getMap(tag.getValue());
        value.put("x", new IntTag(x));
        value.put("y", new IntTag(y));
        value.put("z", new IntTag(z));
    }

    public static void setEntityInfo(CompoundTag tag, Entity entity) {
        Map<String, Tag> map = ReflectionUtils.getMap(tag.getValue());
        map.put("Id", new StringTag(entity.getState().getType().getId()));
        ListTag pos = (ListTag) map.get("Pos");
        if (pos != null) {
            Location loc = entity.getLocation();
            List<Tag> posList = ReflectionUtils.getList(pos.getValue());
            posList.set(0, new DoubleTag(loc.getX()));
            posList.set(1, new DoubleTag(loc.getY()));
            posList.set(2, new DoubleTag(loc.getZ()));
        }
    }

    public static String getText(String url) throws IOException {
        try (Scanner scanner = new Scanner(new URL(url).openStream(), "UTF-8")) {
            return scanner.useDelimiter("\\A").next();
        }
    }

    public static void download(URL url, File out) throws IOException {
        if (!out.exists()) {
            File parent = out.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            File tempFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp", parent);
            tempFile.deleteOnExit();
            try (InputStream is = url.openStream(); ReadableByteChannel rbc = Channels.newChannel(is);
                 FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }
            Files.copy(tempFile.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
            tempFile.delete();
        }
    }


    public static File getJarFile() {
        try {
            return getJarFile(Fawe.class);
        } catch (MalformedURLException | URISyntaxException | SecurityException e) {
            return new File(Fawe.imp().getDirectory().getParentFile(), "FastAsyncWorldEdit.jar");
        }
    }

    public static File getJarFile(Class<?> clazz) throws URISyntaxException, MalformedURLException {
        URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
        return new File(new URL(url.toURI().toString().split("\\!")[0].replaceAll("jar:file", "file")).toURI().getPath());
    }

    public static Thread[] getThreads() {
        ThreadGroup rootGroup = Thread.currentThread( ).getThreadGroup( );
        ThreadGroup parentGroup;
        while ( ( parentGroup = rootGroup.getParent() ) != null ) {
            rootGroup = parentGroup;
        }
        Thread[] threads = new Thread[ rootGroup.activeCount() ];
        if (threads.length != 0) {
            while (rootGroup.enumerate(threads, true) == threads.length) {
                threads = new Thread[threads.length * 2];
            }
        }
        return threads;
    }

    public static File copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            File parent = destFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            destFile.createNewFile();
        }
        try (FileInputStream fIn = new FileInputStream(sourceFile); FileChannel source = fIn.getChannel();
             FileOutputStream fOut = new FileOutputStream(destFile); FileChannel destination = fOut.getChannel()) {
            long transfered = 0;
            long bytes = source.size();
            while (transfered < bytes) {
                transfered += destination.transferFrom(source, 0, source.size());
                destination.position(transfered);
            }
        }
        return destFile;
    }

    public static BufferedImage readImage(InputStream in) throws IOException {
        return MainUtil.toRGB(ImageIO.read(in));
    }

    public static BufferedImage readImage(URL url) throws IOException {
        return readImage(url.openStream());
    }

    public static BufferedImage readImage(File file) throws IOException {
        return readImage(new FileInputStream(file));
    }

    public static BufferedImage toRGB(BufferedImage src) {
        if (src == null) return src;
        BufferedImage img = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.drawImage(src, 0, 0, null);
        g2d.dispose();
        return img;
    }

    public static File copyFile(File jar, String resource, File output) {
        return copyFile(jar, resource, output, resource);
    }

    public static File copyFile(File jar, String resource, File output, String fileName) {
        try {
            if (output == null) {
                output = Fawe.imp().getDirectory();
            }
            if (!output.exists()) {
                output.mkdirs();
            }
            File newFile = new File(output, fileName);
            if (newFile.exists()) {
                return newFile;
            }
            try (InputStream stream = Fawe.class.getResourceAsStream(resource.startsWith("/") ? resource : "/" + resource)) {
                byte[] buffer = new byte[2048];
                if (stream == null) {
                    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(jar))) {
                        ZipEntry ze = zis.getNextEntry();
                        while (ze != null) {
                            String name = ze.getName();
                            if (name.equals(resource)) {
                                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                                    int len;
                                    while ((len = zis.read(buffer)) > 0) {
                                        fos.write(buffer, 0, len);
                                    }
                                }
                                ze = null;
                            } else {
                                ze = zis.getNextEntry();
                            }
                        }
                        zis.closeEntry();
                    }
                    return newFile;
                }
                File parent = newFile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                newFile.createNewFile();
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = stream.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                return newFile;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Fawe.debug("&cCould not save " + resource);
        }
        return null;
    }

    public static int[] regionNameToCoords(String fileName) {
        int[] res = new int[2];
        int len = fileName.length() - 4;
        int val = 0;
        boolean reading = false;
        int index = 1;
        int numIndex = 1;
        for (int i = len; i >= 2; i--) {
            char c = fileName.charAt(i);
            if (!reading) {
                reading = (c == '.');
                continue;
            }
            switch (c) {
                case '-':
                    val = -val;
                    break;
                case '.':
                    res[index--] = val;
                    if (index == -1) {
                        return res;
                    }
                    val = 0;
                    numIndex = 1;
                    break;
                default:
                    val = val + (c - 48) * numIndex;
                    numIndex *= 10;
                    break;
            }
        }
        res[index] = val;
        return res;
    }

    public static File resolve(File dir, String filename, @Nullable ClipboardFormat format, boolean allowDir) {
        if (format != null) {
            if (!filename.matches(".*\\.[\\w].*")) {
                filename = filename + "." + format.getPrimaryFileExtension();
            }
            return MainUtil.resolveRelative(new File(dir, filename));
        }
        if (allowDir) {
            File file = MainUtil.resolveRelative(new File(dir, filename));
            if (file.exists() && file.isDirectory()) return file;
        }
        for (ClipboardFormat f : ClipboardFormats.getAll()) {
            File file = MainUtil.resolveRelative(new File(dir, filename + "." + f.getPrimaryFileExtension()));
            if (file.exists()) return file;
        }
        return null;
    }

    public static boolean isInSubDirectory(File dir, File file) throws IOException {
        if (file == null) return false;
        if (file.equals(dir)) return true;
        file = file.getCanonicalFile();
        dir = dir.getCanonicalFile();
        return isInSubDirectory(dir, file.getParentFile());
    }

    public static void iterateFiles(File directory, Consumer<File> task) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (null != files) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        iterateFiles(file, task);
                    } else {
                        task.accept(file);
                    }
                }
            }
        }
    }

    /**
     * The int[] will be in the form: [chunkx, chunkz, pos1x, pos1z, pos2x, pos2z, isedge] and will represent the bottom and top parts of the chunk
     */
    public static void chunkTaskSync(RegionWrapper region, final RunnableVal<int[]> task) {
        final int p1x = region.minX;
        final int p1z = region.minZ;
        final int p2x = region.maxX;
        final int p2z = region.maxZ;
        final int bcx = p1x >> 4;
        final int bcz = p1z >> 4;
        final int tcx = p2x >> 4;
        final int tcz = p2z >> 4;
        task.value = new int[7];
        for (int x = bcx; x <= tcx; x++) {
            for (int z = bcz; z <= tcz; z++) {
                task.value[0] = x;
                task.value[1] = z;
                task.value[2] = task.value[0] << 4;
                task.value[3] = task.value[1] << 4;
                task.value[4] = task.value[2] + 15;
                task.value[5] = task.value[3] + 15;
                task.value[6] = 0;
                if (task.value[0] == bcx) {
                    task.value[2] = p1x;
                    task.value[6] = 1;
                }
                if (task.value[0] == tcx) {
                    task.value[4] = p2x;
                    task.value[6] = 1;
                }
                if (task.value[1] == bcz) {
                    task.value[3] = p1z;
                    task.value[6] = 1;
                }
                if (task.value[1] == tcz) {
                    task.value[5] = p2z;
                    task.value[6] = 1;
                }
                task.run();
            }
        }
    }

    public static Object copyNd(Object arr) {
        if (arr.getClass().isArray()) {
            int innerArrayLength = Array.getLength(arr);
            Class component = arr.getClass().getComponentType();
            Object newInnerArray = Array.newInstance(component, innerArrayLength);
            if (component.isPrimitive()) {
                arraycopy(arr, 0, newInnerArray, 0, innerArrayLength);
            } else {
                //copy each elem of the array
                for (int i = 0; i < innerArrayLength; i++) {
                    Object elem = copyNd(Array.get(arr, i));
                    Array.set(newInnerArray, i, elem);
                }
            }
            return newInnerArray;
        } else {
            return arr;//cant deep copy an opac object??
        }
    }

    public static String secToTime(long time) {
        StringBuilder toreturn = new StringBuilder();
        if (time >= 33868800) {
            int years = (int) (time / 33868800);
            int time1 = years * 33868800;
            System.out.println(time1);
            time -= time1;
            toreturn.append(years + "y ");
        }
        if (time >= 604800) {
            int weeks = (int) (time / 604800);
            time -= weeks * 604800;
            toreturn.append(weeks + "w ");
        }
        if (time >= 86400) {
            int days = (int) (time / 86400);
            time -= days * 86400;
            toreturn.append(days + "d ");
        }
        if (time >= 3600) {
            int hours = (int) (time / 3600);
            time -= hours * 3600;
            toreturn.append(hours + "h ");
        }
        if (time >= 60) {
            int minutes = (int) (time / 60);
            time -= minutes * 60;
            toreturn.append(minutes + "m ");
        }
        if (toreturn.equals("") || time > 0) {
            toreturn.append((time) + "s ");
        }
        return toreturn.toString().trim();
    }

    public static long timeToSec(String string) {
        if (MathMan.isInteger(string)) {
            return Long.parseLong(string);
        }
        string = string.toLowerCase().trim().toLowerCase();
        if (string.equalsIgnoreCase("false")) {
            return 0;
        }
        String[] split = string.split(" ");
        long time = 0;
        for (String value : split) {
            int nums = Integer.parseInt(value.replaceAll("[^\\d]", ""));
            String letters = value.replaceAll("[^a-z]", "");
            switch (letters) {
                case "week":
                case "weeks":
                case "wks":
                case "w":

                    time += 604800 * nums;
                case "days":
                case "day":
                case "d":
                    time += 86400 * nums;
                case "hour":
                case "hr":
                case "hrs":
                case "hours":
                case "h":
                    time += 3600 * nums;
                case "minutes":
                case "minute":
                case "mins":
                case "min":
                case "m":
                    time += 60 * nums;
                case "seconds":
                case "second":
                case "secs":
                case "sec":
                case "s":
                    time += nums;
            }
        }
        return time;
    }

    public static void deleteOlder(File directory, final long timeDiff, boolean printDebug) {
        final long now = System.currentTimeMillis();
        ForkJoinPool pool = new ForkJoinPool();
        iterateFiles(directory, file -> {
            long age = now - file.lastModified();
            if (age > timeDiff) {
                pool.submit(file::delete);
                if (printDebug) BBC.FILE_DELETED.send((Player)null, file);
            }
        });
        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
