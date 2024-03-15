package com.sk89q.worldedit.util.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;

import java.lang.reflect.Type;

public final class ItemTypeAdapter implements JsonDeserializer<ItemType> {

    @Override
    public ItemType deserialize(JsonElement json, Type type, JsonDeserializationContext cont) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String id = jsonObject.get("id").getAsString();
        ItemType itemType = ItemTypes.get(id);
        if (itemType == null) {
            throw new JsonParseException("Could not parse item type `" + id + "`");
        }
        return itemType;
    }

}
