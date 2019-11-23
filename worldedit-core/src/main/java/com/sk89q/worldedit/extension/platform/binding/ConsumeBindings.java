package com.sk89q.worldedit.extension.platform.binding;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Caption;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.image.ImageUtil;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.util.annotation.Confirm;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.internal.annotation.Time;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Identifiable;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.biome.Biomes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BiomeRegistry;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.converter.ConversionResult;
import org.enginehub.piston.exception.StopExecutionException;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;

import java.util.Collection;
import java.util.UUID;

public class ConsumeBindings extends Bindings {
    private final PlatformCommandManager manager;

    public ConsumeBindings(WorldEdit worldEdit, PlatformCommandManager manager) {
        super(worldEdit);
        this.manager = manager;
    }

    @Time
    @Binding
    public Long time(Actor actor, String argument) {
        return MainUtil.timeToSec(argument) * 1000;
    }

    @Binding
    @Confirm(Confirm.Processor.REGION)
    public int regionMultiple(Actor actor, InjectedValueAccess context, @Selection Region region, String argument) {
        int times = (int) Expression.compile(argument).evaluate();
        return Confirm.Processor.REGION.check(actor, context, times);
    }

    @Binding
    @Confirm(Confirm.Processor.RADIUS)
    public Integer radiusInteger(Actor actor, InjectedValueAccess context, String argument) {
        int times = (int) Expression.compile(argument).evaluate();
        return Confirm.Processor.RADIUS.check(actor, context, times);
    }

    @Binding
    @Confirm(Confirm.Processor.LIMIT)
    public Integer limitInteger(Actor actor, InjectedValueAccess context, String argument) {
        int times = (int) Expression.compile(argument).evaluate();
        return Confirm.Processor.LIMIT.check(actor, context, times);
    }

    @Binding
    @Confirm(Confirm.Processor.RADIUS)
    public Double radiusDouble(Actor actor, InjectedValueAccess context, String argument) {
        double times = Expression.compile(argument).evaluate();
        return Confirm.Processor.RADIUS.check(actor, context, times);
    }

    @Binding
    @Confirm(Confirm.Processor.LIMIT)
    public Double limitDouble(Actor actor, InjectedValueAccess context, String argument) {
        double times = Expression.compile(argument).evaluate();
        return Confirm.Processor.LIMIT.check(actor, context, times);
    }

    @Binding
    @Confirm(Confirm.Processor.RADIUS)
    public BlockVector2 radiusVec2(Actor actor, InjectedValueAccess context, String argument) {
        BlockVector2 radius = manager.parseConverter(argument, context, BlockVector2.class);
        double length = radius.length();
        Confirm.Processor.RADIUS.check(actor, context, length);
        return radius;
    }

    @Binding
    @Confirm(Confirm.Processor.RADIUS)
    public BlockVector3 radiusVec3(Actor actor, InjectedValueAccess context, String argument) {
        BlockVector3 radius = manager.parseConverter(argument, context, BlockVector3.class);
        double length = radius.length();
        Confirm.Processor.RADIUS.check(actor, context, length);
        return radius;
    }

    @Binding
    public UUID playerUUID(Actor actor, String argument) {
        if (argument.equals("me")) {
            return actor.getUniqueId();
        }
        if (argument.equals("*")) {
            return Identifiable.EVERYONE;
        }
        if (argument.equalsIgnoreCase("console") || argument.equalsIgnoreCase("server")) {
            return Identifiable.CONSOLE;
        }
        UUID uuid;
        if (argument.length() > 16) {
            uuid = UUID.fromString(argument);
        } else {
            uuid = Fawe.imp().getUUID(argument);
        }
        if (uuid == null) {
            throw new InputParseException(Caption.toString(TranslatableComponent.of("fawe.error.player.not.found" , argument)));
        }
        return uuid;
    }


    @Binding
    public ProvideBindings.ImageUri getImage(String argument) {
        return new ProvideBindings.ImageUri(ImageUtil.getImageURI(argument));
    }

    @Binding
    public BlockType blockType(Actor actor, String argument) {
        return blockState(actor, argument).getBlockType();
    }

    @Binding
    public BlockStateHolder blockStateHolder(Actor actor, String argument) {
        return blockState(actor, argument);
    }

    @Binding
    public BlockState blockState(Actor actor, String argument) {
        return baseBlock(actor, argument).toBlockState();
    }

    @Binding
    public BaseBlock baseBlock(Actor actor, String argument) {
        ParserContext parserContext = new ParserContext();
        parserContext.setActor(actor);
        if (actor instanceof Entity) {
            Extent extent = ((Entity) actor).getExtent();
            if (extent instanceof World) {
                parserContext.setWorld((World) extent);
            }
        }
        parserContext.setSession(getWorldEdit().getSessionManager().get(actor));
        try {
            return getWorldEdit().getBlockFactory().parseFromInput(argument, parserContext);
        } catch (NoMatchException e) {
            throw new InputParseException(e.getMessage());
        }
    }

    /**
     * Gets an {@link TreeType} from a {@link ArgumentStack}.
     *
     * @param context the context
     * @return a TreeType
     * @throws ParameterException on error
     * @throws WorldEditException on error
     */
    @Binding
    public TreeGenerator.TreeType getTreeType(String argument) throws WorldEditException {
        if (argument != null) {
            TreeGenerator.TreeType type = TreeGenerator.lookup(argument);
            if (type != null) {
                return type;
            } else {
                throw new InputParseException(
                        String.format("Can't recognize tree type '%s' -- choose from: %s", argument,
                                TreeGenerator.TreeType.getPrimaryAliases()));
            }
        } else {
            return TreeGenerator.TreeType.TREE;
        }
    }

    /**
     * Gets an {@link BiomeType} from a {@link ArgumentStack}.
     *
     * @param context the context
     * @return a BiomeType
     * @throws ParameterException on error
     * @throws WorldEditException on error
     */
    @Binding
    public BiomeType getBiomeType(String argument) throws WorldEditException {
        if (argument != null) {

            if (MathMan.isInteger(argument)) return BiomeTypes.getLegacy(Integer.parseInt(argument));
            BiomeRegistry biomeRegistry = WorldEdit.getInstance().getPlatformManager()
                    .queryCapability(Capability.GAME_HOOKS).getRegistries().getBiomeRegistry();
            Collection<BiomeType> knownBiomes = BiomeType.REGISTRY.values();
            BiomeType biome = Biomes.findBiomeByName(knownBiomes, argument, biomeRegistry);
            if (biome != null) {
                return biome;
            } else {
                throw new InputParseException(
                        String.format("Can't recognize biome type '%s' -- use /biomelist to list available types", argument));
            }
        } else {
            throw new InputParseException(
                    "This command takes a 'default' biome if one is not set, except there is no particular " +
                            "biome that should be 'default', so the command should not be taking a default biome");
        }
    }
}
