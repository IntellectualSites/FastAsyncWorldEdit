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

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.util.CachedTextureUtil;
import com.boydti.fawe.util.CleanTextureUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.RandomTextureUtil;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.command.util.WorldEditAsyncCommandBuilder;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.DisallowedUsageException;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.util.formatting.component.PaginationBox;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.item.ItemType;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;

/**
 * General WorldEdit commands.
 */
@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class GeneralCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public GeneralCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
        name = "/limit",
        desc = "Modify block change limit"
    )
    @CommandPermissions("worldedit.limit")
    public void limit(Actor actor, LocalSession session,
                      @Arg(desc = "The limit to set", def = "")
                          Integer limit) {

        LocalConfiguration config = worldEdit.getConfiguration();
        boolean mayDisable = actor.hasPermission("worldedit.limit.unrestricted");

        limit = limit == null ? config.defaultChangeLimit : Math.max(-1, limit);
        if (!mayDisable && config.maxChangeLimit > -1) {
            if (limit > config.maxChangeLimit) {
                actor.printError("Your maximum allowable limit is " + config.maxChangeLimit + ".");
                return;
            }
        }

        session.setBlockChangeLimit(limit);
        actor.print("Block change limit set to " + limit + "."
                + (limit == config.defaultChangeLimit ? "" : " (Use //limit to go back to the default.)"));
    }

    @Command(
        name = "/timeout",
        desc = "Modify evaluation timeout time."
    )
    @CommandPermissions("worldedit.timeout")
    public void timeout(Actor actor, LocalSession session,
                        @Arg(desc = "The timeout time to set", def = "")
                            Integer limit) {
        LocalConfiguration config = worldEdit.getConfiguration();
        boolean mayDisable = actor.hasPermission("worldedit.timeout.unrestricted");

        limit = limit == null ? config.calculationTimeout : Math.max(-1, limit);
        if (!mayDisable && config.maxCalculationTimeout > -1) {
            if (limit > config.maxCalculationTimeout) {
                actor.printError("Your maximum allowable timeout is " + config.maxCalculationTimeout + " ms.");
                return;
            }
        }

        session.setTimeout(limit);
        actor.print("Timeout time set to " + limit + " ms."
                + (limit == config.calculationTimeout ? "" : " (Use //timeout to go back to the default.)"));
    }

    @Command(
            name = "/fast",
            desc = "Toggle fast mode"
    )
    @CommandPermissions("worldedit.fast")
    public void fast(Actor actor, LocalSession session,
                     @Arg(desc = "The new fast mode state", def = "")
                        Boolean fastMode) {
        boolean hasFastMode = session.hasFastMode();
        if (fastMode != null && fastMode == hasFastMode) {
            actor.printError("Fast mode already " + (fastMode ? "enabled" : "disabled") + ".");
            return;
        }
        if (hasFastMode) {
            session.setFastMode(false);
            actor.print(BBC.FAST_DISABLED.s());
        } else {
            session.setFastMode(true);
            actor.print(BBC.FAST_ENABLED.s());
        }
    }

    @Command(
        name = "/reorder",
        desc = "Sets the reorder mode of WorldEdit"
    )
    @CommandPermissions("worldedit.reorder")
    public void reorderMode(Actor actor, LocalSession session,
                            @Arg(desc = "The reorder mode", def = "")
                                EditSession.ReorderMode reorderMode) {
        if (reorderMode == null) {
            actor.print("The reorder mode is " + session.getReorderMode().getDisplayName());
        } else {
            session.setReorderMode(reorderMode);
            actor.print("The reorder mode is now " + session.getReorderMode().getDisplayName());
        }
    }

    @Command(
        name = "/drawsel",
        desc = "Toggle drawing the current selection"
    )
    @CommandPermissions("worldedit.drawsel")
    public void drawSelection(Player player, LocalSession session,
                              @Arg(desc = "The new draw selection state", def = "")
                                  Boolean drawSelection) throws WorldEditException {
        if (!WorldEdit.getInstance().getConfiguration().serverSideCUI) {
            throw new DisallowedUsageException("This functionality is disabled in the configuration!");
        }
        boolean useServerCui = session.shouldUseServerCUI();
        if (drawSelection != null && drawSelection == useServerCui) {
            player.printError("Server CUI already " + (useServerCui ? "enabled" : "disabled") + ".");
            return;
        }
        if (useServerCui) {
            session.setUseServerCUI(false);
            session.updateServerCUI(player);
            player.print("Server CUI disabled.");
        } else {
            session.setUseServerCUI(true);
            session.updateServerCUI(player);
            player.print("Server CUI enabled. This only supports cuboid regions, with a maximum size of 32x32x32.");
        }
    }

//    @Command(
//        name = "/world",
//        desc = "Sets the world override"
//    )
//    @CommandPermissions("worldedit.world")
//    public void worldOverride(Actor actor, LocalSession session,
//        @Arg(desc = "The world override", def = "") World world) {
//        session.setWorldOverride(world);
//        if (world == null) {
//            actor.print("Removed world override.");
//        } else {
//            actor.print("Set the world override to " + world.getId() + ". (Use //world to go back to default)");
//        }
//    }

    @Command(
            name = "gmask",
            aliases = {"/gmask"},
            descFooter = "The global destination mask applies to all edits you do and masks based on the destination blocks (i.e. the blocks in the world).",
            desc = "Set the global mask"
    )
    @CommandPermissions({"worldedit.global-mask", "worldedit.mask.global"})
    public void gmask(Actor actor, LocalSession session,
                      @Arg(desc = "The mask to set", def = "")
                          Mask mask) {
        if (mask == null) {
            session.setMask(null);
            actor.print(BBC.MASK_DISABLED.s());
        } else {
            session.setMask(mask);
            actor.print(BBC.MASK.s());
        }
    }

    @Command(
        name = "toggleplace",
        aliases = {"/toggleplace"},
        desc = "Switch between your position and pos1 for placement"
    )
    public void togglePlace(Player player, LocalSession session) {
        if (session.togglePlacementPosition()) {
            player.print(BBC.PLACE_ENABLED.s());
        } else {
            player.print(BBC.PLACE_DISABLED.s());
        }
    }

    @Command(
        name = "searchitem",
        aliases = {"/searchitem", "/l", "/search"},
        desc = "Search for an item"
    )
    @CommandPermissions("worldedit.searchitem")
    public void searchItem(Actor actor,
                           @Switch(name = 'b', desc = "Only search for blocks")
                               boolean blocksOnly,
                           @Switch(name = 'i', desc = "Only search for items")
                               boolean itemsOnly,
                           @ArgFlag(name = 'p', desc = "Page of results to return", def = "1")
                               int page,
                           @Arg(desc = "Search query", variable = true)
                               List<String> query) {
        String search = String.join(" ", query);
        if (search.length() <= 2) {
            actor.printError("Enter a longer search string (len > 2).");
            return;
        }
        if (blocksOnly && itemsOnly) {
            actor.printError("You cannot use both the 'b' and 'i' flags simultaneously.");
            return;
        }

        WorldEditAsyncCommandBuilder.createAndSendMessage(actor, new ItemSearcher(search, blocksOnly, itemsOnly, page),
                "(Please wait... searching items.)");
    }

    public static class ItemSearcher implements Callable<Component> {
        private final boolean blocksOnly;
        private final boolean itemsOnly;
        private final String search;
        private final int page;

        ItemSearcher(String search, boolean blocksOnly, boolean itemsOnly, int page) {
            this.blocksOnly = blocksOnly;
            this.itemsOnly = itemsOnly;
            this.search = search;
            this.page = page;
        }

        @Override
        public Component call() throws Exception {
            String command = "/searchitem " + (blocksOnly ? "-b " : "") + (itemsOnly ? "-i " : "") + "-p %page% " + search;
            Map<String, String> results = new TreeMap<>();
            String idMatch = search.replace(' ', '_');
            String nameMatch = search.toLowerCase(Locale.ROOT);
            for (ItemType searchType : ItemType.REGISTRY) {
                if (blocksOnly && !searchType.hasBlockType()) {
                    continue;
                }

                if (itemsOnly && searchType.hasBlockType()) {
                    continue;
                }
                final String id = searchType.getId();
                String name = searchType.getName();
                final boolean hasName = !name.equals(id);
                name = name.toLowerCase(Locale.ROOT);
                if (id.contains(idMatch) || (hasName && name.contains(nameMatch))) {
                    results.put(id, name + (hasName ? " (" + id + ")" : ""));
                }
            }
            List<String> list = new ArrayList<>(results.values());
            return PaginationBox.fromStrings("Search results for '" + search + "'", command, list).create(page);
        }
    }

    @Command(
            name = "/gtexture",
            aliases = {"gtexture"},
            descFooter = "The global destination mask applies to all edits you do and masks based on the destination blocks (i.e. the blocks in the world).",
            desc = "Set the global mask"
    )
    @CommandPermissions("worldedit.global-texture")
    public void gtexture(Player player, World world, LocalSession session, EditSession editSession, @Arg(name = "context", desc = "InjectedValueAccess", def = "") List<String> arguments) throws WorldEditException, FileNotFoundException {
        // gtexture <randomize> <min=0> <max=100>
        // TODO NOT IMPLEMENTED convert this to an ArgumentConverter
        if (arguments.isEmpty()) {
            session.setTextureUtil(null);
            BBC.TEXTURE_DISABLED.send(player);
        } else {
            String arg = arguments.get(0);
            String argLower = arg.toLowerCase(Locale.ROOT);

            TextureUtil util = Fawe.get().getTextureUtil();
            int randomIndex = 1;
            boolean checkRandomization = true;
            if (arguments.size() >= 2 && MathMan.isInteger(arguments.get(0)) && MathMan.isInteger(arguments.get(1))) {
                // complexity
                int min = Integer.parseInt(arguments.get(0));
                int max = Integer.parseInt(arguments.get(1));
                if (min < 0 || max > 100) throw new InputParseException("Complexity must be in the range 0-100");
                if (min != 0 || max != 100) util = new CleanTextureUtil(util, min, max);

                randomIndex = 2;
            } else if (arguments.size() == 1 && argLower.equals("true") || argLower.equals("false")) {
                if (argLower.equals("true")) util = new RandomTextureUtil(util);
                checkRandomization = false;
            } else {
                if (argLower.equals("#copy") || argLower.equals("#clipboard")) {
                    Clipboard clipboard = session.getClipboard().getClipboard();
                    util = TextureUtil.fromClipboard(clipboard);
                } else if (argLower.equals("*") || argLower.equals("true")) {
                    util = Fawe.get().getTextureUtil();
                } else {
                    ParserContext parserContext = new ParserContext();
                    parserContext.setActor(player);
                    parserContext.setWorld(world);
                    parserContext.setSession(session);
                    parserContext.setExtent(editSession);
                    Mask mask = worldEdit.getMaskFactory().parseFromInput(arg, parserContext);
                    util = TextureUtil.fromMask(mask);
                }
            }
            if (checkRandomization) {
                if (arguments.size() > randomIndex) {
                    boolean random = Boolean.parseBoolean(arguments.get(randomIndex));
                    if (random) util = new RandomTextureUtil(util);
                }
            }
            if (!(util instanceof CachedTextureUtil)) util = new CachedTextureUtil(util);
            session.setTextureUtil(util);
            BBC.TEXTURE_SET.send(player, StringMan.join(arguments, " "));
        }
    }

    @Command(
            name = "/gsmask",
            aliases = {"gsmask", "globalsourcemask", "/globalsourcemask"},
            desc = "Set the global source mask",
            descFooter = "The global source mask applies to all edits you do and masks based on the source blocks (e.g. the blocks in your clipboard)"
    )
    @CommandPermissions({"worldedit.global-mask", "worldedit.mask.global"})
    public void gsmask(Player player, LocalSession session, EditSession editSession, @Arg(desc = "The mask to set", def = "") Mask maskOpt) throws WorldEditException {
        session.setSourceMask(maskOpt);
        if (maskOpt == null) {
            BBC.SOURCE_MASK_DISABLED.send(player);
        } else {
            BBC.SOURCE_MASK.send(player);
        }
    }


    @Command(
            name = "/gtransform",
            aliases = {"gtransform"},
            desc = "Set the global transform"
    )
    @CommandPermissions({"worldedit.global-transform", "worldedit.transform.global"})
    public void gtransform(Player player, EditSession editSession, LocalSession session, ResettableExtent transform) throws WorldEditException {
        session.setTransform(transform);
        if (transform == null) {
            BBC.TRANSFORM_DISABLED.send(player);
        } else {
            BBC.TRANSFORM.send(player);
        }
    }

    @Command(
            name = "/tips",
            aliases = {"tips"},
            desc = "Toggle FAWE tips"
    )
    @CommandPermissions("fawe.tips")
    public void tips(Player player, LocalSession session) throws WorldEditException {
        if (player.togglePermission("fawe.tips")) {
            BBC.WORLDEDIT_TOGGLE_TIPS_ON.send(player);
        } else {
            BBC.WORLDEDIT_TOGGLE_TIPS_OFF.send(player);
        }
    }
}
