package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.SingleFilterBlock;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.image.ImageUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

// TODO FIXME
public class TextureUtil implements TextureHolder {

    private static final int[] FACTORS = new int[766];

    static {
        for (int i = 1; i < FACTORS.length; i++) {
            FACTORS[i] = 65535 / i;
        }
    }

    private final File folder;
    protected int[] blockColors = new int[BlockTypes.size()];
    protected long[] blockDistance = new long[BlockTypes.size()];
    protected long[] distances;
    protected int[] validColors;
    protected int[] validBlockIds;
    protected int[] validLayerColors;
    protected int[][] validLayerBlocks;
    protected int[] validMixBiomeColors;
    protected long[] validMixBiomeIds;
    /**
     * https://github.com/erich666/Mineways/blob/master/Win/biomes.cpp
     */
    protected BiomeColor[] validBiomes;
    private BiomeColor[] biomes = new BiomeColor[]{
        //    ID    Name             Temperature, rainfall, grass, foliage colors
        //    - note: the colors here are just placeholders, they are computed in the program
        new BiomeColor(0, "ocean", 0.5f, 0.5f, 0x92BD59, 0x77AB2F),
        // default values of temp and rain
        new BiomeColor(1, "plains", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(2, "desert", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
        new BiomeColor(3, "mountains", 0.2f, 0.3f, 0x92BD59, 0x77AB2F),
        new BiomeColor(4, "forest", 0.7f, 0.8f, 0x92BD59, 0x77AB2F),
        new BiomeColor(5, "taiga", 0.25f, 0.8f, 0x92BD59, 0x77AB2F),
        new BiomeColor(6, "swamp", 0.8f, 0.9f, 0x92BD59, 0x77AB2F),
        new BiomeColor(7, "river", 0.5f, 0.5f, 0x92BD59, 0x77AB2F),
        // default values of temp and rain
        new BiomeColor(8, "nether", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
        new BiomeColor(9, "the_end", 0.5f, 0.5f, 0x92BD59, 0x77AB2F),
        // default values of temp and rain
        new BiomeColor(10, "frozen_ocean", 0.0f, 0.5f, 0x92BD59, 0x77AB2F),
        new BiomeColor(11, "frozen_river", 0.0f, 0.5f, 0x92BD59, 0x77AB2F),
        new BiomeColor(12, "snowy_tundra", 0.0f, 0.5f, 0x92BD59, 0x77AB2F),
        new BiomeColor(13, "snowy_mountains", 0.0f, 0.5f, 0x92BD59, 0x77AB2F),
        new BiomeColor(14, "mushroom_fields", 0.9f, 1.0f, 0x92BD59, 0x77AB2F),
        new BiomeColor(15, "mushroom_field_shore", 0.9f, 1.0f, 0x92BD59, 0x77AB2F),
        new BiomeColor(16, "beach", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(17, "desert_hills", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
        new BiomeColor(18, "wooded_hills", 0.7f, 0.8f, 0x92BD59, 0x77AB2F),
        new BiomeColor(19, "taiga_hills", 0.25f, 0.8f, 0x92BD59, 0x77AB2F),
        new BiomeColor(20, "mountain_edge", 0.2f, 0.3f, 0x92BD59, 0x77AB2F),
        new BiomeColor(21, "jungle", 0.95f, 0.9f, 0x92BD59, 0x77AB2F),
        new BiomeColor(22, "jungle_hills", 0.95f, 0.9f, 0x92BD59, 0x77AB2F),
        new BiomeColor(23, "jungle_edge", 0.95f, 0.8f, 0x92BD59, 0x77AB2F),
        new BiomeColor(24, "deep_ocean", 0.5f, 0.5f, 0x92BD59, 0x77AB2F),
        new BiomeColor(25, "stone_shore", 0.2f, 0.3f, 0x92BD59, 0x77AB2F),
        new BiomeColor(26, "snowy_beach", 0.05f, 0.3f, 0x92BD59, 0x77AB2F),
        new BiomeColor(27, "birch_forest", 0.6f, 0.6f, 0x92BD59, 0x77AB2F),
        new BiomeColor(28, "birch_forest_hills", 0.6f, 0.6f, 0x92BD59, 0x77AB2F),
        new BiomeColor(29, "dark_forest", 0.7f, 0.8f, 0x92BD59, 0x77AB2F),
        new BiomeColor(30, "snowy_taiga", -0.5f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(31, "snowy_taiga_hills", -0.5f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(32, "giant_tree_taiga", 0.3f, 0.8f, 0x92BD59, 0x77AB2F),
        new BiomeColor(33, "giant_tree_taiga_hills", 0.3f, 0.8f, 0x92BD59, 0x77AB2F),
        new BiomeColor(34, "wooded_mountains", 0.2f, 0.3f, 0x92BD59, 0x77AB2F),
        new BiomeColor(35, "savanna", 1.2f, 0.0f, 0x92BD59, 0x77AB2F),
        new BiomeColor(36, "savanna_plateau", 1.0f, 0.0f, 0x92BD59, 0x77AB2F),
        new BiomeColor(37, "badlands", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
        new BiomeColor(38, "wooded_badlands_plateau", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
        new BiomeColor(39, "badlands_plateau", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
        new BiomeColor(40, "small_end_islands", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(41, "end_midlands", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(42, "end_highlands", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(43, "end_barrens", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(44, "warm_ocean", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(45, "lukewarm_ocean", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(46, "cold_ocean", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(47, "deep_warm_ocean", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(48, "deep_lukewarm_ocean", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(49, "deep_cold_ocean", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(50, "deep_frozen_ocean", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(51, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(52, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(53, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(54, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(55, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(56, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(57, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(58, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(59, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(60, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(61, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(62, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(63, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(64, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(65, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(66, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(67, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(68, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(69, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(70, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(71, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(72, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(73, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(74, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(75, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(76, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(77, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(78, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(79, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(80, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(81, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(82, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(83, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(84, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(85, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(86, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(87, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(88, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(89, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(90, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(91, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(92, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(93, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(94, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(95, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(96, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(97, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(98, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(99, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(100, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(101, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(102, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(103, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(104, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(105, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(106, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(107, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(108, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(109, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(110, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(111, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(112, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(113, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(114, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(115, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(116, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(117, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(118, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(119, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(120, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(121, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(122, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(123, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(124, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(125, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(126, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(127, "the_void", 0.5f, 0.5f, 0x92BD59, 0x77AB2F),
        // default values of temp and rain; also, no height differences
        new BiomeColor(128, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(129, "sunflower_plains", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(130, "desert_lakes", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
        new BiomeColor(131, "gravelly_mountains", 0.2f, 0.3f, 0x92BD59, 0x77AB2F),
        new BiomeColor(132, "flower_forest", 0.7f, 0.8f, 0x92BD59, 0x77AB2F),
        new BiomeColor(133, "taiga_mountains", 0.25f, 0.8f, 0x92BD59, 0x77AB2F),
        new BiomeColor(134, "swamp_hills", 0.8f, 0.9f, 0x92BD59, 0x77AB2F),
        new BiomeColor(135, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(136, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(137, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(138, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(139, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(140, "ice_spikes", 0.0f, 0.5f, 0x92BD59, 0x77AB2F),
        new BiomeColor(141, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(142, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(143, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(144, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(145, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(146, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(147, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(148, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(149, "modified_jungle", 0.95f, 0.9f, 0x92BD59, 0x77AB2F),
        new BiomeColor(150, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(151, "modified_jungle_edge", 0.95f, 0.8f, 0x92BD59, 0x77AB2F),
        new BiomeColor(152, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(153, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(154, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(155, "tall_birch_forest", 0.6f, 0.6f, 0x92BD59, 0x77AB2F),
        new BiomeColor(156, "tall_birch_hills", 0.6f, 0.6f, 0x92BD59, 0x77AB2F),
        new BiomeColor(157, "dark_forest_hills", 0.7f, 0.8f, 0x92BD59, 0x77AB2F),
        new BiomeColor(158, "snowy_taiga_mountains", -0.5f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(159, "Unknown", -0.5f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(160, "giant_spruce_taiga", 0.25f, 0.8f, 0x92BD59, 0x77AB2F),
        // special exception, temperature not 0.3
        new BiomeColor(161, "giant_spruce_taiga_hills", 0.25f, 0.8f, 0x92BD59, 0x77AB2F),
        new BiomeColor(162, "modified_gravelly_mountains", 0.2f, 0.3f, 0x92BD59, 0x77AB2F),
        new BiomeColor(163, "shattered_savanna", 1.1f, 0.0f, 0x92BD59, 0x77AB2F),
        new BiomeColor(164, "shattered_savanna_plateau", 1.0f, 0.0f, 0x92BD59, 0x77AB2F),
        new BiomeColor(165, "eroded_badlands", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
        new BiomeColor(166, "modified_wooded_badlands_plateau", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
        new BiomeColor(167, "modified_badlands_plateau", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
        new BiomeColor(168, "bamboo_jungle", 0.95f, 0.9f, 0x92BD59, 0x77AB2F),
        new BiomeColor(169, "bamboo_jungle_hills", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(170, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(171, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(172, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(173, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(174, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(175, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(176, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(177, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(178, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(179, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(180, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(181, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(182, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(183, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(184, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(185, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(186, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(187, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(188, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(189, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(190, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(191, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(192, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(193, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(194, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(195, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(196, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(197, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(198, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(199, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(200, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(201, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(202, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(203, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(204, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(205, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(206, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(207, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(208, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(209, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(210, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(211, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(212, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(213, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(214, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(215, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(216, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(217, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(218, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(219, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(220, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(221, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(222, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(223, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(224, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(225, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(226, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(227, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(228, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(229, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(230, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(231, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(232, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(233, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(234, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(235, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(236, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(237, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(238, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(239, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(240, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(241, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(242, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(243, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(244, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(245, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(246, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(247, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(248, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(249, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(250, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(251, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(252, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(253, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(254, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
        new BiomeColor(255, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),};
    private BlockType[] layerBuffer = new BlockType[2];

    public TextureUtil() throws FileNotFoundException {
        this(MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.TEXTURES));
    }

    public TextureUtil(File folder) throws FileNotFoundException {
        this.folder = folder;
        if (!folder.exists()) {
            throw new FileNotFoundException(
                "Please create a `FastAsyncWorldEdit/textures` folder with `.minecraft/versions` jar or mods in it.");
        }
    }

    public static TextureUtil fromClipboard(Clipboard clipboard) throws FileNotFoundException {
        boolean[] ids = new boolean[BlockTypes.size()];
        for (BlockVector3 pt : clipboard.getRegion()) {
            ids[clipboard.getBlock(pt).getInternalBlockTypeId()] = true;
        }
        HashSet<BlockType> blocks = new HashSet<>();
        for (int typeId = 0; typeId < ids.length; typeId++) {
            if (ids[typeId]) {
                blocks.add(BlockTypes.get(typeId));
            }
        }
        return fromBlocks(blocks);
    }

    public static TextureUtil fromBlocks(Set<BlockType> blocks) throws FileNotFoundException {
        return new FilteredTextureUtil(Fawe.get().getTextureUtil(), blocks);
    }

    public static TextureUtil fromMask(Mask mask) throws FileNotFoundException {
        HashSet<BlockType> blocks = new HashSet<>();

        SingleFilterBlock extent = new SingleFilterBlock();
        new MaskTraverser(mask).reset(extent);

        TextureUtil tu = Fawe.get().getTextureUtil();
        for (int typeId : tu.getValidBlockIds()) {
            BlockType block = BlockTypes.get(typeId);
            extent.init(0, 0, 0, block.getDefaultState().toBaseBlock());
            if (mask.test(extent)) {
                blocks.add(block);
            }
        }
        return fromBlocks(blocks);
    }

    protected static int hueDistance(int red1, int green1, int blue1, int red2, int green2,
        int blue2) {
        int total1 = (red1 + green1 + blue1);
        int total2 = (red2 + green2 + blue2);
        if (total1 == 0 || total2 == 0) {
            return 0;
        }
        int factor1 = FACTORS[total1];
        int factor2 = FACTORS[total2];
        long r = (512 * (red1 * factor1 - red2 * factor2)) >> 10;
        long g = (green1 * factor1 - green2 * factor2);
        long b = (767 * (blue1 * factor1 - blue2 * factor2)) >> 10;
        return (int) ((r * r + g * g + b * b) >> 25);
    }

    @Override
    public TextureUtil getTextureUtil() {
        return this;
    }

    public BlockType getNearestBlock(int color) {
        long min = Long.MAX_VALUE;
        int closest = 0;
        int red1 = (color >> 16) & 0xFF;
        int green1 = (color >> 8) & 0xFF;
        int blue1 = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;
        for (int i = 0; i < validColors.length; i++) {
            int other = validColors[i];
            if (((other >> 24) & 0xFF) == alpha) {
                long distance = colorDistance(red1, green1, blue1, other);
                if (distance < min) {
                    min = distance;
                    closest = validBlockIds[i];
                }
            }
        }
        if (min == Long.MAX_VALUE) {
            return null;
        }
        return BlockTypes.get(closest);
    }

    public BlockType getNearestBlock(BlockType block) {
        int color = getColor(block);
        if (color == 0) {
            return null;
        }
        return getNextNearestBlock(color);
    }

    public BlockType getNextNearestBlock(int color) {
        long min = Long.MAX_VALUE;
        int closest = 0;
        int red1 = (color >> 16) & 0xFF;
        int green1 = (color >> 8) & 0xFF;
        int blue1 = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;
        for (int i = 0; i < validColors.length; i++) {
            int other = validColors[i];
            if (other != color && ((other >> 24) & 0xFF) == alpha) {
                long distance = colorDistance(red1, green1, blue1, other);
                if (distance < min) {
                    min = distance;
                    closest = validBlockIds[i];
                }
            }
        }
        if (min == Long.MAX_VALUE) {
            return null;
        }
        return BlockTypes.get(closest);
    }

    /**
     * Returns the block combined ids as an array
     *
     * @param color
     * @return
     */
    public BlockType[] getNearestLayer(int color) {
        int[] closest = null;
        long min = Long.MAX_VALUE;
        int red1 = (color >> 16) & 0xFF;
        int green1 = (color >> 8) & 0xFF;
        int blue1 = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;
        for (int i = 0; i < validLayerColors.length; i++) {
            int other = validLayerColors[i];
            if (((other >> 24) & 0xFF) == alpha) {
                long distance = colorDistance(red1, green1, blue1, other);
                if (distance < min) {
                    min = distance;
                    closest = validLayerBlocks[i];
                }
            }
        }
        layerBuffer[0] = BlockTypes.get(closest[0]);
        layerBuffer[1] = BlockTypes.get(closest[1]);
        return layerBuffer;
    }

    public BlockType getLighterBlock(BlockType block) {
        return getNearestBlock(block, false);
    }

    public BlockType getDarkerBlock(BlockType block) {
        return getNearestBlock(block, true);
    }

    public int getColor(BlockType block) {
        return blockColors[block.getInternalId()];
    }

    public BiomeColor getBiome(int biome) {
        return biomes[biome];
    }

    public boolean getIsBlockCloserThanBiome(int[] blockAndBiomeIdOutput, int color,
        int biomePriority) {
        BlockType block = getNearestBlock(color);
        TextureUtil.BiomeColor biome = getNearestBiome(color);
        int blockColor = getColor(block);
        blockAndBiomeIdOutput[0] = block.getDefaultState().getOrdinalChar();
        blockAndBiomeIdOutput[1] = biome.id;
        if (colorDistance(biome.grassCombined, color) - biomePriority > colorDistance(blockColor,
            color)) {
            return true;
        }
        return false;
    }

    public int getBiomeMix(int[] biomeIdsOutput, int color) {
        long closest = Long.MAX_VALUE;
        int closestAverage = Integer.MAX_VALUE;
        long min = Long.MAX_VALUE;
        int red1 = (color >> 16) & 0xFF;
        int green1 = (color >> 8) & 0xFF;
        int blue1 = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;
        for (int i = 0; i < validMixBiomeColors.length; i++) {
            int other = validMixBiomeColors[i];
            if (((other >> 24) & 0xFF) == alpha) {
                long distance = colorDistance(red1, green1, blue1, other);
                if (distance < min) {
                    min = distance;
                    closest = validMixBiomeIds[i];
                    closestAverage = other;
                }
            }
        }
        biomeIdsOutput[0] = (int) ((closest >> 0) & 0xFF);
        biomeIdsOutput[1] = (int) ((closest >> 8) & 0xFF);
        biomeIdsOutput[2] = (int) ((closest >> 16) & 0xFF);
        return closestAverage;
    }

    public BiomeColor getNearestBiome(int color) {
        int grass = blockColors[BlockTypes.GRASS_BLOCK.getInternalId()];
        if (grass == 0) {
            return null;
        }
        BiomeColor closest = null;
        long min = Long.MAX_VALUE;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = (color >> 0) & 0xFF;
        for (BiomeColor biome : validBiomes) {
            long distance = colorDistance(red, green, blue, biome.grassCombined);
            if (distance < min) {
                min = distance;
                closest = biome;
            }
        }
        return closest;
    }

    public File getFolder() {
        return folder;
    }

    public long colorDistance(int c1, int c2) {
        int red1 = (c1 >> 16) & 0xFF;
        int green1 = (c1 >> 8) & 0xFF;
        int blue1 = (c1 >> 0) & 0xFF;
        return colorDistance(red1, green1, blue1, c2);
    }

    private BufferedImage readImage(ZipFile zipFile, String name) throws IOException {
        ZipEntry entry = getEntry(zipFile, name);
        if (entry != null) {
            try (InputStream is = zipFile.getInputStream(entry)) {
                return ImageIO.read(is);
            }
        }
        return null;
    }

    private ZipEntry getEntry(ZipFile file, String path) {
        ZipEntry entry = file.getEntry(path);
        if (entry == null) {
            path = path.replace("/", File.separator);
            entry = file.getEntry(path);
        }
        return entry;
    }

    public void loadModTextures() throws IOException {
        Int2ObjectOpenHashMap<Integer> colorMap = new Int2ObjectOpenHashMap<>();
        Int2ObjectOpenHashMap<Long> distanceMap = new Int2ObjectOpenHashMap<>();
        Gson gson = new Gson();
        if (folder.exists()) {
            // Get all the jar files
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".jar"));
            for (BlockType blockType : BlockTypes.values) {
                BlockMaterial material = blockType.getMaterial();
                if (!material.isSolid() || !material.isFullCube()) {
                    continue;
                }
                int color = material.getMapColor();
                if (color != 0) {
                    colorMap.put(blockType.getInternalId(), (Integer) color);
                }
            }
            if (files.length == 0) {
                Fawe.debug(
                    "Please create a `FastAsyncWorldEdit/textures` folder with `.minecraft/versions/1.13.jar` jar or mods in it. If the file exists, please make sure the server has read access to the directory");
            } else {
                for (File file : files) {
                    ZipFile zipFile = new ZipFile(file);

                    // Get all the groups in the current jar
                    // The vanilla textures are in `assets/minecraft`
                    // A jar may contain textures for multiple mods
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    Set<String> mods = new HashSet<>();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        String name = entry.getName();
                        Path path = Paths.get(name);
                        if (path.startsWith("assets" + File.separator)) {
                            String[] split =
                                path.toString().split(Pattern.quote(File.separator));
                            if (split.length > 1) {
                                String modId = split[1];
                                mods.add(modId);
                            }
                        }
                        continue;
                    }
                    String modelsDir = "assets/%1$s/models/block/%2$s.json";
                    String texturesDir = "assets/%1$s/textures/%2$s.png";

                    Type typeToken = new TypeToken<Map<String, Object>>() {
                    }.getType();

                    for (BlockType blockType : BlockTypes.values) {
                        if (!blockType.getMaterial().isFullCube()) {
                            continue;
                        }
                        int combined = blockType.getInternalId();
                        String id = blockType.getId();
                        String[] split = id.split(":", 2);
                        String name = split.length == 1 ? id : split[1];
                        String nameSpace = split.length == 1 ? "minecraft" : split[0];

                        Map<String, String> texturesMap = new ConcurrentHashMap<>();
                        // Read models
                        String modelFileName = String.format(modelsDir, nameSpace, name);
                        ZipEntry entry = getEntry(zipFile, modelFileName);
                        if (entry == null) {
                            System.out.println("Cannot find " + modelFileName + " in " + file);
                            continue;
                        }

                        String textureFileName;
                        try (InputStream is = zipFile.getInputStream(entry)) {
                            JsonReader reader = new JsonReader(
                                new InputStreamReader(is, StandardCharsets.UTF_8));
                            Map<String, Object> root = gson.fromJson(reader, typeToken);
                            Map<String, Object> textures = (Map) root.get("textures");

                            if (textures == null) {
                                continue;
                            }
                            Set<String> models = new HashSet<>();
                            // Get models
                            for (Map.Entry<String, Object> stringObjectEntry : textures
                                .entrySet()) {
                                Object value = stringObjectEntry.getValue();
                                if (value instanceof String) {
                                    models.add((String) value);
                                } else if (value instanceof Map) {
                                    value = ((Map) value).get("model");
                                    if (value != null) {
                                        models.add((String) value);
                                    }
                                }
                            }
                            if (models.size() != 1) {
                                continue;
                            }

                            textureFileName =
                                String.format(texturesDir, nameSpace, models.iterator().next());
                        }

                        BufferedImage image = readImage(zipFile, textureFileName);
                        if (image == null) {
                            System.out.println("Cannot find " + textureFileName);
                            continue;
                        }
                        int color = ImageUtil.getColor(image);
                        long distance = getDistance(image, color);
                        distanceMap.put(combined, (Long) distance);
                        colorMap.put(combined, (Integer) color);
                    }
                    Integer grass = null;
                    {
                        String grassFileName =
                            String.format(texturesDir, "minecraft", "grass_block_top");
                        BufferedImage image = readImage(zipFile, grassFileName);
                        if (image != null) {
                            grass = ImageUtil.getColor(image);
                        }
                    }
                    if (grass != null) {
                        // assets\minecraft\textures\colormap
                        ZipEntry grassEntry = getEntry(zipFile,
                            "assets/minecraft/textures/colormap/grass_block.png");
                        if (grassEntry != null) {
                            try (InputStream is = zipFile.getInputStream(grassEntry)) {
                                BufferedImage image = ImageIO.read(is);
                                // Update biome colors
                                for (BiomeColor biome : biomes) {
                                    float adjTemp =
                                        MathMan.clamp(biome.temperature, 0.0f, 1.0f);
                                    float adjRainfall =
                                        MathMan.clamp(biome.rainfall, 0.0f, 1.0f) * adjTemp;
                                    int x = (int) (255 - adjTemp * 255);
                                    int z = (int) (255 - adjRainfall * 255);
                                    biome.grass = image.getRGB(x, z);
                                }
                            }
                            // swampland: perlin - avoid
                            biomes[6].grass = 0;
                            biomes[134].grass = 0;
                            // roofed forest: averaged w/ 0x28340A
                            biomes[29].grass =
                                multiplyColor(biomes[29].grass, 0x28340A + (255 << 24));
                            biomes[157].grass =
                                multiplyColor(biomes[157].grass, 0x28340A + (255 << 24));
                            // mesa : 0x90814D
                            biomes[37].grass = 0x90814D + (255 << 24);
                            biomes[38].grass = 0x90814D + (255 << 24);
                            biomes[39].grass = 0x90814D + (255 << 24);
                            biomes[165].grass = 0x90814D + (255 << 24);
                            biomes[166].grass = 0x90814D + (255 << 24);
                            biomes[167].grass = 0x90814D + (255 << 24);
                            List<BiomeColor> valid = new ArrayList<>();
                            for (BiomeColor biome : biomes) {
                                //                                biome.grass = multiplyColor(biome.grass, grass);
                                if (biome.grass != 0 && !biome.name
                                    .equalsIgnoreCase("Unknown Biome")) {
                                    valid.add(biome);
                                }
                                biome.grassCombined = multiplyColor(grass, biome.grass);
                            }
                            this.validBiomes = valid.toArray(new BiomeColor[0]);

                            ArrayList<BiomeColor> uniqueColors = new ArrayList<>();
                            Set<Integer> uniqueBiomesColors = new IntArraySet();
                            for (BiomeColor color : validBiomes) {
                                if (uniqueBiomesColors.add(color.grass)) {
                                    uniqueColors.add(color);
                                }
                            }
                            int count = 0;
                            int count2 = 0;
                            uniqueBiomesColors.clear();

                            LongArrayList layerIds = new LongArrayList();
                            LongArrayList layerColors = new LongArrayList();
                            for (int i = 0; i < uniqueColors.size(); i++) {
                                for (int j = i; j < uniqueColors.size(); j++) {
                                    for (int k = j; k < uniqueColors.size(); k++) {
                                        BiomeColor c1 = uniqueColors.get(i);
                                        BiomeColor c2 = uniqueColors.get(j);
                                        BiomeColor c3 = uniqueColors.get(k);
                                        int average =
                                            averageColor(c1.grass, c2.grass, c3.grass);
                                        if (uniqueBiomesColors.add(average)) {
                                            count++;
                                            layerColors.add((long) average);
                                            layerIds.add(
                                                (long) ((c1.id) + (c2.id << 8) + (c3.id
                                                    << 16)));
                                        }
                                    }
                                }
                            }

                            validMixBiomeColors = new int[layerColors.size()];
                            for (int i = 0; i < layerColors.size(); i++) {
                                validMixBiomeColors[i] = (int) layerColors.getLong(i);
                            }
                            validMixBiomeIds = layerIds.toLongArray();
                        }

                    }
                    //                 Close the file
                    zipFile.close();
                }
            }
        }
        // Convert the color map to a simple array
        validBlockIds = new int[colorMap.size()];
        validColors = new int[colorMap.size()];
        int index = 0;
        for (Int2ObjectMap.Entry<Integer> entry : colorMap.int2ObjectEntrySet()) {
            int combinedId = entry.getIntKey();
            int color = entry.getValue();
            blockColors[combinedId] = color;
            validBlockIds[index] = combinedId;
            validColors[index] = color;
            index++;
        }
        ArrayList<Long> distances = new ArrayList<>(distanceMap.values());
        Collections.sort(distances);
        this.distances = new long[distances.size()];
        for (int i = 0; i < this.distances.length; i++) {
            this.distances[i] = distances.get(i);
        }
        for (Int2ObjectMap.Entry<Long> entry : distanceMap.int2ObjectEntrySet()) {
            blockDistance[entry.getIntKey()] = entry.getValue();
        }
        calculateLayerArrays();
    }

    public int multiplyColor(int c1, int c2) {
        int alpha1 = (c1 >> 24) & 0xFF;
        int alpha2 = (c2 >> 24) & 0xFF;
        int red1 = (c1 >> 16) & 0xFF;
        int green1 = (c1 >> 8) & 0xFF;
        int blue1 = (c1 >> 0) & 0xFF;
        int red2 = (c2 >> 16) & 0xFF;
        int green2 = (c2 >> 8) & 0xFF;
        int blue2 = (c2 >> 0) & 0xFF;
        int red = ((red1 * red2)) / 255;
        int green = ((green1 * green2)) / 255;
        int blue = ((blue1 * blue2)) / 255;
        int alpha = ((alpha1 * alpha2)) / 255;
        return (alpha << 24) + (red << 16) + (green << 8) + (blue << 0);
    }

    public int averageColor(int c1, int c2) {
        int alpha1 = (c1 >> 24) & 0xFF;
        int alpha2 = (c2 >> 24) & 0xFF;
        int red1 = (c1 >> 16) & 0xFF;
        int green1 = (c1 >> 8) & 0xFF;
        int blue1 = (c1 >> 0) & 0xFF;
        int red2 = (c2 >> 16) & 0xFF;
        int green2 = (c2 >> 8) & 0xFF;
        int blue2 = (c2 >> 0) & 0xFF;
        int red = ((red1 + red2)) >> 1;
        int green = ((green1 + green2)) >> 1;
        int blue = ((blue1 + blue2)) >> 1;
        int alpha = ((alpha1 + alpha2)) >> 1;
        return (alpha << 24) + (red << 16) + (green << 8) + (blue << 0);
    }

    public int averageColor(int... colors) {
        int alpha = 0;
        int red = 0;
        int green = 0;
        int blue = 0;
        for (int c : colors) {
            alpha += (c >> 24) & 0xFF;
            red += (c >> 16) & 0xFF;
            green += (c >> 8) & 0xFF;
            blue += (c >> 0) & 0xFF;
        }
        int num = colors.length;
        alpha /= num;
        red /= num;
        green /= num;
        blue /= num;
        return (alpha << 24) + (red << 16) + (green << 8) + (blue << 0);
    }

    /**
     * Assumes the top layer is a transparent color and the bottom is opaque
     */
    public int combineTransparency(int top, int bottom) {
        int alpha1 = (top >> 24) & 0xFF;
        int alpha2 = 255 - alpha1;
        int red1 = (top >> 16) & 0xFF;
        int green1 = (top >> 8) & 0xFF;
        int blue1 = (top >> 0) & 0xFF;
        int red2 = (bottom >> 16) & 0xFF;
        int green2 = (bottom >> 8) & 0xFF;
        int blue2 = (bottom >> 0) & 0xFF;
        int red = ((red1 * alpha1) + (red2 * alpha2)) / 255;
        int green = ((green1 * alpha1) + (green2 * alpha2)) / 255;
        int blue = ((blue1 * alpha1) + (blue2 * alpha2)) / 255;
        return (red << 16) + (green << 8) + (blue << 0) + (255 << 24);
    }

    protected void calculateLayerArrays() {
        Int2ObjectOpenHashMap<int[]> colorLayerMap = new Int2ObjectOpenHashMap<>();
        for (int i = 0; i < validBlockIds.length; i++) {
            int color = validColors[i];
            int combined = validBlockIds[i];
            if (hasAlpha(color)) {
                for (int j = 0; j < validBlockIds.length; j++) {
                    int colorOther = validColors[j];
                    if (!hasAlpha(colorOther)) {
                        int combinedOther = validBlockIds[j];
                        int combinedColor = combineTransparency(color, colorOther);
                        colorLayerMap.put(combinedColor, new int[]{combined, combinedOther});
                    }
                }
            }
        }
        this.validLayerColors = new int[colorLayerMap.size()];
        this.validLayerBlocks = new int[colorLayerMap.size()][];
        int index = 0;
        for (Int2ObjectMap.Entry<int[]> entry : colorLayerMap.int2ObjectEntrySet()) {
            validLayerColors[index] = entry.getIntKey();
            validLayerBlocks[index++] = entry.getValue();
        }
    }

    protected BlockType getNearestBlock(BlockType block, boolean darker) {
        int color = getColor(block);
        if (color == 0) {
            return block;
        }
        BlockType darkerBlock = getNearestBlock(color, darker);
        return darkerBlock != null ? darkerBlock : block;
    }

    protected BlockType getNearestBlock(int color, boolean darker) {
        long min = Long.MAX_VALUE;
        int closest = 0;
        int red1 = (color >> 16) & 0xFF;
        int green1 = (color >> 8) & 0xFF;
        int blue1 = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;
        int intensity1 = 2 * red1 + 4 * green1 + 3 * blue1;
        for (int i = 0; i < validColors.length; i++) {
            int other = validColors[i];
            if (other != color && ((other >> 24) & 0xFF) == alpha) {
                int red2 = (other >> 16) & 0xFF;
                int green2 = (other >> 8) & 0xFF;
                int blue2 = (other >> 0) & 0xFF;
                int intensity2 = 2 * red2 + 4 * green2 + 3 * blue2;
                if (darker ? intensity2 >= intensity1 : intensity1 >= intensity2) {
                    continue;
                }
                long distance = colorDistance(red1, green1, blue1, other);
                if (distance < min) {
                    min = distance;
                    closest = validBlockIds[i];
                }
            }
        }
        if (min == Long.MAX_VALUE) {
            return null;
        }
        return BlockTypes.get(closest);
    }

    private String getFileName(String path) {
        String[] split = path.split("[/|\\\\]");
        String name = split[split.length - 1];
        int dot = name.indexOf('.');
        if (dot != -1) {
            name = name.substring(0, dot);
        }
        return name;
    }

    private String alphabetize(String asset) {
        String[] split = asset.split("_");
        Arrays.sort(split);
        return StringMan.join(split, "_");
    }

    protected boolean hasAlpha(int color) {
        int alpha = (color >> 24) & 0xFF;
        return alpha != 255;
    }

    protected long colorDistance(int red1, int green1, int blue1, int c2) {
        int red2 = (c2 >> 16) & 0xFF;
        int green2 = (c2 >> 8) & 0xFF;
        int blue2 = (c2 >> 0) & 0xFF;
        int rmean = (red1 + red2) >> 1;
        int r = red1 - red2;
        int g = green1 - green2;
        int b = blue1 - blue2;
        int hd = hueDistance(red1, green1, blue1, red2, green2, blue2);
        return (((512 + rmean) * r * r) >> 8) + 4 * g * g + (((767 - rmean) * b * b) >> 8) + (hd
            * hd);
    }

    public long getDistance(BufferedImage image, int c1) {
        long totalDistSqr = 0;
        int width = image.getWidth();
        int height = image.getHeight();
        int area = width * height;
        int red1 = (c1 >> 16) & 0xFF;
        int green1 = (c1 >> 8) & 0xFF;
        int blue1 = (c1 >> 0) & 0xFF;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int c2 = image.getRGB(x, y);
                long distance = colorDistance(red1, green1, blue1, c2);
                totalDistSqr += distance * distance;
            }
        }
        return totalDistSqr / area;
    }

    public int[] getValidBlockIds() {
        return validBlockIds.clone();
    }

    public static class BiomeColor {

        public int id;
        public String name;
        public float temperature;
        public float rainfall;
        public int grass;
        public int grassCombined;
        public int foliage;

        public BiomeColor(int id, String name, float temperature, float rainfall, int grass,
            int foliage) {
            this.id = id;
            this.name = name;
            this.temperature = temperature;
            this.rainfall = rainfall;
            this.grass = grass;
            this.grassCombined = grass;
            this.foliage = foliage;
        }
    }
}
