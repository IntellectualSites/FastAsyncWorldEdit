package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;

/**
 * http://www.voxelwiki.com/minecraft/Voxelsniper#The_Entity_Brush
 *
 * @author Piotr
 */
public class EntityBrush extends Brush {
    private EntityType entityType = EntityType.ZOMBIE;

    /**
     *
     */
    public EntityBrush() {
        this.setName("Entity");
    }

    private void spawn(final SnipeData v) {
        for (int x = 0; x < v.getBrushSize(); x++) {
            try {
                this.getWorld().spawn(this.getLastBlock().getLocation(), this.entityType.getEntityClass());
            } catch (final IllegalArgumentException exception) {
                v.sendMessage(ChatColor.RED + "Cannot spawn entity!");
            }
        }
    }

    @Override
    protected final void arrow(final SnipeData v) {
        this.spawn(v);
    }

    @Override
    protected final void powder(final SnipeData v) {
        this.spawn(v);
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void info(final Message vm) {
        vm.brushMessage(ChatColor.LIGHT_PURPLE + "Entity brush" + " (" + this.entityType.getName() + ")");
        vm.size();
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        if (par[1].equalsIgnoreCase("info")) {
            String names = "";

            v.sendMessage(ChatColor.BLUE + "The available entity types are as follows:");
            for (final EntityType currentEntity : EntityType.values()) {

                names += ChatColor.AQUA + " | " + ChatColor.DARK_GREEN + currentEntity.getName();
            }
            names += ChatColor.AQUA + " |";
            v.sendMessage(names);
        } else {
            final EntityType currentEntity = EntityType.fromName(par[1]);
            if (currentEntity != null) {
                this.entityType = currentEntity;
                v.sendMessage(ChatColor.GREEN + "Entity type set to " + this.entityType.getName());
            } else {
                v.sendMessage(ChatColor.RED + "This is not a valid entity!");
            }
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.entity";
    }
}
