package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.clipboard.CPUOptimizedClipboard;
import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.boydti.fawe.object.clipboard.OffsetFaweClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import java.util.Arrays;

public class ErodeBrush implements Brush {

    private static final BlockVector3[] FACES_TO_CHECK = Direction.valuesOf(Direction.Flag.CARDINAL).stream().map(Direction::toBlockVector).toArray(BlockVector3[]::new);

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        this.erosion(editSession, 2, 1, 5, position, size);
    }

    void erosion(EditSession es, int erodeFaces, int erodeRec, int fillFaces,
        BlockVector3 target, double size) {
        int brushSize = (int) size + 1;
        int brushSizeSquared = (int) (size * size);
        int dimension = brushSize * 2 + 1;
        FaweClipboard buffer1 = new OffsetFaweClipboard(new CPUOptimizedClipboard(dimension, dimension, dimension), brushSize);
        FaweClipboard buffer2 = new OffsetFaweClipboard(new CPUOptimizedClipboard(dimension, dimension, dimension), brushSize);

        final int bx = target.getBlockX();
        final int by = target.getBlockY();
        final int bz = target.getBlockZ();

        for (int x = -brushSize; x <= brushSize; x++) {
            int x0 = x + bx;
            for (int y = -brushSize; y <= brushSize; y++) {
                int y0 = y + by;
                for (int z = -brushSize; z <= brushSize; z++) {
                    int z0 = z + bz;
                    BlockState state = es.getBlock(x0, y0, z0);
                    buffer1.setBlock(x, y, z, state);
                    buffer2.setBlock(x, y, z, state);
                }
            }
        }

        int swap = 0;
        for (int i = 0; i < erodeRec; ++i) {
            erosionIteration(brushSize, brushSizeSquared, erodeFaces, swap % 2 == 0 ? buffer1 : buffer2, swap % 2 == 1 ? buffer1 : buffer2);
            swap++;
        }

        for (int i = 0; i < 1; ++i) {
            fillIteration(brushSize, brushSizeSquared, fillFaces, swap % 2 == 0 ? buffer1 : buffer2, swap % 2 == 1 ? buffer1 : buffer2);
            swap++;
        }
        FaweClipboard finalBuffer = swap % 2 == 0 ? buffer1 : buffer2;

        finalBuffer.forEach(new FaweClipboard.BlockReader() {
            @Override
            public <B extends BlockStateHolder<B>> void run(int x, int y, int z, B block) {
                es.setBlock(x + bx, y + by, z + bz, block);
            }
        }, true);
    }

    private void fillIteration(int brushSize, int brushSizeSquared, int fillFaces,
        FaweClipboard current, FaweClipboard target) {
        int[] frequency = null;
        for (int x = -brushSize; x <= brushSize; x++) {
            int x2 = x * x;
            for (int z = -brushSize; z <= brushSize; z++) {
                int x2y2 = x2 + z * z;
                for (int y = -brushSize; y <= brushSize; y++) {
                    int cube = x2y2 + y * y;
                    target.setBlock(x, y, z, current.getBlock(x, y, z));
                    if (cube >= brushSizeSquared) {
                        continue;
                    }
                    BaseBlock state = current.getBlock(x, y, z);
                    if (state.getBlockType().getMaterial().isMovementBlocker()) {
                        continue;
                    }
                    int total = 0;
                    int highest = 1;
                    BaseBlock highestState = state;
                    if (frequency == null) {
                        frequency = new int[BlockTypes.size()];
                    } else {
                        Arrays.fill(frequency, 0);
                    }
                    for (BlockVector3 offs : FACES_TO_CHECK) {
                        BaseBlock next = current.getBlock(x + offs.getBlockX(), y + offs.getBlockY(), z + offs.getBlockZ());
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
                        target.setBlock(x, y, z, highestState);
                    }
                }
            }
        }
    }

    private void erosionIteration(int brushSize, int brushSizeSquared, int erodeFaces,
        FaweClipboard current, FaweClipboard target) {
        int[] frequency = null;
        for (int x = -brushSize; x <= brushSize; x++) {
            int x2 = x * x;
            for (int z = -brushSize; z <= brushSize; z++) {
                int x2y2 = x2 + z * z;
                for (int y = -brushSize; y <= brushSize; y++) {
                    int cube = x2y2 + y * y;
                    target.setBlock(x, y, z, current.getBlock(x, y, z));
                    if (cube >= brushSizeSquared) {
                        continue;
                    }
                    BaseBlock state = current.getBlock(x, y, z);
                    if (!state.getBlockType().getMaterial().isMovementBlocker()) {
                        continue;
                    }
                    int total = 0;
                    int highest = 1;
                    BaseBlock highestState = state;
                    if (frequency == null) {
                        frequency = new int[BlockTypes.size()];
                    } else {
                        Arrays.fill(frequency, 0);
                    }
                    for (BlockVector3 offs : FACES_TO_CHECK) {
                        BaseBlock next = current.getBlock(x + offs.getBlockX(), y + offs.getBlockY(), z + offs.getBlockZ());
                        if (next.getBlockType().getMaterial().isMovementBlocker()) {
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
                        target.setBlock(x, y, z, highestState);
                    }
                }
            }
        }
    }
}
