package com.boydti.fawe.bukkit.adapter;

import com.sk89q.worldedit.world.block.BlockID;
import java.util.function.Function;

public class NMSAdapter {
    public static int createPalette(int[] blockToPalette, int[] paletteToBlock, int[] blocksCopy, int[] num_palette_buffer, char[] set) {
        int air = 0;
        int num_palette = 0;
        for (int i = 0; i < 4096; i++) {
            char ordinal = set[i];
            switch (ordinal) {
                case 0:
                    ordinal = BlockID.AIR;
                case BlockID.AIR:
                case BlockID.CAVE_AIR:
                case BlockID.VOID_AIR:
                    air++;
            }
            int palette = blockToPalette[ordinal];
            if (palette == Integer.MAX_VALUE) {
                blockToPalette[ordinal] = palette = num_palette;
                paletteToBlock[num_palette] = ordinal;
                num_palette++;
            }
            blocksCopy[i] = palette;
        }
        num_palette_buffer[0] = num_palette;
        return air;
    }

    public static int createPalette(int layer, int[] blockToPalette, int[] paletteToBlock, int[] blocksCopy, int[] num_palette_buffer, Function<Integer, char[]> get, char[] set) {
        int air = 0;
        int num_palette = 0;
        int i = 0;
        outer:
        for (; i < 4096; i++) {
            char ordinal = set[i];
            switch (ordinal) {
                case BlockID.__RESERVED__:
                    break outer;
                case BlockID.AIR:
                case BlockID.CAVE_AIR:
                case BlockID.VOID_AIR:
                    air++;
            }
            int palette = blockToPalette[ordinal];
            if (palette == Integer.MAX_VALUE) {
                blockToPalette[ordinal] = palette = num_palette;
                paletteToBlock[num_palette] = ordinal;
                num_palette++;
            }
            blocksCopy[i] = palette;
        }
        if (i != 4096) {
            char[] getArr = get.apply(layer);
            for (; i < 4096; i++) {
                char ordinal = set[i];
                switch (ordinal) {
                    case BlockID.__RESERVED__:
                        ordinal = getArr[i];
                        switch (ordinal) {
                            case BlockID.__RESERVED__:
                                ordinal = BlockID.AIR;
                            case BlockID.AIR:
                            case BlockID.CAVE_AIR:
                            case BlockID.VOID_AIR:
                                air++;
                            default:
                                set[i] = ordinal;
                        }
                        break;
                    case BlockID.AIR:
                    case BlockID.CAVE_AIR:
                    case BlockID.VOID_AIR:
                        air++;
                }
                int palette = blockToPalette[ordinal];
                if (palette == Integer.MAX_VALUE) {
                    blockToPalette[ordinal] = palette = num_palette;
                    paletteToBlock[num_palette] = ordinal;
                    num_palette++;
                }
                blocksCopy[i] = palette;
            }
        }

        num_palette_buffer[0] = num_palette;
        return air;
    }
}
