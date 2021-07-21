package com.fastasyncworldedit.bukkit.regions.plotsquared;

import com.fastasyncworldedit.core.FaweAPI;
import com.fastasyncworldedit.core.beta.implementation.lighting.RelightMode;
import com.fastasyncworldedit.core.util.EditSessionBuilder;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import static org.bukkit.Bukkit.getWorld;

public class FaweDelegateRegionManager {

    public boolean setCuboids(final @NonNull PlotArea area,
                              final @NonNull Set<CuboidRegion> regions,
                              final @NonNull Pattern blocks,
                              int minY,
                              int maxY,
                              Runnable whenDone) {
        TaskManager.IMP.async(() -> {
            synchronized (FaweDelegateRegionManager.class) {
                World world = BukkitAdapter.adapt(getWorld(area.getWorldName()));
                EditSession session =
                    new EditSessionBuilder(world).checkMemory(false).fastmode(true).limitUnlimited().changeSetNull()
                        .autoQueue(false).build();
                for (CuboidRegion region : regions) {
                    region.setPos1(region.getPos1().withY(minY));
                    region.setPos2(region.getPos2().withY(maxY));
                    session.setBlocks((Region) region, blocks);
                }
                try {
                    session.flushQueue();
                    for (CuboidRegion region : regions) {
                        FaweAPI.fixLighting(world, region, null,
                            RelightMode.valueOf(com.fastasyncworldedit.core.configuration.Settings.IMP.LIGHTING.MODE));
                    }
                } catch (MaxChangedBlocksException e) {
                    e.printStackTrace();
                } finally {
                    TaskManager.IMP.task(whenDone);
                }
            }
        });
        return true;
    }

    public boolean notifyClear(PlotManager manager) {
        final HybridPlotWorld hpw = ((HybridPlotManager) manager).getHybridPlotWorld();
        return hpw.getType() != PlotAreaType.AUGMENTED || hpw.getTerrain() == PlotAreaTerrainType.NONE;
    }

    public boolean handleClear(@NotNull Plot plot,
                               @Nullable Runnable whenDone,
                               @NotNull PlotManager manager) {
        TaskManager.IMP.async(() -> {
            synchronized (FaweDelegateRegionManager.class) {
                final HybridPlotWorld hybridPlotWorld = ((HybridPlotManager) manager).getHybridPlotWorld();
                World world = BukkitAdapter.adapt(getWorld(hybridPlotWorld.getWorldName()));
                EditSession editSession = new EditSessionBuilder(world).checkMemory(false).fastmode(true).limitUnlimited().changeSetNull().autoQueue(false).build();

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

                    BlockVector3 pos1 = plot.getBottomAbs().getBlockVector3().withY(0);
                    BlockVector3 pos2 = pos1.add(BlockVector3.at(hybridPlotWorld.PLOT_WIDTH - 1, 255, hybridPlotWorld.PLOT_WIDTH - 1));

                    Region bedrockRegion = new CuboidRegion(pos1, pos2.withY(0));
                    Region fillingRegion = new CuboidRegion(pos1.withY(1), pos2.withY(hybridPlotWorld.PLOT_HEIGHT - 1));
                    Region floorRegion = new CuboidRegion(pos1.withY(hybridPlotWorld.PLOT_HEIGHT), pos2.withY(hybridPlotWorld.PLOT_HEIGHT));
                    Region airRegion = new CuboidRegion(pos1.withY(hybridPlotWorld.PLOT_HEIGHT + 1), pos2.withY(manager.getWorldHeight()));

                    editSession.setBlocks(bedrockRegion, bedrock);
                    editSession.setBlocks(fillingRegion, filling);
                    editSession.setBlocks(floorRegion, plotfloor);
                    editSession.setBlocks(airRegion, air);
                    editSession.flushQueue();
                }

                if (hybridPlotWorld.PLOT_SCHEMATIC) {
                    // We cannot reuse the editsession
                    EditSession scheditsession = !Settings.Schematics.PASTE_ON_TOP ? editSession :
                        new EditSessionBuilder(world).checkMemory(false).fastmode(true).limitUnlimited().changeSetNull().autoQueue(false).build();
                    File schematicFile = new File(hybridPlotWorld.getRoot(), "plot.schem");
                    if (!schematicFile.exists()) {
                        schematicFile = new File(hybridPlotWorld.getRoot(), "plot.schematic");
                    }
                    BlockVector3 to = plot.getBottomAbs().getBlockVector3().withY(Settings.Schematics.PASTE_ON_TOP ? hybridPlotWorld.SCHEM_Y : 1);
                    try {
                        Clipboard clip = ClipboardFormats.findByFile(schematicFile).getReader(new FileInputStream(schematicFile)).read();
                        clip.paste(scheditsession, to, true, true, true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // Be verbose in editsession flushing
                    scheditsession.flushQueue();
                }

                // Be verbose in editsession flushing
                editSession.flushQueue();
                FaweAPI.fixLighting(world, new CuboidRegion(plot.getBottomAbs().getBlockVector3(), plot.getTopAbs().getBlockVector3()), null,
                    RelightMode.valueOf(com.fastasyncworldedit.core.configuration.Settings.IMP.LIGHTING.MODE));
                TaskManager.IMP.task(whenDone);
            }
        });
        return true;
    }

    public void swap(Location pos1,
                     Location pos2,
                     Location swapPos,
                     final Runnable whenDone) {
        TaskManager.IMP.async(() -> {
            synchronized (FaweDelegateRegionManager.class) {
                //todo because of the following code this should proably be in the Bukkit module
                World pos1World = BukkitAdapter.adapt(getWorld(pos1.getWorldName()));
                World pos3World = BukkitAdapter.adapt(getWorld(swapPos.getWorldName()));
                WorldEdit.getInstance().getEditSessionFactory().getEditSession(pos1World, -1);
                EditSession sessionA = new EditSessionBuilder(pos1World).checkMemory(false).fastmode(true).limitUnlimited().changeSetNull().autoQueue(false).build();
                EditSession sessionB = new EditSessionBuilder(pos3World).checkMemory(false).fastmode(true).limitUnlimited().changeSetNull().autoQueue(false).build();
                CuboidRegion regionA = new CuboidRegion(pos1.getBlockVector3(), pos2.getBlockVector3());
                CuboidRegion regionB = new CuboidRegion(swapPos.getBlockVector3(), swapPos.getBlockVector3().add(pos2.getBlockVector3()).subtract(pos1.getBlockVector3()));
                regionA.setWorld(pos1World);
                regionB.setWorld(pos3World);
                Clipboard clipA = Clipboard.create(regionA, UUID.randomUUID());
                Clipboard clipB = Clipboard.create(regionB, UUID.randomUUID());
                ForwardExtentCopy copyA = new ForwardExtentCopy(sessionA, regionA, clipA, clipA.getMinimumPoint());
                ForwardExtentCopy copyB = new ForwardExtentCopy(sessionB, regionB, clipB, clipB.getMinimumPoint());
                try {
                    Operations.completeLegacy(copyA);
                    Operations.completeLegacy(copyB);
                    clipA.paste(sessionB, swapPos.getBlockVector3(), true);
                    clipB.paste(sessionA, pos1.getBlockVector3(), true);
                    sessionA.flushQueue();
                    sessionB.flushQueue();
                } catch (MaxChangedBlocksException e) {
                    e.printStackTrace();
                }
                FaweAPI.fixLighting(pos1World, new CuboidRegion(pos1.getBlockVector3(), pos2.getBlockVector3()), null,
                    RelightMode.valueOf(com.fastasyncworldedit.core.configuration.Settings.IMP.LIGHTING.MODE));
                FaweAPI.fixLighting(pos1World, new CuboidRegion(swapPos.getBlockVector3(),
                    BlockVector3.at(swapPos.getX() + pos2.getX() - pos1.getX(), 0, swapPos.getZ() + pos2.getZ() - pos1.getZ())), null,
                    RelightMode.valueOf(com.fastasyncworldedit.core.configuration.Settings.IMP.LIGHTING.MODE));
                TaskManager.IMP.task(whenDone);
            }
        });
    }

    public void setBiome(CuboidRegion region, int extendBiome, BiomeType biome, String world, Runnable whenDone) {
        region.expand(BlockVector3.at(extendBiome, 0, extendBiome));
        region.expand(BlockVector3.at(-extendBiome, 0, -extendBiome));
        TaskManager.IMP.async(() -> {
            synchronized (FaweDelegateRegionManager.class) {
                EditSession editSession = new EditSessionBuilder(BukkitAdapter.adapt(getWorld(world))).checkMemory(false).fastmode(true).limitUnlimited().changeSetNull().autoQueue(false).build();
                FlatRegionFunction replace = new BiomeReplace(editSession, biome);
                FlatRegionVisitor visitor = new FlatRegionVisitor(region, replace);
                try {
                    Operations.completeLegacy(visitor);
                    editSession.flushQueue();
                } catch (MaxChangedBlocksException e) {
                    e.printStackTrace();
                }
                TaskManager.IMP.task(whenDone);
            }
        });
    }

    public boolean copyRegion(final @NonNull Location pos1,
                              final @NonNull Location pos2,
                              final @NonNull Location pos3,
                              final @NonNull Runnable whenDone) {
        TaskManager.IMP.async(() -> {
            synchronized (FaweDelegateRegionManager.class) {
                World pos1World = BukkitAdapter.adapt(getWorld(pos1.getWorldName()));
                World pos3World = BukkitAdapter.adapt(getWorld(pos3.getWorldName()));
                EditSession from = new EditSessionBuilder(pos1World).checkMemory(false).fastmode(true).limitUnlimited().changeSetNull().autoQueue(false).build();
                EditSession to = new EditSessionBuilder(pos3World).checkMemory(false).fastmode(true).limitUnlimited().changeSetNull().autoQueue(false).build();
                CuboidRegion region = new CuboidRegion(BlockVector3.at(pos1.getX(), pos1.getY(), pos1.getZ()), BlockVector3.at(pos2.getX(), pos2.getY(), pos2.getZ()));
                ForwardExtentCopy copy = new ForwardExtentCopy(from, region, to, BlockVector3.at(pos3.getX(), pos3.getY(), pos3.getZ()));
                try {
                    Operations.completeLegacy(copy);
                    to.flushQueue();
                    FaweAPI.fixLighting(pos1World,
                        new CuboidRegion(pos3.getBlockVector3(), pos3.getBlockVector3().add(pos2.getBlockVector3().subtract(pos1.getBlockVector3()))),
                        null, RelightMode.valueOf(com.fastasyncworldedit.core.configuration.Settings.IMP.LIGHTING.MODE));
                } catch (MaxChangedBlocksException e) {
                    e.printStackTrace();
                }
            }
            TaskManager.IMP.task(whenDone);
        });
        return true;
    }

    public boolean regenerateRegion(final Location pos1, final Location pos2, boolean ignore, final Runnable whenDone) {
        TaskManager.IMP.async(() -> {
            synchronized (FaweDelegateRegionManager.class) {
                World pos1World = BukkitAdapter.adapt(getWorld(pos1.getWorldName()));
                try (EditSession editSession = new EditSessionBuilder(pos1World).checkMemory(false).fastmode(true).limitUnlimited().changeSetNull().autoQueue(false).build()) {
                    CuboidRegion region = new CuboidRegion(BlockVector3.at(pos1.getX(), pos1.getY(), pos1.getZ()), BlockVector3.at(pos2.getX(), pos2.getY(), pos2.getZ()));
                    editSession.regenerate(region);
                    editSession.flushQueue();
                }
                TaskManager.IMP.task(whenDone);
            }
        });
        return true;
    }
}
