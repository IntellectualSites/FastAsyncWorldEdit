package com.boydti.fawe.object.brush;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.database.DBHandler;
import com.boydti.fawe.database.RollbackDatabase;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.change.MutableFullBlockChange;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.DoubleActionTraceTool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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

    public Vector getTarget(Player player, boolean adjacent) {
        Location target = null;
        int range = this.range > -1 ? getRange() : MAX_RANGE;
        if (adjacent) {
            Location face = player.getBlockTraceFace(range, true);
            return face.toVector().add(face.getDirection());
        } else {
            return player.getBlockTrace(getRange(), true).toVector();
        }
    }

    public boolean perform(final Player player, LocalSession session, boolean rightClick) {
        if (!session.isToolControlEnabled() || !player.hasPermission("worldedit.tool.inspect")) {
            player.print(BBC.getPrefix() + BBC.NO_PERM.f("worldedit.tool.inspect"));
            return false;
        }
        if (!Settings.IMP.HISTORY.USE_DATABASE) {
            player.print(BBC.getPrefix() + BBC.SETTING_DISABLE.f("history.use-database (Import with /frb #import )"));
            return false;
        }
        Vector target = getTarget(player, rightClick);
        final int x = target.getBlockX();
        final int y = target.getBlockY();
        final int z = target.getBlockZ();
        World world = player.getWorld();
        final FawePlayer fp = FawePlayer.wrap(player);
        EditSessionBuilder editSession = new EditSessionBuilder(world).player(fp);
        RollbackDatabase db = DBHandler.IMP.getDatabase(world);
        final AtomicInteger count = new AtomicInteger();
        db.getPotentialEdits(null, 0, target, target, new RunnableVal<DiskStorageHistory>() {
            @Override
            public void run(DiskStorageHistory value) {
                try {
                    Iterator<MutableFullBlockChange> iter = value.getFullBlockIterator(null, 0, false);
                    while (iter.hasNext()) {
                        MutableFullBlockChange change = iter.next();
                        if (change.x != x || change.y != y || change.z != z) {
                            continue;
                        }
                        int from = change.from;
                        int to = change.to;
                        UUID uuid = value.getUUID();
                        String name = Fawe.imp().getName(uuid);
                        int index = value.getIndex();
                        long age = System.currentTimeMillis() - value.getBDFile().lastModified();
                        String ageFormatted = MainUtil.secToTime(age / 1000);
                        BBC.TOOL_INSPECT_INFO.send(fp, name, BlockState.getFromInternalId(from).getAsString(), BlockState.getFromInternalId(to).getAsString(), ageFormatted);
                        count.incrementAndGet();
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, new Runnable() {
            @Override
            public void run() {
                BBC.TOOL_INSPECT_INFO_FOOTER.send(fp, count);
            }
        }, false, false);
        return true;
    }

    @Override
    public boolean canUse(Actor actor) {
        return actor.hasPermission("worldedit.tool.inspect");
    }
}
