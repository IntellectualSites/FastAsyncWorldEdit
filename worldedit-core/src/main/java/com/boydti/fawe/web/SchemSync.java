package com.boydti.fawe.web;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.UtilityCommands;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SchemSync implements Runnable {

    private final static char PORT = 62522;

    private final File tokensFile;
    private final WorldEdit worldEdit;
    private final File working;
    private Map<UUID, UUID> tokens;

    private ServerSocket serverSocket;
    private Socket clientSocket;

    private enum Error {
        INVALID_HEADER_LENGTH,
        TOKEN_REJECTED,
        FILE_NOT_EXIST,
        NO_FILE_PERMISSIONS,
        ;

    }

    public SchemSync() {
        this.tokensFile = MainUtil
            .getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.TOKENS, "tokens.txt");
        this.worldEdit = WorldEdit.getInstance();
        LocalConfiguration config = worldEdit.getConfiguration();
        this.working = worldEdit.getWorkingDirectoryFile(config.saveDir);
    }

    private void loadTokens() {
        if (tokens == null) {
            String tokensDir = Settings.IMP.PATHS.TOKENS;
            tokens = new HashMap<>();
        }
    }

    private void close(Error error) throws IOException {
        this.clientSocket.getOutputStream().write(error.ordinal());
        throw FaweCache.INSTANCE.getMANUAL();
    }

    @Override
    public synchronized void run() {
        try {
            byte[] header = new byte[32];
            try (ServerSocket serverSocket = this.serverSocket = new ServerSocket(PORT)) {
                while (!Thread.interrupted()) {
                    try (Socket clientSocket = this.clientSocket = serverSocket
                        .accept(); InputStream in = clientSocket.getInputStream()) {
                        int read = in.read(header);
                        if (read != header.length) {
                            close(Error.INVALID_HEADER_LENGTH);
                        }

                        ByteBuffer buf = ByteBuffer.wrap(header);
                        UUID uuid = new UUID(buf.getLong(), buf.getLong());
                        UUID expectedToken = tokens.get(uuid);
                        if (expectedToken == null) {
                            close(Error.TOKEN_REJECTED);
                        }

                        UUID receivedToken = new UUID(buf.getLong(), buf.getLong());
                        if (!receivedToken.equals(expectedToken)) {
                            continue;
                        }

                        File dir = new File(working, uuid.toString());
                        try (DataInputStream dis = new DataInputStream(in)) {
                            int data = dis.readByte() & 0xFF;
                            switch (data) {
                                case 0: // list
                                    try (DataOutputStream out = new DataOutputStream(
                                        clientSocket.getOutputStream())) {
                                        out.write(1);
                                        UtilityCommands.allFiles(dir.listFiles(), true,
                                            file -> {
                                                try {
                                                    String path = dir.toURI()
                                                        .relativize(file.toURI()).getPath();
                                                    out.writeUTF(path);
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            });
                                    }
                                    break;
                                case 1: // get
                                    String input = dis.readUTF();
                                    File file = new File(dir, input);
                                    if (!MainUtil.isInSubDirectory(dir, file)) {
                                        close(Error.NO_FILE_PERMISSIONS);
                                    }
                                    if (!file.exists()) {
                                        close(Error.FILE_NOT_EXIST);
                                    }

                                    // todo send file
                            }
                        }
                    } catch (FaweException ignore) {
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
