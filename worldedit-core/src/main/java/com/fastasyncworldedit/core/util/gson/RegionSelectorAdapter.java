package com.fastasyncworldedit.core.util.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.RegionSelectorType;

import java.lang.reflect.Type;

public class RegionSelectorAdapter implements JsonDeserializer<RegionSelector>, JsonSerializer<RegionSelector> {

    @Override
    public RegionSelector deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        RegionSelectorType regionType = RegionSelectorType.valueOf(json.getAsString());
        return regionType.createSelector();
    }

    @Override
    public JsonElement serialize(RegionSelector selector, Type type, JsonSerializationContext context) {
        RegionSelectorType regionType = RegionSelectorType.getForSelector(selector);
        // Cannot nicely deserialize Fuzzy region type
        if (regionType == null || regionType == RegionSelectorType.FUZZY) {
            return null;
        }
        return new JsonPrimitive(regionType.toString());
    }

}
