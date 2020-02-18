package com.boydti.fawe.object.brush;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Caption;
import com.boydti.fawe.logging.rollback.RollbackOptimizedHistory;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.database.DBHandler;
import com.boydti.fawe.database.RollbackDatabase;
import com.boydti.fawe.object.change.MutableFullBlockChange;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.DoubleActionTraceTool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import com.sk89q.worldedit.util.formatting.text.event.HoverEvent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.Supplier;

public class InspectBrush extends BrushTool implements DoubleActionTraceTool {

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
        if (!session.isToolControlEnabled() || !player.hasPermission("worldedit.tool.inspect")) {
            player.print(Caption.of("", "worldedit.tool.inspect"));
            System.out.println("No tool control");
            return false;
        }
        if (!Settings.IMP.HISTORY.USE_DATABASE) {
            player.print(Caption.of("fawe.error.setting.disable", ("history.use-database (Import with /history import )")));
            System.out.println("No db");
            return false;
        }
        try {
            BlockVector3 target = getTarget(player, rightClick).toBlockPoint();
            final int x = target.getBlockX();
            final int y = target.getBlockY();
            final int z = target.getBlockZ();
            World world = player.getWorld();
            RollbackDatabase db = DBHandler.INSTANCE.getDatabase(world);
            System.out.println("World " + world.getName());
            int count = 0;
            for (Supplier<RollbackOptimizedHistory> supplier : db.getEdits(target, false)) {
                System.out.println("History " + db);
                count++;
                RollbackOptimizedHistory edit = supplier.get();
                Iterator<MutableFullBlockChange> iter = edit.getFullBlockIterator(null, 0, false);
                while (iter.hasNext()) {
                    MutableFullBlockChange change = iter.next();
                    if (change.x != x || change.y != y || change.z != z) {
                        System.out.println("Not pos " + change.x + "," + change.y + "," + change.z + " | " + x + "," + y + "," + z);
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
                    TranslatableComponent msg = Caption.of("fawe.worldedit.tool.tool.inspect.info", name, blockFrom, blockTo, ageFormatted);

                    String cmd = edit.getCommand();
                    TextComponent hover = TextComponent.of(cmd, TextColor.GOLD);
                    String infoCmd = "//history summary " + uuid + " " + index;
                    msg = msg.hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, hover));
                    msg = msg.clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND, infoCmd));
                    player.print(msg);
                }
            }
            player.print(Caption.of("fawe.worldedit.tool.tool.inspect.info.footer" , count));
        } catch (IOException e) {
            System.out.println("IOE");
            throw new RuntimeException(e);
        } catch (Throwable e) {
            System.out.println("E throw");
        }
        return true;
    }

    @Override
    public boolean canUse(Actor actor) {
        return actor.hasPermission("worldedit.tool.inspect");
    }
}
