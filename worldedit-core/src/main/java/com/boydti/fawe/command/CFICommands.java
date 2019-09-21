package com.boydti.fawe.command;

import static com.boydti.fawe.util.image.ImageUtil.load;
import static com.sk89q.worldedit.command.MethodCommands.getArguments;
import static com.sk89q.worldedit.util.formatting.text.TextComponent.newline;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.beta.SingleFilterBlock;
import com.boydti.fawe.config.BBC;
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
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.Switch;
import org.enginehub.piston.exception.StopExecutionException;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.jetbrains.annotations.NotNull;

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
    public void heightmap(Player player, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "yscale", desc = "double", def = "1") double yscale) {
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
        setup(generator, player);
    }

    @Command(
            name = "empty",
            desc = "Start CFI with an empty map as a base"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void heightMap(Player player, int width, int length) {
        HeightMapMCAGenerator generator = new HeightMapMCAGenerator(width, length, getFolder(generateName()));
        setup(generator, player);
    }

    private String generateName() {
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH.mm.ss");
        return df.format(new Date());
    }

    private void setup(HeightMapMCAGenerator generator, Player player) {
        CFISettings settings = getSettings(player).remove();
        generator.setPacketViewer(player);
        settings.setGenerator(generator).bind();
        generator.setImageViewer(Fawe.imp().getImageViewer(player));
        generator.update();
        mainMenu(player);
    }

    @Command(
            name = "brush",
            desc = "Info about using brushes with CFI"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void brush(Player player) {
        CFISettings settings = assertSettings(player);
        settings.popMessages(player);
        @NotNull Builder msg;
        if (settings.getGenerator().getImageViewer() != null) {
            msg = TextComponent.builder("CFI supports using brushes during creation").append(newline())
                    .append(" - Place the map on a wall of item frames").append(newline())
                    .append(" - Use any WorldEdit brush on the item frames").append(newline())
                    .append(" - Example: ").append(TextComponent.of("Video").clickEvent(ClickEvent.openUrl("https://goo.gl/PK4DMG"))).append(newline());
        } else {
            msg = TextComponent.builder("This is not supported with your platform/version").append(newline());
        }
        //TODO msg.text("< [Back]").cmdTip(alias()).send(player);
        player.print(msg.build());
    }

    @Command(
            name = "cancel",
            aliases = {"exit"},
            desc = "Cancel creation"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void cancel(Player player) {
        getSettings(player).remove();
        player.print("Cancelled!");
    }

    @Command(
            name = "done",
            aliases = "create",
            desc = "Create the world"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void done(Player player) {
        CFISettings settings = assertSettings(player);
        HeightMapMCAGenerator generator = settings.getGenerator();

        Function<File, Boolean> function = folder -> {
            if (folder != null) {
                try {
                    generator.setFolder(folder);
                    player.print("Generating " + folder);
                    generator.generate();
                    generator.setPacketViewer(null);
                    generator.setImageViewer(null);
                    settings.remove();
                    player.print("Done!");
                    return true;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                player.print("Unable to generate world... (see console)?");
            }
            return false;
        };

        try {
            new PlotLoader().load(player, settings, function);
        } catch (Throwable e) {
            e.printStackTrace();
            function.apply(generator.getFolder().getParentFile());
        }

        File folder = generator.getFolder();
        if (folder != null) {
            World world = FaweAPI.getWorld(folder.getName());
            if (world != null) {
                if (player.getWorld() != world) {
                    TaskManager.IMP.sync(new RunnableVal<Object>() {
                        @Override
                        public void run(Object value) {
                            Location spawn = new Location(world, world.getSpawnPosition().toVector3());
                            player.setPosition(spawn);
                        }
                    });
                }
            } else {
                player.print("Unable to import world (" + folder.getName() + ") please do so manually");
            }
        }
    }

    @Command(
            name = "column",
            desc = "Set the floor and main block"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void column(Player player, @Arg(name = "pattern", desc = "Pattern") Pattern patternArg, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask maskOpt, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly){
        HeightMapMCAGenerator gen = assertSettings(player).getGenerator();
        if (image != null) {
            gen.setColumn(load(image), patternArg, !disableWhiteOnly);
        } else if (maskOpt != null) {
            gen.setColumn(maskOpt, patternArg);
        } else {
            gen.setColumn(patternArg);
        }
        player.print("Set column!");
        assertSettings(player).resetComponent();
        component(player);
    }

    @Command(
            name = "floor",
            desc = "Set the floor (default: grass)"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void floorCmd(Player player, @Arg(name = "pattern", desc = "Pattern") Pattern patternArg, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask maskOpt, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly){
        floor(player, patternArg, image, maskOpt, disableWhiteOnly);
        player.print("Set floor!");
        assertSettings(player).resetComponent();
        component(player);
    }

    private void floor(Player player, @Arg(name = "pattern", desc = "Pattern") Pattern patternArg, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask maskOpt, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly) {
        HeightMapMCAGenerator gen = assertSettings(player).getGenerator();
        if (image != null) {
            gen.setFloor(load(image), patternArg, !disableWhiteOnly);
        } else if (maskOpt != null) {
            gen.setFloor(maskOpt, patternArg);
        } else {
            gen.setFloor(patternArg);
        }
    }

    @Command(
            name = "main",
            desc = "Set the main block (default: stone)"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void mainCmd(Player player, @Arg(name = "pattern", desc = "Pattern") Pattern patternArg, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask maskOpt, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly){
        main(player, patternArg, image, maskOpt, disableWhiteOnly);
        player.print("Set main!");
        assertSettings(player).resetComponent();
        component(player);
    }

    public void main(Player player, @Arg(name = "pattern", desc = "Pattern") Pattern patternArg, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask maskOpt, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly){
        HeightMapMCAGenerator gen = assertSettings(player).getGenerator();
        if (image != null) {
            gen.setMain(load(image), patternArg, !disableWhiteOnly);
        } else if (maskOpt != null) {
            gen.setMain(maskOpt, patternArg);
        } else {
            gen.setMain(patternArg);
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
    public void overlay(Player player, @Arg(name = "pattern", desc = "Pattern") Pattern patternArg, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask maskOpt, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly){
        HeightMapMCAGenerator gen = assertSettings(player).getGenerator();
        if (image != null) {
            gen.setOverlay(load(image), patternArg, !disableWhiteOnly);
        } else if (maskOpt != null) {
            gen.setOverlay(maskOpt, patternArg);
        } else {
            gen.setOverlay(patternArg);
        }
        player.print("Set overlay!");
        component(player);
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
    public void smoothCmd(Player player, int radius, int iterations, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask maskOpt, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly){
        smooth(player, radius, iterations, image, maskOpt, disableWhiteOnly);
        assertSettings(player).resetComponent();
        component(player);
    }

    private void smooth(Player player, int radius, int iterations, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask maskOpt, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly){
        HeightMapMCAGenerator gen = assertSettings(player).getGenerator();
        if (image != null) {
            gen.smooth(load(image), !disableWhiteOnly, radius, iterations);
        } else {
            gen.smooth(maskOpt, radius, iterations);
        }
    }

    @Command(
            name = "snow",
            desc = "Create some snow"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void snow(Player player, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask maskOpt, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly){
        HeightMapMCAGenerator gen = assertSettings(player).getGenerator();
        floor(player, BlockTypes.SNOW.getDefaultState().with(PropertyKey.LAYERS, 7), image, maskOpt, disableWhiteOnly);
        main(player, BlockTypes.SNOW_BLOCK, image, maskOpt, disableWhiteOnly);
        smooth(player, 1, 8, image, maskOpt, disableWhiteOnly);
        player.print(TextComponent.of("Added snow!"));
        assertSettings(player).resetComponent();
        component(player);
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
    public void biomepriority(Player fp, int value) {
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
    public void paletteblocks(Player player, LocalSession session, @Arg(name = "arg", desc = "String", def = "") String argOpt) throws EmptyClipboardException, InputParseException, FileNotFoundException {
        if (argOpt == null) {
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
                    .runCommand("/cfi coloring")))
                .build();
            player.print(build);
            return;
        }
        HeightMapMCAGenerator generator = assertSettings(player).getGenerator();
        ParserContext context = new ParserContext();
        context.setActor(player);
        context.setWorld(player.getWorld());
        context.setSession(player.getSession());
        context.setExtent(generator);
        Request.request().setExtent(generator);

        Set<BlockType> blocks;
        switch (argOpt.toLowerCase()) {
            case "true":
            case "*": {
                generator.setTextureUtil(Fawe.get().getTextureUtil());
                return;
            }
            case "#clipboard": {
                ClipboardHolder holder = player.getSession().getClipboard();
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
                Mask mask = worldEdit.getMaskFactory().parseFromInput(argOpt, parserContext);
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
        coloring(player);
    }

    @Command(
            name = "randomization",
            desc = "Set whether randomization is enabled",
            descFooter = "This is enabled by default, randomization will add some random variation in the blocks used to closer match the provided image.\n" +
                    "If disabled, the closest block to the color will always be used.\n" +
                    "Randomization will allow mixing biomes when coloring with biomes"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void randomization(Player player, boolean enabled) {
        assertSettings(player).getGenerator().setTextureRandomVariation(enabled);
        coloring(player);
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
    public void complexity(Player player, int min, int max) throws  FileNotFoundException {
        HeightMapMCAGenerator gen = assertSettings(player).getGenerator();
        if (min == 0 && max == 100) {
            gen.setTextureUtil(Fawe.get().getTextureUtil());
        } else {
            gen.setTextureUtil(new CleanTextureUtil(Fawe.get().getTextureUtil(), min, max));
        }
        coloring(player);
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
    public void schem(Player player, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri imageMask, @Arg(name = "mask", desc = "Mask") Mask mask, String schematic, int rarity, int distance, boolean rotate)throws IOException, WorldEditException {
        HeightMapMCAGenerator gen = assertSettings(player).getGenerator();

        World world = player.getWorld();
        MultiClipboardHolder multi = ClipboardFormats.loadAllFromInput(player, schematic, null, true);
        if (multi == null) {
            return;
        }
        if (imageMask == null) {
            gen.addSchems(mask, multi.getHolders(), rarity, distance, rotate);
        } else {
            gen.addSchems(load(imageMask), mask, multi.getHolders(), rarity, distance, rotate);
        }
        player.print(TextComponent.of("Added schematics!"));
        populate(player);
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
    public void biome(Player player, @Arg(name = "biome", desc = "Biome type") BiomeType biomeType, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(name = "mask", desc = "Mask", def = "") Mask maskOpt, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly){
        HeightMapMCAGenerator gen = assertSettings(player).getGenerator();
        if (image != null) {
            gen.setBiome(load(image), biomeType, !disableWhiteOnly);
        } else if (maskOpt != null) {
            gen.setBiome(maskOpt, biomeType);
        } else {
            gen.setBiome(biomeType);
        }
        player.print(TextComponent.of("Set biome!"));
        assertSettings(player).resetComponent();
        component(player);
    }

    @Command(
            name = "caves",
            desc = "Generate vanilla caves"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void caves(Player fp) throws WorldEditException {
        assertSettings(fp).getGenerator().addCaves();
        fp.print(TextComponent.of("Added caves!"));
        populate(fp);
    }

    @Command(
            name = "ore",
            desc = "Add an ore",
            descFooter = "Use a specific pattern and settings to generate ore"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void ore(Player fp, @Arg(name = "mask", desc = "Mask") Mask mask, @Arg(name = "pattern", desc = "Pattern") Pattern patternArg, int size, int frequency, int rariry, int minY, int maxY) throws WorldEditException {
        assertSettings(fp).getGenerator().addOre(mask, patternArg, size, frequency, rariry, minY, maxY);
        fp.print(TextComponent.of("Added ore!"));
        populate(fp);
    }

    @Command(
            name = "ores",
            desc = "Generate the vanilla ores"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void ores(Player fp, @Arg(name = "mask", desc = "Mask") Mask mask) throws WorldEditException {
        assertSettings(fp).getGenerator().addDefaultOres(mask);
        fp.print(TextComponent.of("Added ores!"));
        populate(fp);
    }

    @Command(
            name = "height",
            desc = "Set the height",
            descFooter = "Set the terrain height either based on an image heightmap, or a numeric value."
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void height(Player fp, String imageStr) throws WorldEditException {
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (!MathMan.isInteger(imageStr)) {
            gen.setHeight(ImageUtil.getImage(imageStr));
        } else {
            gen.setHeights(Integer.parseInt(imageStr));
        }
        fp.print("Set Height!");
        component(fp);
    }

    @Command(
            name = "water",
            desc = "Change the block used for water\ne.g. Lava"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void waterId(Player fp, BlockStateHolder block) throws WorldEditException {
        CFISettings settings = assertSettings(fp);
        settings.getGenerator().setWaterId(block.getBlockType().getInternalId());

        fp.print("Set water id!");
        settings.resetComponent();
        component(fp);
    }

    @Command(
            name = "baseid",
            aliases = {"bedrockid"},
            desc = "Change the block used for the base\ne.g. Bedrock"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void baseId(Player fp, BlockStateHolder block) throws WorldEditException {
        CFISettings settings = assertSettings(fp);
        settings.getGenerator().setBedrockId(block.getBlockType().getInternalId());
        fp.print(TextComponent.of("Set base id!"));
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
    public void worldthickness(Player fp, @Arg(name = "height", desc = "brush height") int heightArg) throws WorldEditException {
        assertSettings(fp).getGenerator().setWorldThickness(heightArg);
        fp.print("Set world thickness!");
        component(fp);
    }

    @Command(
            name = "floorthickness",
            aliases = {"floorheight", "floorwidth"},
            desc = "Set the thickness of the top layer\n" +
                    " - A value of 0 is the default and will only set the top block"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void floorthickness(Player fp, @Arg(name = "height", desc = "brush height") int heightArg) throws WorldEditException {
        assertSettings(fp).getGenerator().setFloorThickness(heightArg);
        fp.print("Set floor thickness!");
        component(fp);
    }

    @Command(
            name = "update",
            aliases = {"refresh", "resend"},
            desc = "Resend the CFI chunks"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void update(Player fp) throws WorldEditException {
        assertSettings(fp).getGenerator().update();
        fp.print("Chunks refreshed!");
        mainMenu(fp);
    }

    @Command(
            name = "tp",
            aliases = {"visit", "home"},
            desc = "Teleport to the CFI virtual world"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void tp(Player player) throws WorldEditException {
        HeightMapMCAGenerator gen = assertSettings(player).getGenerator();
        player.print("Teleporting...");
        Vector3 origin = gen.getOrigin();
        player.setPosition(origin.subtract(16, 0, 16));
        player.findFreePosition();
        mainMenu(player);
    }

    @Command(
            name = "waterheight",
            aliases = {"sealevel", "setwaterheight"},
            desc = "Set the level water is generated at\n" +
                    "Set the level water is generated at\n" +
                    " - By default water is disabled (with a value of 0)"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void waterheight(Player fp, @Arg(name = "height", desc = "brush height") int heightArg) throws WorldEditException {
        assertSettings(fp).getGenerator().setWaterHeight(heightArg);
        fp.print("Set water height!");
        component(fp);
    }

    @Command(
            name = "glass",
            aliases = {"glasscolor", "setglasscolor"},
            desc = "Color terrain using glass"
    )
    // ![79,174,212,5:3,5:4,18,161,20]
    @CommandPermissions("worldedit.anvil.cfi")
    public void glass(Player fp, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri imageMask, @Arg(name = "mask", desc = "Mask", def = "") Mask maskOpt, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly) throws WorldEditException {
        CFISettings settings = assertSettings(fp);
        settings.getGenerator().setColorWithGlass(load(image));
        fp.print("Set color with glass!");
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
    public void color(Player fp, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri imageMask, @Arg(name = "mask", desc = "Mask", def = "") Mask maskOpt, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly) throws WorldEditException {
        CFISettings settings = assertSettings(fp);
        HeightMapMCAGenerator gen = settings.getGenerator();
        if (imageMask != null) {
            gen.setColor(load(image), load(imageMask), !disableWhiteOnly);
        } else if (maskOpt != null) {
            gen.setColor(load(image), maskOpt);
        } else {
            gen.setColor(load(image));
        }
        settings.resetColoring();
        fp.print("Set color with blocks!");
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
    public void blockbiome(Player fp, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri imageMask, @Arg(name = "mask", desc = "Mask", def = "") Mask maskOpt, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly) throws WorldEditException {
        CFISettings settings = assertSettings(fp);
        settings.getGenerator().setBlockAndBiomeColor(load(image), maskOpt, load(imageMask), !disableWhiteOnly);
        fp.print(TextComponent.of("Set color with blocks and biomes!"));
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
    public void biomecolor(Player fp, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri image, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri imageMask, @Arg(name = "mask", desc = "Mask", def = "") Mask maskOpt, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly) throws WorldEditException {
        CFISettings settings = assertSettings(fp);
        settings.getGenerator().setBiomeColor(load(image));
        fp.print(TextComponent.of("Set color with biomes!"));
        settings.resetColoring();
        mainMenu(fp);
    }


    @Command(
            name = "coloring",
            aliases = {"palette"},
            desc = "Color the world using an image"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void coloring(Player fp) {
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
        @NotNull Builder builder = TextComponent.builder(">> Current Settings <<").append(newline())
                .append("Randomization ").append("[" + Boolean.toString(rand).toUpperCase() + "]")//.cmdTip("/cfi randomization " + (!rand))
                .append(newline())
                .append("Mask ").append("[" + mask + "]")//.cmdTip("/cfi mask")
                .append(newline())
                .append("Blocks ").append("[" + blocks + "]")//.tooltip(blockList).command("/cfi paletteBlocks")
                .append(newline())
                .append("BiomePriority ").append("[" + biomePriority + "]")//.cmdTip("/cfi biomepriority")
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
                    .append("[" + settings.imageArg + "]")//.cmdTip("/cfi " + Commands.getAlias(CFICommands.class, "image"))
                    .append(newline()).append(newline())
                    .append("Let's Color: ")
                    //.cmdOptions("/cfi ", colorArgs.toString(), "Biomes", "Blocks", "BlockAndBiome", "Glass")
                    .append(newline());
        } else {
            builder.append(newline()).append("You can color a world using an image like ")
                    .append(TextComponent.of("[This]").clickEvent(ClickEvent.openUrl("http://i.imgur.com/vJYinIU.jpg"))).append(newline())
                    .append("You MUST provide an image: ")
                    .append("[None]");//.cmdTip("/cfi " + Commands.getAlias(Command.class, "image")).append(newline());
        }
        builder.append("< [Back]");//.cmdTip(alias()).send(fp);
        fp.print(builder.build());
    }

    @Command(
            name = "mask",
            desc = "Select a mask"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void mask(Player fp, @Arg(def = "", desc = "image url or filename") ProvideBindings.ImageUri imageMask, @Arg(name = "mask", desc = "Mask", def = "") Mask maskOpt, @Switch(name = 'w', desc = "TODO") boolean disableWhiteOnly, InjectedValueAccess context){
        CFISettings settings = assertSettings(fp);
        String[] split = getArguments(context).split(" ");
        int index = 2;
        settings.imageMask = imageMask;
        settings.imageMaskArg = imageMask != null ? split[index++] : null;
        settings.mask = maskOpt;
        settings.maskArg = maskOpt != null ? split[index++] : null;
        settings.whiteOnly = !disableWhiteOnly;

        String s = "/cfi mask http://";
        String s1 = "/cfi mask <mask>";
        String s2 = "/cfi " + settings.getCategory();
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
        fp.print(build);
    }

    @Command(
            name = "pattern",
            desc = "Select a pattern"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void pattern(Player fp, @Arg(name = "pattern", desc = "Pattern", def = "") Pattern patternArg, InjectedValueAccess context)throws CommandException {
        CFISettings settings = assertSettings(fp);
        String[] split = getArguments(context).split(" ");
        int index = 2;
        settings.pattern = patternArg;
        settings.patternArg = patternArg == null ? null : split[index++];

        StringBuilder cmd = new StringBuilder("/cfi pattern ");

        if (patternArg != null) {
            settings.getCategory().accept(fp);
        } else {
            String s = cmd + " stone";
            String s1 = "/cfi " + settings.getCategory();
            TextComponent build = TextComponent.builder(">> Current Settings <<").append(newline())
                .append("Pattern ").append(TextComponent.of("[Click Here]")
                    .hoverEvent(HoverEvent.showText(TextComponent.of(s)))
                    .clickEvent(ClickEvent.suggestCommand(s)))
                .append(newline())
                .append(TextComponent.of("< [Back]")
                    .hoverEvent(HoverEvent.showText(TextComponent.of(s1)))
                    .clickEvent(ClickEvent.runCommand(s1))).build();
            fp.print(build);
        }
    }

    @Command(
            name = "download",
            desc = "Download the current image"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void download(Player player)throws IOException {
        CFISettings settings = assertSettings(player);
        BufferedImage image = settings.getGenerator().draw();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] data = baos.toByteArray();
        player.print("Please wait...");
        URL url = ImgurUtility.uploadImage(data);
        BBC.DOWNLOAD_LINK.send(player, url);
    }

    @Command(
            name = "image",
            desc = "Select an image"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void image(Player fp, @Arg(desc = "image url or filename", def = "") ProvideBindings.ImageUri image, InjectedValueAccess context)throws CommandException {
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
            fp.print(build);
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
    public void populate(Player player) {
        CFISettings settings = assertSettings(player);
        settings.popMessages(player);
        settings.setCategory(this::populate);
        TextComponent build = TextComponent.builder("What would you like to populate?")
            .append(newline())
            .append("(You will need to type these commands)").append(newline())
            //TODO .cmdOptions("/cfi ", "", "Ores", "Ore", "Caves", "Schematics", "Smooth")
            .append(newline())
            .append(TextComponent.of("< [Back]").clickEvent(ClickEvent.runCommand("/cfi")))
            .build();
        player.print(build);
    }

    @Command(
            name = "component",
            aliases = {"components"},
            desc = "Components menu"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void component(Player player) {
        CFISettings settings = assertSettings(player);
        settings.popMessages(player);
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
            maskArgs.append(" ").append(settings.imageMaskArg);
        }
        if (settings.mask != null) {
            maskArgs.append(" ").append(settings.maskArg);
        }
        if (!settings.whiteOnly) {
            maskArgs.append(" -w");
        }

        String height = "/cfi height";
        String waterHeight = "/cfi waterheight";
        String snow = "/cfi snow";

        //TODO
        @NotNull Builder msg = TextComponent.builder(">> Current Settings <<").append(newline())
                .append("Mask ").append(TextComponent.of("[" + mask + "]")
                .hoverEvent(HoverEvent.showText(TextComponent.of("/cfi mask")))
                .clickEvent(ClickEvent.runCommand("/cfi mask")))
                .append(newline())
                .append("Pattern ").append(TextComponent.of("[" + pattern + "]")
                .hoverEvent(HoverEvent.showText(TextComponent.of("/cfi pattern")))
                .clickEvent(ClickEvent.runCommand("/cfi pattern")))
                .append(newline())
                .append(newline())
                .append(">> Components <<")
                .append(newline())
                .append(TextComponent.of("[Height]")
                    .hoverEvent(HoverEvent.showText(TextComponent.of("/cfi height 120")))
                    .clickEvent(ClickEvent.suggestCommand("/cfi height 120"))).append(" - Terrain height for whole map")
                .append(newline())
                .append(TextComponent.of("[WaterHeight]")
                    .hoverEvent(HoverEvent.showText(TextComponent.of("/cfi waterheight 60")))
                    .clickEvent(ClickEvent.suggestCommand("/cfi waterheight 60"))).append(" - Sea level for whole map")
                .append(newline())
                .append(TextComponent.of("[FloorThickness]").hoverEvent(HoverEvent.showText(TextComponent.of("/cfi floorthickness 60")))
                    .clickEvent(ClickEvent.suggestCommand("/cfi floorthickness 60"))).append(" - Floor thickness of entire map")
                .append(newline())
                .append(TextComponent.of("[WorldThickness]").hoverEvent(HoverEvent.showText(TextComponent.of("/cfi worldthickness 60")))
                    .clickEvent(ClickEvent.suggestCommand("/cfi worldthickness 60"))).append(" - World thickness of entire map")
                .append(newline())
                .append(TextComponent.of("[Snow]").hoverEvent(HoverEvent.showText(TextComponent.of("/cfi snow" + maskArgs)))
                    .clickEvent(ClickEvent.suggestCommand("/cfi snow" + maskArgs))).append(" - Set snow in the masked areas")
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
                        .hoverEvent(HoverEvent.showText(TextComponent.of("/cfi waterId " + pattern)))
                        .clickEvent(ClickEvent.runCommand("/cfi waterId " + pattern)))
                .append(" - Water id for whole map")
                .append(newline())
                    .append(TextComponent.of("[BedrockId]")
                        .hoverEvent(HoverEvent.showText(TextComponent.of("/cfi baseId " + pattern)))
                        .clickEvent(ClickEvent.runCommand("/cfi baseId " + pattern)))
                .append(TextComponent.of(" - Bedrock id for whole map"))
                .append(newline())
                    .append(TextComponent.of("[Floor]")
                        .hoverEvent(HoverEvent.showText(TextComponent.of("/cfi floor " + compArgs)))
                        .clickEvent(ClickEvent.runCommand("/cfi floor " + compArgs)))
                .append(TextComponent.of(" - Set the floor in the masked areas")).append(newline())
                    .append(TextComponent.of("[Main]")
                        .hoverEvent(HoverEvent.showText(TextComponent.of("/cfi main " + compArgs)))
                        .clickEvent(ClickEvent.runCommand("/cfi main " + compArgs)))
                .append(TextComponent.of(" - Set the main block in the masked areas")).append(newline())
                    .append(TextComponent.of("[Column]").hoverEvent(HoverEvent.showText(TextComponent.of("/cfi column" + compArgs)))
                        .clickEvent(ClickEvent.runCommand("/cfi column" + compArgs))).append(" - Set the columns in the masked areas").append(newline())
                    .append(TextComponent.of("[Overlay]").hoverEvent(HoverEvent.showText(TextComponent.of("/cfi overlay" + compArgs)))
                        .clickEvent(ClickEvent.runCommand("/cfi overlay" + compArgs))).append(" - Set the overlay in the masked areas").append(newline());
        }

        msg.append(newline())
                .append(TextComponent.of("< [Back]").hoverEvent(HoverEvent.showText(TextComponent.of("/cfi"))).clickEvent(ClickEvent.runCommand("/cfi")));
        player.print(msg.build());
    }

    private static CFISettings assertSettings(Player player) {
        CFISettings settings = getSettings(player);
        if (!settings.hasGenerator()) {
            throw new StopExecutionException(TextComponent.of("Please use /cfi"));
        }
        return settings;
    }


    protected static CFISettings getSettings(Player fp) {
        CFISettings settings = fp.getMeta("CFISettings");
        return settings == null ? new CFISettings(fp) : settings;
    }

    public static class CFISettings {
        private final Player player;

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

        protected Consumer<Player> category;

        private boolean bound;

        public CFISettings(Player player) {
            this.player = player;
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

        public Consumer<Player> getCategory() {
            return category;
        }

        public void setCategory(Consumer<Player> methodRef) {
            this.category = category;
        }

        public CFISettings setGenerator(HeightMapMCAGenerator generator) {
            this.generator = generator;
            if (bound) {
                player.getSession().setVirtualWorld(generator);
            }
            return this;
        }

        public CFISettings bind() {
            if (generator != null) {
                player.getSession().setVirtualWorld(generator);
            }
            bound = true;
            player.setMeta("CFISettings", this);
            return this;
        }

        public void popMessages(Player player) {
            ArrayDeque<String> messages = player.deleteMeta("CFIBufferedMessages");
            if (messages != null) {
                for (String message : messages) {
                    player.print(message);
                }
            }
        }

        public CFISettings remove() {
            player.deleteMeta("CFISettings");
            HeightMapMCAGenerator gen = this.generator;
            if (gen != null) {
                player.getSession().setVirtualWorld(null);
            }
            popMessages(player);
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

    @SuppressWarnings("unused")
    protected static void mainMenu(Player player) {
        //TODO
//        msg("What do you want to do now?").append(newline())
//                .cmdOptions("/cfi ", "", "Coloring", "Component", "Populate", "Brush")
//                .append(newline()).text("<> [View]").command("/cfi " + Commands.getAlias(CFICommands.class, "download")).tooltip("View full resolution image")
//                .append(newline()).text(">< [Cancel]").cmdTip("/cfi " + Commands.getAlias(CFICommands.class, "cancel"))
//                .append(newline()).text("&2>> [Done]").cmdTip("/cfi " + Commands.getAlias(CFICommands.class, "done"))
//                .send(fp);
    }
}
