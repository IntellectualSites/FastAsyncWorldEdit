package com.thevoxelbox.voxelsniper.brush;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Random;

// Proposal: Use /v and /vr for leave and wood material // or two more parameters -- Monofraps

public class GenerateTreeBrush extends Brush {
    // Tree Variables.
    private Random randGenerator = new Random();
    private ArrayList<Block> branchBlocks = new ArrayList<>();
    private Undo undo;
    // If these default values are edited. Remember to change default values in the default preset.
    private Material leafType = Material.OAK_LEAVES;
    private Material woodType = Material.OAK_WOOD;
    private boolean rootFloat = false;
    private int startHeight = 0;
    private int rootLength = 9;
    private int maxRoots = 2;
    private int minRoots = 1;
    private int thickness = 1;
    private int slopeChance = 40;
    private int twistChance = 5; // This is a hidden value not available through Parameters. Otherwise messy.
    private int heightMininmum = 14;
    private int heightMaximum = 18;
    private int branchLength = 8;
    private int nodeMax = 4;
    private int nodeMin = 3;

    private int blockPositionX;
    private int blockPositionY;
    private int blockPositionZ;

public GenerateTreeBrush() {
        this.setName("Generate Tree");
    }

    public boolean isLog(Material m) {
        switch (m) {
            case ACACIA_LOG:
            case BIRCH_LOG:
            case DARK_OAK_LOG:
            case JUNGLE_LOG:
            case OAK_LOG:
            case SPRUCE_LOG:
                return true;
            default:
                return false;
        }
    }

    public boolean isLeave(Material m) {
        switch (m) {
            case ACACIA_LEAVES:
            case BIRCH_LEAVES:
            case DARK_OAK_LEAVES:
            case JUNGLE_LEAVES:
            case OAK_LEAVES:
            case SPRUCE_LEAVES:
                return true;
            default:
                return false;
        }
    }

    // Branch Creation based on direction chosen from the parameters passed.
    private void branchCreate(final int xDirection, final int zDirection) {

        // Sets branch origin.
        final int originX = blockPositionX;
        final int originY = blockPositionY;
        final int originZ = blockPositionZ;

        // Sets direction preference.
        final int xPreference = this.randGenerator.nextInt(60) + 20;
        final int zPreference = this.randGenerator.nextInt(60) + 20;

        // Iterates according to branch length.
        for (int r = 0; r < this.branchLength; r++) {

            // Alters direction according to preferences.
            if (this.randGenerator.nextInt(100) < xPreference) {
                blockPositionX = blockPositionX + 1 * xDirection;
            }
            if (this.randGenerator.nextInt(100) < zPreference) {
                blockPositionZ = blockPositionZ + 1 * zDirection;
            }

            // 50% chance to increase elevation every second block.
            if (Math.abs(r % 2) == 1) {
                blockPositionY = blockPositionY + this.randGenerator.nextInt(2);
            }

            // Add block to undo function.
            if (!isLog(this.getBlockType(blockPositionX, blockPositionY, blockPositionZ))) {
                this.undo.put(this.clampY(blockPositionX, blockPositionY, blockPositionZ));
            }

            // Creates a branch block.
            this.clampY(blockPositionX, blockPositionY, blockPositionZ).setType(this.woodType);
            this.branchBlocks.add(this.clampY(blockPositionX, blockPositionY, blockPositionZ));
        }

        // Resets the origin
        blockPositionX = originX;
        blockPositionY = originY;
        blockPositionZ = originZ;
    }

    private void leafNodeCreate() {
        // Generates the node size.
        final int nodeRadius = this.randGenerator.nextInt(this.nodeMax - this.nodeMin + 1) + this.nodeMin;
        final double bSquared = Math.pow(nodeRadius + 0.5, 2);

        // Lowers the current block in order to start at the bottom of the node.
        blockPositionY = blockPositionY - 2;


        for (int z = nodeRadius; z >= 0; z--) {
            final double zSquared = Math.pow(z, 2);

            for (int x = nodeRadius; x >= 0; x--) {
                final double xSquared = Math.pow(x, 2);

                for (int y = nodeRadius; y >= 0; y--) {
                    if ((xSquared + Math.pow(y, 2) + zSquared) <= bSquared) {
                        // Chance to skip creation of a block.
                        if (this.randGenerator.nextInt(100) >= 30) {
                            // If block is Air, create a leaf block.
                            if (this.getWorld().getBlockAt(blockPositionX + x, blockPositionY + y, blockPositionZ + z).isEmpty()) {
                                // Adds block to undo function.
                                if (!isLeave(this.getBlockType(blockPositionX + x, blockPositionY + y, blockPositionZ + z))) {
                                    this.undo.put(this.clampY(blockPositionX + x, blockPositionY + y, blockPositionZ + z));
                                }
                                // Creates block.
                                this.clampY(blockPositionX + x, blockPositionY + y, blockPositionZ + z).setType(this.leafType);
                            }
                        }
                        if (this.randGenerator.nextInt(100) >= 30) {
                            if (this.getWorld().getBlockAt(blockPositionX + x, blockPositionY + y, blockPositionZ - z).isEmpty()) {
                                if (!isLeave(this.getBlockType(blockPositionX + x, blockPositionY + y, blockPositionZ - z))) {
                                    this.undo.put(this.clampY(blockPositionX + x, blockPositionY + y, blockPositionZ - z));
                                }
                                this.clampY(blockPositionX + x, blockPositionY + y, blockPositionZ - z).setType(this.leafType);
                            }
                        }
                        if (this.randGenerator.nextInt(100) >= 30) {
                            if (this.getWorld().getBlockAt(blockPositionX - x, blockPositionY + y, blockPositionZ + z).isEmpty()) {
                                if (!isLeave(this.getBlockType(blockPositionX - x, blockPositionY + y, blockPositionZ + z))) {
                                    this.undo.put(this.clampY(blockPositionX - x, blockPositionY + y, blockPositionZ + z));
                                }
                                this.clampY(blockPositionX - x, blockPositionY + y, blockPositionZ + z).setType(this.leafType);
                            }
                        }
                        if (this.randGenerator.nextInt(100) >= 30) {
                            if (this.getWorld().getBlockAt(blockPositionX - x, blockPositionY + y, blockPositionZ - z).isEmpty()) {
                                if (!isLeave(this.getBlockType(blockPositionX - x, blockPositionY + y, blockPositionZ - z))) {
                                    this.undo.put(this.clampY(blockPositionX - x, blockPositionY + y, blockPositionZ - z));
                                }
                                this.clampY(blockPositionX - x, blockPositionY + y, blockPositionZ - z).setType(this.leafType);
                            }
                        }
                        if (this.randGenerator.nextInt(100) >= 30) {
                            if (this.getWorld().getBlockAt(blockPositionX + x, blockPositionY - y, blockPositionZ + z).isEmpty()) {
                                if (!isLeave(this.getBlockType(blockPositionX + x, blockPositionY - y, blockPositionZ + z))) {
                                    this.undo.put(this.clampY(blockPositionX + x, blockPositionY - y, blockPositionZ + z));
                                }
                                this.clampY(blockPositionX + x, blockPositionY - y, blockPositionZ + z).setType(this.leafType);
                            }
                        }
                        if (this.randGenerator.nextInt(100) >= 30) {
                            if (this.getWorld().getBlockAt(blockPositionX + x, blockPositionY - y, blockPositionZ - z).isEmpty()) {
                                if (!isLeave(this.getBlockType(blockPositionX + x, blockPositionY - y, blockPositionZ - z))) {
                                    this.undo.put(this.clampY(blockPositionX + x, blockPositionY - y, blockPositionZ - z));
                                }
                                this.clampY(blockPositionX + x, blockPositionY - y, blockPositionZ - z).setType(this.leafType);
                            }
                        }
                        if (this.randGenerator.nextInt(100) >= 30) {
                            if (this.getWorld().getBlockAt(blockPositionX - x, blockPositionY - y, blockPositionZ + z).isEmpty()) {
                                if (!isLeave(this.getBlockType(blockPositionX - x, blockPositionY - y, blockPositionZ + z))) {
                                    this.undo.put(this.clampY(blockPositionX - x, blockPositionY - y, blockPositionZ + z));
                                }
                                this.clampY(blockPositionX - x, blockPositionY - y, blockPositionZ + z).setType(this.leafType);
                            }
                        }
                        if (this.randGenerator.nextInt(100) >= 30) {
                            if (this.getWorld().getBlockAt(blockPositionX - x, blockPositionY - y, blockPositionZ - z).isEmpty()) {
                                if (!isLeave(this.getBlockType(blockPositionX - x, blockPositionY - y, blockPositionZ - z))) {
                                    this.undo.put(this.clampY(blockPositionX - x, blockPositionY - y, blockPositionZ - z));
                                }
                                this.clampY(blockPositionX - x, blockPositionY - y, blockPositionZ - z).setType(this.leafType);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Code Concerning Root Generation.
     *
     * @param xDirection
     * @param zDirection
     */
    private void rootCreate(final int xDirection, final int zDirection) {
        // Sets Origin.
        final int originX = blockPositionX;
        final int originY = blockPositionY;
        final int originZ = blockPositionZ;

        // Generates the number of roots to create.
        final int roots = this.randGenerator.nextInt(this.maxRoots - this.minRoots + 1) + this.minRoots;

        // A roots preference to move along the X and Y axis.


        // Loops for each root to be created.
        for (int i = 0; i < roots; i++) {
            // Pushes the root'world starting point out from the center of the tree.
            for (int t = 0; t < this.thickness - 1; t++) {
                blockPositionX = blockPositionX + xDirection;
                blockPositionZ = blockPositionZ + zDirection;
            }

            // Generate directional preference between 30% and 70%
            final int xPreference = this.randGenerator.nextInt(30) + 40;
            final int zPreference = this.randGenerator.nextInt(30) + 40;

            for (int j = 0; j < this.rootLength; j++) {
                // For the purposes of this algorithm, logs aren't considered solid.

                // If not solid then...
                // Save for undo function
                if (!isLog(this.getBlockType(blockPositionX, blockPositionY, blockPositionZ))) {
                    this.undo.put(this.clampY(blockPositionX, blockPositionY, blockPositionZ));

                    // Place log block.
                    this.clampY(blockPositionX, blockPositionY, blockPositionZ).setType(this.woodType);
                } else {
                    // If solid then...
                    // End loop
                    break;
                }

                // Checks is block below is solid
                if (this.clampY(blockPositionX, blockPositionY - 1, blockPositionZ).isEmpty() || this.clampY(blockPositionX, blockPositionY - 1, blockPositionZ).getType() == Material.WATER || this.clampY(blockPositionX, blockPositionY - 1, blockPositionZ).getType() == Material.SNOW || isLog(this.clampY(blockPositionX, blockPositionY - 1, blockPositionZ).getType())) {
                    // Mos down if solid.
                    blockPositionY = blockPositionY - 1;
                    if (this.rootFloat) {
                        if (this.randGenerator.nextInt(100) < xPreference) {
                            blockPositionX = blockPositionX + xDirection;
                        }
                        if (this.randGenerator.nextInt(100) < zPreference) {
                            blockPositionZ = blockPositionZ + zDirection;
                        }
                    }
                } else {
                    // If solid then move.
                    if (this.randGenerator.nextInt(100) < xPreference) {
                        blockPositionX = blockPositionX + xDirection;
                    }
                    if (this.randGenerator.nextInt(100) < zPreference) {
                        blockPositionZ = blockPositionZ + zDirection;
                    }
                    // Checks if new location is solid, if not then move down.
                    if (this.clampY(blockPositionX, blockPositionY - 1, blockPositionZ).isEmpty() || this.clampY(blockPositionX, blockPositionY - 1, blockPositionZ).getType() == Material.WATER || this.clampY(blockPositionX, blockPositionY - 1, blockPositionZ).getType() == Material.SNOW || isLog(this.clampY(blockPositionX, blockPositionY - 1, blockPositionZ).getType())) {
                        blockPositionY = blockPositionY - 1;
                    }
                }
            }

            // Reset origin.
            blockPositionX = originX;
            blockPositionY = originY;
            blockPositionZ = originZ;

        }
    }

    private void rootGen() {
        // Quadrant 1
        this.rootCreate(1, 1);

        // Quadrant 2
        this.rootCreate(-1, 1);

        // Quadrant 3
        this.rootCreate(1, -1);

        // Quadrant 4
        this.rootCreate(-1, -1);
    }

    private void trunkCreate() {
        // Creates true circle discs of the set size using the wood type selected.
        final double bSquared = Math.pow(this.thickness + 0.5, 2);

        for (int x = this.thickness; x >= 0; x--) {
            final double xSquared = Math.pow(x, 2);

            for (int z = this.thickness; z >= 0; z--) {
                if ((xSquared + Math.pow(z, 2)) <= bSquared) {
                    // If block is air, then create a block.
                    if (this.getWorld().getBlockAt(blockPositionX + x, blockPositionY, blockPositionZ + z).isEmpty()) {
                        // Adds block to undo function.
                        if (!isLog(this.getBlockType(blockPositionX + x, blockPositionY, blockPositionZ + z))) {
                            this.undo.put(this.clampY(blockPositionX + x, blockPositionY, blockPositionZ + z));
                        }
                        // Creates block.
                        this.clampY(blockPositionX + x, blockPositionY, blockPositionZ + z).setType(this.woodType);
                    }
                    if (this.getWorld().getBlockAt(blockPositionX + x, blockPositionY, blockPositionZ - z).isEmpty()) {
                        if (!isLog(this.getBlockType(blockPositionX + x, blockPositionY, blockPositionZ - z))) {
                            this.undo.put(this.clampY(blockPositionX + x, blockPositionY, blockPositionZ - z));
                        }
                        this.clampY(blockPositionX + x, blockPositionY, blockPositionZ - z).setType(this.woodType);
                    }
                    if (this.getWorld().getBlockAt(blockPositionX - x, blockPositionY, blockPositionZ + z).isEmpty()) {
                        if (!isLog(this.getBlockType(blockPositionX - x, blockPositionY, blockPositionZ + z))) {
                            this.undo.put(this.clampY(blockPositionX - x, blockPositionY, blockPositionZ + z));
                        }
                        this.clampY(blockPositionX - x, blockPositionY, blockPositionZ + z).setType(this.woodType);
                    }
                    if (this.getWorld().getBlockAt(blockPositionX - x, blockPositionY, blockPositionZ - z).isEmpty()) {
                        if (!isLog(this.getBlockType(blockPositionX - x, blockPositionY, blockPositionZ - z))) {
                            this.undo.put(this.clampY(blockPositionX - x, blockPositionY, blockPositionZ - z));
                        }
                        this.clampY(blockPositionX - x, blockPositionY, blockPositionZ - z).setType(this.woodType);
                    }
                }
            }
        }
    }

    /*
     *
     * Code Concerning Trunk Generation
     */
    private void trunkGen() {
        // Sets Origin
        final int originX = blockPositionX;
        final int originY = blockPositionY;
        final int originZ = blockPositionZ;

        // ----------
        // Main Trunk
        // ----------
        // Sets diretional preferences.
        int xPreference = this.randGenerator.nextInt(this.slopeChance);
        int zPreference = this.randGenerator.nextInt(this.slopeChance);

        // Sets direction.
        int xDirection = 1;
        if (this.randGenerator.nextInt(100) < 50) {
            xDirection = -1;
        }

        int zDirection = 1;
        if (this.randGenerator.nextInt(100) < 50) {
            zDirection = -1;
        }

        // Generates a height for trunk.
        int height = this.randGenerator.nextInt(this.heightMaximum - this.heightMininmum + 1) + this.heightMininmum;

        for (int p = 0; p < height; p++) {
            if (p > 3) {
                if (this.randGenerator.nextInt(100) <= this.twistChance) {
                    xDirection *= -1;
                }
                if (this.randGenerator.nextInt(100) <= this.twistChance) {
                    zDirection *= -1;
                }
                if (this.randGenerator.nextInt(100) < xPreference) {
                    blockPositionX += xDirection;
                }
                if (this.randGenerator.nextInt(100) < zPreference) {
                    blockPositionZ += zDirection;
                }
            }

            // Creates trunk section
            this.trunkCreate();

            // Mos up for next section
            blockPositionY = blockPositionY + 1;
        }

        // Generates branchs at top of trunk for each quadrant.
        this.branchCreate(1, 1);
        this.branchCreate(-1, 1);
        this.branchCreate(1, -1);
        this.branchCreate(-1, -1);

        // Reset Origin for next trunk.
        blockPositionX = originX;
        blockPositionY = originY + 4;
        blockPositionZ = originZ;

        // ---------------
        // Secondary Trunk
        // ---------------
        // Sets diretional preferences.
        xPreference = this.randGenerator.nextInt(this.slopeChance);
        zPreference = this.randGenerator.nextInt(this.slopeChance);

        // Sets direction.
        xDirection = 1;
        if (this.randGenerator.nextInt(100) < 50) {
            xDirection = -1;
        }

        zDirection = 1;
        if (this.randGenerator.nextInt(100) < 50) {
            zDirection = -1;
        }

        // Generates a height for trunk.
        height = this.randGenerator.nextInt(this.heightMaximum - this.heightMininmum + 1) + this.heightMininmum;

        if (height > 4) {
            for (int p = 0; p < height; p++) {
                if (this.randGenerator.nextInt(100) <= this.twistChance) {
                    xDirection *= -1;
                }
                if (this.randGenerator.nextInt(100) <= this.twistChance) {
                    zDirection *= -1;
                }
                if (this.randGenerator.nextInt(100) < xPreference) {
                    blockPositionX = blockPositionX + 1 * xDirection;
                }
                if (this.randGenerator.nextInt(100) < zPreference) {
                    blockPositionZ = blockPositionZ + 1 * zDirection;
                }

                // Creates a trunk section
                this.trunkCreate();

                // Mos up for next section
                blockPositionY = blockPositionY + 1;
            }

            // Generates branchs at top of trunk for each quadrant.
            this.branchCreate(1, 1);
            this.branchCreate(-1, 1);
            this.branchCreate(1, -1);
            this.branchCreate(-1, -1);
        }
    }

    @Override
    protected final void arrow(final SnipeData v) {
        this.undo = new Undo();

        this.branchBlocks.clear();

        // Sets the location variables.
        blockPositionX = this.getTargetBlock().getX();
        blockPositionY = this.getTargetBlock().getY() + this.startHeight;
        blockPositionZ = this.getTargetBlock().getZ();

        // Generates the roots.
        this.rootGen();

        // Generates the trunk, which also generates branches.
        this.trunkGen();

        // Each branch block was saved in an array. This is now fed through an array.
        // This array takes each branch block and constructs a leaf node around it.
        for (final Block block : this.branchBlocks) {
            blockPositionX = block.getX();
            blockPositionY = block.getY();
            blockPositionZ = block.getZ();
            this.leafNodeCreate();
        }

        // Ends the undo function and mos on.
        v.owner().storeUndo(this.undo);
    }

    // The Powder currently does nothing extra.
    @Override
    protected final void powder(final SnipeData v) {
        this.arrow(v);
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        for (int i = 1; i < par.length; i++) {
            final String parameter = par[i];

            try {
                if (parameter.equalsIgnoreCase("info")) {
                    v.sendMessage(ChatColor.GOLD + "This brush takes the following parameters:");
                    v.sendMessage(ChatColor.AQUA + "lt# - leaf type (data value)");
                    v.sendMessage(ChatColor.AQUA + "wt# - wood type (data value)");
                    v.sendMessage(ChatColor.AQUA + "tt# - tree thickness (whote number)");
                    v.sendMessage(ChatColor.AQUA + "rfX - root float (true or false)");
                    v.sendMessage(ChatColor.AQUA + "sh# - starting height (whole number)");
                    v.sendMessage(ChatColor.AQUA + "rl# - root length (whole number)");
                    v.sendMessage(ChatColor.AQUA + "ts# - trunk slope chance (0-100)");
                    v.sendMessage(ChatColor.AQUA + "bl# - branch length (whole number)");
                    v.sendMessage(ChatColor.AQUA + "info2 - more parameters");
                    return;
                }

                if (parameter.equalsIgnoreCase("info2")) {
                    v.sendMessage(ChatColor.GOLD + "This brush takes the following parameters:");
                    v.sendMessage(ChatColor.AQUA + "minr# - minimum roots (whole number)");
                    v.sendMessage(ChatColor.AQUA + "maxr# - maximum roots (whole number)");
                    v.sendMessage(ChatColor.AQUA + "minh# - minimum height (whole number)");
                    v.sendMessage(ChatColor.AQUA + "maxh# - maximum height (whole number)");
                    v.sendMessage(ChatColor.AQUA + "minl# - minimum leaf node size (whole number)");
                    v.sendMessage(ChatColor.AQUA + "maxl# - maximum leaf node size (whole number)");
                    v.sendMessage(ChatColor.AQUA + "default - restore default params");
                    return;
                }
                if (parameter.startsWith("lt")) { // Leaf Type
                    this.leafType = BukkitAdapter.adapt(BlockTypes.parse(parameter.replace("lt", "")));
                    v.sendMessage(ChatColor.BLUE + "Leaf Type set to " + this.leafType);
                } else if (parameter.startsWith("wt")) { // Wood Type
                    this.woodType = BukkitAdapter.adapt(BlockTypes.parse(parameter.replace("wt", "")));
                    v.sendMessage(ChatColor.BLUE + "Wood Type set to " + this.woodType);
                } else if (parameter.startsWith("tt")) { // Tree Thickness
                    this.thickness = Integer.parseInt(parameter.replace("tt", ""));
                    v.sendMessage(ChatColor.BLUE + "Thickness set to " + this.thickness);
                } else if (parameter.startsWith("rf")) { // Root Float
                    this.rootFloat = Boolean.parseBoolean(parameter.replace("rf", ""));
                    v.sendMessage(ChatColor.BLUE + "Floating Roots set to " + this.rootFloat);
                } else if (parameter.startsWith("sh")) { // Starting Height
                    this.startHeight = Integer.parseInt(parameter.replace("sh", ""));
                    v.sendMessage(ChatColor.BLUE + "Starting Height set to " + this.startHeight);
                } else if (parameter.startsWith("rl")) { // Root Length
                    this.rootLength = Integer.parseInt(parameter.replace("rl", ""));
                    v.sendMessage(ChatColor.BLUE + "Root Length set to " + this.rootLength);
                } else if (parameter.startsWith("minr")) { // Minimum Roots
                    this.minRoots = Integer.parseInt(parameter.replace("minr", ""));
                    if (this.minRoots > this.maxRoots) {
                        this.minRoots = this.maxRoots;
                        v.sendMessage(ChatColor.RED + "Minimum Roots can't exceed Maximum Roots, has  been set to " + this.minRoots + " Instead!");
                    } else {
                        v.sendMessage(ChatColor.BLUE + "Minimum Roots set to " + this.minRoots);
                    }
                } else if (parameter.startsWith("maxr")) { // Maximum Roots
                    this.maxRoots = Integer.parseInt(parameter.replace("maxr", ""));
                    if (this.minRoots > this.maxRoots) {
                        this.maxRoots = this.minRoots;
                        v.sendMessage(ChatColor.RED + "Maximum Roots can't be lower than Minimum Roots, has been set to " + this.minRoots + " Instead!");
                    } else {
                        v.sendMessage(ChatColor.BLUE + "Maximum Roots set to " + this.maxRoots);
                    }
                } else if (parameter.startsWith("ts")) { // Trunk Slope Chance
                    this.slopeChance = Integer.parseInt(parameter.replace("ts", ""));
                    v.sendMessage(ChatColor.BLUE + "Trunk Slope set to " + this.slopeChance);
                } else if (parameter.startsWith("minh")) { // Height Minimum
                    this.heightMininmum = Integer.parseInt(parameter.replace("minh", ""));
                    if (this.heightMininmum > this.heightMaximum) {
                        this.heightMininmum = this.heightMaximum;
                        v.sendMessage(ChatColor.RED + "Minimum Height exceed than Maximum Height, has been set to " + this.heightMininmum + " Instead!");
                    } else {
                        v.sendMessage(ChatColor.BLUE + "Minimum Height set to " + this.heightMininmum);
                    }
                } else if (parameter.startsWith("maxh")) { // Height Maximum
                    this.heightMaximum = Integer.parseInt(parameter.replace("maxh", ""));
                    if (this.heightMininmum > this.heightMaximum) {
                        this.heightMaximum = this.heightMininmum;
                        v.sendMessage(ChatColor.RED + "Maximum Height can't be lower than Minimum Height, has been set to " + this.heightMaximum + " Instead!");
                    } else {
                        v.sendMessage(ChatColor.BLUE + "Maximum Roots set to " + this.heightMaximum);
                    }
                } else if (parameter.startsWith("bl")) { // Branch Length
                    this.branchLength = Integer.parseInt(parameter.replace("bl", ""));
                    v.sendMessage(ChatColor.BLUE + "Branch Length set to " + this.branchLength);
                } else if (parameter.startsWith("maxl")) { // Leaf Node Max Size
                    this.nodeMax = Integer.parseInt(parameter.replace("maxl", ""));
                    v.sendMessage(ChatColor.BLUE + "Leaf Max Thickness set to " + this.nodeMax + " (Default 4)");
                } else if (parameter.startsWith("minl")) { // Leaf Node Min Size
                    this.nodeMin = Integer.parseInt(parameter.replace("minl", ""));
                    v.sendMessage(ChatColor.BLUE + "Leaf Min Thickness set to " + this.nodeMin + " (Default 3)");

                    // -------
                    // Presets
                    // -------
                } else if (parameter.startsWith("default")) { // Default settings.
                    this.leafType = Material.OAK_LEAVES;
                    this.woodType = Material.OAK_WOOD;
                    this.rootFloat = false;
                    this.startHeight = 0;
                    this.rootLength = 9;
                    this.maxRoots = 2;
                    this.minRoots = 1;
                    this.thickness = 1;
                    this.slopeChance = 40;
                    this.heightMininmum = 14;
                    this.heightMaximum = 18;
                    this.branchLength = 8;
                    this.nodeMax = 4;
                    this.nodeMin = 3;
                    v.sendMessage(ChatColor.GOLD + "Brush reset to default parameters.");
                } else {
                    v.sendMessage(ChatColor.RED + "Invalid brush parameters! Use the info parameter to display parameter info.");
                }
            } catch (final Exception exception) {
                v.sendMessage(ChatColor.RED + "Invalid brush parameters! \"" + par[i] + "\" is not a valid statement. Please use the 'info' parameter to display parameter info.");
            }

        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.generatetree";
    }
}
