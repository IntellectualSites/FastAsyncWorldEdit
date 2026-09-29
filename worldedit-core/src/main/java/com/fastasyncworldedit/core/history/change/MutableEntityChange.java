package com.fastasyncworldedit.core.history.change;

import com.fastasyncworldedit.core.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MutableEntityChange implements Change {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

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

    @SuppressWarnings({"unchecked"})
    public void delete(UndoContext context) {
        Map<String, Tag<?, ?>> map = tag.getValue();
        UUID uuid = tag.getUUID();
        if (uuid == null) {
            LOGGER.info("Skipping entity without uuid.");
            return;
        }
        List<DoubleTag> pos = (List<DoubleTag>) map.get("Pos").getValue();
        int x = MathMan.roundInt(pos.get(0).getValue());
        int y = MathMan.roundInt(pos.get(1).getValue());
        int z = MathMan.roundInt(pos.get(2).getValue());
        context.getExtent().removeEntity(x, y, z, uuid);
    }

    public void create(UndoContext context) {
        Map<String, Tag<?, ?>> map = tag.getValue();
        Tag posTag = map.get("Pos");
        if (posTag == null) {
            LOGGER.warn("Missing pos tag: {}", tag);
            return;
        }
        List<DoubleTag> pos = (List<DoubleTag>) posTag.getValue();
        double x = pos.get(0).getValue();
        double y = pos.get(1).getValue();
        double z = pos.get(2).getValue();
        Extent extent = context.getExtent();
        Location location = new Location(extent, x, y, z, 0, 0);
        String id = tag.getString("id");
        EntityType type = EntityTypes.parse(id);
        BaseEntity entity = new BaseEntity(type, tag);
        extent.createEntity(location, entity, tag.getUUID());
    }

}
