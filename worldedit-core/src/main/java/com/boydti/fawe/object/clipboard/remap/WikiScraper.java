package com.boydti.fawe.object.clipboard.remap;


import com.boydti.fawe.util.MainUtil;
import com.google.common.io.Resources;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class WikiScraper {
    public enum Wiki {
        ITEM_MAPPINGS_PE("https://minecraft.gamepedia.com/index.php?title=Bedrock_Edition_data_values&action=edit&section=1"),
        ITEM_MAPPINGS_PC("https://minecraft.gamepedia.com/index.php?title=Java_Edition_data_values/Item_IDs&action=edit"),
        ENTITY_MAPPINGS("https://minecraft.gamepedia.com/index.php?title=Bedrock_Edition_data_values&action=edit&section=4"),
        ;
        public final String url;
        Wiki(String url) {this.url = url;}
    }

    private EnumMap<Wiki, Map<String, Integer>> cache = new EnumMap<>(WikiScraper.Wiki.class);

    public Map<String, Integer> expand(Map<String, Integer> map) {
        HashMap<String, Integer> newMap = new HashMap<>(map);
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            newMap.put(entry.getKey().replace("_", ""), entry.getValue());
        }
        return newMap;
    }

    protected String getName() {
        return "item-mappings";
    }

    public synchronized Map<String, Integer> scapeOrCache(Wiki wiki) throws IOException {
        Map<String, Integer> map;
        try {
            Map<String, Integer> cached = cache.get(wiki);
            if (cached != null) return cached;

            File file = new File("lib" + File.separator + wiki.name().toLowerCase().replace('_', '-') + ".json");
            Gson gson = new Gson();
            if (file.exists()) {
                try {
                    String str = Resources.toString(file.toURL(), Charset.defaultCharset());
                    return gson.fromJson(str, new TypeToken<Map<String, Integer>>() {
                    }.getType());
                } catch (JsonSyntaxException | IOException e) {
                    e.printStackTrace();
                }
            }
            map = scrape(wiki);
            java.io.File parent = file.getParentFile();
            parent.mkdirs();
            file.createNewFile();
            Files.write(file.toPath(), gson.toJson(map).getBytes(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            map = new HashMap<>();
        }
        cache.put(wiki, map);
        return map;
    }

    protected Map<String, Integer> scrape(Wiki wiki) throws IOException {
        String url = wiki.url;
        String text = MainUtil.getText(url);
        HashMap<String, Integer> map = new HashMap<>();

        if (wiki == Wiki.ENTITY_MAPPINGS) {
            String header = "{|";
            String footer = "|}";


            int headerIndex = text.indexOf(header);
            if (headerIndex == -1) return map;
            int endIndex = text.indexOf(footer, headerIndex);
            String part = text.substring(headerIndex, endIndex == -1 ? text.length() : endIndex);

            for (String line : part.split("\n")) {
                if (line.startsWith("| {{")) {
                    String[] split = line.split("\\|\\|");
                    if (split.length == 5) {
                        int id = Integer.parseInt(split[1].trim());
                        String name = split[3].trim();
                        map.put(name, id);
                    }
                }
            }
            return map;
        } else {
            String header = wiki == Wiki.ITEM_MAPPINGS_PE ? "=== Item IDs ===" : "{{";

            int headerIndex = text.indexOf(header);
            if (headerIndex == -1) return map;
            String footer = "{{-}}";
            int endIndex = text.indexOf(footer, headerIndex);
            String part = text.substring(headerIndex, endIndex == -1 ? text.length() : endIndex);

            int id = 255;
            String prefix = "{{id table|";
            for (String line : part.split("\n")) {
                String lower = line.toLowerCase();
                if (lower.startsWith(prefix)) {
                    line = line.substring(prefix.length(), line.indexOf("}}"));
                    String[] split = line.split("\\|");
                    String nameId = null;
                    for (String entry : split) {
                        String[] pair = entry.split("=");
                        switch (pair[0].toLowerCase()) {
                            case "dv":
                                id = Integer.parseInt(pair[1]);
                                break;
                            case "nameid":
                                nameId = pair[1];
                                break;
                        }
                    }
                    if (nameId == null) nameId = split[0].toLowerCase().replace(' ', '_');
                    map.put(nameId, id);
                }
                id++;
            }
            return map;
        }
    }
}
