package com.boydti.fawe.logging;

import com.boydti.fawe.object.changeset.AbstractDelegateChangeSet;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.sk89q.worldedit.entity.Player;
//import org.primesoft.blockshub.IBlocksHubApi;
//import org.primesoft.blockshub.api.IPlayer;
//import org.primesoft.blockshub.api.IWorld;

public class LoggingChangeSet extends AbstractDelegateChangeSet {

    private static boolean initialized = false;

    public static FaweChangeSet wrap(Player player, FaweChangeSet parent) {
        if (!initialized) {
            initialized = true;
//            api = (IBlocksHubApi) Fawe.imp().getBlocksHubApi();
        }
//        if (api == null) {
            return parent;
//        }
//        return new LoggingChangeSet(player, parent);
    }

//    public static IBlocksHubApi api;
//
//    private final MutableVector3 loc;
//    private final IPlayer player;
//    private IWorld world;
//    private final MutableBlockData oldBlock;
//    private final MutableBlockData newBlock;

    private LoggingChangeSet(Player player, FaweChangeSet parent) {
        super(parent);
//        String world = player.getLocation().world;
//        try {
//            Class<?> classBukkitWorld = Class.forName("org.primesoft.blockshub.platform.bukkit.BukkitWorld");
//            Class<?> classAsyncWorld = Class.forName("com.boydti.fawe.bukkit.wrapper.AsyncWorld");
//            Object asyncWorld = classAsyncWorld.getConstructor(String.class, boolean.class).newInstance(world, false);
//            Constructor<?> constructor = classBukkitWorld.getDeclaredConstructors()[0];
//            constructor.setAccessible(true);
//            this.world = (IWorld) constructor.newInstance(asyncWorld);
//        } catch (Throwable ignore) {
//            this.world = api.getWorld(world);
//        }
//        this.loc = new MutableVector3();
//        this.oldBlock = new MutableBlockData();
//        this.newBlock = new MutableBlockData();
//        this.player = api.getPlayer(player.getUUID());
    }

    @Override
    public void add(int x, int y, int z, int combinedId4DataFrom, int combinedId4DataTo) {
        // Mutable (avoids object creation)
//        loc.x = x;
//        loc.y = y;
//        loc.z = z;
//        oldBlock.id = FaweCache.IMP.getId(combinedId4DataFrom);
//        oldBlock.data = FaweCache.IMP.getData(combinedId4DataFrom);
//        newBlock.id = FaweCache.IMP.getId(combinedId4DataTo);
//        newBlock.data = FaweCache.IMP.getData(combinedId4DataTo);
//        // Log to BlocksHub and parent
//        api.logBlock(loc, player, world, oldBlock, newBlock);
        parent.add(x, y, z, combinedId4DataFrom, combinedId4DataTo);
    }
}
