package com.boydti.fawe.command;

import static com.boydti.fawe.util.image.ImageUtil.load;
import static com.sk89q.worldedit.command.MethodCommands.getArguments;
import static com.sk89q.worldedit.util.formatting.text.TextComponent.newline;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.beta.SingleFilterBlock;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Commands;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.brush.visualization.cfi.HeightMapMCAGenerator;
import com.boydti.fawe.object.clipboard.MultiClipboardHolder;
import com.boydti.fawe.util.CleanTextureUtil;
import com.boydti.fawe.util.FilteredTextureUtil;
import com.boydti.fawe.util.ImgurUtility;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.TextureUtil;
import com.boydti.fawe.util.image.ImageUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extension.platform.binding.ProvideBindings;
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
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TextComponent.Builder;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import com.sk89q.worldedit.util.formatting.text.event.HoverEvent;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.Switch;
import org.enginehub.piston.exception.StopExecutionException;
import org.enginehub.piston.inject.InjectedValueAccess;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class CFICommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public CFICommands(WorldEdit worldEdit) {
        this.worldEdit = worldEdit;
    }

    public static File getFolder(String worldName) {
        Platform platform = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING);
        List<? extends World> worlds = platform.getWorlds();
        Path path = worlds.get(0).getStoragePath();
        return new File(path.toFile().getParentFile().getParentFile(), worldName + File.separator + "region");
    }

    @Command(
            name = "heightmap",
            desc = "Start CFI with a height map as a base"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void heightmap(FawePlayer fp, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "yscale", desc = "double", def = "1") double yscale) {
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
    public void heightMap(FawePlayer fp, int width, int length) {
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
    public void brush(FawePlayer fp) {
        CFISettings settings = assertSettings(fp);
        settings.popMessages(fp);
        @NonNull Builder msg;
        if (settings.getGenerator().getImageViewer() != null) {
            msg = TextComponent.builder("CFI supports using brushes during creation").append(newline())
                    .append(" - Place the map on a wall of item frames").append(newline())
                    .append(" - Use any WorldEdit brush on the item frames").append(newline())
                    .append(" - Example: ").append(TextComponent.of("Video").clickEvent(ClickEvent.openUrl("https://goo.gl/PK4DMG"))).append(newline());
        } else {
            msg = TextComponent.builder("This is not supported with your platform/version").append(newline());
        }
        //TODO msg.text("< [Back]").cmdTip(alias()).send(fp);
        fp.toWorldEditPlayer().print(msg.build());
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
    public void done(FawePlayer fp) {
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
    public void column(FawePlayer fp, Pattern pattern, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly){
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
    public void floorCmd(FawePlayer fp, Pattern pattern, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly){
        floor(fp, pattern, image, mask, disableWhiteOnly);
        fp.sendMessage("Set floor!");
        assertSettings(fp).resetComponent();
        component(fp);
    }

    private void floor(FawePlayer fp, Pattern pattern, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly) {
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
    public void mainCmd(FawePlayer fp, Pattern pattern, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly){
        main(fp, pattern, image, mask, disableWhiteOnly);
        fp.sendMessage("Set main!");
        assertSettings(fp).resetComponent();
        component(fp);
    }

    public void main(FawePlayer fp, Pattern pattern, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly){
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
    public void overlay(FawePlayer fp, Pattern pattern, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly){
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
    public void smoothCmd(FawePlayer fp, int radius, int iterations, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly){
        smooth(fp, radius, iterations, image, mask, disableWhiteOnly);
        assertSettings(fp).resetComponent();
        component(fp);
    }

    private void smooth(FawePlayer fp, int radius, int iterations, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly){
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
    public void snow(FawePlayer fp, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly){
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        floor(fp, BlockTypes.SNOW.getDefaultState().with(PropertyKey.LAYERS, 7), image, mask, disableWhiteOnly);
        main(fp, BlockTypes.SNOW_BLOCK, image, mask, disableWhiteOnly);
        smooth(fp, 1, 8, image, mask, disableWhiteOnly);
        fp.toWorldEditPlayer().print(TextComponent.of("Added snow!"));
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
    public void biomepriority(FawePlayer fp, int value) {
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
    public void paletteblocks(FawePlayer fp, Player player, LocalSession session, @Arg(name = "arg", desc = "String", def = "") String arg) throws EmptyClipboardException, InputParseException, FileNotFoundException {
        if (arg == null) {
            TextComponent build = TextComponent.builder("What blocks do you want to color with?")
                .append(newline())
                .append(TextComponent.of("[All]")
                    .clickEvent(ClickEvent.runCommand("/cfi PaletteBlocks *")))
                .append(" - All available blocks")
                .append(newline())
                .append(TextComponent.of("[Clipboard]")
                    .clickEvent(ClickEvent.runCommand("/cfi PaletteBlocks #clipboard")))
                .append(" - The blocks in your clipboard")
                .append(newline())
                .append(TextComponent.of("[List]")
                    .clickEvent(ClickEvent.runCommand("/cfi PaletteBlocks stone,gravel")))
                .append(" - A comma separated list of blocks")
                .append(newline())
                .append(TextComponent.of("[Complexity]")
                    .clickEvent(ClickEvent.runCommand("/cfi Complexity")))
                .append(" - Block textures within a complexity range")
                .append(newline())
                .append(TextComponent.of("< [Back]").clickEvent(ClickEvent
                    .runCommand("/cfi " + Commands.getAlias(CFICommands.class, "coloring"))))
                .build();
            fp.toWorldEditPlayer().print(build);
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
    public void randomization(FawePlayer fp, boolean enabled) {
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
    public void complexity(FawePlayer fp, int min, int max) throws  FileNotFoundException {
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
    public void schem(FawePlayer fp, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri imageMask, Mask mask, String schematic, int rarity, int distance, boolean rotate)throws IOException, WorldEditException {
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
        fp.toWorldEditPlayer().print(TextComponent.of("Added schematics!"));
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
    public void biome(FawePlayer fp, BiomeType biome, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly){
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (image != null) {
            gen.setBiome(load(image), biome, !disableWhiteOnly);
        } else if (mask != null) {
            gen.setBiome(mask, biome);
        } else {
            gen.setBiome(biome);
        }
        fp.toWorldEditPlayer().print(TextComponent.of("Set biome!"));
        assertSettings(fp).resetComponent();
        component(fp);
    }

    @Command(
            name = "caves",
            desc = "Generate vanilla caves"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void caves(FawePlayer fp) throws WorldEditException {
        assertSettings(fp).getGenerator().addCaves();
        fp.toWorldEditPlayer().print(TextComponent.of("Added caves!"));
        populate(fp);
    }

    @Command(
            name = "ore",
            desc = "Add an ore",
            descFooter = "Use a specific pattern and settings to generate ore"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void ore(FawePlayer fp, Mask mask, Pattern pattern, int size, int frequency, int rariry, int minY, int maxY) throws WorldEditException {
        assertSettings(fp).getGenerator().addOre(mask, pattern, size, frequency, rariry, minY, maxY);
        fp.toWorldEditPlayer().print(TextComponent.of("Added ore!"));
        populate(fp);
    }

    @Command(
            name = "ores",
            desc = "Generate the vanilla ores"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void ores(FawePlayer fp, Mask mask) throws WorldEditException {
        assertSettings(fp).getGenerator().addDefaultOres(mask);
        fp.toWorldEditPlayer().print(TextComponent.of("Added ores!"));
        populate(fp);
    }

    @Command(
            name = "height",
            desc = "Set the height",
            descFooter = "Set the terrain height either based on an image heightmap, or a numeric value."
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void height(FawePlayer fp, String arg) throws WorldEditException {
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (!MathMan.isInteger(arg)) {
            gen.setHeight(ImageUtil.getImage(arg));
        } else {
            gen.setHeights(Integer.parseInt(arg));
        }
        fp.toWorldEditPlayer().print("Set Height!");
        component(fp);
    }

    @Command(
            name = "water",
            desc = "Change the block used for water\ne.g. Lava"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void waterId(FawePlayer fp, BlockStateHolder block) throws WorldEditException {
        CFISettings settings = assertSettings(fp);
        settings.getGenerator().setWaterId(block.getBlockType().getInternalId());

        fp.toWorldEditPlayer().print("Set water id!");
        settings.resetComponent();
        component(fp);
    }

    @Command(
            name = "baseid",
            aliases = {"bedrockid"},
            desc = "Change the block used for the base\ne.g. Bedrock"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void baseId(FawePlayer fp, BlockStateHolder block) throws WorldEditException {
        CFISettings settings = assertSettings(fp);
        settings.getGenerator().setBedrockId(block.getBlockType().getInternalId());
        fp.toWorldEditPlayer().print(TextComponent.of("Set base id!"));
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
    public void worldthickness(FawePlayer fp, int height) throws WorldEditException {
        assertSettings(fp).getGenerator().setWorldThickness(height);
        fp.toWorldEditPlayer().print(TextComponent.of("Set world thickness!"));
        component(fp);
    }

    @Command(
            name = "floorthickness",
            aliases = {"floorheight", "floorwidth"},
            desc = "Set the thickness of the top layer\n" +
                    " - A value of 0 is the default and will only set the top block"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void floorthickness(FawePlayer fp, int height) throws WorldEditException {
        assertSettings(fp).getGenerator().setFloorThickness(height);
        fp.toWorldEditPlayer().print(TextComponent.of("Set floor thickness!"));
        component(fp);
    }

    @Command(
            name = "update",
            aliases = {"refresh", "resend"},
            desc = "Resend the CFI chunks"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void update(FawePlayer fp) throws WorldEditException {
        assertSettings(fp).getGenerator().update();
        fp.toWorldEditPlayer().print(TextComponent.of("Chunks refreshed!"));
        mainMenu(fp);
    }

    @Command(
            name = "tp",
            aliases = {"visit", "home"},
            desc = "Teleport to the CFI virtual world"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void tp(FawePlayer fp) throws WorldEditException {
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        fp.toWorldEditPlayer().print(TextComponent.of("Teleporting..."));
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
    public void waterheight(FawePlayer fp, int height) throws WorldEditException {
        assertSettings(fp).getGenerator().setWaterHeight(height);
        fp.toWorldEditPlayer().print(TextComponent.of("Set water height!"));
        component(fp);
    }

    @Command(
            name = "glass",
            aliases = {"glasscolor", "setglasscolor"},
            desc = "Color terrain using glass"
    )
    // ![79,174,212,5:3,5:4,18,161,20]
    @CommandPermissions("worldedit.anvil.cfi")
    public void glass(FawePlayer fp, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri imageMask, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly) throws WorldEditException {
        CFISettings settings = assertSettings(fp);
        settings.getGenerator().setColorWithGlass(load(image));
        fp.toWorldEditPlayer().print(TextComponent.of("Set color with glass!"));
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
    public void color(FawePlayer fp, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri imageMask, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly) throws WorldEditException {
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
        fp.toWorldEditPlayer().print(TextComponent.of("Set color with blocks!"));
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
    public void blockbiome(FawePlayer fp, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri imageMask, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly) throws WorldEditException {
        CFISettings settings = assertSettings(fp);
        settings.getGenerator().setBlockAndBiomeColor(load(image), mask, load(imageMask), !disableWhiteOnly);
        fp.toWorldEditPlayer().print(TextComponent.of("Set color with blocks and biomes!"));
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
    public void biomecolor(FawePlayer fp, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri imageMask, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly) throws WorldEditException {
        CFISettings settings = assertSettings(fp);
        settings.getGenerator().setBiomeColor(load(image));
        fp.toWorldEditPlayer().print(TextComponent.of("Set color with biomes!"));
        settings.resetColoring();
        mainMenu(fp);
    }


    @Command(
            name = "coloring",
            aliases = {"palette"},
            desc = "Color the world using an image"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void coloring(FawePlayer fp) {
        CFISettings settings = assertSettings(fp);
        settings.popMessages(fp);
        settings.setCategory(this::coloring);
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

        //TODO fix this so it can execute commands and show tooltips.
        @NonNull Builder builder = TextComponent.builder(">> Current Settings <<").append(newline())
                .append("Randomization ").append("[" + Boolean.toString(rand).toUpperCase() + "]")//.cmdTip(alias() + " randomization " + (!rand))
                .append(newline())
                .append("Mask ").append("[" + mask + "]")//.cmdTip(alias() + " mask")
                .append(newline())
                .append("Blocks ").append("[" + blocks + "]")//.tooltip(blockList).command(alias() + " paletteBlocks")
                .append(newline())
                .append("BiomePriority ").append("[" + biomePriority + "]")//.cmdTip(alias() + " biomepriority")
                .append(newline());

        if (settings.image != null) {
            StringBuilder colorArgs = new StringBuilder(" " + settings.imageArg);
            if (settings.imageMask != null) {
                colorArgs.append(" ").append(settings.imageMaskArg);
            }
            if (settings.mask != null) {
                colorArgs.append(" ").append(settings.maskArg);
            }
            if (!settings.whiteOnly) {
                colorArgs.append(" -w");
            }

            builder.append("Image: ")
                    .append("[" + settings.imageArg + "]")//.cmdTip(alias() + " " + Commands.getAlias(CFICommands.class, "image"))
                    .append(newline()).append(newline())
                    .append("Let's Color: ")
                    //.cmdOptions(alias() + " ", colorArgs.toString(), "Biomes", "Blocks", "BlockAndBiome", "Glass")
                    .append(newline());
        } else {
            builder.append(newline()).append("You can color a world using an image like ")
                    .append(TextComponent.of("[This]").clickEvent(ClickEvent.openUrl("http://i.imgur.com/vJYinIU.jpg"))).append(newline())
                    .append("You MUST provide an image: ")
                    .append("[None]");//.cmdTip(alias() + " " + Commands.getAlias(Command.class, "image")).append(newline());
        }
        builder.append("< [Back]");//.cmdTip(alias()).send(fp);
        fp.toWorldEditPlayer().print(builder.build());
    }

    @Command(
            name = "mask",
            desc = "Select a mask"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void mask(FawePlayer fp, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri imageMask, @Arg(name = "mask", desc = "Mask", def = "") Mask mask, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly, InjectedValueAccess context){
        CFISettings settings = assertSettings(fp);
        String[] split = getArguments(context).split(" ");
        int index = 2;
        settings.imageMask = imageMask;
        settings.imageMaskArg = imageMask != null ? split[index++] : null;
        settings.mask = mask;
        settings.maskArg = mask != null ? split[index++] : null;
        settings.whiteOnly = !disableWhiteOnly;

        StringBuilder cmd = new StringBuilder("/cfi mask ");

        String s = "/cfi mask http://";
        String s1 = "/cfi mask <mask>";
        String s2 = alias() + " " + settings.getCategory();
        TextComponent build = TextComponent.builder(">> Current Settings <<")
            .append(newline())
            .append("Image Mask ").append(
                TextComponent.of("[" + settings.imageMaskArg + "]")
                    .hoverEvent(HoverEvent.showText(TextComponent.of(s)))
                    .clickEvent(ClickEvent.suggestCommand("/cfi mask http://")))
            .append(newline())
            .append("WorldEdit Mask ").append(TextComponent.of("[" + settings.maskArg + "]")
                .hoverEvent(HoverEvent.showText(TextComponent.of(s1)))
                .clickEvent(ClickEvent.suggestCommand(s1)))
            .append(newline())
            .append(
                TextComponent.of("< [Back]").hoverEvent(HoverEvent.showText(TextComponent.of(s2)))
                    .clickEvent(ClickEvent.runCommand(s2))).build();
        fp.toWorldEditPlayer().print(build);
    }

    @Command(
            name = "pattern",
            desc = "Select a pattern"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void pattern(FawePlayer fp, @Arg(name = "pattern", desc = "Pattern", def = "") Pattern pattern, InjectedValueAccess context)throws CommandException {
        CFISettings settings = assertSettings(fp);
        String[] split = getArguments(context).split(" ");
        int index = 2;
        settings.pattern = pattern;
        settings.patternArg = pattern == null ? null : split[index++];

        StringBuilder cmd = new StringBuilder(alias() + " pattern ");

        if (pattern != null) {
            settings.getCategory().accept(fp);
        } else {
            String s = cmd + " stone";
            String s1 = alias() + " " + settings.getCategory();
            TextComponent build = TextComponent.builder(">> Current Settings <<").append(newline())
                .append("Pattern ").append(TextComponent.of("[Click Here]")
                    .hoverEvent(HoverEvent.showText(TextComponent.of(s)))
                    .clickEvent(ClickEvent.suggestCommand(s)))
                .append(newline())
                .append(TextComponent.of("< [Back]")
                    .hoverEvent(HoverEvent.showText(TextComponent.of(s1)))
                    .clickEvent(ClickEvent.runCommand(s1))).build();
            fp.toWorldEditPlayer().print(build);
        }
    }

    @Command(
            name = "download",
            desc = "Download the current image"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void download(FawePlayer fp)throws IOException {
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
    public void image(FawePlayer fp, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, InjectedValueAccess context)throws CommandException {
        CFISettings settings = getSettings(fp);
        String[] split = getArguments(context).split(" ");
        int index = 2;

        settings.image = image;
        settings.imageArg = image != null ? split[index++] : null;

        if (image == null) {
            TextComponent build = TextComponent.builder("Please provide an image:")
                .append(newline())
                .append("From a URL: ").append(TextComponent.of("[Click Here]").clickEvent(ClickEvent.suggestCommand("/cfi image http://")))
                .append(newline())
                .append("From a file: ").append(TextComponent.of("[Click Here]").clickEvent(ClickEvent.suggestCommand("/cfi image file://")))
                .build();
            fp.toWorldEditPlayer().print(build);
        } else {
            if (settings.hasGenerator()) {
                coloring(fp);
                return;
            } else {
                heightmap(fp, image, 1);
                return;
            }
        }
    }

    @Command(
            name = "populate",
            desc = ""
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void populate(FawePlayer fp) {
        CFISettings settings = assertSettings(fp);
        settings.popMessages(fp);
        settings.setCategory(this::populate);
        TextComponent build = TextComponent.builder("What would you like to populate?")
            .append(newline())
            .append("(You will need to type these commands)").append(newline())
            //TODO .cmdOptions(alias() + " ", "", "Ores", "Ore", "Caves", "Schematics", "Smooth")
            .append(newline())
            .append(TextComponent.of("< [Back]").clickEvent(ClickEvent.runCommand(alias())))
            .build();
        fp.toWorldEditPlayer().print(build);
    }

    @Command(
            name = "component",
            aliases = {"components"},
            desc = "Components menu"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void component(FawePlayer fp) {
        CFISettings settings = assertSettings(fp);
        settings.popMessages(fp);
        settings.setCategory(this::component);

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

        //TODO
        @NonNull Builder msg = TextComponent.builder(">> Current Settings <<").append(newline())
                .append("Mask ").append(TextComponent.of("[" + mask + "]")
                .hoverEvent(HoverEvent.showText(TextComponent.of(alias() + " mask")))
                .clickEvent(ClickEvent.runCommand(alias() + " mask")))
                .append(newline())
                .append("Pattern ").append(TextComponent.of("[" + pattern + "]")
                .hoverEvent(HoverEvent.showText(TextComponent.of(alias() + " pattern")))
                .clickEvent(ClickEvent.runCommand(alias() + " pattern")))
                .append(newline())
                .append(newline())
                .append(">> Components <<")
                .append(newline())
                .append(TextComponent.of("[Height]")
                    .hoverEvent(HoverEvent.showText(TextComponent.of(alias() + " " + alias("height") + " 120")))
                    .clickEvent(ClickEvent.suggestCommand(alias() + " " + alias("height") + " 120"))).append(" - Terrain height for whole map")
                .append(newline())
                .append(TextComponent.of("[WaterHeight]")
                    .hoverEvent(HoverEvent.showText(TextComponent.of(alias() + " " + alias("waterheight") + " 60")))
                    .clickEvent(ClickEvent.suggestCommand(alias() + " " + alias("waterheight") + " 60"))).append(" - Sea level for whole map")
                .append(newline())
                .append(TextComponent.of("[FloorThickness]").hoverEvent(HoverEvent.showText(TextComponent.of(alias() + " " + alias("floorthickness") + " 60")))
                    .clickEvent(ClickEvent.suggestCommand(alias() + " " + alias("floorthickness") + " 60"))).append(" - Floor thickness of entire map")
                .append(newline())
                .append(TextComponent.of("[WorldThickness]").hoverEvent(HoverEvent.showText(TextComponent.of(alias() + " " + alias("worldthickness") + " 60")))
                    .clickEvent(ClickEvent.suggestCommand(alias() + " " + alias("worldthickness") + " 60"))).append(" - World thickness of entire map")
                .append(newline())
                .append(TextComponent.of("[Snow]").hoverEvent(HoverEvent.showText(TextComponent.of(alias() + " " + alias("snow") + maskArgs)))
                    .clickEvent(ClickEvent.suggestCommand(alias() + " " + alias("snow") + maskArgs))).append(" - Set snow in the masked areas")
                .append(newline());

        if (pattern != null) {
            String disabled = "You must specify a pattern";
            msg.append(TextComponent.of("[&cWaterId]").hoverEvent(HoverEvent.showText(TextComponent.of(disabled)))).append(newline())
                    .append(TextComponent.of("[&cBedrockId]").hoverEvent(HoverEvent.showText(TextComponent.of(disabled)))).append(newline()).append(newline())
                    .append(TextComponent.of("[&cFloor]").hoverEvent(HoverEvent.showText(TextComponent.of(disabled)))).append(newline()).append(newline())
                    .append(TextComponent.of("[&cMain]").hoverEvent(HoverEvent.showText(TextComponent.of(disabled)))).append(newline()).append(newline())
                    .append(TextComponent.of("[&cColumn]").hoverEvent(HoverEvent.showText(TextComponent.of(disabled)))).append(newline()).append(newline())
                    .append(TextComponent.of("[&cOverlay]").hoverEvent(HoverEvent.showText(TextComponent.of(disabled)))).append(newline()).append(newline());
        } else {
            StringBuilder compArgs = new StringBuilder();
            compArgs.append(" " + settings.patternArg + maskArgs);

            msg
                    .append(TextComponent.of("[WaterId]")
                        .hoverEvent(HoverEvent.showText(TextComponent.of(alias() + " waterId " + pattern)))
                        .clickEvent(ClickEvent.runCommand(alias() + " waterId " + pattern)))
                .append(" - Water id for whole map")
                .append(newline())
                    .append(TextComponent.of("[BedrockId]")
                        .hoverEvent(HoverEvent.showText(TextComponent.of(alias() + " baseId " + pattern)))
                        .clickEvent(ClickEvent.runCommand(alias() + " baseId " + pattern)))
                .append(TextComponent.of(" - Bedrock id for whole map"))
                .append(newline())
                    .append(TextComponent.of("[Floor]")
                        .hoverEvent(HoverEvent.showText(TextComponent.of(alias() + " floor " + compArgs)))
                        .clickEvent(ClickEvent.runCommand(alias() + " floor " + compArgs)))
                .append(TextComponent.of(" - Set the floor in the masked areas")).append(newline())
                    .append(TextComponent.of("[Main]")
                        .hoverEvent(HoverEvent.showText(TextComponent.of(alias() + " main " + compArgs)))
                        .clickEvent(ClickEvent.runCommand(alias() + " main " + compArgs)))
                .append(TextComponent.of(" - Set the main block in the masked areas")).append(newline())
                    .append(TextComponent.of("[Column]").hoverEvent(HoverEvent.showText(TextComponent.of(alias() + " column" + compArgs)))
                        .clickEvent(ClickEvent.runCommand(alias() + " column" + compArgs))).append(" - Set the columns in the masked areas").append(newline())
                    .append(TextComponent.of("[Overlay]").hoverEvent(HoverEvent.showText(TextComponent.of(alias() + " overlay" + compArgs)))
                        .clickEvent(ClickEvent.runCommand(alias() + " overlay" + compArgs))).append(" - Set the overlay in the masked areas").append(newline());
        }

        msg.append(newline())
                .append(TextComponent.of("< [Back]").hoverEvent(HoverEvent.showText(TextComponent.of(alias()))).clickEvent(ClickEvent.runCommand(alias())));
        fp.toWorldEditPlayer().print(msg.build());
    }

    private static CFISettings assertSettings(FawePlayer fp) {
        CFISettings settings = getSettings(fp);
        if (!settings.hasGenerator()) {
            throw new StopExecutionException(TextComponent.of("Please use /" + alias()));
        }
        return settings;
    }


    protected static CFISettings getSettings(FawePlayer fp) {
        CFISettings settings = fp.getMeta("CFISettings");
        return settings == null ? new CFISettings(fp) : settings;
    }

    public static class CFISettings {
        private final FawePlayer fp;

        private HeightMapMCAGenerator generator;

        protected ProvideBindings.ImageUri image;
        protected String imageArg;
        protected Mask mask;
        protected ProvideBindings.ImageUri imageMask;
        protected boolean whiteOnly = true;
        protected String maskArg;
        protected String imageMaskArg;

        protected Pattern pattern;
        protected String patternArg;

        protected Consumer<FawePlayer> category;

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

        public void setImage(ProvideBindings.ImageUri image, String arg) {
            this.image = image;
        }

        public void setImageMask(ProvideBindings.ImageUri imageMask, String arg) {
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

        public Consumer<FawePlayer> getCategory() {
            return category;
        }

        public void setCategory(Consumer<FawePlayer> methodRef) {
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

    protected static String alias() {
        return Commands.getAlias(CFICommand.class, "/cfi");
    }

    protected static String alias(String command) {
        return Commands.getAlias(CFICommands.class, command);
    }

    @SuppressWarnings("unused")
    protected static void mainMenu(FawePlayer fp) {
        //TODO
//        msg("What do you want to do now?").append(newline())
//                .cmdOptions(alias() + " ", "", "Coloring", "Component", "Populate", "Brush")
//                .append(newline()).text("<> [View]").command(alias() + " " + Commands.getAlias(CFICommands.class, "download")).tooltip("View full resolution image")
//                .append(newline()).text(">< [Cancel]").cmdTip(alias() + " " + Commands.getAlias(CFICommands.class, "cancel"))
//                .append(newline()).text("&2>> [Done]").cmdTip(alias() + " " + Commands.getAlias(CFICommands.class, "done"))
//                .send(fp);
    }
}
