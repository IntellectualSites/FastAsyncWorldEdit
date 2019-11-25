package com.sk89q.worldedit.registry.state;

import com.boydti.fawe.util.ReflectionUtils;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * This class will be generated at runtime - these are just example values
 */
public enum PropertyKey {
    // TODO FIXME Generate this at runtime
    AGE,
    ATTACHED,
    AXIS,
    BITES,
    CONDITIONAL,
    DELAY,
    DISARMED,
    DISTANCE,
    DOWN,
    DRAG,
    EAST,
    EGGS,
    ENABLED,
    EXTENDED,
    EYE,
    FACE,
    FACING,
    HALF,
    HAS_BOTTLE_0,
    HAS_BOTTLE_1,
    HAS_BOTTLE_2,
    HAS_RECORD,
    HATCH,
    HINGE,
    IN_WALL,
    INSTRUMENT,
    INVERTED,
    LAYERS,
    LEVEL,
    LIT,
    LOCKED,
    MODE,
    MOISTURE,
    NORTH,
    NOTE,
    OCCUPIED,
    OPEN,
    PART,
    PERSISTENT,
    PICKLES,
    POWER,
    POWERED,
    ROTATION,
    SHAPE,
    SHORT,
    SNOWY,
    SOUTH,
    STAGE,
    TRIGGERED,
    TYPE,
    UP,
    WATERLOGGED,
    WEST,
    UNSTABLE,
    LEAVES,
    ATTACHMENT,
    SIGNAL_FIRE,
    HANGING,
    HAS_BOOK,
    BOTTOM,



    ;

    private final String id;

    PropertyKey() {
        this.id = name().toLowerCase();
    }

    private static final Map<String, PropertyKey> keys = new HashMap<>();
    static {
        for (PropertyKey key : values()) {
            keys.put(key.name().toLowerCase(), key);
        }
    }

    public final String getId() {
        return this.id;
    }

    public static final PropertyKey get(CharSequence name) {
        return keys.get(name);
    }

    /**
     * Get or create the property key
     * @param id The name of the property (e.g., `waterlogged`)
     * @return PropertyKey enum
     */
    public static PropertyKey getOrCreate(String id) {
        PropertyKey property = PropertyKey.get(id);
        if (property == null) {
            property = ReflectionUtils.addEnum(PropertyKey.class, id.toUpperCase(Locale.ROOT));
            if (property.getId() == null) {
                try {
                    ReflectionUtils.setFailsafeFieldValue(PropertyKey.class.getDeclaredField("id"), property, property.name().toLowerCase());
                } catch (Throwable e) {
                    throw new RuntimeException("Could not register property with an id of " + id , e);
                }
            }
            keys.put(property.name().toLowerCase(), property);
        }
        return property;
    }
}
