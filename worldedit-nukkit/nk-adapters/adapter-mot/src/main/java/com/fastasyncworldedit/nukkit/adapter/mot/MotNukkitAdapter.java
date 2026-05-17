package com.fastasyncworldedit.nukkit.adapter.mot;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.GlobalBlockPalette;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.format.leveldb.BlockStateMapping;
import cn.nukkit.level.format.leveldb.NukkitLegacyMapper;
import cn.nukkit.level.format.leveldb.structure.BlockStateSnapshot;
import cn.nukkit.level.generator.object.tree.ObjectCherryTree;
import cn.nukkit.level.generator.object.tree.ObjectMangroveTree;
import cn.nukkit.level.generator.object.tree.ObjectPaleOakTree;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.Identifier;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplAdapter;
import org.cloudburstmc.nbt.NbtMap;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Adapter implementation for the Nukkit-MOT platform.
 */
public class MotNukkitAdapter implements NukkitImplAdapter {

    @Override
    public String getPlatformName() {
        return "Nukkit-MOT";
    }

    @Override
    public int getBlockDataBits() {
        return Block.DATA_BITS;
    }

    @Override
    @Nullable
    public String getPlayerLanguageCode(Player player) {
        var langCode = player.getLanguageCode();
        return langCode != null ? langCode.name() : null;
    }

    @Override
    public int getBlockRuntimeId(Player player, int blockId, int meta) {
        return GlobalBlockPalette.getOrCreateRuntimeId(player.getGameVersion(), blockId, meta);
    }

    @Override
    @Nullable
    public String getEntityIdentifier(Entity entity) {
        Identifier identifier = entity.getIdentifier();
        return identifier != null ? identifier.toString() : null;
    }

    @Override
    public List<NbtMap> loadBlockPalette() {
        return NukkitLegacyMapper.loadBlockPalette();
    }

    @Override
    @Nullable
    public BlockStateSnapshot getBlockStateSnapshot(NbtMap nbtState) {
        return BlockStateMapping.get().getStateUnsafe(nbtState);
    }

    @Override
    public boolean generateTree(String treeType, Level level, int x, int y, int z, NukkitRandom random, Vector3 pos) {
        // MOT: Mangrove, Cherry, PaleOak all extend TreeGenerator
        return switch (treeType) {
            case "MANGROVE" -> new ObjectMangroveTree().generate(level, random, pos);
            case "CHERRY" -> new ObjectCherryTree().generate(level, random, pos);
            case "PALE_OAK" -> new ObjectPaleOakTree().generate(level, random, pos);
            default -> false;
        };
    }

    @Override
    public int getBlockId(FullChunk chunk, int x, int y, int z, int layer) {
        return chunk.getBlockId(x, y, z, layer);
    }

    @Override
    public void setFullBlockId(FullChunk chunk, int x, int y, int z, int layer, int fullId) {
        chunk.setFullBlockId(x, y, z, layer, fullId);
    }

    @Override
    public UUID getEntityUUID(Entity entity) {
        return entity.getUniqueId();
    }

}
