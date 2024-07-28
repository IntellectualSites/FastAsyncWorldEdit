package com.fastasyncworldedit.core.command.tool.brush;

import com.fastasyncworldedit.core.extent.clipboard.CPUOptimizedClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.Arrays;

public class ErodeBrush implements Brush {

    private static final BlockVector3[] FACES_TO_CHECK = Direction
            .valuesOf(Direction.Flag.CARDINAL)
            .stream()
            .map(Direction::toBlockVector)
            .toArray(BlockVector3[]::new);
    private final int erodeFaces;
    private final int erodeRecursion;
    private final int fillFaces;
    private final int fillRecursion;

    public ErodeBrush() {
        this(2, 1, 5, 1);
    }

    public ErodeBrush(int erodeFaces, int erodeRecursion, int fillFaces, int fillRecursion) {
        this.erodeFaces = erodeFaces;
        this.erodeRecursion = erodeRecursion;
        this.fillFaces = fillFaces;
        this.fillRecursion = fillRecursion;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws
            MaxChangedBlocksException {
        this.erosion(editSession, erodeFaces, erodeRecursion, fillFaces, fillRecursion, position, size);
    }

    public void erosion(
            final EditSession es,
            int erodeFaces,
            int erodeRecursion,
            int fillFaces,
            int fillRecursion,
            BlockVector3 target,
            double size
    ) {
        int brushSize = (int) size;
        int brushSizeSquared = (int) (size * size);
        Location min = new Location(es.getWorld(), target.toVector3().subtract(size, size, size));
        Location max = new Location(es.getWorld(), target.toVector3().add(size, size, size));
        Region region = new CuboidRegion(es.getWorld(), min.toBlockPoint(), max.toBlockPoint());
        Clipboard buffer1 = new CPUOptimizedClipboard(region);
        Clipboard buffer2 = new CPUOptimizedClipboard(region);

        final int bx = target.x();
        final int by = target.y();
        final int bz = target.z();

        for (int x = -brushSize, relx = 0; x <= brushSize && relx < buffer1.getWidth(); x++, relx++) {
            int x0 = x + bx;
            for (int y = -brushSize, rely = 0; y <= brushSize && rely < buffer1.getHeight(); y++, rely++) {
                int y0 = y + by;
                for (int z = -brushSize, relz = 0; z <= brushSize && relz < buffer1.getLength(); z++, relz++) {
                    int z0 = z + bz;
                    BlockState state = es.getBlock(x0, y0, z0);
                    buffer1.setBlock(relx, rely, relz, state);
                    buffer2.setBlock(relx, rely, relz, state);
                }
            }
        }

        int swap = 0;
        for (int i = 0; i < erodeRecursion; ++i) {
            erosionIteration(
                    brushSize,
                    brushSizeSquared,
                    erodeFaces,
                    swap % 2 == 0 ? buffer1 : buffer2,
                    swap % 2 == 1 ? buffer1 : buffer2
            );
            swap++;
        }

        for (int i = 0; i < fillRecursion; ++i) {
            fillIteration(
                    brushSize,
                    brushSizeSquared,
                    fillFaces,
                    swap % 2 == 0 ? buffer1 : buffer2,
                    swap % 2 == 1 ? buffer1 : buffer2
            );
            swap++;
        }
        Clipboard finalBuffer = swap % 2 == 0 ? buffer1 : buffer2;

        for (BlockVector3 pos : finalBuffer) {
            BlockState block = pos.getBlock(finalBuffer);
            es.setBlock(pos.x() + bx - brushSize, pos.y() + by - brushSize, pos.z() + bz - brushSize, block);
        }
    }

    private void fillIteration(
            int brushSize, int brushSizeSquared, int fillFaces,
            Clipboard current, Clipboard target
    ) {
        int[] frequency = null;
        for (int x = -brushSize, relx = 0; x <= brushSize && relx < target.getWidth(); x++, relx++) {
            int x2 = x * x;
            for (int z = -brushSize, relz = 0; z <= brushSize && relz < target.getLength(); z++, relz++) {
                int x2y2 = x2 + z * z;
                for (int y = -brushSize, rely = 0; y <= brushSize && rely < target.getHeight(); y++, rely++) {
                    int cube = x2y2 + y * y;
                    target.setBlock(relx, rely, relz, current.getBlock(relx, rely, relz));
                    if (cube >= brushSizeSquared) {
                        continue;
                    }
                    BaseBlock state = current.getFullBlock(relx, rely, relz);
                    if (state.getBlockType().getMaterial().isMovementBlocker()) {
                        continue;
                    }
                    BaseBlock highestState = state;
                    if (frequency == null) {
                        frequency = new int[BlockTypes.size()];
                    } else {
                        Arrays.fill(frequency, 0);
                    }
                    int total = 0;
                    int highest = 1;
                    for (BlockVector3 offs : FACES_TO_CHECK) {
                        BaseBlock next = current.getFullBlock(
                                relx + offs.x(),
                                rely + offs.y(),
                                relz + offs.z()
                        );
                        if (!next.getBlockType().getMaterial().isMovementBlocker()) {
                            continue;
                        }
                        total++;
                        int count = ++frequency[next.getInternalBlockTypeId()];
                        if (count >= highest) {
                            highest = count;
                            highestState = next;
                        }
                    }
                    if (total >= fillFaces) {
                        target.setBlock(relx, rely, relz, highestState);
                    }
                }
            }
        }
    }

    private void erosionIteration(
            int brushSize, int brushSizeSquared, int erodeFaces,
            Clipboard current, Clipboard target
    ) {
        int[] frequency = null;
        for (int x = -brushSize, relx = 0; x <= brushSize && relx < target.getWidth(); x++, relx++) {
            int x2 = x * x;
            for (int z = -brushSize, relz = 0; z <= brushSize && relz < target.getLength(); z++, relz++) {
                int x2y2 = x2 + z * z;
                for (int y = -brushSize, rely = 0; y <= brushSize && rely < target.getHeight(); y++, rely++) {
                    int cube = x2y2 + y * y;
                    target.setBlock(relx, rely, relz, current.getBlock(relx, rely, relz));
                    if (cube >= brushSizeSquared) {
                        continue;
                    }
                    BaseBlock state = current.getFullBlock(relx, rely, relz);
                    if (!state.getMaterial().isMovementBlocker()) {
                        continue;
                    }
                    BaseBlock highestState = state;
                    if (frequency == null) {
                        frequency = new int[BlockTypes.size()];
                    } else {
                        Arrays.fill(frequency, 0);
                    }
                    int highest = 1;
                    int total = 0;
                    for (BlockVector3 offs : FACES_TO_CHECK) {
                        BaseBlock next = current.getFullBlock(
                                relx + offs.x(),
                                rely + offs.y(),
                                relz + offs.z()
                        );
                        if (next.getMaterial().isMovementBlocker()) {
                            continue;
                        }
                        total++;
                        int count = ++frequency[next.getInternalBlockTypeId()];
                        if (count > highest) {
                            highest = count;
                            highestState = next;
                        }
                    }
                    if (total >= erodeFaces) {
                        target.setBlock(relx, rely, relz, highestState);
                    }
                }
            }
        }
    }

}
