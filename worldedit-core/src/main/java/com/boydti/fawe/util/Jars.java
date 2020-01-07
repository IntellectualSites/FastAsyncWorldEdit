package com.boydti.fawe.util;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public enum Jars {

    MM_v1_7_3(
        "https://github.com/InventivetalentDev/MapManager/releases/download/1.7.3-SNAPSHOT/MapManager_v1.7.3-SNAPSHOT.jar",
        "m3YLUqZz66k2DmvdcYLeu38u3zKRKhrAXqGGpVKfO6g=", 554831),

    PL_v3_7_3(
        "https://github.com/InventivetalentDev/PacketListenerAPI/releases/download/3.7.3-SNAPSHOT/PacketListenerAPI_v3.7.3-SNAPSHOT.jar",
        "etdBRzLn5pRVDfr/mSQdPm6Jjer3wQOKhcn8fUxo5zM=", 167205),

    ;

    public final String url;
    public final int fileSize;
    public final String digest;

    /**
     * @param url Where this jar can be found and downloaded
     * @param digest The Base64-encoded SHA-256 digest
     * @param fileSize Size of this jar in bytes
     */
    Jars(String url, String digest, int fileSize) {
        this.url = url;
        this.digest = digest;
        this.fileSize = fileSize;
    }

    /**
     * Download a jar, verify hash, return byte[] containing the jar
     */
    public byte[] download() throws IOException {
        byte[] jarBytes = new byte[this.fileSize];
        URL url = new URL(this.url);
        try (DataInputStream dis = new DataInputStream(url.openConnection().getInputStream())) {
            dis.readFully(jarBytes);
            if (dis.read() != -1) { // assert that we've read everything
                throw new IllegalStateException("downloaded jar is longer than expected");
            }
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] jarDigestBytes = md.digest(jarBytes);

            String jarDigest = Base64.getEncoder().encodeToString(jarDigestBytes);

            if (this.digest.equals(jarDigest)) {
                getLogger(Jars.class).debug("++++ HASH CHECK ++++");
                getLogger(Jars.class).debug(this.url);
                getLogger(Jars.class).debug(this.digest);
                return jarBytes;
            } else {
                getLogger(Jars.class).debug(jarDigest + " | " + url);
                throw new IllegalStateException("The downloaded jar does not match the hash");
            }
        } catch (NoSuchAlgorithmException e) {
            // Shouldn't ever happen, Minecraft won't even run on such a JRE
            throw new IllegalStateException("Your JRE does not support SHA-256");
        }
    }
}
