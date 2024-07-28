package com.fastasyncworldedit.core.extension.platform.binding;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.database.DBHandler;
import com.fastasyncworldedit.core.database.RollbackDatabase;
import com.fastasyncworldedit.core.extent.LimitExtent;
import com.fastasyncworldedit.core.extent.processor.ExtentBatchProcessorHolder;
import com.fastasyncworldedit.core.internal.exception.FaweException;
import com.fastasyncworldedit.core.regions.FaweMaskManager;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.fastasyncworldedit.core.util.TextureUtil;
import com.fastasyncworldedit.core.util.image.ImageUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.argument.Arguments;
import com.sk89q.worldedit.command.util.annotation.AllowedRegion;
import com.sk89q.worldedit.command.util.annotation.SynchronousSettingExpected;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.world.World;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.InjectedValueStore;
import org.enginehub.piston.inject.Key;
import org.enginehub.piston.util.ValueProvider;

import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
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
        throw new InputParseException(Caption.of("worldedit.command.player-only"));
    }

    @Binding
    public LocalSession getLocalSession(Actor actor) {
        return getWorldEdit().getSessionManager().get(actor);
    }

    @Binding
    public EditSession editSession(LocalSession localSession, Actor actor, InjectedValueAccess context) {
        Method commandMethod =
                context.injectedValue(Key.of(InjectedValueStore.class)).get().injectedValue(Key.of(Method.class)).get();

        Arguments arguments = context.injectedValue(Key.of(Arguments.class)).orElse(null);
        String command = arguments == null ? null : arguments.get();
        boolean synchronousSetting = commandMethod.getAnnotation(SynchronousSettingExpected.class) != null;
        EditSessionHolder holder = context.injectedValue(Key.of(EditSessionHolder.class)).orElse(null);
        EditSession editSession = holder != null ? holder.session() : null;
        if (editSession == null) {
            editSession = localSession.createEditSession(actor, command, synchronousSetting);
            editSession.enableStandardMode();
        } else {
            LimitExtent limitExtent = new ExtentTraverser<>(editSession).findAndGet(LimitExtent.class);
            if (limitExtent != null) {
                limitExtent.setProcessing(!synchronousSetting);
                if (!synchronousSetting) {
                    ExtentBatchProcessorHolder processorHolder = new ExtentTraverser<>(editSession).findAndGet(
                            ExtentBatchProcessorHolder.class);
                    if (processorHolder != null) {
                        processorHolder.addProcessor(limitExtent);
                    } else {
                        throw new FaweException(Caption.of("fawe.error.no-process-non-synchronous-edit"));
                    }
                }
            }
            Request.request().setEditSession(editSession);
        }
        return editSession;
    }

    @Selection
    @Binding
    public Region selection(LocalSession localSession) {
        return localSession.getSelection();
    }

    @Binding
    public RollbackDatabase database(World world) {
        return DBHandler.dbHandler().getDatabase(world);
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

    private Region[] regions(Player player, FaweMaskManager.MaskType type) {
        Region[] regions = player.getAllowedRegions(type);
        if (regions == null) {
            throw new IllegalArgumentException(Caption.toString(Caption.of("fawe.error.no.region")));
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
        EditSession editSession = editSession(getLocalSession(actor), actor, access);
        if (access instanceof InjectedValueStore store) {
            store.injectValue(Key.of(EditSession.class), ValueProvider.constant(editSession));
        }
        return editSession;
    }

}
