package com.fastasyncworldedit.bukkit.regions.plotsquared;

import com.fastasyncworldedit.core.FaweAPI;
import com.fastasyncworldedit.core.extent.processor.lighting.RelightMode;
import com.fastasyncworldedit.core.util.TaskManager;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.generator.HybridPlotManager;
import com.plotsquared.core.generator.HybridPlotWorld;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.PlotAreaTerrainType;
import com.plotsquared.core.plot.PlotAreaType;
import com.plotsquared.core.plot.PlotManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.FlatRegionFunction;
import com.sk89q.worldedit.function.biome.BiomeReplace;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.FlatRegionVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import static org.bukkit.Bukkit.getWorld;

@SuppressWarnings("unused")
public class FaweDelegateRegionManager {

    public boolean setCuboids(
            final @NonNull PlotArea area,
            final @NonNull Set<CuboidRegion> regions,
            final @NonNull Pattern blocks,
            int minY,
            int maxY,
            Runnable whenDone
    ) {
        TaskManager.taskManager().async(() -> {
            synchronized (FaweDelegateRegionManager.class) {
                World world = BukkitAdapter.adapt(getWorld(area.getWorldName()));
                EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(world).checkMemory(false).
                        fastMode(true).limitUnlimited().changeSetNull().build();
                for (CuboidRegion region : regions) {
                    region.setPos1(region.getPos1().withY(minY));
                    region.setPos2(region.getPos2().withY(maxY));
                    session.setBlocks((Region) region, blocks);
                }
                try {
                    session.flushQueue();
                    for (CuboidRegion region : regions) {
                        FaweAPI.fixLighting(world, region, null,
                                RelightMode.valueOf(com.fastasyncworldedit.core.configuration.Settings.settings().LIGHTING.MODE)
                        );
                    }
                } catch (MaxChangedBlocksException e) {
                    e.printStackTrace();
                } finally {
                    if (whenDone != null) {
                        TaskManager.taskManager().task(whenDone);
                    }
                }
            }
        });
        return true;
    }

    public boolean notifyClear(PlotManager manager) {
        final HybridPlotWorld hpw = ((HybridPlotManager) manager).getHybridPlotWorld();
        return hpw.getType() != PlotAreaType.AUGMENTED || hpw.getTerrain() == PlotAreaTerrainType.NONE;
    }

    public boolean handleClear(
            @Nonnull Plot plot,
            @Nullable Runnable whenDone,
            @Nonnull PlotManager manager
    ) {
        TaskManager.taskManager().async(() -> {
            synchronized (FaweDelegateRegionManager.class) {
                final HybridPlotWorld hybridPlotWorld = ((HybridPlotManager) manager).getHybridPlotWorld();
                World world = BukkitAdapter.adapt(getWorld(hybridPlotWorld.getWorldName()));
                EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(world)
                        .checkMemory(false)
                        .fastMode(true)
                        .limitUnlimited()
                        .changeSetNull()
                        .build();

                if (!hybridPlotWorld.PLOT_SCHEMATIC || !Settings.Schematics.PASTE_ON_TOP) {
                    final BlockType bedrock;
                    final BlockType air = BlockTypes.AIR;
                    if (hybridPlotWorld.PLOT_BEDROCK) {
                        bedrock = BlockTypes.BEDROCK;
                    } else {
                        bedrock = air;
                    }

                    final Pattern filling = hybridPlotWorld.MAIN_BLOCK.toPattern();
                    final Pattern plotfloor = hybridPlotWorld.TOP_BLOCK.toPattern();
                    final BiomeType biome = hybridPlotWorld.getPlotBiome();

                    BlockVector3 pos1 = plot.getBottomAbs().getBlockVector3();
                    BlockVector3 pos2 = pos1.add(BlockVector3.at(
                            hybridPlotWorld.PLOT_WIDTH - 1,
                            hybridPlotWorld.getMaxGenHeight(),
                            hybridPlotWorld.PLOT_WIDTH - 1
                    ));

                    if (hybridPlotWorld.PLOT_BEDROCK) {
                        Region bedrockRegion = new CuboidRegion(pos1, pos2.withY(hybridPlotWorld.getMinGenHeight()));
                        editSession.setBlocks(bedrockRegion, bedrock);
                    }

                    Region fillingRegion = new CuboidRegion(
                            pos1.withY(hybridPlotWorld.getMinGenHeight() + 1),
                            pos2.withY(hybridPlotWorld.PLOT_HEIGHT - 1)
                    );
                    Region floorRegion = new CuboidRegion(
                            pos1.withY(hybridPlotWorld.PLOT_HEIGHT),
                            pos2.withY(hybridPlotWorld.PLOT_HEIGHT)
                    );
                    Region airRegion = new CuboidRegion(
                            pos1.withY(hybridPlotWorld.PLOT_HEIGHT + 1),
                            pos2.withY(hybridPlotWorld.getMaxGenHeight())
                    );

                    editSession.setBlocks(fillingRegion, filling);
                    editSession.setBlocks(floorRegion, plotfloor);
                    editSession.setBlocks(airRegion, air);

                    if (hybridPlotWorld.getMinBuildHeight() < hybridPlotWorld.getMinGenHeight()) {
                        Region underneath = new CuboidRegion(
                                pos1.withY(hybridPlotWorld.getMinBuildHeight()),
                                pos2.withY(hybridPlotWorld.getMinGenHeight())
                        );
                        editSession.setBlocks(underneath, air);
                    }
                    if (hybridPlotWorld.getMaxGenHeight() < hybridPlotWorld.getMaxBuildHeight() - 1) {
                        Region onTop = new CuboidRegion(
                                pos1.withY(hybridPlotWorld.getMaxGenHeight()),
                                pos2.withY(hybridPlotWorld.getMaxBuildHeight() - 1)
                        );
                        editSession.setBlocks(onTop, air);
                    }

                    new CuboidRegion(pos1, pos2).forEach(bv3 -> editSession.setBiome(bv3, biome));
                }

                if (hybridPlotWorld.PLOT_SCHEMATIC) {
                    // We cannot reuse the editsession
                    EditSession scheditsession = !Settings.Schematics.PASTE_ON_TOP ? editSession :
                            WorldEdit.getInstance().newEditSessionBuilder().world(world)
                                    .checkMemory(false)
                                    .fastMode(true)
                                    .limitUnlimited()
                                    .changeSetNull()
                                    .build();
                    File schematicFile = new File(hybridPlotWorld.getSchematicRoot(), "plot.schem");
                    if (!schematicFile.exists()) {
                        schematicFile = new File(hybridPlotWorld.getSchematicRoot(), "plot.schematic");
                    }
                    BlockVector3 to = plot.getBottomAbs().getBlockVector3().withY(hybridPlotWorld.getPlotYStart());
                    try {
                        Clipboard clip = ClipboardFormats
                                .findByFile(schematicFile)
                                .getReader(new FileInputStream(schematicFile))
                                .read();
                        clip.setOrigin(clip.getRegion().getMinimumPoint());
                        clip.paste(scheditsession, to, true, true, true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // Be verbose in editsession flushing
                    scheditsession.flushQueue();
                }

                editSession.flushQueue();
                FaweAPI.fixLighting(
                        world,
                        new CuboidRegion(plot.getBottomAbs().getBlockVector3(), plot.getTopAbs().getBlockVector3()),
                        null,
                        RelightMode.valueOf(com.fastasyncworldedit.core.configuration.Settings.settings().LIGHTING.MODE)
                );
                if (whenDone != null) {
                    TaskManager.taskManager().task(whenDone);
                }
            }
        });
        return true;
    }

    public void swap(
            Location pos1,
            Location pos2,
            Location swapPos,
            final Runnable whenDone
    ) {
        TaskManager.taskManager().async(() -> {
            synchronized (FaweDelegateRegionManager.class) {
                //todo because of the following code this should probably be in the Bukkit module
                World pos1World = BukkitAdapter.adapt(getWorld(pos1.getWorldName()));
                World pos3World = BukkitAdapter.adapt(getWorld(swapPos.getWorldName()));
                EditSession sessionA = WorldEdit.getInstance().newEditSessionBuilder().world(pos1World)
                        .checkMemory(false)
                        .fastMode(true)
                        .limitUnlimited()
                        .changeSetNull()
                        .build();
                EditSession sessionB = WorldEdit.getInstance().newEditSessionBuilder().world(pos3World)
                        .checkMemory(false)
                        .fastMode(true)
                        .limitUnlimited()
                        .changeSetNull()
                        .build();
                CuboidRegion regionA = new CuboidRegion(pos1World, pos1.getBlockVector3(), pos2.getBlockVector3());
                CuboidRegion regionB = new CuboidRegion(
                        pos3World,
                        swapPos.getBlockVector3(),
                        swapPos.getBlockVector3().add(pos2.getBlockVector3()).subtract(pos1.getBlockVector3())
                );
                Clipboard clipA = Clipboard.create(regionA, UUID.randomUUID());
                Clipboard clipB = Clipboard.create(regionB, UUID.randomUUID());
                ForwardExtentCopy copyA = new ForwardExtentCopy(sessionA, regionA, clipA, clipA.getMinimumPoint());
                ForwardExtentCopy copyB = new ForwardExtentCopy(sessionB, regionB, clipB, clipB.getMinimumPoint());
                copyA.setCopyingBiomes(true);
                copyB.setCopyingBiomes(true);
                try {
                    Operations.completeLegacy(copyA);
                    Operations.completeLegacy(copyB);
                    clipA.flush();
                    clipB.flush();
                    clipA.paste(sessionB, swapPos.getBlockVector3(), true, true, true);
                    clipB.paste(sessionA, pos1.getBlockVector3(), true, true, true);
                } catch (MaxChangedBlocksException e) {
                    e.printStackTrace();
                } finally {
                    sessionA.close();
                    sessionB.close();
                }
                FaweAPI.fixLighting(pos1World, new CuboidRegion(pos1.getBlockVector3(), pos2.getBlockVector3()), null,
                        RelightMode.valueOf(com.fastasyncworldedit.core.configuration.Settings.settings().LIGHTING.MODE)
                );
                FaweAPI.fixLighting(pos1World, new CuboidRegion(
                                swapPos.getBlockVector3(),
                                BlockVector3.at(
                                        swapPos.getX() + pos2.getX() - pos1.getX(),
                                        0,
                                        swapPos.getZ() + pos2.getZ() - pos1.getZ()
                                )
                        ), null,
                        RelightMode.valueOf(com.fastasyncworldedit.core.configuration.Settings.settings().LIGHTING.MODE)
                );
                if (whenDone != null) {
                    TaskManager.taskManager().task(whenDone);
                }
            }
        });
    }

    public void setBiome(CuboidRegion region, int extendBiome, BiomeType biome, String world, Runnable whenDone) {
        region.expand(BlockVector3.at(extendBiome, 0, extendBiome));
        region.expand(BlockVector3.at(-extendBiome, 0, -extendBiome));
        TaskManager.taskManager().async(() -> {
            synchronized (FaweDelegateRegionManager.class) {
                EditSession editSession = WorldEdit
                        .getInstance()
                        .newEditSessionBuilder()
                        .world(BukkitAdapter.adapt(getWorld(world)))
                        .checkMemory(false)
                        .fastMode(true)
                        .limitUnlimited()
                        .changeSetNull()
                        .build();
                FlatRegionFunction replace = new BiomeReplace(editSession, biome);
                FlatRegionVisitor visitor = new FlatRegionVisitor(region, replace, editSession);
                try {
                    Operations.completeLegacy(visitor);
                    editSession.flushQueue();
                } catch (MaxChangedBlocksException e) {
                    e.printStackTrace();
                }
                if (whenDone != null) {
                    TaskManager.taskManager().task(whenDone);
                }
            }
        });
    }

    public boolean copyRegion(
            final @NonNull Location pos1,
            final @NonNull Location pos2,
            final @NonNull Location pos3,
            final @NonNull Runnable whenDone
    ) {
        TaskManager.taskManager().async(() -> {
            synchronized (FaweDelegateRegionManager.class) {
                World pos1World = BukkitAdapter.adapt(getWorld(pos1.getWorldName()));
                World pos3World = BukkitAdapter.adapt(getWorld(pos3.getWorldName()));
                EditSession from = WorldEdit.getInstance().newEditSessionBuilder().world(pos1World)
                        .checkMemory(false)
                        .fastMode(true)
                        .limitUnlimited()
                        .changeSetNull()
                        .build();
                EditSession to = WorldEdit.getInstance().newEditSessionBuilder().world(pos3World)
                        .checkMemory(false)
                        .fastMode(true)
                        .limitUnlimited()
                        .changeSetNull()
                        .build();
                CuboidRegion region = new CuboidRegion(
                        BlockVector3.at(pos1.getX(), pos1.getY(), pos1.getZ()),
                        BlockVector3.at(pos2.getX(), pos2.getY(), pos2.getZ())
                );
                ForwardExtentCopy copy = new ForwardExtentCopy(
                        from,
                        region,
                        to,
                        BlockVector3.at(pos3.getX(), pos3.getY(), pos3.getZ())
                );
                try {
                    Operations.completeLegacy(copy);
                    to.flushQueue();
                    FaweAPI.fixLighting(pos1World,
                            new CuboidRegion(
                                    pos3.getBlockVector3(),
                                    pos3.getBlockVector3().add(pos2.getBlockVector3().subtract(pos1.getBlockVector3()))
                            ),
                            null, RelightMode.valueOf(com.fastasyncworldedit.core.configuration.Settings.settings().LIGHTING.MODE)
                    );
                } catch (MaxChangedBlocksException e) {
                    e.printStackTrace();
                }
            }
            if (whenDone != null) {
                TaskManager.taskManager().task(whenDone);
            }
        });
        return true;
    }

    public boolean regenerateRegion(final Location pos1, final Location pos2, boolean ignore, final Runnable whenDone) {
        TaskManager.taskManager().async(() -> {
            synchronized (FaweDelegateRegionManager.class) {
                World pos1World = BukkitAdapter.adapt(getWorld(pos1.getWorldName()));
                try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(pos1World)
                        .checkMemory(false)
                        .fastMode(true)
                        .limitUnlimited()
                        .changeSetNull()
                        .build()) {
                    CuboidRegion region = new CuboidRegion(
                            BlockVector3.at(pos1.getX(), pos1.getY(), pos1.getZ()),
                            BlockVector3.at(pos2.getX(), pos2.getY(), pos2.getZ())
                    );
                    editSession.regenerate(region);
                    editSession.flushQueue();
                }
                if (whenDone != null) {
                    TaskManager.taskManager().task(whenDone);
                }
            }
        });
        return true;
    }

}
