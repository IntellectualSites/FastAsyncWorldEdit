package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringUtil;

import java.lang.reflect.Type;
import java.util.Iterator;

public class DataConverterSignText implements DataConverter {

    public static final Gson a = new GsonBuilder().registerTypeAdapter(Component.class, new JsonDeserializer() {
        MutableComponent a(JsonElement jsonelement, Type type, JsonDeserializationContext jsondeserializationcontext) throws
                JsonParseException {
            if (jsonelement.isJsonPrimitive()) {
                return new TextComponent(jsonelement.getAsString());
            } else if (jsonelement.isJsonArray()) {
                JsonArray jsonarray = jsonelement.getAsJsonArray();
                MutableComponent ichatbasecomponent = null;
                Iterator iterator = jsonarray.iterator();

                while (iterator.hasNext()) {
                    JsonElement jsonelement1 = (JsonElement) iterator.next();
                    MutableComponent ichatbasecomponent1 = this.a(
                            jsonelement1,
                            jsonelement1.getClass(),
                            jsondeserializationcontext
                    );

                    if (ichatbasecomponent == null) {
                        ichatbasecomponent = ichatbasecomponent1;
                    } else {
                        ichatbasecomponent.append(ichatbasecomponent1);
                    }
                }

                return ichatbasecomponent;
            } else {
                throw new JsonParseException("Don't know how to turn " + jsonelement + " into a Component");
            }
        }

        public Object deserialize(
                JsonElement jsonelement,
                Type type,
                JsonDeserializationContext jsondeserializationcontext
        ) throws JsonParseException {
            return this.a(jsonelement, type, jsondeserializationcontext);
        }
    }).create();

    public DataConverterSignText() {
    }

    public int getDataVersion() {
        return 101;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if ("Sign".equals(cmp.getString("id"))) {
            this.convert(cmp, "Text1");
            this.convert(cmp, "Text2");
            this.convert(cmp, "Text3");
            this.convert(cmp, "Text4");
        }

        return cmp;
    }

    private void convert(net.minecraft.nbt.CompoundTag nbttagcompound, String s) {
        String s1 = nbttagcompound.getString(s);
        Component object = null;

        if (!"null".equals(s1) && !StringUtil.isNullOrEmpty(s1)) {
            if ((s1.charAt(0) != 34 || s1.charAt(s1.length() - 1) != 34) && (s1.charAt(0) != 123 || s1.charAt(s1.length() - 1) != 125)) {
                object = new TextComponent(s1);
            } else {
                try {
                    object = GsonHelper.fromJson(DataConverterSignText.a, s1, Component.class, true);
                    if (object == null) {
                        object = new TextComponent("");
                    }
                } catch (JsonParseException jsonparseexception) {
                    ;
                }

                if (object == null) {
                    try {
                        object = Component.Serializer.fromJson(s1);
                    } catch (JsonParseException jsonparseexception1) {
                        ;
                    }
                }

                if (object == null) {
                    try {
                        object = Component.Serializer.fromJsonLenient(s1);
                    } catch (JsonParseException jsonparseexception2) {
                        ;
                    }
                }

                if (object == null) {
                    object = new TextComponent(s1);
                }
            }
        } else {
            object = new TextComponent("");
        }

        nbttagcompound.putString(s, Component.Serializer.toJson(object));
    }

}
