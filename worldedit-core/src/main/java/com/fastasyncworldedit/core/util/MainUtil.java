package com.fastasyncworldedit.core.util;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.history.changeset.FaweStreamChangeSet;
import com.fastasyncworldedit.core.internal.exception.FaweException;
import com.fastasyncworldedit.core.internal.io.AbstractDelegateOutputStream;
import com.fastasyncworldedit.core.internal.io.FaweInputStream;
import com.fastasyncworldedit.core.internal.io.FaweOutputStream;
import com.fastasyncworldedit.core.regions.RegionWrapper;
import com.fastasyncworldedit.core.util.task.RunnableVal;
import com.fastasyncworldedit.core.util.task.RunnableVal2;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.text.Component;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.System.arraycopy;

public class MainUtil {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private static final String CURL_USER_AGENT = "curl/8.1.1";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

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

    public static long getTotalSize(Path path) {
        final AtomicLong size = new AtomicLong(0);
        traverse(path, new RunnableVal2<>() {
            @Override
            public void run(Path path, BasicFileAttributes attrs) {
                size.addAndGet(attrs.size());
            }
        });
        return size.get();
    }

    public static void traverse(Path path, final BiConsumer<Path, BasicFileAttributes> onEach) {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
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
        if (!file.exists()) {
            return new File(relativize(file.getPath()));
        }
        return file;
    }

    public static String relativize(String path) {
        String[] split = path.split(Pattern.quote(File.separator));
        StringBuilder out = new StringBuilder();
        int skip = 0;
        int len = split.length - 1;
        for (int i = len; i >= 0; i--) {
            if (skip > 0) {
                skip--;
            } else {
                String arg = split[i];
                if (arg.equals("..")) {
                    skip++;
                } else {
                    out.insert(0, arg + (i == len ? "" : File.separator));
                }
            }
        }
        return out.toString();
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
                if (i != -1) {
                    val = StringMan.toInteger(name, 0, i);
                }
            }
            if (val != null && val > max[0]) {
                max[0] = val;
            }
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
        if (changeSet instanceof FaweStreamChangeSet fscs) {
            return fscs.getSizeOnDisk() + fscs.getSizeInMemory();
//        } else if (changeSet instanceof CPUOptimizedChangeSet) {
//            return changeSet.size() + 32;
        } else if (changeSet != null) {
            return changeSet.longSize() * 128; // Approx
        } else {
            return 0;
        }
    }

    public static FaweOutputStream getCompressedOS(OutputStream os, int amount) throws IOException {
        return getCompressedOS(os, amount, Settings.settings().HISTORY.BUFFER_SIZE);
    }

    private static final LZ4Factory FACTORY = LZ4Factory.fastestInstance();
    private static final LZ4Compressor COMPRESSOR = FACTORY.fastCompressor();
    private static final LZ4FastDecompressor DECOMPRESSOR = FACTORY.fastDecompressor();

    public static int getMaxCompressedLength(int size) {
        return COMPRESSOR.maxCompressedLength(size);
    }

    public static int compress(byte[] bytes, int length, byte[] buffer, OutputStream out, Deflater deflate) throws IOException {
        if (buffer == null) {
            buffer = new byte[8192];
        }
        if (deflate == null) {
            deflate = new Deflater(1, false);
        } else {
            deflate.reset();
        }
        deflate.setInput(bytes, 0, length);
        deflate.finish();
        int written = 0;
        while (!deflate.finished()) {
            int n = deflate.deflate(buffer);
            if (n != 0) {
                written += n;
                out.write(buffer, 0, n);
            }
        }
        return written;
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

    /**
     * Note: The returned stream is not thread safe.
     */
    public static FaweOutputStream getCompressedOS(OutputStream os, int amount, int buffer) throws IOException {
        os.write((byte) 10 + amount);
        os = new FastBufferedOutputStream(os, buffer);
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
        os = new FastBufferedOutputStream(os, buffer);
        return new FaweOutputStream(os);
    }

    public static FaweInputStream getCompressedIS(InputStream is) throws IOException {
        return getCompressedIS(is, Settings.settings().HISTORY.BUFFER_SIZE);
    }

    public static FaweInputStream getCompressedIS(InputStream is, int buffer) throws IOException {
        int mode = (byte) is.read();
        is = new FastBufferedInputStream(is, buffer);
        if (mode == 0) {
            return new FaweInputStream(is);
        }
        if (mode >= 10) {
            mode = -mode + 10;
        }
        if (mode == 0) {
            return new FaweInputStream(is);
        }
        int amountAbs = Math.abs(mode);
        if (amountAbs > 6) {
            if (mode > 0) {
                is = new FastBufferedInputStream(new GZIPInputStream(is, buffer));
            } else {
                is = new ZstdInputStream(is);
            }
        }
        amountAbs = (1 + ((amountAbs - 1) % 3)) + (amountAbs > 3 ? 1 : 0);
        for (int i = 0; i < amountAbs; i++) {
            is = new LZ4BlockInputStream(is);
        }
        return new FaweInputStream(new FastBufferedInputStream(is));
    }

    public static URL upload(UUID uuid, String file, String extension, @Nonnull final RunnableVal<OutputStream> writeTask) {
        return upload(Settings.settings().WEB.URL, uuid != null, uuid != null ? uuid.toString() : null, file, extension, writeTask);
    }

    public static URL upload(
            String urlStr,
            boolean save,
            String uuid,
            String file,
            String extension,
            @Nonnull final RunnableVal<OutputStream> writeTask
    ) {
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
            try (OutputStream output = con.getOutputStream(); PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                    output,
                    StandardCharsets.UTF_8
            ), true)) {
                String crlf = "\r\n";
                writer.append("--").append(boundary).append(crlf);
                writer.append("Content-Disposition: form-data; name=\"param\"").append(crlf);
                writer.append("Content-Type: text/plain; charset=").append(StandardCharsets.UTF_8.displayName()).append(crlf);
                String param = "value";
                writer.append(crlf).append(param).append(crlf).flush();
                writer.append("--").append(boundary).append(crlf);
                writer.append("Content-Disposition: form-data; name=\"schematicFile\"; filename=\"").append(filename).append(String.valueOf('"'))
                        .append(crlf);
                writer.append("Content-Type: ").append(URLConnection.guessContentTypeFromName(filename)).append(crlf);
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
                writer.append("--").append(boundary).append("--").append(crlf).flush();
            }
            int responseCode = ((HttpURLConnection) con).getResponseCode();
            String content;
            try (Scanner scanner = new Scanner(con.getInputStream()).useDelimiter("\\A")) {
                content = scanner.next().trim();
            }
            if (!content.startsWith("<")) {
                LOGGER.info(content);
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

    /**
     * Create a copy of the tag and modify the (x, y, z) coordinates
     *
     * @param tag Tag to copy
     * @param x   New X coordinate
     * @param y   New Y coordinate
     * @param z   New Z coordinate
     * @return New tag
     */
    @Nonnull
    public static CompoundTag setPosition(@Nonnull CompoundTag tag, int x, int y, int z) {
        Map<String, Tag<?, ?>> value = new HashMap<>(tag.getValue());
        value.put("x", new IntTag(x));
        value.put("y", new IntTag(y));
        value.put("z", new IntTag(z));
        return new CompoundTag(value);
    }

    /**
     * Create a copy of the tag and modify the entity inf
     *
     * @param tag    Tag to copy
     * @param entity Entity
     * @return New tag
     */
    @Nonnull
    public static CompoundTag setEntityInfo(@Nonnull CompoundTag tag, @Nonnull Entity entity) {
        Map<String, Tag<?, ?>> map = new HashMap<>(tag.getValue());
        map.put("Id", new StringTag(entity.getState().getType().id()));
        ListTag pos = (ListTag) map.get("Pos");
        if (pos != null) {
            Location loc = entity.getLocation();
            // Create a copy, because the list is immutable...
            List<Tag> posList = new ArrayList<>(pos.getValue());
            posList.set(0, new DoubleTag(loc.x()));
            posList.set(1, new DoubleTag(loc.y()));
            posList.set(2, new DoubleTag(loc.z()));
            map.put("Pos", new ListTag(pos.getType(), posList));
        }
        return new CompoundTag(map);
    }

    public static String getText(String url) throws IOException {
        try (Scanner scanner = new Scanner(new URL(url).openStream(), StandardCharsets.UTF_8)) {
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
            return new File(Fawe.platform().getDirectory().getParentFile(), "FastAsyncWorldEdit.jar");
        }
    }

    public static File getJarFile(Class<?> clazz) throws URISyntaxException, MalformedURLException {
        URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
        return new File(new URL(url.toURI().toString().split("\\!")[0].replaceAll("jar:file", "file")).toURI().getPath());
    }

    public static Thread[] getThreads() {
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup parentGroup;
        while ((parentGroup = rootGroup.getParent()) != null) {
            rootGroup = parentGroup;
        }
        Thread[] threads = new Thread[rootGroup.activeCount()];
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
            long transferred = 0;
            long bytes = source.size();
            while (transferred < bytes) {
                transferred += destination.transferFrom(source, 0, source.size());
                destination.position(transferred);
            }
        }
        return destFile;
    }

    public static BufferedImage readImage(InputStream stream) throws IOException {
        final ImageInputStream imageStream = ImageIO.createImageInputStream(stream);
        if (imageStream == null) {
            throw new IOException("Can't find suitable ImageInputStream");
        }
        Iterator<ImageReader> iter = ImageIO.getImageReaders(imageStream);
        if (!iter.hasNext()) {
            throw new IOException("Could not get image reader from stream.");
        }
        ImageReader reader = iter.next();
        ImageReadParam param = reader.getDefaultReadParam();
        reader.setInput(imageStream, true, true);
        BufferedImage bi;
        try {
            bi = reader.read(0, param);
        } finally {
            reader.dispose();
            stream.close();
            imageStream.close();
        }
        return MainUtil.toRGB(bi);
    }

    public static BufferedImage readImage(URL url) throws IOException {
        try (final InputStream stream = readImageStream(url.toURI())) {
            return readImage(stream);
        } catch (URISyntaxException e) {
            throw new IOException("failed to parse url to uri reference", e);
        }
    }

    public static InputStream readImageStream(final URI uri) throws IOException {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri).GET();

            if (uri.getHost().equalsIgnoreCase("i.imgur.com")) {
                requestBuilder = requestBuilder.setHeader("User-Agent", CURL_USER_AGENT);
            }

            final HttpResponse<InputStream> response = HTTP_CLIENT.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            final InputStream body = response.body();
            if (response.statusCode() > 299) {
                throw new IOException("Expected 2xx as response code, but received " + response.statusCode());
            }
            return body;
        } catch (InterruptedException e) {
            throw new IOException("request was interrupted", e);
        }
    }

    public static BufferedImage readImage(File file) throws IOException {
        return readImage(new FileInputStream(file));
    }

    public static void checkImageHost(URI uri) throws IOException {
        if (Settings.settings().WEB.ALLOWED_IMAGE_HOSTS.contains("*")) {
            return;
        }
        String host = uri.getHost();
        if (Settings.settings().WEB.ALLOWED_IMAGE_HOSTS.stream().anyMatch(host::equalsIgnoreCase)) {
            return;
        }
        throw new IOException(String.format(
                "Host `%s` not allowed! Whitelisted image hosts are: %s",
                host,
                StringMan.join(Settings.settings().WEB.ALLOWED_IMAGE_HOSTS, ", ")
        ));
    }

    public static BufferedImage toRGB(BufferedImage src) {
        if (src == null) {
            return src;
        } else if ((long) src.getWidth() * src.getHeight() > Settings.settings().WEB.MAX_IMAGE_SIZE) {
            throw new FaweException(Caption.of("fawe.web.image.load.size.too-large", Settings.settings().WEB.MAX_IMAGE_SIZE));
        }
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
                output = Fawe.platform().getDirectory();
            }
            if (!output.exists()) {
                output.mkdirs();
            }
            File newFile = new File(output, fileName);
            if (newFile.exists()) {
                return newFile;
            }
            try (InputStream stream = Fawe.class.getResourceAsStream(resource.startsWith("/") ? resource : "/" + resource)) {
                if (stream == null) {
                    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(jar))) {
                        ZipEntry ze = zis.getNextEntry();
                        while (ze != null) {
                            String name = ze.getName();
                            if (name.equals(resource)) {
                                try (OutputStream outputStream = Files.newOutputStream(newFile.toPath())) {
                                    zis.transferTo(outputStream);
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
                try (OutputStream outputStream = Files.newOutputStream(newFile.toPath())) {
                    stream.transferTo(outputStream);
                }
                return newFile;
            }
        } catch (IOException e) {
            LOGGER.error("Could not save {}, {}", resource, e);
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
                case '-' -> val = -val;
                case '.' -> {
                    res[index--] = val;
                    if (index == -1) {
                        return res;
                    }
                    val = 0;
                    numIndex = 1;
                }
                default -> {
                    val = val + (c - 48) * numIndex;
                    numIndex *= 10;
                }
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
            if (file.exists() && file.isDirectory()) {
                return file;
            }
        }
        for (ClipboardFormat f : ClipboardFormats.getAll()) {
            File file = MainUtil.resolveRelative(new File(dir, filename + "." + f.getPrimaryFileExtension()));
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    public static boolean isInSubDirectory(File dir, File file) throws IOException {
        if (file == null) {
            return false;
        }
        if (file.equals(dir)) {
            return true;
        }
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
            Class<?> component = arr.getClass().getComponentType();
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
            return arr;//can't deep copy an opac object??
        }
    }

    public static String secToTime(long time) {
        StringBuilder toreturn = new StringBuilder();
        if (time >= 33868800) {
            int years = (int) (time / 33868800);
            int time1 = years * 33868800;
            time -= time1;
            toreturn.append(years).append("y ");
        }
        if (time >= 604800) {
            int weeks = (int) (time / 604800);
            time -= weeks * 604800L;
            toreturn.append(weeks).append("w ");
        }
        if (time >= 86400) {
            int days = (int) (time / 86400);
            time -= days * 86400L;
            toreturn.append(days).append("d ");
        }
        if (time >= 3600) {
            int hours = (int) (time / 3600);
            time -= hours * 3600L;
            toreturn.append(hours).append("h ");
        }
        if (time >= 60) {
            int minutes = (int) (time / 60);
            time -= minutes * 60L;
            toreturn.append(minutes).append("m ");
        }
        if (toreturn.equals("") || time > 0) {
            toreturn.append(time).append("s ");
        }
        return toreturn.toString().trim();
    }

    public static long timeToSec(String string) {
        if (MathMan.isInteger(string)) {
            return Long.parseLong(string);
        }
        string = string.toLowerCase(Locale.ROOT).trim().toLowerCase(Locale.ROOT);
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

                    time += 604800L * nums;
                case "days":
                case "day":
                case "d":
                    time += 86400L * nums;
                case "hour":
                case "hr":
                case "hrs":
                case "hours":
                case "h":
                    time += 3600L * nums;
                case "minutes":
                case "minute":
                case "mins":
                case "min":
                case "m":
                    time += 60L * nums;
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
                Component msg = Caption.of("worldedit.schematic.delete.deleted");
                if (printDebug) {
                    LOGGER.info(msg.toString());
                }
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
