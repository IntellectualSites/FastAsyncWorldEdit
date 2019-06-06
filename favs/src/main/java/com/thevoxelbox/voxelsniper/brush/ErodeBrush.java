package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.boydti.fawe.bukkit.wrapper.AsyncWorld;
import com.google.common.base.Objects;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;
import com.thevoxelbox.voxelsniper.jsap.HelpJSAP;
import com.thevoxelbox.voxelsniper.jsap.NullableIntegerStringParser;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.ChatPaginator;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * @author Piotr
 * @author MikeMatrix
 */
public class ErodeBrush extends Brush {
    private static final Vector[] FACES_TO_CHECK = {new Vector(0, 0, 1), new Vector(0, 0, -1), new Vector(0, 1, 0), new Vector(0, -1, 0), new Vector(1, 0, 0), new Vector(-1, 0, 0)};
    private final HelpJSAP parser = new HelpJSAP("/b e", "Brush for eroding landscape.", ChatPaginator.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH);
    private ErosionPreset currentPreset = new ErosionPreset(0, 1, 0, 1);

    /**
     *
     */
    public ErodeBrush() {
        this.setName("Erode");

        try {
            this.parser.registerParameter(new UnflaggedOption("preset", EnumeratedStringParser.getParser(Preset.getValuesString(";"), false), null, false, false, "Preset options: " + Preset.getValuesString(", ")));
            this.parser.registerParameter(new FlaggedOption("fill", NullableIntegerStringParser.getParser(), null, false, 'f', "fill", "Surrounding blocks required to fill the block."));
            this.parser.registerParameter(new FlaggedOption("erode", NullableIntegerStringParser.getParser(), null, false, 'e', "erode", "Surrounding air required to erode the block."));
            this.parser.registerParameter(new FlaggedOption("fillrecursion", NullableIntegerStringParser.getParser(), null, false, 'F', "fillrecursion", "Repeated fill iterations."));
            this.parser.registerParameter(new FlaggedOption("eroderecursion", NullableIntegerStringParser.getParser(), null, false, 'E', "eroderecursion", "Repeated erode iterations."));
        } catch (JSAPException ignored) {
        }
    }

    /**
     * @param result
     * @param player
     * @param helpJSAP
     * @return if a message was sent.
     */
    public static boolean sendHelpOrErrorMessageToPlayer(final JSAPResult result, final Player player, final HelpJSAP helpJSAP) {
        final List<String> output = helpJSAP.writeHelpOrErrorMessageIfRequired(result);
        if (!output.isEmpty()) {
            for (final String string : output) {
                player.sendMessage(string);
            }
            return true;
        }
        return false;
    }

    @Override
    protected final void arrow(final SnipeData v) {
        this.erosion(v, this.currentPreset);
    }

    @SuppressWarnings("deprecation")
    private void erosion(final SnipeData v, final ErosionPreset erosionPreset) {

        final BlockChangeTracker blockChangeTracker = new BlockChangeTracker(this.getTargetBlock().getWorld());

        final Vector targetBlockVector = this.getTargetBlock().getLocation().toVector();

        for (int i = 0; i < erosionPreset.getErosionRecursion(); ++i) {
            erosionIteration(v, erosionPreset, blockChangeTracker, targetBlockVector);
        }

        for (int i = 0; i < erosionPreset.getFillRecursion(); ++i) {
            fillIteration(v, erosionPreset, blockChangeTracker, targetBlockVector);
        }

        final Undo undo = new Undo();
        for (final BlockWrapper blockWrapper : blockChangeTracker.getAll()) {
            undo.put(blockWrapper.getBlock());
            blockWrapper.getBlock().setTypeIdAndPropertyId(BukkitAdapter.adapt(blockWrapper.getMaterial()).getInternalId(), blockWrapper.getPropertyId(), true);
        }

        v.owner().storeUndo(undo);
    }

    private void fillIteration(final SnipeData v, final ErosionPreset erosionPreset, final BlockChangeTracker blockChangeTracker, final Vector targetBlockVector) {
        final int currentIteration = blockChangeTracker.nextIteration();
        for (int x = this.getTargetBlock().getX() - v.getBrushSize(); x <= this.getTargetBlock().getX() + v.getBrushSize(); ++x) {
            for (int z = this.getTargetBlock().getZ() - v.getBrushSize(); z <= this.getTargetBlock().getZ() + v.getBrushSize(); ++z) {
                for (int y = this.getTargetBlock().getY() - v.getBrushSize(); y <= this.getTargetBlock().getY() + v.getBrushSize(); ++y) {
                    final Vector currentPosition = new Vector(x, y, z);
                    if (currentPosition.isInSphere(targetBlockVector, v.getBrushSize())) {
                        final BlockWrapper currentBlock = blockChangeTracker.get(currentPosition, currentIteration);

                        if (!(currentBlock.isEmpty() || currentBlock.isLiquid())) {
                            continue;
                        }

                        int count = 0;

                        final Map<BlockWrapper, Integer> blockCount = new HashMap<>();

                        for (final Vector vector : ErodeBrush.FACES_TO_CHECK) {
                            final Vector relativePosition = currentPosition.clone().add(vector);
                            final BlockWrapper relativeBlock = blockChangeTracker.get(relativePosition, currentIteration);

                            if (!(relativeBlock.isEmpty() || relativeBlock.isLiquid())) {
                                count++;
                                final BlockWrapper typeBlock = new BlockWrapper(null, relativeBlock.getMaterial(), relativeBlock.getPropertyId());
                                if (blockCount.containsKey(typeBlock)) {
                                    blockCount.put(typeBlock, blockCount.get(typeBlock) + 1);
                                } else {
                                    blockCount.put(typeBlock, 1);
                                }
                            }
                        }

                        BlockWrapper currentMaterial = new BlockWrapper(null, Material.AIR, 0);
                        int amount = 0;

                        for (final BlockWrapper wrapper : blockCount.keySet()) {
                            final Integer currentCount = blockCount.get(wrapper);
                            if (amount <= currentCount) {
                                currentMaterial = wrapper;
                                amount = currentCount;
                            }
                        }

                        if (count >= erosionPreset.getFillFaces()) {
                            blockChangeTracker.put(currentPosition, new BlockWrapper(currentBlock.getBlock(), currentMaterial.getMaterial(), currentMaterial.getPropertyId()), currentIteration);
                        }
                    }
                }
            }
        }
    }

    private void erosionIteration(final SnipeData v, final ErosionPreset erosionPreset, final BlockChangeTracker blockChangeTracker, final Vector targetBlockVector) {
        final int currentIteration = blockChangeTracker.nextIteration();
        for (int x = this.getTargetBlock().getX() - v.getBrushSize(); x <= this.getTargetBlock().getX() + v.getBrushSize(); ++x) {
            for (int z = this.getTargetBlock().getZ() - v.getBrushSize(); z <= this.getTargetBlock().getZ() + v.getBrushSize(); ++z) {
                for (int y = this.getTargetBlock().getY() - v.getBrushSize(); y <= this.getTargetBlock().getY() + v.getBrushSize(); ++y) {
                    final Vector currentPosition = new Vector(x, y, z);
                    if (currentPosition.isInSphere(targetBlockVector, v.getBrushSize())) {
                        final BlockWrapper currentBlock = blockChangeTracker.get(currentPosition, currentIteration);

                        if (currentBlock.isEmpty() || currentBlock.isLiquid()) {
                            continue;
                        }

                        int count = 0;
                        for (final Vector vector : ErodeBrush.FACES_TO_CHECK) {
                            final Vector relativePosition = currentPosition.clone().add(vector);
                            final BlockWrapper relativeBlock = blockChangeTracker.get(relativePosition, currentIteration);

                            if (relativeBlock.isEmpty() || relativeBlock.isLiquid()) {
                                count++;
                            }
                        }

                        if (count >= erosionPreset.getErosionFaces()) {
                            blockChangeTracker.put(currentPosition, new BlockWrapper(currentBlock.getBlock(), Material.AIR, 0), currentIteration);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected final void powder(final SnipeData v) {
        this.erosion(v, this.currentPreset.getInverted());
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
        vm.size();
        vm.custom(ChatColor.AQUA + "Erosion minimum exposed faces set to " + this.currentPreset.getErosionFaces());
        vm.custom(ChatColor.BLUE + "Fill minumum touching faces set to " + this.currentPreset.getFillFaces());
        vm.custom(ChatColor.DARK_BLUE + "Erosion recursion amount set to " + this.currentPreset.getErosionRecursion());
        vm.custom(ChatColor.DARK_GREEN + "Fill recursion amount set to " + this.currentPreset.getFillRecursion());
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        JSAPResult result = this.parser.parse(Arrays.copyOfRange(par, 1, par.length));

        if (sendHelpOrErrorMessageToPlayer(result, v.owner().getPlayer(), this.parser)) {
            return;
        }

        if (result.getString("preset") != null) {
            try {
                this.currentPreset = Preset.valueOf(result.getString("preset").toUpperCase()).getPreset();
                v.getVoxelMessage().brushMessage("Brush preset set to " + result.getString("preset"));
                return;
            } catch (final IllegalArgumentException exception) {
                v.getVoxelMessage().brushMessage("No such preset.");
                return;
            }
        }

        ErosionPreset currentPresetBackup = this.currentPreset;

        if (result.getObject("fill") != null) {
            this.currentPreset = new ErosionPreset(this.currentPreset.getErosionFaces(), this.currentPreset.getErosionRecursion(), result.getInt("fill"), this.currentPreset.getFillRecursion());
        }

        if (result.getObject("erode") != null) {
            this.currentPreset = new ErosionPreset(result.getInt("erode"), this.currentPreset.getErosionRecursion(), this.currentPreset.getFillFaces(), this.currentPreset.getFillRecursion());
        }

        if (result.getObject("fillrecursion") != null) {
            this.currentPreset = new ErosionPreset(this.currentPreset.getErosionFaces(), this.currentPreset.getErosionRecursion(), this.currentPreset.getFillFaces(), result.getInt("fillrecursion"));
        }

        if (result.getObject("eroderecursion") != null) {
            this.currentPreset = new ErosionPreset(this.currentPreset.getErosionFaces(), result.getInt("eroderecursion"), this.currentPreset.getFillFaces(), this.currentPreset.getFillRecursion());
        }

        if (!currentPreset.equals(currentPresetBackup)) {
            if (currentPreset.getErosionFaces() != currentPresetBackup.getErosionFaces()) {
                v.sendMessage(ChatColor.AQUA + "Erosion faces set to: " + ChatColor.WHITE + currentPreset.getErosionFaces());
            }
            if (currentPreset.getFillFaces() != currentPresetBackup.getFillFaces()) {
                v.sendMessage(ChatColor.AQUA + "Fill faces set to: " + ChatColor.WHITE + currentPreset.getFillFaces());
            }
            if (currentPreset.getErosionRecursion() != currentPresetBackup.getErosionRecursion()) {
                v.sendMessage(ChatColor.AQUA + "Erosion recursions set to: " + ChatColor.WHITE + currentPreset.getErosionRecursion());
            }
            if (currentPreset.getFillRecursion() != currentPresetBackup.getFillRecursion()) {
                v.sendMessage(ChatColor.AQUA + "Fill recursions set to: " + ChatColor.WHITE + currentPreset.getFillRecursion());
            }
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.erode";
    }

    /**
     * @author MikeMatrix
     */
    private enum Preset {
        MELT(new ErosionPreset(2, 1, 5, 1)), FILL(new ErosionPreset(5, 1, 2, 1)), SMOOTH(new ErosionPreset(3, 1, 3, 1)), LIFT(new ErosionPreset(6, 0, 1, 1)), FLOATCLEAN(new ErosionPreset(6, 1, 6, 1));
        private ErosionPreset preset;

        Preset(final ErosionPreset preset) {
            this.preset = preset;
        }

        /**
         * Generates a concat string of all options.
         *
         * @param seperator Seperator for delimiting entries.
         * @return
         */
        public static String getValuesString(String seperator) {
            String valuesString = "";

            boolean delimiterHelper = true;
            for (final Preset preset : Preset.values()) {
                if (delimiterHelper) {
                    delimiterHelper = false;
                } else {
                    valuesString += seperator;
                }
                valuesString += preset.name();
            }
            return valuesString;
        }

        public ErosionPreset getPreset() {
            return this.preset;
        }


    }

    /**
     * @author MikeMatrix
     */
    private static final class BlockChangeTracker {
        private final Map<Integer, Map<Vector, BlockWrapper>> blockChanges;
        private final Map<Vector, BlockWrapper> flatChanges;
        private final AsyncWorld world;
        private int nextIterationId = 0;

        public BlockChangeTracker(final AsyncWorld world) {
            this.blockChanges = new HashMap<>();
            this.flatChanges = new HashMap<>();
            this.world = world;
        }

        public BlockWrapper get(final Vector position, final int iteration) {
            BlockWrapper changedBlock = null;

            for (int i = iteration - 1; i >= 0; --i) {
                if (this.blockChanges.containsKey(i) && this.blockChanges.get(i).containsKey(position)) {
                    changedBlock = this.blockChanges.get(i).get(position);
                    return changedBlock;
                }
            }

            changedBlock = new BlockWrapper((AsyncBlock) position.toLocation(this.world).getBlock());

            return changedBlock;
        }

        public Collection<BlockWrapper> getAll() {
            return this.flatChanges.values();
        }

        public int nextIteration() {
            return this.nextIterationId++;
        }

        public void put(final Vector position, final BlockWrapper changedBlock, final int iteration) {
            if (!this.blockChanges.containsKey(iteration)) {
                this.blockChanges.put(iteration, new HashMap<>());
            }

            this.blockChanges.get(iteration).put(position, changedBlock);
            this.flatChanges.put(position, changedBlock);
        }
    }

    /**
     * @author MikeMatrix
     */
    private static final class BlockWrapper {

        private final AsyncBlock block;
        private final Material material;
        private final int data;

        @SuppressWarnings("deprecation")
        public BlockWrapper(final AsyncBlock block) {
            this.block = block;
            this.data = block.getPropertyId();
            this.material = block.getType();
        }

        public BlockWrapper(final AsyncBlock block, final Material material, final int data) {
            this.block = block;
            this.material = material;
            this.data = data;
        }

        /**
         * @return the block
         */
        public AsyncBlock getBlock() {
            return this.block;
        }

        /**
         * @return the data
         */
        public int getPropertyId() {
            return this.data;
        }

        /**
         * @return the material
         */
        public Material getMaterial() {
            return this.material;
        }

        /**
         * @return if the block is Empty.
         */
        public boolean isEmpty() {
            switch (material) {
                case AIR:
                case CAVE_AIR:
                case VOID_AIR:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * @return if the block is a Liquid.
         */
        public boolean isLiquid() {
            switch (this.material) {
                case WATER:
                case LAVA:
                    return true;
                default:
                    return false;
            }
        }

    }

    /**
     * @author MikeMatrix
     */
    private static final class ErosionPreset {
        private final int erosionFaces;
        private final int erosionRecursion;
        private final int fillFaces;
        private final int fillRecursion;

        public ErosionPreset(final int erosionFaces, final int erosionRecursion, final int fillFaces, final int fillRecursion) {
            this.erosionFaces = erosionFaces;
            this.erosionRecursion = erosionRecursion;
            this.fillFaces = fillFaces;
            this.fillRecursion = fillRecursion;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(erosionFaces, erosionRecursion, fillFaces, fillRecursion);
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof ErosionPreset) {
                ErosionPreset other = (ErosionPreset) obj;
                return Objects.equal(this.erosionFaces, other.erosionFaces) && Objects.equal(this.erosionRecursion, other.erosionRecursion) && Objects.equal(this.fillFaces, other.fillFaces) && Objects.equal(this.fillRecursion, other.fillRecursion);
            }
            return false;
        }

        /**
         * @return the erosionFaces
         */
        public int getErosionFaces() {
            return this.erosionFaces;
        }

        /**
         * @return the erosionRecursion
         */
        public int getErosionRecursion() {
            return this.erosionRecursion;
        }

        /**
         * @return the fillFaces
         */
        public int getFillFaces() {
            return this.fillFaces;
        }

        /**
         * @return the fillRecursion
         */
        public int getFillRecursion() {
            return this.fillRecursion;
        }

        public ErosionPreset getInverted() {
            return new ErosionPreset(this.fillFaces, this.fillRecursion, this.erosionFaces, this.erosionRecursion);
        }
    }
}
