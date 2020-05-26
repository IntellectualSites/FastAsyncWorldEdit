package com.boydti.fawe.object.change;

import static org.slf4j.LoggerFactory.getLogger;

import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MutableEntityChange implements Change {

    public CompoundTag tag;
    public boolean create;

    public MutableEntityChange(CompoundTag tag, boolean create) {
        this.tag = tag;
        this.create = create;
    }

    @Override
    public void undo(UndoContext context) throws WorldEditException {
        if (!create) {
            create(context);
        } else {
            delete(context);
        }
    }

    @Override
    public void redo(UndoContext context) throws WorldEditException {
        if (create) {
            create(context);
        } else {
            delete(context);
        }
    }

    public void delete(UndoContext context) {
        Map<String, Tag> map = tag.getValue();
        long most;
        long least;
        if (map.containsKey("UUIDMost")) {
            most = ((LongTag) map.get("UUIDMost")).getValue();
            least = ((LongTag) map.get("UUIDLeast")).getValue();
        } else if (map.containsKey("PersistentIDMSB")) {
            most = ((LongTag) map.get("PersistentIDMSB")).getValue();
            least = ((LongTag) map.get("PersistentIDLSB")).getValue();
        } else {
            getLogger(MutableEntityChange.class).debug("Skipping entity without uuid.");
            return;
        }
        List<DoubleTag> pos = (List<DoubleTag>) map.get("Pos").getValue();
        int x = MathMan.roundInt(pos.get(0).getValue());
        int y = MathMan.roundInt(pos.get(1).getValue());
        int z = MathMan.roundInt(pos.get(2).getValue());
        UUID uuid = new UUID(most, least);
        context.getExtent().removeEntity(x, y, z, uuid);
    }

    public void create(UndoContext context) {
        Map<String, Tag> map = tag.getValue();
        Tag posTag = map.get("Pos");
        if (posTag == null) {
            getLogger(MutableEntityChange.class).debug("Missing pos tag: " + tag);
            return;
        }
        List<DoubleTag> pos = (List<DoubleTag>) posTag.getValue();
        int x = MathMan.roundInt(pos.get(0).getValue());
        int y = MathMan.roundInt(pos.get(1).getValue());
        int z = MathMan.roundInt(pos.get(2).getValue());
        Extent extent = context.getExtent();
        Location location = new Location(extent, x, y, z, 0, 0);
        String id = tag.getString("id");
        EntityType type = EntityTypes.parse(id);
        BaseEntity entity = new BaseEntity(type, tag);
        context.getExtent().createEntity(location, entity);
    }

}
