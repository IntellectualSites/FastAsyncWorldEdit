package com.sk89q.worldedit.command.argument;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.World;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.converter.ArgumentConverter;
import org.enginehub.piston.converter.ConversionResult;
import org.enginehub.piston.converter.FailedConversion;
import org.enginehub.piston.converter.SuccessfulConversion;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;

import java.util.Collections;
import java.util.List;

public class LocationConverter implements ArgumentConverter<Location> {

    public static void register(CommandManager commandManager) {
        commandManager.registerConverter(Key.of(Location.class), LOCATION_CONVERTER);
    }

    public static final LocationConverter LOCATION_CONVERTER = new LocationConverter();

    private final WorldConverter worldConverter = WorldConverter.WORLD_CONVERTER;
    private final VectorConverter<Integer, BlockVector3> vectorConverter = VectorConverter.BLOCK_VECTOR_3_CONVERTER;
    private final Component desc = TextComponent.of("any world, x, y, and z");

    private LocationConverter() {
    }

    @Override
    public Component describeAcceptableArguments() {
        return desc;
    }

    @Override
    public List<String> getSuggestions(String input, InjectedValueAccess context) {
        if (input.contains(",")) {
            return Collections.emptyList();
        }
        return worldConverter.getSuggestions(input, context);
    }

    @Override
    public ConversionResult<Location> convert(String s, InjectedValueAccess injectedValueAccess) {
        String[] split4 = s.split(",", 4);
        if (split4.length != 4) {
            return FailedConversion.from(new IllegalArgumentException(
                    "Must have exactly 1 world and 3 vector components"));
        }

        String[] split2 = s.split(",", 2);

        ConversionResult<World> world = worldConverter.convert(split2[0], injectedValueAccess);
        if (!world.isSuccessful()) {
            return world;
        }
        ConversionResult<BlockVector3> vector = vectorConverter.convert(split2[1], injectedValueAccess);
        if (!vector.isSuccessful()) {
            return vector;
        }

        Location location = new Location(world.get().iterator().next(), vector.get().iterator().next().toVector3());
        return SuccessfulConversion.fromSingle(location);
    }

}
