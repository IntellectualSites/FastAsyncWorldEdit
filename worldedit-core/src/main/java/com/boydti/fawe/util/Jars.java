package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public enum Jars {

    MM_v1_4_0("https://github.com/InventivetalentDev/MapManager/releases/download/1.4.0-SNAPSHOT/MapManager_v1.4.0-SNAPSHOT.jar",
              "AEO5SKBUGN4YJRS8XGGNLBM2QRZPTI1KF0/1W1URTGA=", 163279),

    PL_v3_6_0("https://github.com/InventivetalentDev/PacketListenerAPI/releases/download/3.6.0-SNAPSHOT/PacketListenerAPI_v3.6.0-SNAPSHOT.jar",
              "OYBE75VIU+NNWHRVREBLDARWA+/TBDQZ1RC562QULBA=", 166508),

    ;

    public final String url;
    public final int filesize;
    public final String digest;

    /**
     * @param url Where this jar can be found and downloaded
     * @param digest The SHA-256 hexadecimal digest
     * @param filesize Size of this jar in bytes
     */
    Jars(String url, String digest, int filesize) {
        this.url = url;
        this.digest = digest.toUpperCase();
        this.filesize = filesize;
    }

    /**
     * download a jar, verify hash, return byte[] containing the jar
     */
    public byte[] download() throws IOException {
        byte[] jarBytes = new byte[this.filesize];
        URL url = new URL(this.url);
        try (DataInputStream dis = new DataInputStream(url.openConnection().getInputStream())) {
            dis.readFully(jarBytes);
            if (dis.read() != -1) { // assert that we've read everything
                throw new IllegalStateException("downloaded jar is longer than expected");
            }
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] jarDigestBytes = md.digest(jarBytes);

            String jarDigest = Base64.getEncoder().encodeToString(jarDigestBytes).toUpperCase();

            if (this.digest.equals(jarDigest)) {
                Fawe.debug("++++ HASH CHECK ++++");
                Fawe.debug(this.url);
                Fawe.debug(this.digest);
                return jarBytes;
            } else {

                Fawe.debug(jarDigest + " | " + url);
                throw new IllegalStateException("downloaded jar does not match the hash");
            }
        } catch (NoSuchAlgorithmException e) {
            // Shouldn't ever happen, Minecraft won't even run on such a JRE
            throw new IllegalStateException("Your JRE does not support SHA-256");
        }
    }
}
