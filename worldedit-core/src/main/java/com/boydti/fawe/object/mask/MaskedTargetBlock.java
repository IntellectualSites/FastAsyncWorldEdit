package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.TargetBlock;
import com.sk89q.worldedit.world.World;

public class MaskedTargetBlock extends TargetBlock {
    private final Mask mask;
    private final World world;

    public MaskedTargetBlock(Mask mask, Player player, int maxDistance, double checkDistance) {
        super(player, maxDistance, checkDistance);
        this.mask = mask;
        this.world = player.getWorld();
    }

    public Location getMaskedTargetBlock(boolean useLastBlock) {
        boolean searchForLastBlock = true;
        Location lastBlock = null;
        while (getNextBlock() != null) {
            Location current = getCurrentBlock();
            if (!mask.test(current.toVector())) {
                if (searchForLastBlock) {
                    lastBlock = current;
                    if (lastBlock.getBlockY() <= 0 || lastBlock.getBlockY() >= world.getMaxY()) {
                        searchForLastBlock = false;
                    }
                } else if (current.getBlockY() <= 0) break;
            } else {
                break;
            }
        }
        Location currentBlock = getCurrentBlock();
        return (currentBlock != null || !useLastBlock ? currentBlock : lastBlock);
    }
}