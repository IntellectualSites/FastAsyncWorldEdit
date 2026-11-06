package cn.nukkit.block;

import cn.nukkit.item.Item;

public class Block {

    public static final int WOOD = 17;
    public static final int LEAVES = 18;

    public static Block get(int id, int meta) {
        return new Block();
    }

    public int getId() {
        return 0;
    }

    public int getDamage() {
        return 0;
    }

    public Item[] getDrops(Item item) {
        return new Item[0];
    }

    public boolean canBePlaced() {
        return true;
    }
}
