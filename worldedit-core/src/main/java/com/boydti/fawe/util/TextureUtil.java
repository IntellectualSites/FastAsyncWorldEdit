package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.pattern.PatternExtent;
import com.boydti.fawe.util.image.ImageUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockMaterial;
import com.sk89q.worldedit.util.command.binding.Text;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// TODO FIXME
public class TextureUtil implements TextureHolder{

    public static TextureUtil fromClipboard(Clipboard clipboard) throws FileNotFoundException {
        boolean[] ids = new boolean[BlockTypes.size()];
        for (com.sk89q.worldedit.Vector pt : clipboard.getRegion()) {
            ids[clipboard.getBlock(pt).getInternalBlockTypeId()] = true;
        }
        HashSet<BlockType> blocks = new HashSet<>();
        for (int typeId = 0; typeId < ids.length; typeId++) {
            if (ids[typeId]) blocks.add(BlockTypes.get(typeId));
        }
        return fromBlocks(blocks);
    }

    public static TextureUtil fromBlocks(Set<BlockType> blocks) throws FileNotFoundException {
        return new FilteredTextureUtil(Fawe.get().getTextureUtil(), blocks);
    }

    public static TextureUtil fromMask(Mask mask) throws FileNotFoundException {
        HashSet<BlockType> blocks = new HashSet<>();
        BlockPattern pattern = new BlockPattern(BlockTypes.AIR.getDefaultState());
        PatternExtent extent = new PatternExtent(pattern);
        new MaskTraverser(mask).reset(extent);
        TextureUtil tu = Fawe.get().getTextureUtil();
        for (int typeId : tu.getValidBlockIds()) {
            BlockType block = BlockTypes.get(typeId);
            pattern.setBlock(block.getDefaultState());
            if (mask.test(Vector.ZERO)) blocks.add(block);
        }
        return fromBlocks(blocks);
    }

    @Override
    public TextureUtil getTextureUtil() {
        return this;
    }

    private final File folder;
    private static final int[] FACTORS = new int[766];

    static {
        for (int i = 1; i < FACTORS.length; i++) {
            FACTORS[i] = 65535 / i;
        }
    }

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
            new BiomeColor(0, "Ocean", 0.5f, 0.5f, 0x92BD59, 0x77AB2F),    // default values of temp and rain
            new BiomeColor(1, "Plains", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(2, "Desert", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
            new BiomeColor(3, "Extreme Hills", 0.2f, 0.3f, 0x92BD59, 0x77AB2F),
            new BiomeColor(4, "Forest", 0.7f, 0.8f, 0x92BD59, 0x77AB2F),
            new BiomeColor(5, "Taiga", 0.25f, 0.8f, 0x92BD59, 0x77AB2F),
            new BiomeColor(6, "Swampland", 0.8f, 0.9f, 0x92BD59, 0x77AB2F),
            new BiomeColor(7, "River", 0.5f, 0.5f, 0x92BD59, 0x77AB2F),    // default values of temp and rain
            new BiomeColor(8, "Nether", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
            new BiomeColor(9, "End", 0.5f, 0.5f, 0x92BD59, 0x77AB2F),    // default values of temp and rain
            new BiomeColor(10, "Frozen Ocean", 0.0f, 0.5f, 0x92BD59, 0x77AB2F),
            new BiomeColor(11, "Frozen River", 0.0f, 0.5f, 0x92BD59, 0x77AB2F),
            new BiomeColor(12, "Ice Plains", 0.0f, 0.5f, 0x92BD59, 0x77AB2F),
            new BiomeColor(13, "Ice Mountains", 0.0f, 0.5f, 0x92BD59, 0x77AB2F),
            new BiomeColor(14, "Mushroom Island", 0.9f, 1.0f, 0x92BD59, 0x77AB2F),
            new BiomeColor(15, "Mushroom Island Shore", 0.9f, 1.0f, 0x92BD59, 0x77AB2F),
            new BiomeColor(16, "Beach", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(17, "Desert Hills", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
            new BiomeColor(18, "Forest Hills", 0.7f, 0.8f, 0x92BD59, 0x77AB2F),
            new BiomeColor(19, "Taiga Hills", 0.25f, 0.8f, 0x92BD59, 0x77AB2F),
            new BiomeColor(20, "Extreme Hills Edge", 0.2f, 0.3f, 0x92BD59, 0x77AB2F),
            new BiomeColor(21, "Jungle", 0.95f, 0.9f, 0x92BD59, 0x77AB2F),
            new BiomeColor(22, "Jungle Hills", 0.95f, 0.9f, 0x92BD59, 0x77AB2F),
            new BiomeColor(23, "Jungle Edge", 0.95f, 0.8f, 0x92BD59, 0x77AB2F),
            new BiomeColor(24, "Deep Ocean", 0.5f, 0.5f, 0x92BD59, 0x77AB2F),
            new BiomeColor(25, "Stone Beach", 0.2f, 0.3f, 0x92BD59, 0x77AB2F),
            new BiomeColor(26, "Cold Beach", 0.05f, 0.3f, 0x92BD59, 0x77AB2F),
            new BiomeColor(27, "Birch Forest", 0.6f, 0.6f, 0x92BD59, 0x77AB2F),
            new BiomeColor(28, "Birch Forest Hills", 0.6f, 0.6f, 0x92BD59, 0x77AB2F),
            new BiomeColor(29, "Roofed Forest", 0.7f, 0.8f, 0x92BD59, 0x77AB2F),
            new BiomeColor(30, "Cold Taiga", -0.5f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(31, "Cold Taiga Hills", -0.5f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(32, "Mega Taiga", 0.3f, 0.8f, 0x92BD59, 0x77AB2F),
            new BiomeColor(33, "Mega Taiga Hills", 0.3f, 0.8f, 0x92BD59, 0x77AB2F),
            new BiomeColor(34, "Extreme Hills+", 0.2f, 0.3f, 0x92BD59, 0x77AB2F),
            new BiomeColor(35, "Savanna", 1.2f, 0.0f, 0x92BD59, 0x77AB2F),
            new BiomeColor(36, "Savanna Plateau", 1.0f, 0.0f, 0x92BD59, 0x77AB2F),
            new BiomeColor(37, "Mesa", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
            new BiomeColor(38, "Mesa Plateau F", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
            new BiomeColor(39, "Mesa Plateau", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
            new BiomeColor(40, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(41, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(42, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(43, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(44, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(45, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(46, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(47, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(48, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(49, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(50, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
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
            new BiomeColor(127, "The Void", 0.5f, 0.5f, 0x92BD59, 0x77AB2F),    // default values of temp and rain; also, no height differences
            new BiomeColor(128, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(129, "Sunflower Plains", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(130, "Desert M", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
            new BiomeColor(131, "Extreme Hills M", 0.2f, 0.3f, 0x92BD59, 0x77AB2F),
            new BiomeColor(132, "Flower Forest", 0.7f, 0.8f, 0x92BD59, 0x77AB2F),
            new BiomeColor(133, "Taiga M", 0.25f, 0.8f, 0x92BD59, 0x77AB2F),
            new BiomeColor(134, "Swampland M", 0.8f, 0.9f, 0x92BD59, 0x77AB2F),
            new BiomeColor(135, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(136, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(137, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(138, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(139, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(140, "Ice Plains Spikes", 0.0f, 0.5f, 0x92BD59, 0x77AB2F),
            new BiomeColor(141, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(142, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(143, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(144, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(145, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(146, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(147, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(148, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(149, "Jungle M", 0.95f, 0.9f, 0x92BD59, 0x77AB2F),
            new BiomeColor(150, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(151, "JungleEdge M", 0.95f, 0.8f, 0x92BD59, 0x77AB2F),
            new BiomeColor(152, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(153, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(154, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(155, "Birch Forest M", 0.6f, 0.6f, 0x92BD59, 0x77AB2F),
            new BiomeColor(156, "Birch Forest Hills M", 0.6f, 0.6f, 0x92BD59, 0x77AB2F),
            new BiomeColor(157, "Roofed Forest M", 0.7f, 0.8f, 0x92BD59, 0x77AB2F),
            new BiomeColor(158, "Cold Taiga M", -0.5f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(159, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(160, "Mega Spruce Taiga", 0.25f, 0.8f, 0x92BD59, 0x77AB2F),    // special exception, temperature not 0.3
            new BiomeColor(161, "Mega Spruce Taiga Hills", 0.25f, 0.8f, 0x92BD59, 0x77AB2F),
            new BiomeColor(162, "Extreme Hills+ M", 0.2f, 0.3f, 0x92BD59, 0x77AB2F),
            new BiomeColor(163, "Savanna M", 1.1f, 0.0f, 0x92BD59, 0x77AB2F),
            new BiomeColor(164, "Savanna Plateau M", 1.0f, 0.0f, 0x92BD59, 0x77AB2F),
            new BiomeColor(165, "Mesa (Bryce)", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
            new BiomeColor(166, "Mesa Plateau F M", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
            new BiomeColor(167, "Mesa Plateau M", 2.0f, 0.0f, 0x92BD59, 0x77AB2F),
            new BiomeColor(168, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
            new BiomeColor(169, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
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
            new BiomeColor(255, "Unknown Biome", 0.8f, 0.4f, 0x92BD59, 0x77AB2F),
    };

    public TextureUtil() throws FileNotFoundException {
        this(MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.TEXTURES));
    }

    public TextureUtil(File folder) throws FileNotFoundException {
        this.folder = folder;
        if (!folder.exists()) {
            throw new FileNotFoundException("Please create a `FastAsyncWorldEdit/textures` folder with `.minecraft/versions` jar or mods in it.");
        }
    }

    public BlockTypes getNearestBlock(int color) {
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
        if (min == Long.MAX_VALUE) return null;
        return BlockTypes.get(closest);
    }

    public BlockType getNearestBlock(BlockType block) {
        int color = getColor(block);
        if (color == 0) return null;
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
        if (min == Long.MAX_VALUE) return null;
        return BlockTypes.get(closest);
    }

    private BlockTypes[] layerBuffer = new BlockTypes[2];

    /**
     * Returns the block combined ids as an array
     *
     * @param color
     * @return
     */
    public BlockTypes[] getNearestLayer(int color) {
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

    public boolean getIsBlockCloserThanBiome(int[] blockAndBiomeIdOutput, int color, int biomePriority) {
        BlockType block = getNearestBlock(color);
        TextureUtil.BiomeColor biome = getNearestBiome(color);
        int blockColor = getColor(block);
        blockAndBiomeIdOutput[0] = block.getInternalId();
        blockAndBiomeIdOutput[1] = biome.id;
        if (colorDistance(biome.grassCombined, color) - biomePriority > colorDistance(blockColor, color)) {
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
        for (int i = 0; i < validBiomes.length; i++) {
            BiomeColor biome = validBiomes[i];
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
            File[] files = folder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            });
            for (BlockType blockType : BlockTypes.values) {
                BlockMaterial material = blockType.getMaterial();
                if (!material.isSolid() || !material.isFullCube()) continue;
                int color = material.getMapColor();
                if (color != 0) {
                    colorMap.put((int) blockType.getInternalId(), (Integer) color);
                }
            }
            if (files.length == 0) {
                Fawe.debug("Please create a `FastAsyncWorldEdit/textures` folder with `.minecraft/versions/1.13.jar` jar or mods in it. If the file exists, please make sure the server has read access to the directory");
            } else {
                for (File file : files) {
                    ZipFile zipFile = new ZipFile(file);

                    // Get all the groups in the current jar
                    // The vanilla textures are in `assets/minecraft`
                    // A jar may contain textures for multiple mods
                    Set<String> mods = new HashSet<String>();
                    {
                        Enumeration<? extends ZipEntry> entries = zipFile.entries();
                        while (entries.hasMoreElements()) {
                            ZipEntry entry = entries.nextElement();
                            String name = entry.getName();
                            Path path = Paths.get(name);
                            if (path.startsWith("assets" + File.separator)) {
                                String[] split = path.toString().split(Pattern.quote(File.separator));
                                if (split.length > 1) {
                                    String modId = split[1];
                                    mods.add(modId);
                                }
                            }
                            continue;
                        }
                    }
                    String modelsDir = "assets/%1$s/models/block/%2$s.json";
                    String texturesDir = "assets/%1$s/textures/%2$s.png";

                    Type typeToken = new TypeToken<Map<String, Object>>() {
                    }.getType();

                    for (BlockType blockType : BlockTypes.values) {
                        if (!blockType.getMaterial().isFullCube()) continue;
                        int combined = blockType.getInternalId();
                        String id = blockType.getId();
                        String[] split = id.split(":", 2);
                        String name = split.length == 1 ? id : split[1];
                        String nameSpace = split.length == 1 ? "minecraft" : split[0];

                        Map<String, String> texturesMap = new ConcurrentHashMap<>();
                        { // Read models
                            String modelFileName = String.format(modelsDir, nameSpace, name);
                            ZipEntry entry = getEntry(zipFile, modelFileName);
                            if (entry == null) {
                                System.out.println("Cannot find " + modelFileName + " in " + file);
                                continue;
                            }

                            String textureFileName;
                            try (InputStream is = zipFile.getInputStream(entry)) {
                                JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
                                Map<String, Object> root = gson.fromJson(reader, typeToken);
                                Map<String, Object> textures = (Map) root.get("textures");

                                if (textures == null) continue;
                                Set<String> models = new HashSet<>();
                                // Get models
                                for (Map.Entry<String, Object> stringObjectEntry : textures.entrySet()) {
                                    Object value = stringObjectEntry.getValue();
                                    if (value instanceof String) {
                                        models.add((String) value);
                                    } else if (value instanceof Map) {
                                        value = ((Map) value).get("model");
                                        if (value != null) models.add((String) value);
                                    }
                                }
                                if (models.size() != 1) continue;

                                textureFileName = String.format(texturesDir, nameSpace, models.iterator().next());
                            }

                            BufferedImage image = readImage(zipFile, textureFileName);
                            if (image == null) {
                                System.out.println("Cannot find " + textureFileName);
                                continue;
                            }
                            int color = ImageUtil.getColor(image);
                            long distance = getDistance(image, color);
                            distanceMap.put((int) combined, (Long) distance);
                            colorMap.put((int) combined, (Integer) color);
                        }
                    }
                    {
                        Integer grass = null;
                        {
                            String grassFileName = String.format(texturesDir, "minecraft", "grass_block_top");
                            BufferedImage image = readImage(zipFile, grassFileName);
                            if (image != null) {
                                grass = ImageUtil.getColor(image);
                            }
                        }
                        if (grass != null) {
                            // assets\minecraft\textures\colormap
                            ZipEntry grassEntry = getEntry(zipFile, "assets/minecraft/textures/colormap/grass_block.png");
                            if (grassEntry != null) {
                                try (InputStream is = zipFile.getInputStream(grassEntry)) {
                                    BufferedImage image = ImageIO.read(is);
                                    // Update biome colors
                                    for (int i = 0; i < biomes.length; i++) {
                                        BiomeColor biome = biomes[i];
                                        float adjTemp = MathMan.clamp(biome.temperature, 0.0f, 1.0f);
                                        float adjRainfall = MathMan.clamp(biome.rainfall, 0.0f, 1.0f) * adjTemp;
                                        int x = (int) (255 - adjTemp * 255);
                                        int z = (int) (255 - adjRainfall * 255);
                                        biome.grass = image.getRGB(x, z);
                                    }
                                }
                                // swampland: perlin - avoid
                                biomes[6].grass = 0;
                                biomes[134].grass = 0;
                                // roofed forest: averaged w/ 0x28340A
                                biomes[29].grass = multiplyColor(biomes[29].grass, 0x28340A + (255 << 24));
                                biomes[157].grass = multiplyColor(biomes[157].grass, 0x28340A + (255 << 24));
                                // mesa : 0x90814D
                                biomes[37].grass = 0x90814D + (255 << 24);
                                biomes[38].grass = 0x90814D + (255 << 24);
                                biomes[39].grass = 0x90814D + (255 << 24);
                                biomes[165].grass = 0x90814D + (255 << 24);
                                biomes[166].grass = 0x90814D + (255 << 24);
                                biomes[167].grass = 0x90814D + (255 << 24);
                                List<BiomeColor> valid = new ArrayList<>();
                                for (int i = 0; i < biomes.length; i++) {
                                    BiomeColor biome = biomes[i];
//                                biome.grass = multiplyColor(biome.grass, grass);
                                    if (biome.grass != 0 && !biome.name.equalsIgnoreCase("Unknown Biome")) {
                                        valid.add(biome);
                                    }
                                    biome.grassCombined = multiplyColor(grass, biome.grass);
                                }
                                this.validBiomes = valid.toArray(new BiomeColor[valid.size()]);

                                {
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
                                                int average = averageColor(c1.grass, c2.grass, c3.grass);
                                                if (uniqueBiomesColors.add(average)) {
                                                    count++;
                                                    layerColors.add((long) average);
                                                    layerIds.add((long) ((c1.id) + (c2.id << 8) + (c3.id << 16)));
                                                }
                                            }
                                        }
                                    }

                                    validMixBiomeColors = new int[layerColors.size()];
                                    for (int i = 0; i < layerColors.size(); i++)
                                        validMixBiomeColors[i] = (int) layerColors.getLong(i);
                                    validMixBiomeIds = layerIds.toLongArray();
                                }
                            }

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
        if (min == Long.MAX_VALUE) return null;
        return BlockTypes.get(closest);
    }

    private String getFileName(String path) {
        String[] split = path.toString().split("[/|\\\\]");
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
        return (((512 + rmean) * r * r) >> 8) + 4 * g * g + (((767 - rmean) * b * b) >> 8) + (hd * hd);
    }

    protected static int hueDistance(int red1, int green1, int blue1, int red2, int green2, int blue2) {
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

    public static class BiomeColor {
        public int id;
        public String name;
        public float temperature;
        public float rainfall;
        public int grass;
        public int grassCombined;
        public int foliage;

        public BiomeColor(int id, String name, float temperature, float rainfall, int grass, int foliage) {
            this.id = id;
            this.name = name;
            this.temperature = temperature;
            this.rainfall = rainfall;
            this.grass = grass;
            this.grassCombined = grass;
            this.foliage = foliage;
        }
    }

    public int[] getValidBlockIds() {
        return validBlockIds.clone();
    }
}
