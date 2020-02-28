/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.command;

import com.boydti.fawe.config.Caption;
import com.google.common.collect.Lists;

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.clipboard.MultiClipboardHolder;
import com.boydti.fawe.object.clipboard.ReadOnlyClipboard;
import com.boydti.fawe.object.clipboard.URIClipboardHolder;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.util.ImgurUtility;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MaskTraverser;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.command.util.Logging;
import com.sk89q.worldedit.command.util.annotation.Confirm;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.extent.PasteEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.block.BlockReplace;

import static com.sk89q.worldedit.command.util.Logging.LogMode.PLACEMENT;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.annotation.ClipboardMask;
import com.sk89q.worldedit.internal.annotation.Direction;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static com.sk89q.worldedit.command.util.Logging.LogMode.REGION;

import java.util.List;


/**
 * Clipboard commands.
 */
@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class ClipboardCommands {


    @Command(
        name = "/copy",
        aliases = "/cp",
        desc = "Copy the selection to the clipboard"
    )
    @CommandPermissions("worldedit.clipboard.copy")
    @Confirm(Confirm.Processor.REGION)
    public void copy(Actor actor, LocalSession session, EditSession editSession,
                     @Selection Region region,
                     @Switch(name = 'e', desc = "Also copy entities")
                         boolean copyEntities,
                     @Switch(name = 'b', desc = "Also copy biomes")
                             boolean copyBiomes,
                     @ArgFlag(name = 'm', desc = "Set the include mask, non-matching blocks become air", def = "")
                        Mask mask) throws WorldEditException {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        long volume =
            ((long) max.getX() - (long) min.getX() + 1) * ((long) max.getY() - (long) min.getY() + 1) * ((long) max.getZ() - (long) min.getZ() + 1);
        FaweLimit limit = actor.getLimit();
        if (volume >= limit.MAX_CHECKS) {
            throw FaweCache.MAX_CHECKS;
        }
        session.setClipboard(null);

        Clipboard clipboard = new BlockArrayClipboard(region, actor.getUniqueId());
        
        clipboard.setOrigin(session.getPlacementPosition(actor));
        ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
        copy.setCopyingEntities(copyEntities);
        copy.setCopyingBiomes(copyBiomes);

        Mask sourceMask = editSession.getSourceMask();
        if (sourceMask != null) {
            new MaskTraverser(sourceMask).reset(editSession);
            copy.setSourceMask(sourceMask);
            editSession.setSourceMask(null);
        }
        if (mask != null) {
            copy.setSourceMask(mask);
        }
        Operations.completeLegacy(copy);
        session.setClipboard(new ClipboardHolder(clipboard));
        copy.getStatusMessages().forEach(actor::print);
    }

    @Command(
            name = "/lazycopy",
            desc = "Lazily copy the selection to the clipboard"
    )
    @CommandPermissions("worldedit.clipboard.lazycopy")
    public void lazyCopy(Actor actor, LocalSession session, EditSession editSession,
                         @Selection Region region,
                         @Switch(name = 'e', desc = "Skip copy entities")
                                 boolean skipEntities,
                         @Switch(name = 'b', desc = "Also copy biomes")
                                 boolean copyBiomes) throws WorldEditException {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        long volume = (((long) max.getX() - (long) min.getX() + 1) * ((long) max.getY() - (long) min.getY() + 1) * ((long) max.getZ() - (long) min.getZ() + 1));
        FaweLimit limit = actor.getLimit();
        if (volume >= limit.MAX_CHECKS) {
            throw new FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.max.checks"));
        }
        session.setClipboard(null);
        ReadOnlyClipboard lazyClipboard = ReadOnlyClipboard.of(region, !skipEntities, copyBiomes);

        lazyClipboard.setOrigin(session.getPlacementPosition(actor));
        session.setClipboard(new ClipboardHolder(lazyClipboard));
        actor.print(Caption.of("fawe.worldedit.copy.command.copy" , region.getArea()));
    }

//    @Command(
//            name = "/lazycut",
//            desc = "Lazily cut the selection to the clipboard"
//    )
//    @CommandPermissions("worldedit.clipboard.lazycut")
//    public void lazyCut(Actor actor, LocalSession session, EditSession editSession,
//                        @Selection final Region region,
//                        @Switch(name = 'e', desc = "Skip copy entities")
//                                boolean skipEntities,
//                        @ArgFlag(name = 'm', desc = "Set the exclude mask, matching blocks become air", def = "")
//                                Mask maskOpt,
//                        @Switch(name = 'b', desc = "Also copy biomes")
//                                boolean copyBiomes) throws WorldEditException {
//        BlockVector3 min = region.getMinimumPoint();
//        BlockVector3 max = region.getMaximumPoint();
//        long volume = ((long) max.getX() - (long) min.getX() + 1) * ((long) max.getY() - (long) min.getY() + 1) * ((long) max.getZ() - (long) min.getZ() + 1);
//        FaweLimit limit = actor.getLimit();
//        if (volume >= limit.MAX_CHECKS) {
//            throw FaweCache.MAX_CHECKS;
//        }
//        if (volume >= limit.MAX_CHANGES) {
//            throw FaweCache.MAX_CHANGES;
//        }
//        session.setClipboard(null);
//
//        ReadOnlyClipboard lazyClipboard = new WorldCutClipboard(editSession, region, !skipEntities, copyBiomes);
//        clipboard.setOrigin(session.getPlacementPosition(actor));
//        session.setClipboard(new ClipboardHolder(lazyClipboard));
//        actor.print(TranslatableComponent.of("fawe.worldedit.cut.command.cut.lazy" , region.getArea()));
//    }

    @Command(
        name = "/cut",
        desc = "Cut the selection to the clipboard",
        descFooter = "WARNING: Cutting and pasting entities cannot be undone!"

    )
    @CommandPermissions("worldedit.clipboard.cut")
    @Logging(REGION)
    @Confirm(Confirm.Processor.REGION)
    public void cut(Actor actor, LocalSession session, EditSession editSession,
                    @Selection Region region,
                    @Arg(desc = "Pattern to leave in place of the selection", def = "air")
                        Pattern leavePattern,
                    @Switch(name = 'e', desc = "Also cut entities")
                        boolean copyEntities,
                    @Switch(name = 'b', desc = "Also copy biomes, source biomes are unaffected")
                        boolean copyBiomes,
                    @ArgFlag(name = 'm', desc = "Set the exclude mask, non-matching blocks become air", def = "")
                        Mask mask) throws WorldEditException {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        long volume = (((long) max.getX() - (long) min.getX() + 1) * ((long) max.getY() - (long) min.getY() + 1) * ((long) max.getZ() - (long) min.getZ() + 1));
        FaweLimit limit = actor.getLimit();
        if (volume >= limit.MAX_CHECKS) {
            throw FaweCache.MAX_CHECKS;
        }
        if (volume >= limit.MAX_CHANGES) {
            throw FaweCache.MAX_CHANGES;
        }
        session.setClipboard(null);

        BlockArrayClipboard clipboard = new BlockArrayClipboard(region, actor.getUniqueId());
        clipboard.setOrigin(session.getPlacementPosition(actor));

        ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
        copy.setSourceFunction(new BlockReplace(editSession, leavePattern));
        copy.setCopyingEntities(copyEntities);
        copy.setRemovingEntities(true);
        copy.setCopyingBiomes(copyBiomes);
        Mask sourceMask = editSession.getSourceMask();
        if (sourceMask != null) {
            new MaskTraverser(sourceMask).reset(editSession);
            copy.setSourceMask(sourceMask);
            editSession.setSourceMask(null);
        }
        if (mask != null) {
            copy.setSourceMask(mask);
        }
        Operations.completeLegacy(copy);
        session.setClipboard(new ClipboardHolder(clipboard));

        if (!actor.hasPermission("fawe.tips")) {
            actor.print(TranslatableComponent.of("fawe.tips.tip.lazycut"));
        }
		copy.getStatusMessages().forEach(actor::print);
    }

    @Command(
            name = "download",
            desc = "Downloads your clipboard through the configured web interface"
    )
    @Deprecated
    @CommandPermissions({"worldedit.clipboard.download"})
    public void download(final Player player, final LocalSession session, @Arg(name = "format", desc = "String", def = "schem") final String formatName) throws WorldEditException {
        final ClipboardFormat format = ClipboardFormats.findByAlias(formatName);
        if (format == null) {
            player.print(Caption.of("fawe.worldedit.clipboard.clipboard.invalid.format" , formatName));
            return;
        }

        player.print(Caption.of("fawe.web.generating.link" , formatName));
        ClipboardHolder holder = session.getClipboard();

        URL url;
        if (holder instanceof MultiClipboardHolder) {
            MultiClipboardHolder multi = (MultiClipboardHolder) holder;
            Set<File> files = new HashSet<>();
            Set<URI> invalid = new HashSet<>();
            for (ClipboardHolder cur : multi.getHolders()) {
                if (cur instanceof URIClipboardHolder) {
                    URIClipboardHolder uriHolder = (URIClipboardHolder) cur;
                    URI uri = uriHolder.getUri();
                    File file = new File(uri.getPath());
                    if (file.exists() && file.isFile()) {
                        files.add(file.getAbsoluteFile());
                    } else if (!uri.getPath().isEmpty()) {
                        invalid.add(uri);
                    }
                }
            }

            final LocalConfiguration config = WorldEdit.getInstance().getConfiguration();
            final File working = WorldEdit.getInstance().getWorkingDirectoryFile(config.saveDir).getAbsoluteFile();

            url = MainUtil.upload(null, null, "zip", new RunnableVal<OutputStream>() {
                @Override
                public void run(OutputStream out) {
                    try (ZipOutputStream zos = new ZipOutputStream(out)) {
                        for (File file : files) {
                            String fileName = file.getName();
                            if (MainUtil.isInSubDirectory(working, file)) {
                                fileName = working.toURI().relativize(file.toURI()).getPath();
                            }
                            ZipEntry ze = new ZipEntry(fileName);
                            zos.putNextEntry(ze);
                            Files.copy(file.toPath(), zos);
                            zos.closeEntry();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } else {
            Clipboard clipboard = holder.getClipboard();
            final Transform transform = holder.getTransform();
            final Clipboard target;
            // If we have a transform, bake it into the copy
            if (!transform.isIdentity()) {
                final FlattenedClipboardTransform result = FlattenedClipboardTransform.transform(clipboard, transform);
                target = new BlockArrayClipboard(result.getTransformedRegion(), player.getUniqueId());
                target.setOrigin(clipboard.getOrigin());
                Operations.completeLegacy(result.copyTo(target));
            } else {
                target = clipboard;
            }
            if (format == BuiltInClipboardFormat.PNG) {
                try {
                    FastByteArrayOutputStream baos = new FastByteArrayOutputStream(Short.MAX_VALUE);
                    ClipboardWriter writer = format.getWriter(baos);
                    writer.write(target);
                    baos.flush();
                    url = ImgurUtility.uploadImage(baos.toByteArray());
                } catch (IOException e) {
                    e.printStackTrace();
                    url = null;
                }
            } else {
                if (Settings.IMP.WEB.URL.isEmpty()) {
                    player.print(Caption.of("fawe.error.setting.disable", "web.url"));
                    return;
                }
                url = FaweAPI.upload(target, format);
            }
        }
        if (url == null) {
            player.printError(TranslatableComponent.of("fawe.web.generating.link.failed"));
        } else {
            String urlText = url.toString();
            if (Settings.IMP.WEB.SHORTEN_URLS) {
                try {
                    urlText = MainUtil.getText("https://athion.net/s/?" + URLEncoder.encode(url.toString(), "UTF-8"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            player.print(Caption.of("fawe.web.download.link" , urlText));
        }
    }

    @Command(
            name = "asset",
            desc = "Saves your clipboard to the asset web interface"
)
    @CommandPermissions({"worldedit.clipboard.asset"})
    public void asset(final Player player, final LocalSession session, String category) throws WorldEditException {
        final ClipboardFormat format = BuiltInClipboardFormat.MCEDIT_SCHEMATIC;
        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();
        final Transform transform = holder.getTransform();
        final Clipboard target;
        // If we have a transform, bake it into the copy
        if (!transform.isIdentity()) {
            final FlattenedClipboardTransform result = FlattenedClipboardTransform.transform(clipboard, transform);
            target = new BlockArrayClipboard(result.getTransformedRegion(), player.getUniqueId());
            target.setOrigin(clipboard.getOrigin());
            Operations.completeLegacy(result.copyTo(target));
        } else {
            target = clipboard;
        }
        player.print(Caption.of("fawe.web.generating.link" , format.getName()));
        if (Settings.IMP.WEB.ASSETS.isEmpty()) {
            player.print(Caption.of("fawe.error.setting.disable", "web.assets"));
            return;
        }
        URL url = format.uploadPublic(target, category.replaceAll("[/|\\\\]", "."), player.getName());
        if (url == null) {
            player.printError(TranslatableComponent.of("fawe.web.generating.link.failed"));
        } else {
            player.print(Caption.of("fawe.web.download.link" , Settings.IMP.WEB.ASSETS));
        }
    }

    @Command(
        name = "/paste",
        aliases = { "/p", "/pa" },
        desc = "Paste the clipboard's contents"

    )
    @CommandPermissions("worldedit.clipboard.paste")
    @Logging(PLACEMENT)
    public void paste(Actor actor, World world, LocalSession session, EditSession editSession,
                      @Switch(name = 'a', desc = "Skip air blocks")
                          boolean ignoreAirBlocks,
                      @Switch(name = 'o', desc = "Paste at the original position")
                          boolean atOrigin,
                      @Switch(name = 's', desc = "Select the region after pasting")
                          boolean selectPasted,
                      @Switch(name = 'n', desc = "No paste, select only. (Implies -s)")
                          boolean onlySelect,
                      @Switch(name = 'e', desc = "Paste entities if available")
                          boolean pasteEntities,
                      @Switch(name = 'b', desc = "Paste biomes if available")
                          boolean pasteBiomes,
                      @ArgFlag(name = 'm', desc = "Only paste blocks matching this mask", def = "")
                      @ClipboardMask
                          Mask sourceMask) throws WorldEditException {

        ClipboardHolder holder = session.getClipboard();
        if (holder.getTransform().isIdentity() && editSession.getSourceMask() == null) {
            place(actor, world, session, editSession, ignoreAirBlocks, atOrigin, selectPasted, pasteEntities, pasteBiomes);
            return;
        }
        Clipboard clipboard = holder.getClipboard();
        Region region = clipboard.getRegion();
        List<Component> messages = Lists.newArrayList();

        BlockVector3 to = atOrigin ? clipboard.getOrigin() : session.getPlacementPosition(actor);
        checkPaste(actor, editSession, to, holder, clipboard);

        if (!onlySelect) {
            Operation operation = holder
                    .createPaste(editSession)
                    .to(to)
                    .ignoreAirBlocks(ignoreAirBlocks)
                    .copyBiomes(pasteBiomes)
                    .copyEntities(pasteEntities)
                    .maskSource(sourceMask)
                    .build();
            Operations.completeLegacy(operation);
			messages.addAll(Lists.newArrayList(operation.getStatusMessages()));
        }

        if (selectPasted || onlySelect) {
            BlockVector3 clipboardOffset = clipboard.getRegion().getMinimumPoint().subtract(clipboard.getOrigin());
            Vector3 realTo = to.toVector3().add(holder.getTransform().apply(clipboardOffset.toVector3()));
            Vector3 max = realTo.add(holder.getTransform().apply(region.getMaximumPoint().subtract(region.getMinimumPoint()).toVector3()));
            RegionSelector selector = new CuboidRegionSelector(world, realTo.toBlockPoint(), max.toBlockPoint());
            session.setRegionSelector(world, selector);
            selector.learnChanges();
            selector.explainRegionAdjust(actor, session);
        }
		if (onlySelect) {
            actor.printInfo(TranslatableComponent.of("worldedit.paste.selected"));
        } else {
            actor.printInfo(TranslatableComponent.of("worldedit.paste.pasted", TextComponent.of(to.toString())));
        }
		messages.forEach(actor::print);
    }

    private void checkPaste(Actor player, EditSession editSession, BlockVector3 to, ClipboardHolder holder, Clipboard clipboard) {
        URI uri = null;
        if (holder instanceof URIClipboardHolder) {
            uri = ((URIClipboardHolder) holder).getURI(clipboard);
        }
        PasteEvent event = new PasteEvent(player, clipboard, uri, editSession, to);
        WorldEdit.getInstance().getEventBus().post(event);
        if (event.isCancelled()) {
            throw new FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.manual"));
        }
    }

    @Command(
            name = "/place",
            desc = "Place the clipboard's contents without applying transformations (e.g. rotate)"
)

    @CommandPermissions("worldedit.clipboard.place")
    @Logging(PLACEMENT)
    public void place(Actor actor, World world, LocalSession session, final EditSession editSession,
                      @Switch(name = 'a', desc = "Skip air blocks")
                            boolean ignoreAirBlocks,
                      @Switch(name = 'o', desc = "Paste at the original position")
                            boolean atOrigin,
                      @Switch(name = 's', desc = "Select the region after pasting")
                            boolean selectPasted,
                      @Switch(name = 'e', desc = "Paste entities if available")
                            boolean pasteEntities,
                      @Switch(name = 'b', desc = "Paste biomes if available")
                            boolean pasteBiomes) throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();
        final Clipboard clipboard = holder.getClipboard();
        final BlockVector3 origin = clipboard.getOrigin();
        final BlockVector3 to = atOrigin ? origin : session.getPlacementPosition(actor);
        checkPaste(actor, editSession, to, holder, clipboard);

        clipboard.paste(editSession, to, !ignoreAirBlocks, pasteEntities, pasteBiomes);

        Region region = clipboard.getRegion().clone();
        if (selectPasted) {
            BlockVector3 clipboardOffset = clipboard.getRegion().getMinimumPoint().subtract(clipboard.getOrigin());
            BlockVector3 realTo = to.add(holder.getTransform().apply(clipboardOffset.toVector3()).toBlockPoint());
            BlockVector3 max = realTo.add(holder.getTransform().apply(region.getMaximumPoint().subtract(region.getMinimumPoint()).toVector3()).toBlockPoint());
            RegionSelector selector = new CuboidRegionSelector(world, realTo, max);
            session.setRegionSelector(world, selector);
            selector.learnChanges();
            selector.explainRegionAdjust(actor, session);
        }
        actor.print(Caption.of("fawe.worldedit.paste.command.paste" , to));

        if (!actor.hasPermission("fawe.tips")) {
            actor.print(TranslatableComponent.of("fawe.tips.tip.copypaste"));
        }
    }

    @Command(
        name = "/rotate",
        desc = "Rotate the contents of the clipboard",
        descFooter = "Non-destructively rotate the contents of the clipboard.\n" +
            "Angles are provided in degrees and a positive angle will result in a clockwise rotation. " +
            "Multiple rotations can be stacked. Interpolation is not performed so angles should be a multiple of 90 degrees.\n"
    )
    @CommandPermissions("worldedit.clipboard.rotate")
    public void rotate(Actor actor, LocalSession session,
                       @Arg(desc = "Amount to rotate on the y-axis")
                           double yRotate,
                       @Arg(desc = "Amount to rotate on the x-axis", def = "0")
                           double xRotate,
                       @Arg(desc = "Amount to rotate on the z-axis", def = "0")
                           double zRotate) throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();
        AffineTransform transform = new AffineTransform();
        transform = transform.rotateY(-yRotate);
        transform = transform.rotateX(-xRotate);
        transform = transform.rotateZ(-zRotate);
        holder.setTransform(holder.getTransform().combine(transform));
        actor.printInfo(TranslatableComponent.of("worldedit.rotate.rotated"));
    }

    @Command(
        name = "/flip",
        desc = "Flip the contents of the clipboard across the origin"
    )
    @CommandPermissions("worldedit.clipboard.flip")
    public void flip(Actor actor, LocalSession session,
                     @Arg(desc = "The direction to flip, defaults to look direction.", def = Direction.AIM)
                     @Direction BlockVector3 direction) throws WorldEditException {

        ClipboardHolder holder = session.getClipboard();
        AffineTransform transform = new AffineTransform();
        transform = transform.scale(direction.abs().multiply(-2).add(1, 1, 1).toVector3());
        holder.setTransform(holder.getTransform().combine(transform));
        actor.printInfo(TranslatableComponent.of("worldedit.flip.flipped"));
    }

    @Command(
        name = "clearclipboard",
        desc = "Clear your clipboard"
    )
    @CommandPermissions("worldedit.clipboard.clear")
    public void clearClipboard(Actor actor, LocalSession session) throws WorldEditException {
        session.setClipboard(null);
        actor.printInfo(TranslatableComponent.of("worldedit.clearclipboard.cleared"));
    }
}
