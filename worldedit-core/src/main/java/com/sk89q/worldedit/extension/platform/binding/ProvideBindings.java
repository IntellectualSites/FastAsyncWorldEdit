package com.sk89q.worldedit.extension.platform.binding;

import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TextureUtil;
import com.boydti.fawe.util.image.ImageUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
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
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.biome.Biomes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BiomeRegistry;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.InjectedValueStore;
import org.enginehub.piston.inject.Key;
import org.enginehub.piston.util.ValueProvider;

public class ProvideBindings extends Bindings {
    private final WorldEdit worldEdit;

    public ProvideBindings(WorldEdit worldEdit) {
        this.worldEdit = worldEdit;
    }

    /*
    Provided
     */

    public Player getPlayer(Actor actor) {
        if (actor.isPlayer()) {
            return (Player) actor;
        }
        throw new InputParseException("This command must be used with a player.");
    }

    public LocalSession getLocalSession(Player player) {
        return worldEdit.getSessionManager().get(player);
    }

    public EditSession editSession(LocalSession localSession, Player player) {
        EditSession editSession = localSession.createEditSession(player);
        editSession.enableStandardMode();
        return editSession;
    }

    @Selection
    public Region selection(LocalSession localSession, Player player) {
        return localSession.getSelection(player.getWorld());
    }

    public TextureUtil getTexture(LocalSession session) {
        return session.getTextureUtil();
    }

    public class ImageUri {
        public final URI uri;
        private BufferedImage image;

        ImageUri(URI uri) {
            this.uri = uri;
        }

        public BufferedImage load() {
            if (image != null) {
                return image;
            }
            return image = ImageUtil.load(uri);
        }
    }

    public Extent getExtent(Actor actor, InjectedValueAccess access, InjectedValueStore store) {
        Optional<EditSession> editSessionOpt = access.injectedValue(Key.of(EditSession.class));
        if (editSessionOpt.isPresent()) {
            return editSessionOpt.get();
        }
        Extent extent = Request.request().getExtent();
        if (extent != null) {
            return extent;
        }
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        Player plr = getPlayer(actor);
        EditSession editSession = editSession(getLocalSession(plr), plr);
        store.injectValue(Key.of(EditSession.class), ValueProvider.constant(editSession));
        return editSession;
    }

    /*
    Parsed
     */
    public ImageUri getImage(String argument) {
        return new ImageUri(ImageUtil.getImageURI(argument));
    }

    public BlockType blockType(Actor actor, String argument) {
        return blockState(actor, argument).getBlockType();
    }

    public BlockState blockState(Actor actor, String argument) {
        return baseBlock(actor, argument).toBlockState();
    }

    public BaseBlock baseBlock(Actor actor, String argument) {
        ParserContext parserContext = new ParserContext();
        parserContext.setActor(actor);
        if (actor instanceof Entity) {
            Extent extent = ((Entity) actor).getExtent();
            if (extent instanceof World) {
                parserContext.setWorld((World) extent);
            }
        }
        parserContext.setSession(worldEdit.getSessionManager().get(actor));
        try {
            return worldEdit.getBlockFactory().parseFromInput(argument, parserContext);
        } catch (NoMatchException e) {
            throw new InputParseException(e.getMessage());
        }
    }

    /**
     * Get a direction from the player.
     *
     * @param context the context
     * @param direction the direction annotation
     * @return a BlockVector3
     * @throws ParameterException on error
     * @throws UnknownDirectionException on an unknown direction
     */
    @Direction
    public BlockVector3 getDirection(Player player, Direction direction, String argument) throws UnknownDirectionException {
        if (direction.includeDiagonals()) {
            return worldEdit.getDiagonalDirection(player, argument);
        } else {
            return worldEdit.getDirection(player, argument);
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
