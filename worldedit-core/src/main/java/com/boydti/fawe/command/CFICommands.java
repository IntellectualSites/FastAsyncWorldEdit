package com.boydti.fawe.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.beta.SingleFilterBlock;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Commands;
import com.boydti.fawe.jnbt.anvil.HeightMapMCAGenerator;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.clipboard.MultiClipboardHolder;
import com.boydti.fawe.util.CleanTextureUtil;
import com.boydti.fawe.util.FilteredTextureUtil;
import com.boydti.fawe.util.ImgurUtility;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.TextureUtil;
import com.boydti.fawe.util.chat.Message;
import com.boydti.fawe.util.image.ImageUtil;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import java.util.stream.IntStream;
import org.enginehub.piston.annotation.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.MethodCommands;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.command.Dispatcher;
import org.enginehub.piston.annotation.param.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.util.command.parametric.ParameterException;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;

import static com.boydti.fawe.util.image.ImageUtil.load;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class CFICommands extends MethodCommands {

    private final Dispatcher dispathcer;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public CFICommands(WorldEdit worldEdit, Dispatcher dispatcher) {
        super(worldEdit);
        this.dispathcer = dispatcher;
    }

    public static File getFolder(String worldName) {
        Platform platform = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING);
        List<? extends World> worlds = platform.getWorlds();
        FaweQueue queue = SetQueue.IMP.getNewQueue(worlds.get(0), true, false);
        return new File(queue.getSaveFolder().getParentFile().getParentFile(), worldName + File.separator + "region");
    }

    @Command(
            name = "heightmap",
            desc = "Start CFI with a height map as a base"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void heightmap(FawePlayer fp, FawePrimitiveBinding.ImageUri image, @Arg(name = "yscale", desc = "double", def = "1") double yscale) throws ParameterException {
        if (yscale != 0) {
            int[] raw = ((DataBufferInt) image.load().getRaster().getDataBuffer()).getData();
            int[] table = IntStream.range(0, 256).map(i -> Math.min(255, (int) (i * yscale)))
                .toArray();
            for (int i = 0; i < raw.length; i++) {
                int color = raw[i];
                int red = table[(color >> 16) & 0xFF];
                int green = table[(color >> 8) & 0xFF];
                int blue = table[(color >> 0) & 0xFF];
                raw[i] = (red << 16) + (green << 8) + (blue << 0);
            }
        }
        HeightMapMCAGenerator generator = new HeightMapMCAGenerator(image.load(), getFolder(generateName()));
        setup(generator, fp);
    }

    @Command(
            name = "empty",
            desc = "Start CFI with an empty map as a base"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void heightmap(FawePlayer fp, int width, int length) {
        HeightMapMCAGenerator generator = new HeightMapMCAGenerator(width, length, getFolder(generateName()));
        setup(generator, fp);
    }

    private String generateName() {
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH.mm.ss");
        return df.format(new Date());
    }

    private void setup(HeightMapMCAGenerator generator, FawePlayer fp) {
        CFISettings settings = getSettings(fp).remove();
        generator.setPacketViewer(fp);
        settings.setGenerator(generator).bind();
        generator.setImageViewer(Fawe.imp().getImageViewer(fp));
        generator.update();
        mainMenu(fp);
    }

    @Command(
            name = "brush",
            desc = "Info about using brushes with CFI"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void brush(FawePlayer fp) throws ParameterException {
        CFISettings settings = assertSettings(fp);
        settings.popMessages(fp);
        Message msg;
        if (settings.getGenerator().getImageViewer() != null) {
            msg = msg("CFI supports using brushes during creation").newline()
                                                                   .text(" - Place the map on a wall of item frames").newline()
                                                                   .text(" - Use any WorldEdit brush on the item frames").newline()
                                                                   .text(" - Example: ").text("Video").linkTip("https://goo.gl/PK4DMG").newline();
        } else {
            msg = msg("This is not supported with your platform/version").newline();
        }
        msg.text("< [Back]").cmdTip(alias()).send(fp);
    }

    @Command(
            name = "cancel",
            aliases = {"exit"},
            desc = "Cancel creation"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void cancel(FawePlayer fp) {
        getSettings(fp).remove();
        fp.sendMessage("Cancelled!");
    }

    @Command(
            name = "done",
            aliases = "create",
            desc = "Create the world"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void done(FawePlayer fp) throws ParameterException, IOException {
        CFISettings settings = assertSettings(fp);
        HeightMapMCAGenerator generator = settings.getGenerator();

        Function<File, Boolean> function = folder -> {
            if (folder != null) {
                try {
                    generator.setFolder(folder);
                    fp.sendMessage("Generating " + folder);
                    generator.generate();
                    generator.setPacketViewer(null);
                    generator.setImageViewer(null);
                    settings.remove();
                    fp.sendMessage("Done!");
                    return true;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                fp.sendMessage("Unable to generate world... (see console)?");
            }
            return false;
        };

        try {
            new PlotLoader().load(fp, settings, function);
        } catch (Throwable e) {
            e.printStackTrace();
            function.apply(generator.getFolder().getParentFile());
        }

        File folder = generator.getFolder();
        if (folder != null) {
            World world = FaweAPI.getWorld(folder.getName());
            if (world != null) {
                if (fp.getWorld() != world) {
                    TaskManager.IMP.sync(new RunnableVal<Object>() {
                        @Override
                        public void run(Object value) {
                            Location spawn = new Location(world, world.getSpawnPosition().toVector3());
                            fp.getPlayer().setPosition(spawn);
                        }
                    });
                }
            } else {
                fp.sendMessage("Unable to import world (" + folder.getName() + ") please do so manually");
            }
        }
    }

    @Command(
            name = "column",
            desc = "Set the floor and main block"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void column(FawePlayer fp, Pattern pattern, @Optional FawePrimitiveBinding.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name='w', desc = "TODO") boolean disableWhiteOnly) throws ParameterException{
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (image != null) {
            gen.setColumn(load(image), pattern, !disableWhiteOnly);
        } else if (mask != null) {
            gen.setColumn(mask, pattern);
        } else {
            gen.setColumn(pattern);
        }
        fp.sendMessage("Set column!");
        assertSettings(fp).resetComponent();
        component(fp);
    }

    @Command(
            name = "floor",
            desc = "Set the floor (default: grass)"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void floorCmd(FawePlayer fp, Pattern pattern, @Optional FawePrimitiveBinding.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name='w', desc = "TODO") boolean disableWhiteOnly) throws ParameterException{
        floor(fp, pattern, image, mask, disableWhiteOnly);
        fp.sendMessage("Set floor!");
        assertSettings(fp).resetComponent();
        component(fp);
    }

    private void floor(FawePlayer fp, Pattern pattern, @Optional FawePrimitiveBinding.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name='w', desc = "TODO") boolean disableWhiteOnly) throws ParameterException {
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (image != null) {
            gen.setFloor(load(image), pattern, !disableWhiteOnly);
        } else if (mask != null) {
            gen.setFloor(mask, pattern);
        } else {
            gen.setFloor(pattern);
        }
    }

    @Command(
            name = "main",
            desc = "Set the main block (default: stone)"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void mainCmd(FawePlayer fp, Pattern pattern, @Optional FawePrimitiveBinding.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name='w', desc = "TODO") boolean disableWhiteOnly) throws ParameterException{
        main(fp, pattern, image, mask, disableWhiteOnly);
        fp.sendMessage("Set main!");
        assertSettings(fp).resetComponent();
        component(fp);
    }

    public void main(FawePlayer fp, Pattern pattern, @Optional FawePrimitiveBinding.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name='w', desc = "TODO") boolean disableWhiteOnly) throws ParameterException{
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (image != null) {
            gen.setMain(load(image), pattern, !disableWhiteOnly);
        } else if (mask != null) {
            gen.setMain(mask, pattern);
        } else {
            gen.setMain(pattern);
        }
    }

    @Command(
            name = "overlay",
            aliases = {"setoverlay"},
            desc = "Set the overlay block",
            descFooter = "Change the block directly above the floor (default: air)\n" +
                    "e.g. Tallgrass"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void overlay(FawePlayer fp, Pattern pattern, @Optional FawePrimitiveBinding.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name='w', desc = "TODO") boolean disableWhiteOnly) throws ParameterException{
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (image != null) {
            gen.setOverlay(load(image), pattern, !disableWhiteOnly);
        } else if (mask != null) {
            gen.setOverlay(mask, pattern);
        } else {
            gen.setOverlay(pattern);
        }
        fp.sendMessage("Set overlay!");
        component(fp);
    }

    @Command(
            name = "smooth",
            desc = "Smooth the terrain",
            descFooter = "Smooth terrain within an image-mask, or worldedit mask\n" +
                    " - You can use !0 as the mask to smooth everything\n" +
                    " - This supports smoothing snow layers (set the floor to 78:7)\n" +
                    " - A good value for radius and iterations would be 1 8."
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void smoothCmd(FawePlayer fp, int radius, int iterations, @Optional FawePrimitiveBinding.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name='w', desc = "TODO") boolean disableWhiteOnly) throws ParameterException{
        smooth(fp, radius, iterations, image, mask, disableWhiteOnly);
        assertSettings(fp).resetComponent();
        component(fp);
    }

    private void smooth(FawePlayer fp, int radius, int iterations, @Optional FawePrimitiveBinding.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name='w', desc = "TODO") boolean disableWhiteOnly) throws ParameterException{
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (image != null) {
            gen.smooth(load(image), !disableWhiteOnly, radius, iterations);
        } else {
            gen.smooth(mask, radius, iterations);
        }
    }

    @Command(
            name = "snow",
            desc = "Create some snow"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void snow(FawePlayer fp, @Optional FawePrimitiveBinding.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name='w', desc = "TODO") boolean disableWhiteOnly) throws ParameterException{
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        floor(fp, BlockTypes.SNOW.getDefaultState().with(PropertyKey.LAYERS, 7), image, mask, disableWhiteOnly);
        main(fp, BlockTypes.SNOW_BLOCK, image, mask, disableWhiteOnly);
        smooth(fp, 1, 8, image, mask, disableWhiteOnly);
        msg("Added snow!").send(fp);
        assertSettings(fp).resetComponent();
        component(fp);
    }

    @Command(
            name = "biomepriority",
            desc = "Set the biome priority",
            descFooter = "Increase or decrease biome priority when using blockBiomeColor.\n" +
                    "A value of 50 is the default\n" +
                    "Above 50 will prefer to color with biomes\n" +
                    "Below 50 will prefer to color with blocks"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void biomepriority(FawePlayer fp, int value) throws ParameterException {
        assertSettings(fp).getGenerator().setBiomePriority(value);
        coloring(fp);
    }

    @Command(
            name = "paletteblocks",
            desc = "Set the blocks used for coloring",
            descFooter = "Allow only specific blocks to be used for coloring\n" +
                    "`blocks` is a list of blocks e.g. stone,bedrock,wool\n" +
                    "`#clipboard` will only use the blocks present in your clipboard."
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void paletteblocks(FawePlayer fp, Player player, LocalSession session, @Arg(name = "arg", desc = "String", def = "") String arg) throws ParameterException, EmptyClipboardException, InputParseException, FileNotFoundException {
        if (arg == null) {
            msg("What blocks do you want to color with?").newline()
            .text("[All]").cmdTip(alias() + " PaletteBlocks *").text(" - All available blocks")
            .newline()
            .text("[Clipboard]").cmdTip(alias() + " PaletteBlocks #clipboard").text(" - The blocks in your clipboard")
            .newline()
            .text("[List]").suggestTip(alias() + " PaletteBlocks stone,gravel").text(" - A comma separated list of blocks")
            .newline()
            .text("[Complexity]").cmdTip(alias() + " Complexity").text(" - Block textures within a complexity range")
            .newline()
            .text("< [Back]").cmdTip(alias() + " " + Commands.getAlias(CFICommands.class, "coloring"))
            .send(fp);
            return;
        }
        HeightMapMCAGenerator generator = assertSettings(fp).getGenerator();
        ParserContext context = new ParserContext();
        context.setActor(fp.getPlayer());
        context.setWorld(fp.getWorld());
        context.setSession(fp.getSession());
        context.setExtent(generator);
        Request.request().setExtent(generator);

        Set<BlockType> blocks;
        switch (arg.toLowerCase()) {
            case "true":
            case "*": {
                generator.setTextureUtil(Fawe.get().getTextureUtil());
                return;
            }
            case "#clipboard": {
                ClipboardHolder holder = fp.getSession().getClipboard();
                Clipboard clipboard = holder.getClipboard();
                boolean[] ids = new boolean[BlockTypes.size()];
                for (BlockVector3 pt : clipboard.getRegion()) {
                    ids[clipboard.getBlock(pt).getInternalBlockTypeId()] = true;
                }
                blocks = new HashSet<>();
                for (int combined = 0; combined < ids.length; combined++) {
                    if (ids[combined]) {
                        blocks.add(BlockTypes.get(combined));
                    }
                }
                break;
            }
            default: {
                blocks = new HashSet<>();
                SingleFilterBlock extent = new SingleFilterBlock();
                ParserContext parserContext = new ParserContext();
                parserContext.setActor(player);
                parserContext.setWorld(player.getWorld());
                parserContext.setSession(session);
                parserContext.setExtent(extent);
                Request.request().setExtent(extent);
                Mask mask = worldEdit.getMaskFactory().parseFromInput(arg, parserContext);
                TextureUtil tu = Fawe.get().getTextureUtil();
                for (int typeId : tu.getValidBlockIds()) {
                    BlockType type = BlockTypes.get(typeId);
                    extent.init(0, 0, 0, type.getDefaultState().toBaseBlock());
                    if (mask.test(extent)) {
                        blocks.add(type);
                    }
                }
                break;
            }
        }
        generator.setTextureUtil(new FilteredTextureUtil(Fawe.get().getTextureUtil(), blocks));
        coloring(fp);
    }

    @Command(
            name = "randomization",
            desc = "Set whether randomization is enabled",
            descFooter = "This is enabled by default, randomization will add some random variation in the blocks used to closer match the provided image.\n" +
                    "If disabled, the closest block to the color will always be used.\n" +
                    "Randomization will allow mixing biomes when coloring with biomes"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void randomization(FawePlayer fp, boolean enabled) throws ParameterException {
        assertSettings(fp).getGenerator().setTextureRandomVariation(enabled);
        coloring(fp);
    }

    @Command(
            name = "complexity",
            desc = "Set the complexity for coloring",
            descFooter = "Set the complexity for coloring\n" +
                    "Filter out blocks to use based on their complexity, which is a measurement of how much color variation there is in the texture for that block.\n" +
                    "Glazed terracotta is complex, and not very pleasant for terrain, whereas stone and wool are simpler textures.\n" +
                    "Using 0 73 for the min/max would use the simplest 73% of blocks for coloring, and is a reasonable value."
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void complexity(FawePlayer fp, int min, int max) throws ParameterException, FileNotFoundException {
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (min == 0 && max == 100) {
            gen.setTextureUtil(Fawe.get().getTextureUtil());
        } else {
            gen.setTextureUtil(new CleanTextureUtil(Fawe.get().getTextureUtil(), min, max));
        }
        coloring(fp);
    }

    @Command(
            name = "schem",
            desc = "Populate schematics",
            descFooter = "Populate a schematic on the terrain\n" +
                    " - Change the mask (e.g. angle mask) to only place the schematic in specific locations.\n" +
                    " - The rarity is a value between 0 and 100.\n" +
                    " - The distance is the spacing between each schematic"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void schem(FawePlayer fp, @Optional FawePrimitiveBinding.ImageUri imageMask, Mask mask, String schematic, int rarity, int distance, boolean rotate) throws ParameterException, IOException, WorldEditException {
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();

        World world = fp.getWorld();
        MultiClipboardHolder multi = ClipboardFormats.loadAllFromInput(fp.getPlayer(), schematic, null, true);
        if (multi == null) {
            return;
        }
        if (imageMask == null) {
            gen.addSchems(mask, multi.getHolders(), rarity, distance, rotate);
        } else {
            gen.addSchems(load(imageMask), mask, multi.getHolders(), rarity, distance, rotate);
        }
        msg("Added schematics!").send(fp);
        populate(fp);
    }

    @Command(
            name = "biome",
            desc = "Set the biome",
            descFooter = "Set the biome in specific parts of the map.\n" +
                    " - If an image is used, the biome will have a chance to be set based on how white the pixel is (white #FFF = 100% chance)" +
                    " - The whiteOnly parameter determines if only white values on the image are set" +
                    " - If a mask is used, the biome will be set anywhere the mask applies"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void biome(FawePlayer fp, BiomeType biome, @Optional FawePrimitiveBinding.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name='w', desc = "TODO") boolean disableWhiteOnly) throws ParameterException{
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (image != null) {
            gen.setBiome(load(image), biome, !disableWhiteOnly);
        } else if (mask != null) {
            gen.setBiome(mask, biome);
        } else {
            gen.setBiome(biome);
        }
        msg("Set biome!").send(fp);
        assertSettings(fp).resetComponent();
        component(fp);
    }

    @Command(
            name = "caves",
            desc = "Generate vanilla caves"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void caves(FawePlayer fp) throws ParameterException, WorldEditException {
        assertSettings(fp).getGenerator().addCaves();
        msg("Added caves!").send(fp);
        populate(fp);
    }

    @Command(
            name = "ore",
            desc = "Add an ore",
            descFooter = "Use a specific pattern and settings to generate ore"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void ore(FawePlayer fp, Mask mask, Pattern pattern, int size, int frequency, int rariry, int minY, int maxY) throws ParameterException, WorldEditException {
        assertSettings(fp).getGenerator().addOre(mask, pattern, size, frequency, rariry, minY, maxY);
        msg("Added ore!").send(fp);
        populate(fp);
    }

    @Command(
            name = "ores",
            desc = "Generate the vanilla ores"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void ores(FawePlayer fp, Mask mask) throws ParameterException, WorldEditException {
        assertSettings(fp).getGenerator().addDefaultOres(mask);
        msg("Added ores!").send(fp);
        populate(fp);
    }

    @Command(
            name = "height",
            desc = "Set the height",
            descFooter = "Set the terrain height either based on an image heightmap, or a numeric value."
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void height(FawePlayer fp, String arg) throws ParameterException, WorldEditException {
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (!MathMan.isInteger(arg)) {
            gen.setHeight(ImageUtil.getImage(arg));
        } else {
            gen.setHeights(Integer.parseInt(arg));
        }
        msg("Set height!").send(fp);
        component(fp);
    }

    @Command(
            name = "water",
            desc = "Change the block used for water\ne.g. Lava"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void waterId(FawePlayer fp, BlockStateHolder block) throws ParameterException, WorldEditException {
        CFISettings settings = assertSettings(fp);
        settings.getGenerator().setWaterId(block.getBlockType().getInternalId());
        msg("Set water id!").send(fp);
        settings.resetComponent();
        component(fp);
    }

    @Command(
            name = "baseid",
            aliases = {"bedrockid"},
            desc = "Change the block used for the base\ne.g. Bedrock"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void baseId(FawePlayer fp, BlockStateHolder block) throws ParameterException, WorldEditException {
        CFISettings settings = assertSettings(fp);
        settings.getGenerator().setBedrockId(block.getBlockType().getInternalId());
        msg("Set base id!").send(fp);
        settings.resetComponent();
        component(fp);
    }

    @Command(
            name = "worldthickness",
            aliases = {"width", "thickness"},
            desc = "Set the thickness of the generated world\n" +
                    " - A value of 0 is the default and will not modify the height"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void worldthickness(FawePlayer fp, int height) throws ParameterException, WorldEditException {
        assertSettings(fp).getGenerator().setWorldThickness(height);
        msg("Set world thickness!").send(fp);
        component(fp);
    }

    @Command(
            name = "floorthickness",
            aliases = {"floorheight", "floorwidth"},
            desc = "Set the thickness of the top layer\n" +
                    " - A value of 0 is the default and will only set the top block"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void floorthickness(FawePlayer fp, int height) throws ParameterException, WorldEditException {
        assertSettings(fp).getGenerator().setFloorThickness(height);
        msg("Set floor thickness!").send(fp);
        component(fp);
    }

    @Command(
            name = "update",
            aliases = {"refresh", "resend"},
            desc = "Resend the CFI chunks"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void update(FawePlayer fp) throws ParameterException, WorldEditException {
        assertSettings(fp).getGenerator().update();
        msg("Chunks refreshed!").send(fp);
        mainMenu(fp);
    }

    @Command(
            name = "tp",
            aliases = {"visit", "home"},
            desc = "Teleport to the CFI virtual world"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void tp(FawePlayer fp) throws ParameterException, WorldEditException {
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        msg("Teleporting...").send(fp);
        Vector3 origin = gen.getOrigin();
        Player player = fp.getPlayer();
        player.setPosition(origin.subtract(16, 0, 16));
        player.findFreePosition();
        mainMenu(fp);
    }

    @Command(
            name = "waterheight",
            aliases = {"sealevel", "setwaterheight"},
            desc = "Set the level water is generated at\n" +
                    "Set the level water is generated at\n" +
                    " - By default water is disabled (with a value of 0)"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void waterheight(FawePlayer fp, int height) throws ParameterException, WorldEditException {
        assertSettings(fp).getGenerator().setWaterHeight(height);
        msg("Set water height!").send(fp);
        component(fp);
    }

    @Command(
            name = "glass",
            aliases = {"glasscolor", "setglasscolor"},
            desc = "Color terrain using glass"
    )
    // ![79,174,212,5:3,5:4,18,161,20]
    @CommandPermissions("worldedit.anvil.cfi")
    public void glass(FawePlayer fp, FawePrimitiveBinding.ImageUri image, @Optional FawePrimitiveBinding.ImageUri imageMask, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name='w', desc = "TODO") boolean disableWhiteOnly) throws ParameterException, WorldEditException {
        CFISettings settings = assertSettings(fp);
        settings.getGenerator().setColorWithGlass(load(image));
        msg("Set color with glass!").send(fp);
        settings.resetColoring();
        mainMenu(fp);
    }

    @Command(
            name = "color",
            aliases = {"setcolor", "blockcolor", "blocks"},
            desc = "Set the color with blocks and biomes",
            descFooter = "Color the terrain using only blocks\n" +
                    "Provide an image, or worldedit mask for the 2nd argument to restrict what areas are colored\n" +
                    "The -w (disableWhiteOnly) will randomly apply depending on the pixel luminance"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void color(FawePlayer fp, FawePrimitiveBinding.ImageUri image, @Optional FawePrimitiveBinding.ImageUri imageMask, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name='w', desc = "TODO") boolean disableWhiteOnly) throws ParameterException, WorldEditException {
        CFISettings settings = assertSettings(fp);
        HeightMapMCAGenerator gen = settings.getGenerator();
        if (imageMask != null) {
            gen.setColor(load(image), load(imageMask), !disableWhiteOnly);
        } else if (mask != null) {
            gen.setColor(load(image), mask);
        } else {
            gen.setColor(load(image));
        }
        settings.resetColoring();
        msg("Set color with blocks!").send(fp);
        mainMenu(fp);
    }

    @Command(
            name = "blockbiomecolor",
            aliases = {"setblockandbiomecolor", "blockandbiome"},
            desc = "Set the color with blocks and biomes",
            descFooter = "Color the terrain using blocks and biomes.\n" +
                    "Provide an image, or worldedit mask to restrict what areas are colored\n" +
                    "The -w (disableWhiteOnly) will randomly apply depending on the pixel luminance"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void blockbiome(FawePlayer fp, FawePrimitiveBinding.ImageUri image, @Optional FawePrimitiveBinding.ImageUri imageMask, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name='w', desc = "TODO") boolean disableWhiteOnly) throws ParameterException, WorldEditException {
        CFISettings settings = assertSettings(fp);
        settings.getGenerator().setBlockAndBiomeColor(load(image), mask, load(imageMask), !disableWhiteOnly);
        msg("Set color with blocks and biomes!").send(fp);
        settings.resetColoring();
        mainMenu(fp);
    }

    @Command(
            name = "biomecolor",
            aliases = {"setbiomecolor", "biomes"},
            desc = "Color the terrain using biomes.\n" +
                    "Note: Biome coloring does not change blocks:\n" +
                    " - If you changed the block to something other than grass you will not see anything."
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void biomecolor(FawePlayer fp, FawePrimitiveBinding.ImageUri image, @Optional FawePrimitiveBinding.ImageUri imageMask, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name='w', desc = "TODO") boolean disableWhiteOnly) throws ParameterException, WorldEditException {
        CFISettings settings = assertSettings(fp);
        settings.getGenerator().setBiomeColor(load(image));
        msg("Set color with biomes!").send(fp);
        settings.resetColoring();
        mainMenu(fp);
    }


    @Command(
            name = "coloring",
            aliases = {"palette"},
            desc = "Color the world using an image"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void coloring(FawePlayer fp) throws ParameterException {
        CFISettings settings = assertSettings(fp);
        settings.popMessages(fp);
        settings.setCategory("coloring");
        HeightMapMCAGenerator gen = settings.getGenerator();
        boolean rand = gen.getTextureRandomVariation();
        String mask;
        if (settings.imageMask != null) {
            mask = settings.imageMaskArg;
        } else if (settings.mask != null) {
            mask = settings.maskArg;
        } else {
            mask = "NONE";
        }
        TextureUtil tu = gen.getRawTextureUtil();
        String blocks;
        if (tu.getClass() == TextureUtil.class) {
            blocks = "All";
        } else if (tu.getClass() == CleanTextureUtil.class) {
            CleanTextureUtil clean = (CleanTextureUtil) tu;
            blocks = "Complexity(" + clean.getMin() + "," + clean.getMax() + ")";
        } else if (tu.getClass() == FilteredTextureUtil.class) {
            blocks = "Selected";
        } else {
            blocks = "Undefined";
        }

        Set<String> materials = new HashSet<>();
        int[] blockArray = tu.getValidBlockIds();
        for (int typeId : blockArray) {
            BlockType type = BlockTypes.get(typeId);
            String name = type.getName();
            if (name.contains(":")) {
                name = name.split(":")[1];
            }
            materials.add(name);
        }
        String blockList = materials.size() > 100 ? materials.size() + " blocks" : StringMan.join(materials, ',');

        int biomePriority = gen.getBiomePriority();

        Message msg = msg(">> Current Settings <<").newline()
        .text("Randomization ").text("[" + (Boolean.toString(rand).toUpperCase()) + "]").cmdTip(alias() + " randomization " + (!rand))
        .newline()
        .text("Mask ").text("[" + mask + "]").cmdTip(alias() + " mask")
        .newline()
        .text("Blocks ").text("[" + blocks + "]").tooltip(blockList).command(alias() + " paletteBlocks")
        .newline()
        .text("BiomePriority ").text("[" + biomePriority + "]").cmdTip(alias() + " biomepriority")
        .newline();

        if (settings.image != null) {
            StringBuilder colorArgs = new StringBuilder();
            colorArgs.append(" " + settings.imageArg);
            if (settings.imageMask != null) {
                colorArgs.append(" " + settings.imageMaskArg);
            }
            if (settings.mask != null) {
                colorArgs.append(" " + settings.maskArg);
            }
            if (!settings.whiteOnly) {
                colorArgs.append(" -w");
            }

            msg.text("Image: ")
            .text("[" + settings.imageArg + "]").cmdTip(alias() + " " + Commands.getAlias(CFICommands.class, "image"))
            .newline().newline()
            .text("&cLet's Color: ")
            .cmdOptions(alias() + " ", colorArgs.toString(), "Biomes", "Blocks", "BlockAndBiome", "Glass")
            .newline();
        } else {
            msg.newline().text("You can color a world using an image like ")
            .text("[This]").linkTip("http://i.imgur.com/vJYinIU.jpg").newline()
            .text("&cYou MUST provide an image: ")
            .text("[None]").cmdTip(alias() + " " + Commands.getAlias(Command.class, "image")).newline();
        }
        msg.text("< [Back]").cmdTip(alias()).send(fp);
    }

    @Command(
            name = "mask",
            desc = "Select a mask"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void mask(FawePlayer fp, @Optional FawePrimitiveBinding.ImageUri imageMask, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name='w', desc = "TODO") boolean disableWhiteOnly, CommandContext context) throws ParameterException{
        CFISettings settings = assertSettings(fp);
        String[] split = getArguments(context).split(" ");
        int index = 2;
        settings.imageMask = imageMask;
        settings.imageMaskArg = imageMask != null ? split[index++] : null;
        settings.mask = mask;
        settings.maskArg = mask != null ? split[index++] : null;
        settings.whiteOnly = !disableWhiteOnly;

        StringBuilder cmd = new StringBuilder(alias() + " mask ");

        msg(">> Current Settings <<").newline()
                .text("Image Mask ").text("[" + settings.imageMaskArg + "]").suggestTip(cmd + "http://")
                .newline()
                .text("WorldEdit Mask ").text("[" + settings.maskArg + "]").suggestTip(cmd + "<mask>")
                .newline()
                .text("< [Back]").cmdTip(alias() + " " + settings.getCategory()).send(fp);
    }

    @Command(
            name = "pattern",
            desc = "Select a pattern"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void pattern(FawePlayer fp, @Arg(name = "pattern", desc = "Pattern", def = "") Pattern pattern, CommandContext context) throws ParameterException, CommandException {
        CFISettings settings = assertSettings(fp);
        String[] split = getArguments(context).split(" ");
        int index = 2;
        settings.pattern = pattern;
        settings.patternArg = pattern == null ? null : split[index++];

        StringBuilder cmd = new StringBuilder(alias() + " pattern ");

        if (pattern != null) {
            dispathcer.call(settings.getCategory(), context.getLocals(), new String[0]);
        } else {
            msg(">> Current Settings <<").newline()
                    .text("Pattern ").text("[Click Here]").suggestTip(cmd + " stone")
                    .newline()
                    .text("< [Back]").cmdTip(alias() + " " + settings.getCategory()).send(fp);
        }
    }

    @Command(
            name = "download",
            desc = "Download the current image"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void download(FawePlayer fp) throws ParameterException, IOException {
        CFISettings settings = assertSettings(fp);
        BufferedImage image = settings.getGenerator().draw();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] data = baos.toByteArray();
        fp.sendMessage("Please wait...");
        URL url = ImgurUtility.uploadImage(data);
        BBC.DOWNLOAD_LINK.send(fp, url);
    }

    @Command(
            name = "image",
            desc = "Select an image"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void image(FawePlayer fp, @Optional FawePrimitiveBinding.ImageUri image, CommandContext context) throws ParameterException, CommandException {
        CFISettings settings = getSettings(fp);
        String[] split = getArguments(context).split(" ");
        int index = 2;

        settings.image = image;
        settings.imageArg = image != null ? split[index++] : null;
        String maskArg = settings.maskArg == null ? "Click Here" : settings.maskArg;

        StringBuilder cmd = new StringBuilder(alias() + " image ");
        if (image == null) {
            msg("Please provide an image:").newline()
            .text("From a URL: ").text("[Click Here]").suggestTip(cmd + "http://")
            .newline()
            .text("From a file: ").text("[Click Here]").suggestTip(cmd + "file://")
            .send(fp);
        } else {
            if (settings.hasGenerator()) {
                coloring(fp);
                return;
            } else {
                String next = Commands.getAlias(CFICommands.class, "heightmap " + settings.imageArg);
                dispathcer.call(next, context.getLocals(), new String[0]);
                return;
            }
        }
    }

    @Command(
            name = "populate",
            desc = ""
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void populate(FawePlayer fp) throws ParameterException {
        CFISettings settings = assertSettings(fp);
        settings.popMessages(fp);
        settings.setCategory("populate");
        msg("What would you like to populate?").newline()
        .text("(You will need to type these commands)").newline()
        .cmdOptions(alias() + " ", "", "Ores", "Ore", "Caves", "Schematics", "Smooth")
        .newline().text("< [Back]").cmdTip(alias())
        .send(fp);
    }

    @Command(
            name = "component",
            aliases = {"components"},
            desc = "Components menu"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void component(FawePlayer fp) throws ParameterException {
        CFISettings settings = assertSettings(fp);
        settings.popMessages(fp);
        settings.setCategory("component");

        String mask;
        if (settings.imageMask != null) {
            mask = settings.imageMaskArg;
        } else if (settings.mask != null) {
            mask = settings.maskArg;
        } else {
            mask = "NONE";
        }

        String pattern = settings.pattern == null ? "NONE" : settings.patternArg;

        StringBuilder maskArgs = new StringBuilder();
        if (settings.imageMask != null) {
            maskArgs.append(" " + settings.imageMaskArg);
        }
        if (settings.mask != null) {
            maskArgs.append(" " + settings.maskArg);
        }
        if (!settings.whiteOnly) {
            maskArgs.append(" -w");
        }

        String height = Commands.getAlias(CFICommands.class, "height");
        String waterHeight = Commands.getAlias(CFICommands.class, "waterheight");
        String snow = Commands.getAlias(CFICommands.class, "snow");

        Message msg = msg(">> Current Settings <<").newline()
        .text("Mask ").text("[" + mask + "]").cmdTip(alias() + " mask")
        .newline()
        .text("Pattern ").text("[" + pattern + "]").cmdTip(alias() + " pattern")
        .newline()
        .newline()
        .text(">> Components <<")
        .newline()
        .text("[Height]").suggestTip(alias() + " " + alias("height") + " 120").text(" - Terrain height for whole map")
        .newline()
        .text("[WaterHeight]").suggestTip(alias() + " " + alias("waterheight") + " 60").text(" - Sea level for whole map")
        .newline()
        .text("[FloorThickness]").suggestTip(alias() + " " + alias("floorthickness") + " 60").text(" - Floor thickness of entire map")
        .newline()
        .text("[WorldThickness]").suggestTip(alias() + " " + alias("worldthickness") + " 60").text(" - World thickness of entire map")
        .newline()
        .text("[Snow]").suggestTip(alias() + " " + alias("snow") + maskArgs).text(" - Set snow in the masked areas")
        .newline();

        if (pattern != null) {
            String disabled = "You must specify a pattern";
            msg
            .text("[&cWaterId]").tooltip(disabled).newline()
            .text("[&cBedrockId]").tooltip(disabled).newline()
            .text("[&cFloor]").tooltip(disabled).newline()
            .text("[&cMain]").tooltip(disabled).newline()
            .text("[&cColumn]").tooltip(disabled).newline()
            .text("[&cOverlay]").tooltip(disabled).newline();
        } else {
            StringBuilder compArgs = new StringBuilder();
            compArgs.append(" " + settings.patternArg + maskArgs);

            msg
            .text("[WaterId]").cmdTip(alias() + " waterId " + pattern).text(" - Water id for whole map").newline()
            .text("[BedrockId]").cmdTip(alias() + " baseId " + pattern).text(" - Bedrock id for whole map").newline()
            .text("[Floor]").cmdTip(alias() + " floor" + compArgs).text(" - Set the floor in the masked areas").newline()
            .text("[Main]").cmdTip(alias() + " main" + compArgs).text(" - Set the main block in the masked areas").newline()
            .text("[Column]").cmdTip(alias() + " column" + compArgs).text(" - Set the columns in the masked areas").newline()
            .text("[Overlay]").cmdTip(alias() + " overlay" + compArgs).text(" - Set the overlay in the masked areas").newline();
        }

        msg.newline()
        .text("< [Back]").cmdTip(alias())
        .send(fp);
    }


    private CFISettings assertSettings(FawePlayer fp) throws ParameterException {
        CFISettings settings = getSettings(fp);
        if (!settings.hasGenerator()) {
            throw new ParameterException("Please use /" + alias());
        }
        return settings;
    }


    protected CFISettings getSettings(FawePlayer fp) {
        CFISettings settings = fp.getMeta("CFISettings");
        return settings == null ? new CFISettings(fp) : settings;
    }

    public static class CFISettings {
        private final FawePlayer fp;

        private HeightMapMCAGenerator generator;

        protected FawePrimitiveBinding.ImageUri image;
        protected String imageArg;
        protected Mask mask;
        protected FawePrimitiveBinding.ImageUri imageMask;
        protected boolean whiteOnly = true;
        protected String maskArg;
        protected String imageMaskArg;

        protected Pattern pattern;
        protected String patternArg;

        protected String category;

        private boolean bound;

        public CFISettings(FawePlayer player) {
            this.fp = player;
        }

        public boolean hasGenerator() {
            return generator != null;
        }

        public HeightMapMCAGenerator getGenerator() {
            return generator;
        }

        public void setMask(Mask mask, String arg) {
            this.mask = mask;
            this.maskArg = arg;
        }

        public void setImage(FawePrimitiveBinding.ImageUri image, String arg) {
            this.image = image;
        }

        public void setImageMask(FawePrimitiveBinding.ImageUri imageMask, String arg) {
            this.imageMask = imageMask;
            this.imageMaskArg = arg;
        }

        public void resetColoring() {
            image = null;
            imageArg = null;
            mask = null;
            imageMask = null;
            whiteOnly = true;
            maskArg = null;
            imageMaskArg = null;
            generator.setTextureUtil(Fawe.get().getTextureUtil());
        }

        public void resetComponent() {
            mask = null;
            imageMask = null;
            whiteOnly = true;
            maskArg = null;
            imageMaskArg = null;
            patternArg = null;
            pattern = null;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public CFISettings setGenerator(HeightMapMCAGenerator generator) {
            this.generator = generator;
            if (bound) {
                fp.getSession().setVirtualWorld(generator);
            }
            return this;
        }

        public CFISettings bind() {
            if (generator != null) {
                fp.getSession().setVirtualWorld(generator);
            }
            bound = true;
            fp.setMeta("CFISettings", this);
            return this;
        }

        public void popMessages(FawePlayer fp) {
            ArrayDeque<String> messages = fp.deleteMeta("CFIBufferedMessages");
            if (messages != null) {
                for (String message : messages) {
                    fp.sendMessage(message);
                }
            }
        }

        public CFISettings remove() {
            fp.deleteMeta("CFISettings");
            HeightMapMCAGenerator gen = this.generator;
            if (gen != null) {
                fp.getSession().setVirtualWorld(null);
            }
            popMessages(fp);
            bound = false;
            generator = null;
            image = null;
            imageArg = null;
            mask = null;
            imageMask = null;
            whiteOnly = true;
            maskArg = null;
            imageMaskArg = null;
            return this;
        }
    }

    protected String alias() {
        return Commands.getAlias(CFICommand.class, "/cfi");
    }

    protected String alias(String command) {
        return Commands.getAlias(CFICommands.class, command);
    }

    protected Message msg(String text) {
        return new Message().newline()
                            .text(BBC.getPrefix())
                            .text(text);
    }

    protected void mainMenu(FawePlayer fp) {
        msg("What do you want to do now?").newline()
                .cmdOptions(alias() + " ", "", "Coloring", "Component", "Populate", "Brush")
                .newline().text("<> [View]").command(alias() + " " + Commands.getAlias(CFICommands.class, "download")).tooltip("View full resolution image")
                .newline().text(">< [Cancel]").cmdTip(alias() + " " + Commands.getAlias(CFICommands.class, "cancel"))
                .newline().text("&2>> [Done]").cmdTip(alias() + " " + Commands.getAlias(CFICommands.class, "done"))
                .send(fp);
    }
}
