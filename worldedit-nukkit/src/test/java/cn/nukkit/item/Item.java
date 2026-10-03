package cn.nukkit.item;

public class Item {

    public static final int AIR = 0;

    private final int id;
    private final int damage;
    private int count;

    public Item() {
        this(AIR, 0, 0);
    }

    public Item(int id, int damage, int count) {
        this.id = id;
        this.damage = damage;
        this.count = count;
    }

    public static Item get(int id) {
        return new Item(id, 0, id == AIR ? 0 : 1);
    }

    public static Item get(int id, int damage, int count) {
        return new Item(id, damage, count);
    }

    public int getId() {
        return id;
    }

    public int getDamage() {
        return damage;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getMaxStackSize() {
        return 64;
    }

    public boolean isNull() {
        return id == AIR || count <= 0;
    }

}
