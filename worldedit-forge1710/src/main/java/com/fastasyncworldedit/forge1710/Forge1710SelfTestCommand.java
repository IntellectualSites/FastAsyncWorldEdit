package com.fastasyncworldedit.forge1710;

import com.fastasyncworldedit.core.util.TaskManager;
import com.fastasyncworldedit.forge1710.registry.NativeBlockMapper;
import com.fastasyncworldedit.forge1710.world.Forge1710World;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

/**
 * Console-only self test used during porting ({@code -Dfawe.forge1710.selftest=true}). Edits a region high above spawn
 * through the full FAWE queue, verifies the native world, then undoes and verifies again.
 */
public class Forge1710SelfTestCommand extends CommandBase {

    private static final Logger LOGGER = LogManager.getLogger("FAWE-SelfTest");

    @Override
    public String getCommandName() {
        return "faweselftest";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/faweselftest [modBlockId]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 4;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        String modBlock = args.length > 0 ? args[0] : null;
        TaskManager.taskManager().async(() -> run(modBlock));
    }

    private static <T> T sync(Supplier<T> supplier) {
        return TaskManager.taskManager().sync(supplier);
    }

    private static void run(String modBlockArg) {
        try {
            WorldServer nmsWorld = MinecraftServer.getServer().worldServers[0];
            Forge1710World world = Forge1710Adapter.adapt(nmsWorld);
            ChunkCoordinates spawn = sync(nmsWorld::getSpawnPoint);
            int cx = (spawn.posX >> 4) << 4;
            int cz = (spawn.posZ >> 4) << 4;
            // Two chunks wide and crossing a section boundary (y 200..215 is one section, 216..219 the next).
            CuboidRegion region = new CuboidRegion(world, BlockVector3.at(cx, 200, cz), BlockVector3.at(cx + 31, 219, cz + 15));
            BlockState stone = BlockTypes.get("minecraft:stone").getDefaultState();

            long start = System.nanoTime();
            EditSession edit = WorldEdit.getInstance().newEditSessionBuilder().world(world).limitUnlimited().build();
            edit.setBlocks((Region) region, stone);
            edit.close();
            long millis = (System.nanoTime() - start) / 1_000_000;
            int changed = edit.getBlockChangeCount();
            int stoneMismatch = sync(() -> countMismatch(nmsWorld, region, Block.getBlockById(1), 0));
            LOGGER.info("[SELFTEST] set stone: changed={} volume={} mismatches={} time={}ms -> {}",
                    changed, region.getVolume(), stoneMismatch, millis, stoneMismatch == 0 ? "PASS" : "FAIL");

            if (modBlockArg != null) {
                BlockState modState = BlockTypes.get(modBlockArg.toLowerCase()).getDefaultState()
                        .with(BlockTypes.get(modBlockArg.toLowerCase()).getProperty(NativeBlockMapper.META_PROPERTY_NAME), 3);
                CuboidRegion small = new CuboidRegion(world, BlockVector3.at(cx + 2, 205, cz + 2), BlockVector3.at(cx + 5, 208, cz + 5));
                EditSession modEdit = WorldEdit.getInstance().newEditSessionBuilder().world(world).limitUnlimited().build();
                modEdit.setBlocks((Region) small, modState);
                modEdit.close();
                Block modNative = NativeBlockMapper.get().getBlock(modBlockArg.toLowerCase());
                int modMismatch = sync(() -> countMismatch(nmsWorld, small, modNative, 3));
                LOGGER.info("[SELFTEST] set {}[legacy_meta=3]: mismatches={} -> {}", modBlockArg, modMismatch,
                        modMismatch == 0 ? "PASS" : "FAIL");
                EditSession modUndo = WorldEdit.getInstance().newEditSessionBuilder().world(world).limitUnlimited().build();
                modEdit.undo(modUndo);
                modUndo.close();
            }

            EditSession undo = WorldEdit.getInstance().newEditSessionBuilder().world(world).limitUnlimited().build();
            edit.undo(undo);
            undo.close();
            int airMismatch = sync(() -> countMismatch(nmsWorld, region, Block.getBlockById(0), -1));
            LOGGER.info("[SELFTEST] undo: mismatches={} -> {}", airMismatch, airMismatch == 0 ? "PASS" : "FAIL");

            BlockState read = world.getBlock(BlockVector3.at(cx, 1, cz));
            LOGGER.info("[SELFTEST] read back bedrock layer at y=1: {}", read);
        } catch (Throwable t) {
            LOGGER.error("[SELFTEST] FAILED with exception", t);
        }
    }

    /**
     * Server thread only. {@code meta < 0} ignores metadata.
     */
    private static int countMismatch(WorldServer world, CuboidRegion region, Block expected, int meta) {
        int mismatches = 0;
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        for (int x = min.x(); x <= max.x(); x++) {
            for (int y = min.y(); y <= max.y(); y++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    if (world.getBlock(x, y, z) != expected || (meta >= 0 && world.getBlockMetadata(x, y, z) != meta)) {
                        mismatches++;
                    }
                }
            }
        }
        return mismatches;
    }

}
