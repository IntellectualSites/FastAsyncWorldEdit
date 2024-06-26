package com.fastasyncworldedit.core.util.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import org.enginehub.linbus.format.snbt.LinStringIO;
import org.enginehub.linbus.tree.LinCompoundTag;

import java.io.IOException;
import java.lang.reflect.Type;

public final class BaseItemAdapter implements JsonDeserializer<BaseItem>, JsonSerializer<BaseItem> {

    @Override
    public BaseItem deserialize(JsonElement json, Type type, JsonDeserializationContext cont) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        JsonElement id = jsonObject.get("id");
        if (id != null) {
            ItemType itemType = ItemTypes.get(id.getAsString());
            if (itemType == null) {
                throw new JsonParseException("Could not parse item type `" + id + "`");
            }
            return new BaseItem(itemType);
        }
        ItemType itemType = cont.deserialize(jsonObject.get("itemType").getAsJsonObject(), ItemType.class);
        JsonElement nbt = jsonObject.get("nbt");
        if (nbt == null) {
            return new BaseItem(itemType);
        }
        try {
            return new BaseItem(
                    itemType,
                    LazyReference.computed(LinCompoundTag.readFrom(LinStringIO.readFromString(nbt.getAsString())))
            );
        } catch (IOException e) {
            throw new JsonParseException("Could not deserialize BaseItem", e);
        }
    }

    @Override
    public JsonElement serialize(
            final BaseItem baseItem,
            final Type type,
            final JsonSerializationContext jsonSerializationContext
    ) {
        JsonObject obj = new JsonObject();
        obj.add("itemType", jsonSerializationContext.serialize(baseItem.getType()));
        obj.add("nbt", baseItem.getNbt() == null ? null : new JsonPrimitive(LinStringIO.writeToString(baseItem.getNbt())));
        return obj;
    }

}
