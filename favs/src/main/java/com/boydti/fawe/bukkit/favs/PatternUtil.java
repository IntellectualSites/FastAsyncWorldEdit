package com.boydti.fawe.bukkit.favs;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.thevoxelbox.voxelsniper.SnipeData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PatternUtil {
    public static Pattern parsePattern(Player player, SnipeData snipeData, String arg) {
        ParserContext context = new ParserContext();
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        context.setActor(fp.getPlayer());
        context.setWorld(fp.getWorld());
        context.setSession(fp.getSession());
        try {
            Pattern pattern = WorldEdit.getInstance().getPatternFactory().parseFromInput(arg, context);
            snipeData.setPattern(pattern, arg);
            snipeData.sendMessage(ChatColor.GOLD + "Voxel: " + ChatColor.RED + arg);
            return pattern;
        } catch (InputParseException e) {
            fp.sendMessage(e.getMessage());
            return null;
        }
    }
}
