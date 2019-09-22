package com.boydti.fawe.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.stream.Collectors;

public class ImgurUtility {
    public static final String CLIENT_ID = "50e34b65351eb07";

    public static String getImgurContent(String clientID, byte[] image) throws IOException {
        String imageString = Base64.getEncoder().encodeToString(image);
        URL url = new URL("https://api.imgur.com/3/image");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String data = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(imageString, "UTF-8");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Client-ID " + clientID);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.connect();
        String stb;
        try (OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream())) {
            wr.write(data);
            wr.flush();
            try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                stb = rd.lines().map(line -> line + "\n").collect(Collectors.joining());
            }
        }
        return stb;
    }
}
