package com.sk89q.worldedit.extension.platform.binding;

import com.boydti.fawe.config.Caption;
import com.boydti.fawe.database.DBHandler;
import com.boydti.fawe.database.RollbackDatabase;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.TextureUtil;
import com.boydti.fawe.util.image.ImageUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.argument.Arguments;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.internal.annotation.AllowedRegion;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
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

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Optional;

public class ProvideBindings extends Bindings {
    public ProvideBindings(WorldEdit worldEdit) {
        super(worldEdit);
    }

    /*
    Provided
     */
    @Binding
    public Player getPlayer(Actor actor) {
        if (actor.isPlayer()) {
            return (Player) actor;
        }
        throw new InputParseException("This command must be used with a player.");
    }

    @Binding
    public LocalSession getLocalSession(Player player) {
        return getWorldEdit().getSessionManager().get(player);
    }

    @Binding
    public EditSession editSession(LocalSession localSession, Player player, InjectedValueAccess context) {
        Arguments arguments = context.injectedValue(Key.of(Arguments.class)).orElse(null);
        String command = arguments == null ? null : arguments.get();
        EditSession editSession = localSession.createEditSession(player, command);
        editSession.enableStandardMode();
        Request.request().setEditSession(editSession);
        return editSession;
    }

    @Selection
    @Binding
    public Region selection(LocalSession localSession, Player player) {
        return localSession.getSelection(player.getWorld());
    }

    @Binding
    public RollbackDatabase database(World world) {
        return DBHandler.INSTANCE.getDatabase(world);
    }

    @AllowedRegion(FaweMaskManager.MaskType.OWNER)
    @Binding
    public Region[] regionsOwner(Player player) {
        return regions(player, FaweMaskManager.MaskType.OWNER);
    }

    @AllowedRegion(FaweMaskManager.MaskType.MEMBER)
    @Binding
    public Region[] regionsMember(Player player) {
        return regions(player, FaweMaskManager.MaskType.MEMBER);
    }

    public Region[] regions(Player player, FaweMaskManager.MaskType type) {
        Region[] regions = player.getCurrentRegions(type);
        if (regions == null) {
            throw new IllegalArgumentException(Caption.toString(TranslatableComponent.of("fawe.error.no.region")));
        }
        return regions;
    }

    @Binding
    public TextureUtil getTexture(LocalSession session) {
        return session.getTextureUtil();
    }

    public static class ImageUri {
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

    @Binding
    public Extent getExtent(Actor actor, InjectedValueAccess access) {
        Optional<EditSession> editSessionOpt = access.injectedValue(Key.of(EditSession.class));
        if (editSessionOpt.isPresent()) {
            return editSessionOpt.get();
        }
        Extent extent = Request.request().getExtent();
        if (extent != null) {
            return extent;
        }
        Player plr = getPlayer(actor);
        EditSession editSession = editSession(getLocalSession(plr), plr, access);
        if (access instanceof InjectedValueStore) {
            InjectedValueStore store = (InjectedValueStore) access;
            store.injectValue(Key.of(EditSession.class), ValueProvider.constant(editSession));
        }
        return editSession;
    }
}
