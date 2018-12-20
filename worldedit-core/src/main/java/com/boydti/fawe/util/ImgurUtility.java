package com.boydti.fawe.util;

import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;

public class ImgurUtility {
    public static final String CLIENT_ID = "50e34b65351eb07";

    public static URL uploadImage(File file) throws IOException {
        return uploadImage(new FileInputStream(file));
    }

    public static URL uploadImage(InputStream is) throws IOException {
        is = new BufferedInputStream(is);
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream(Short.MAX_VALUE);
        int d;
        while ((d = is.read()) != -1) {
            baos.write(d);
        }
        baos.flush();
        return uploadImage(baos.toByteArray());
    }

    public static URL uploadImage(byte[] image) throws IOException {
        String json = getImgurContent(CLIENT_ID, image);
        Gson gson = new Gson();
        JsonObject obj = gson.fromJson(json, JsonObject.class);
        JsonObject data = obj.get("data").getAsJsonObject();
        String link = data.get("link").getAsString();
        return new URL(link);
    }

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
        StringBuilder stb = new StringBuilder();
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            stb.append(line).append("\n");
        }
        wr.close();
        rd.close();
        return stb.toString();
    }
}
