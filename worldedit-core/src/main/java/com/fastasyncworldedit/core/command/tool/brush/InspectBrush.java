package com.fastasyncworldedit.core.command.tool.brush;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.database.DBHandler;
import com.fastasyncworldedit.core.database.RollbackDatabase;
import com.fastasyncworldedit.core.history.RollbackOptimizedHistory;
import com.fastasyncworldedit.core.history.change.MutableFullBlockChange;
import com.fastasyncworldedit.core.util.MainUtil;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import com.sk89q.worldedit.util.formatting.text.event.HoverEvent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.Supplier;

public class InspectBrush extends BrushTool {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    /**
     * Construct the tool.
     */
    public InspectBrush() {
        super("worldedit.tool.inspect");
    }

    @Override
    public boolean actSecondary(Platform server, LocalConfiguration config, Player player, LocalSession session) {
        return perform(player, session, false);
    }

    @Override
    public boolean actPrimary(Platform server, LocalConfiguration config, Player player, LocalSession session) {
        return perform(player, session, true);
    }

    public Vector3 getTarget(Player player, boolean adjacent) {
        int range = this.range > -1 ? getRange() : DEFAULT_RANGE;
        if (adjacent) {
            Location face = player.getBlockTraceFace(range, true);
            return face.add(face.getDirection());
        } else {
            return player.getBlockTrace(getRange(), true);
        }
    }

    public boolean perform(final Player player, LocalSession session, boolean rightClick) {
        if (!player.hasPermission("worldedit.tool.inspect")) {
            player.print(Caption.of("fawe.error.no-perm", "worldedit.tool.inspect"));
            return false;
        }
        if (!Settings.IMP.HISTORY.USE_DATABASE) {
            player.print(Caption.of(
                    "fawe.error.setting.disable",
                    "history.use-database (Import with /history import )"
            ));
            return false;
        }
        try {
            BlockVector3 target = getTarget(player, rightClick).toBlockPoint();
            final int x = target.getBlockX();
            final int y = target.getBlockY();
            final int z = target.getBlockZ();
            World world = player.getWorld();
            RollbackDatabase db = DBHandler.IMP.getDatabase(world);
            int count = 0;
            for (Supplier<RollbackOptimizedHistory> supplier : db.getEdits(target, false)) {
                count++;
                RollbackOptimizedHistory edit = supplier.get();
                Iterator<MutableFullBlockChange> iter = edit.getFullBlockIterator(null, 0, false);
                while (iter.hasNext()) {
                    MutableFullBlockChange change = iter.next();
                    if (change.x != x || change.y != y || change.z != z) {
                        continue;
                    }
                    int from = change.from;
                    int to = change.to;
                    UUID uuid = edit.getUUID();
                    String name = Fawe.imp().getName(uuid);
                    int index = edit.getIndex();
                    long age = System.currentTimeMillis() - edit.getBDFile().lastModified();
                    String ageFormatted = MainUtil.secToTime(age / 1000);
                    BlockState blockFrom = BlockState.getFromOrdinal(from);
                    BlockState blockTo = BlockState.getFromOrdinal(to);
                    TranslatableComponent msg = Caption.of(
                            "fawe.worldedit.tool.tool.inspect.info",
                            name,
                            blockFrom,
                            blockTo,
                            ageFormatted
                    );

                    TextComponent hover = TextComponent.of("/tool inspect", TextColor.GOLD);
                    String infoCmd = "//history summary " + uuid + " " + index;
                    msg = msg.hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, hover));
                    msg = msg.clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND, infoCmd));
                    player.print(msg);
                }
            }
            player.print(Caption.of("fawe.worldedit.tool.tool.inspect.info.footer", count));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public boolean canUse(Actor actor) {
        return actor.hasPermission("worldedit.tool.inspect");
    }

}
