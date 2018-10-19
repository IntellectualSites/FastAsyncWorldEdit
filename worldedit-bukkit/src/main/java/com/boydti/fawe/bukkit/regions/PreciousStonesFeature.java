package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMask;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.List;
import net.sacredlabyrinth.Phaed.PreciousStones.PreciousStones;
import net.sacredlabyrinth.Phaed.PreciousStones.field.Field;
import net.sacredlabyrinth.Phaed.PreciousStones.field.FieldFlag;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class PreciousStonesFeature extends BukkitMaskManager implements Listener {
    FaweBukkit plugin;
    Plugin preciousstones;

    public PreciousStonesFeature(final Plugin preciousstonesPlugin, final FaweBukkit p3) {
        super(preciousstonesPlugin.getName());
        this.preciousstones = preciousstonesPlugin;
        this.plugin = p3;

    }

    public boolean isAllowed(Player player, Field field, MaskType type, boolean allowMember) {
        return field != null && (field.isOwner(player.getName()) || (type == MaskType.MEMBER && allowMember && field.getAllAllowed().contains(player.getName())));
    }

    @Override
    public FaweMask getMask(final FawePlayer<Player> fp, MaskType type) {
        final Player player = fp.parent;
        final Location location = player.getLocation();
        final List<Field> fields = PreciousStones.API().getFieldsProtectingArea(FieldFlag.ALL, location);
        if (fields.isEmpty()) {
            return null;
        }
        String name = player.getName();
        boolean member = fp.hasPermission("fawe.preciousstones.member");
        for (final Field myField : fields) {
            if (isAllowed(player, myField, type, member)) {
                BlockVector3 pos1 = BlockVector3.at(myField.getMinx(), myField.getMiny(), myField.getMinz());
                BlockVector3 pos2 = BlockVector3.at(myField.getMaxx(), myField.getMaxy(), myField.getMaxz());
                return new FaweMask(pos1, pos2, "FIELD: " + myField) {
                    @Override
                    public boolean isValid(FawePlayer player, MaskType type) {
                        return isAllowed((Player) player.parent, myField, type, fp.hasPermission("fawe.preciousstones.member"));
                    }
                };
            }
        }
        return null;
    }
}
