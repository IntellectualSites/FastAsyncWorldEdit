package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

import com.google.gson.JsonParseException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringUtil;

public class DataConverterBook implements DataConverter {
    public DataConverterBook() {
    }

    public int getDataVersion() {
        return 165;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if ("minecraft:written_book".equals(cmp.getString("id"))) {
            net.minecraft.nbt.CompoundTag nbttagcompound1 = cmp.getCompound("tag");

            if (nbttagcompound1.contains("pages", 9)) {
                net.minecraft.nbt.ListTag nbttaglist = nbttagcompound1.getList("pages", 8);

                for (int i = 0; i < nbttaglist.size(); ++i) {
                    String s = nbttaglist.getString(i);
                    Component object = null;

                    if (!"null".equals(s) && !StringUtil.isNullOrEmpty(s)) {
                        if ((s.charAt(0) != 34 || s.charAt(s.length() - 1) != 34) && (s.charAt(0) != 123 || s.charAt(s.length() - 1) != 125)) {
                            object = new TextComponent(s);
                        } else {
                            try {
                                object = GsonHelper.fromJson(DataConverterSignText.a, s, Component.class, true);
                                if (object == null) {
                                    object = new TextComponent("");
                                }
                            } catch (JsonParseException jsonparseexception) {
                                ;
                            }

                            if (object == null) {
                                try {
                                    object = Component.Serializer.fromJson(s);
                                } catch (JsonParseException jsonparseexception1) {
                                    ;
                                }
                            }

                            if (object == null) {
                                try {
                                    object = Component.Serializer.fromJsonLenient(s);
                                } catch (JsonParseException jsonparseexception2) {
                                    ;
                                }
                            }

                            if (object == null) {
                                object = new TextComponent(s);
                            }
                        }
                    } else {
                        object = new TextComponent("");
                    }

                    nbttaglist.set(i, net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(object)));
                }

                nbttagcompound1.put("pages", nbttaglist);
            }
        }

        return cmp;
    }
}
