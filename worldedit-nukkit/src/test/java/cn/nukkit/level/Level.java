package cn.nukkit.level;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.item.Item;
import cn.nukkit.level.particle.Particle;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.Vector3;

import java.util.Collections;
import java.util.Map;

public class Level implements ChunkManager {

    public String getName() {
        return "world";
    }

    public int getMinBlockY() {
        return 0;
    }

    public int getMaxBlockY() {
        return 319;
    }

    public BaseFullChunk getChunk(int chunkX, int chunkZ, boolean create) {
        return null;
    }

    public int getBiomeId(int x, int z) {
        return 0;
    }

    public void setBiomeId(int x, int z, byte biomeId) {
    }

    public Block getBlock(int x, int y, int z) {
        return Block.get(0, 0);
    }

    public Block getBlock(Vector3 position) {
        return getBlock(position.getFloorX(), position.getFloorY(), position.getFloorZ());
    }

    public void addLevelEvent(Vector3 position, int type, int data) {
    }

    public void addParticle(Particle particle) {
    }

    public Item useBreakOn(Vector3 position) {
        return Item.get(Item.AIR);
    }

    public Item useItemOn(Vector3 position, Item item, BlockFace face, float fx, float fy, float fz) {
        return item;
    }

    public Map<Long, Entity> getChunkEntities(int chunkX, int chunkZ) {
        return Collections.emptyMap();
    }

    public Map<Long, Player> getChunkPlayers(int chunkX, int chunkZ) {
        return Collections.emptyMap();
    }

    public void requestChunk(int chunkX, int chunkZ, Player player) {
    }

    public Position getSpawnLocation() {
        return new Position(0, 0, 0);
    }
}
