package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.regions.FaweMask;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
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

    public PreciousStonesFeature(Plugin preciousstonesPlugin) {
        super(preciousstonesPlugin.getName());

    }

    public boolean isAllowed(Player player, Field field, MaskType type, boolean allowMember) {
        return field != null && (field.isOwner(player.getName()) || type == MaskType.MEMBER && allowMember && field.getAllAllowed().contains(player.getName()));
    }

    @Override
    public FaweMask getMask(com.sk89q.worldedit.entity.Player fp, MaskType type) {
        final Player player = BukkitAdapter.adapt(fp);
        final Location location = player.getLocation();
        final List<Field> fields = PreciousStones.API().getFieldsProtectingArea(FieldFlag.ALL, location);
        if (fields.isEmpty()) {
            return null;
        }
        String name = player.getName();
        boolean member = fp.hasPermission("fawe.preciousstones.member");
        for (Field myField : fields) {
            if (isAllowed(player, myField, type, member)) {
                BlockVector3 pos1 = BlockVector3.at(myField.getMinx(), myField.getMiny(), myField.getMinz());
                BlockVector3 pos2 = BlockVector3.at(myField.getMaxx(), myField.getMaxy(), myField.getMaxz());
                return new FaweMask(pos1, pos2) {
                    @Override
                    public boolean isValid(com.sk89q.worldedit.entity.Player player, MaskType type) {
                        return isAllowed(BukkitAdapter.adapt(player), myField, type, fp.hasPermission("fawe.preciousstones.member"));
                    }
                };
            }
        }
        return null;
    }
}
