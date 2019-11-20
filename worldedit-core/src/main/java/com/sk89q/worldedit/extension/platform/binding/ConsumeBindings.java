package com.sk89q.worldedit.extension.platform.binding;

import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.image.ImageUtil;
import com.sk89q.worldedit.UnknownDirectionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.annotation.Direction;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.TreeGenerator;
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
import org.enginehub.piston.inject.InjectedValueStore;
import org.enginehub.piston.inject.Key;

import java.util.Collection;

public class ConsumeBindings extends Bindings {
    public ConsumeBindings(WorldEdit worldEdit) {
        super(worldEdit);
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
