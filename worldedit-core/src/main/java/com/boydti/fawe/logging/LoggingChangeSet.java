package com.boydti.fawe.logging;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.changeset.AbstractDelegateChangeSet;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.primesoft.blockshub.IBlocksHubApi;
import org.primesoft.blockshub.api.IPlayer;
import org.primesoft.blockshub.api.IWorld;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class LoggingChangeSet extends AbstractDelegateChangeSet {

    public static IBlocksHubApi api;
    private static boolean initialized = false;
    private static Method getBlockData;
    private final MutableBlockData oldBlock;
    private final MutableBlockData newBlock;
    private final IPlayer player;
    private final IWorld world;

    private LoggingChangeSet(FawePlayer player, FaweChangeSet parent) {
        super(parent);
        String worldName = player.getWorld().getName();
        IWorld world;
        try {
            Class<?> classBukkitWorld = Class.forName("org.primesoft.blockshub.platform.bukkit.BukkitWorld");
            Class<?> classAsyncWorld = Class.forName("com.boydti.fawe.bukkit.wrapper.AsyncWorld");
            Object asyncWorld = classAsyncWorld.getConstructor(String.class, boolean.class).newInstance(worldName, false);
            Constructor<?> constructor = classBukkitWorld.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            world = (IWorld) constructor.newInstance(asyncWorld);
        } catch (Throwable ignore) {
            world = api.getWorld(worldName);
        }
        this.world = world;
        this.oldBlock = new MutableBlockData();
        this.newBlock = new MutableBlockData();
        this.player = api.getPlayer(player.getUUID());
    }

    public static FaweChangeSet wrap(FawePlayer player, FaweChangeSet parent) {
        if (!initialized) {
            initialized = true;
            api = (IBlocksHubApi) Fawe.imp().getBlocksHubApi();
            try {
                getBlockData = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter").getDeclaredMethod("getBlockData", Integer.TYPE);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (api == null || getBlockData == null) {
            return parent;
        }
        return new LoggingChangeSet(player, parent);
    }

    @Override
    public void add(int x, int y, int z, int combinedFrom, int combinedTo) {
        try {
            oldBlock.type = BlockTypes.getFromStateId(combinedFrom);
            oldBlock.data = getBlockData.invoke(null, combinedFrom);
            newBlock.type = BlockTypes.getFromStateId(combinedTo);
            newBlock.data = getBlockData.invoke(null, combinedTo);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        // Log to BlocksHub and parent
        api.logBlock(player, world, x, y, z, oldBlock, newBlock);
        parent.add(x, y, z, combinedFrom, combinedTo);
    }
}
