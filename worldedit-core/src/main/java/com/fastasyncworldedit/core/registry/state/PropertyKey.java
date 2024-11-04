package com.fastasyncworldedit.core.registry.state;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class will be generated at runtime - these are just example values.
 */
@SuppressWarnings("unused")
public class PropertyKey implements Comparable<PropertyKey> {

    // needs to be declared before constants
    private static final Map<CharSequence, PropertyKey> keys = new HashMap<>();
    private static final List<PropertyKey> keyList = new ArrayList<>();

    // constants
    public static final PropertyKey AGE = getOrCreate("age");
    public static final PropertyKey ATTACHED = getOrCreate("attached");
    public static final PropertyKey ATTACHMENT = getOrCreate("attachement");
    public static final PropertyKey AXIS = getOrCreate("axis");
    public static final PropertyKey BERRIES = getOrCreate("berries");
    public static final PropertyKey BITES = getOrCreate("bites");
    public static final PropertyKey BOTTOM = getOrCreate("bottom");
    public static final PropertyKey CANDLES = getOrCreate("candles");
    public static final PropertyKey CHARGES = getOrCreate("charges");
    public static final PropertyKey CONDITIONAL = getOrCreate("conditional");
    public static final PropertyKey DELAY = getOrCreate("delay");
    public static final PropertyKey DISARMED = getOrCreate("disarmed");
    public static final PropertyKey DISTANCE = getOrCreate("distance");
    public static final PropertyKey DOWN = getOrCreate("down");
    public static final PropertyKey DRAG = getOrCreate("drag");
    public static final PropertyKey EAST = getOrCreate("east");
    public static final PropertyKey EGGS = getOrCreate("eggs");
    public static final PropertyKey ENABLED = getOrCreate("enabled");
    public static final PropertyKey EXTENDED = getOrCreate("extended");
    public static final PropertyKey EYE = getOrCreate("eye");
    public static final PropertyKey FACE = getOrCreate("face");
    public static final PropertyKey FACING = getOrCreate("facing");
    public static final PropertyKey FALLING = getOrCreate("falling");
    public static final PropertyKey HALF = getOrCreate("half");
    public static final PropertyKey HANGING = getOrCreate("hanging");
    public static final PropertyKey HAS_BOOK = getOrCreate("has_book");
    public static final PropertyKey HAS_BOTTLE_0 = getOrCreate("has_bottle_0");
    public static final PropertyKey HAS_BOTTLE_1 = getOrCreate("has_bottle_1");
    public static final PropertyKey HAS_BOTTLE_2 = getOrCreate("has_bottle_2");
    public static final PropertyKey HAS_RECORD = getOrCreate("has_record");
    public static final PropertyKey HATCH = getOrCreate("hatch");
    public static final PropertyKey HINGE = getOrCreate("hinge");
    public static final PropertyKey HONEY_LEVEL = getOrCreate("honey_level");
    public static final PropertyKey INSTRUMENT = getOrCreate("instrument");
    public static final PropertyKey INVERTED = getOrCreate("inverted");
    public static final PropertyKey IN_WALL = getOrCreate("in_wall");
    public static final PropertyKey LAYERS = getOrCreate("layers");
    public static final PropertyKey LEAVES = getOrCreate("leaves");
    public static final PropertyKey LEVEL = getOrCreate("level");
    public static final PropertyKey LIT = getOrCreate("lit");
    public static final PropertyKey LOCKED = getOrCreate("locked");
    public static final PropertyKey MODE = getOrCreate("mode");
    public static final PropertyKey MOISTURE = getOrCreate("moisture");
    public static final PropertyKey NORTH = getOrCreate("north");
    public static final PropertyKey NOTE = getOrCreate("note");
    public static final PropertyKey OCCUPIED = getOrCreate("occupied");
    public static final PropertyKey OPEN = getOrCreate("open");
    public static final PropertyKey ORIENTATION = getOrCreate("orientation");
    public static final PropertyKey PART = getOrCreate("part");
    public static final PropertyKey PERSISTENT = getOrCreate("persistent");
    public static final PropertyKey PICKLES = getOrCreate("pickles");
    public static final PropertyKey POWER = getOrCreate("power");
    public static final PropertyKey POWERED = getOrCreate("powered");
    public static final PropertyKey ROTATION = getOrCreate("rotation");
    public static final PropertyKey SCULK_SENSOR_PHASE = getOrCreate("sculk_sensor_phase");
    public static final PropertyKey SHAPE = getOrCreate("shape");
    public static final PropertyKey SHORT = getOrCreate("short");
    public static final PropertyKey SIGNAL_FIRE = getOrCreate("signal_fire");
    public static final PropertyKey SNOWY = getOrCreate("snowy");
    public static final PropertyKey SOUTH = getOrCreate("south");
    public static final PropertyKey STAGE = getOrCreate("stage");
    public static final PropertyKey TILT = getOrCreate("tilt");
    public static final PropertyKey THICKNESS = getOrCreate("thickness");
    public static final PropertyKey TRIGGERED = getOrCreate("triggered");
    public static final PropertyKey TYPE = getOrCreate("type");
    public static final PropertyKey UNSTABLE = getOrCreate("unstable");
    public static final PropertyKey UP = getOrCreate("up");
    public static final PropertyKey VERTICAL_DIRECTION = getOrCreate("vertical_direction");
    public static final PropertyKey VINE_END = getOrCreate("vine_end");
    public static final PropertyKey WATERLOGGED = getOrCreate("waterlogged");
    public static final PropertyKey WEST = getOrCreate("west");

    private final String name;
    private final int id;


    private PropertyKey(String name, int ordinal) {
        this.name = name;
        this.id = ordinal;
    }

    public static int getCount() {
        return keyList.size();
    }

    public static PropertyKey getByName(CharSequence name) {
        return keys.get(name);
    }

    public static PropertyKey getById(int id) {
        return keyList.get(id);
    }

    /**
     * Get or create the property key.
     *
     * @param id The name of the property (e.g., `waterlogged`)
     * @return PropertyKey enum
     */
    public static PropertyKey getOrCreate(String id) {
        return keys.computeIfAbsent(id, k -> {
            PropertyKey key = new PropertyKey(id, keyList.size());
            keyList.add(key);
            return key;
        });
    }

    public final String getName() {
        return this.name;
    }

    public int getId() {
        return id;
    }

    @Override
    public int compareTo(@Nonnull PropertyKey o) {
        return Integer.compare(this.id, o.id);
    }

    @Override
    public String toString() {
        return "PropertyKey[" + getName() + "]";
    }

}
