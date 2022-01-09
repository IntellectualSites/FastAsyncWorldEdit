package com.fastasyncworldedit.core.command.tool.brush;

import com.fastasyncworldedit.core.FaweCache;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;

public record CircleBrush(boolean filled) implements Brush {

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws
            MaxChangedBlocksException {
        Actor actor = editSession.getActor();
        if (!(actor instanceof Player player)) {
            throw FaweCache.PLAYER_ONLY;
        }
        Vector3 normal = position.toVector3().subtract(player.getLocation());
        editSession.makeCircle(position, pattern, size, size, size, filled, normal);
    }

}
