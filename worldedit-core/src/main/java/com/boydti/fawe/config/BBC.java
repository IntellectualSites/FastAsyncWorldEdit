package com.boydti.fawe.config;

import static org.slf4j.LoggerFactory.getLogger;

import com.boydti.fawe.configuration.MemorySection;
import com.boydti.fawe.configuration.file.YamlConfiguration;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.formatting.WorldEditText;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.formatting.text.serializer.legacy.LegacyComponentSerializer;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public enum BBC {

    /*
     * Things to note about this class:
     * Can use multiple arguments %s, %s1, %s2, %s3 etc
     */
    PREFIX("&4&lFAWE&7: ", "Info"),
    FILE_DELETED("%s0 has been deleted.", "Info"),
    SCHEMATIC_PASTING("&7The schematic is pasting. This cannot be undone.", "Info"),
    LIGHTING_PROPAGATE_SELECTION("Lighting has been propogated in %s0 chunks. (Note: To remove light use //removelight)", "Info"),
    UPDATED_LIGHTING_SELECTION("Lighting has been updated in %s0 chunks. (It may take a second for the packets to send)", "Info"),
    SET_REGION("Selection set to your current allowed region", "Info"),
    WORLDEDIT_COMMAND_LIMIT("Please wait until your current action completes", "Info"),
    WORLDEDIT_DELAYED("Please wait while we process your FAWE action...", "Info"),
    WORLDEDIT_RUN("Apologies for the delay. Now executing: %s", "Info"),
    WORLDEDIT_COMPLETE("Edit completed.", "Info"),
    REQUIRE_SELECTION_IN_MASK("&7%s of your selection is not within your mask. You can only make edits within allowed regions.", "Info"),
    WORLDEDIT_VOLUME("&7You cannot select a volume of %current%. The maximum volume you can modify is %max%.", "Info"),
    WORLDEDIT_ITERATIONS("You cannot iterate %current% times. The maximum number of iterations allowed is %max%.", "Info"),
    WORLDEDIT_UNSAFE("&7Access to that command has been blocked", "Info"),
    WORLDEDIT_DANGEROUS_WORLDEDIT("&cProcessed unsafe edit at %s0 by %s1", "Info"),
    WORLDEDIT_EXTEND("&cYour edit may have extended outside your allowed region.", "Error"),
    WORLDEDIT_TOGGLE_TIPS_ON("Disabled FAWE tips.", "Info"),
    WORLDEDIT_TOGGLE_TIPS_OFF("Enabled FAWE tips.", "Info"),

    WORLDEDIT_BYPASSED("Currently bypassing FAWE restriction.", "Info"),
    WORLDEDIT_UNMASKED("&6Your FAWE edits are now unrestricted.", "Info"),

    WORLDEDIT_RESTRICTED("Your FAWE edits are now restricted.", "Info"),
    WORLDEDIT_OOM_ADMIN("Possible options:\n - //fast\n - Do smaller edits\n - Allocate more memory\n - Disable `max-memory-percent`", "Info"),
    COMPRESSED("History compressed. Saved ~ %s0b (%s1x smaller)", "Info"),

    WEB_UNAUTHORIZED("Only links from the configured web host is allowed: %s0", "Error"),
    ACTION_COMPLETE("Action completed in %s0 seconds", "Info"),
    GENERATING_LINK("Uploading %s, please wait...", "Web"),
    GENERATING_LINK_FAILED("Failed to generate download link!", "Web"),
    DOWNLOAD_LINK("%s", "Web"),

    MASK_DISABLED("Global mask disabled", "WorldEdit.General"),
    MASK("Global mask set", "WorldEdit.General"),
    TEXTURE_DISABLED("Texturing reset", "WorldEdit.General"),
    TEXTURE_SET("Set texturing to %s1", "WorldEdit.General"),
    SOURCE_MASK_DISABLED("Global source mask disabled", "WorldEdit.General"),
    SOURCE_MASK("Global source mask set", "WorldEdit.General"),
    TRANSFORM_DISABLED("Global transform disabled", "WorldEdit.General"),
    TRANSFORM("Global transform set", "WorldEdit.General"),

    COMMAND_COPY("%s0 blocks were copied.", "WorldEdit.Copy"),

    COMMAND_CUT_SLOW("%s0 blocks were cut.", "WorldEdit.Cut"),
    COMMAND_CUT_LAZY("%s0 blocks will be removed on paste", "WorldEdit.Cut"),

    COMMAND_PASTE("The clipboard has been pasted at %s0", "WorldEdit.Paste"),

    COMMAND_ROTATE("The clipboard has been rotated", "WorldEdit.Rotate"),

    COMMAND_FLIPPED("The clipboard has been flipped", "WorldEdit.Flip"),
    COMMAND_REGEN_0("Region regenerated.", "WorldEdit.Regen"),
    COMMAND_REGEN_1("Region regenerated.", "WorldEdit.Regen"),
    COMMAND_REGEN_2("Region regenerated.", "WorldEdit.Regen"),

    COMMAND_TREE("%s0 trees created.", "WorldEdit.Tree"),
    COMMAND_PUMPKIN("%s0 pumpkin patches created.", "WorldEdit.Tree"),
    COMMAND_FLORA("%s0 flora created.", "WorldEdit.Flora"),
    COMMAND_HISTORY_CLEAR("History cleared", "WorldEdit.History"),
    COMMAND_REDO_ERROR("Nothing left to redo. (See also `/inspect` and `/frb`)", "WorldEdit.History"),
    COMMAND_HISTORY_OTHER_ERROR("Unable to find session for %s0.", "WorldEdit.History"),
    COMMAND_REDO_SUCCESS("Redo successful%s0.", "WorldEdit.History"),
    COMMAND_UNDO_ERROR("Nothing left to undo. (See also `/inspect` and `/frb`)", "WorldEdit.History"),
    COMMAND_UNDO_DISABLED("Undo disabled, use: //fast", "WorldEdit.History"),
    COMMAND_UNDO_SUCCESS("Undo successful%s0.", "WorldEdit.History"),

    OPERATION("Operation queued (%s0)", "WorldEdit.Operation"),

    SELECTION_WAND("Left click: select pos #1; Right click: select pos #2", "WorldEdit.Selection"),
    NAVIGATION_WAND_ERROR("Nothing to pass through", "WorldEdit.Navigation"),

    SELECTION_WAND_DISABLE("Edit wand disabled.", "WorldEdit.Selection"),
    SELECTION_WAND_ENABLE("Edit wand enabled.", "WorldEdit.Selection"),
    SELECTION_CHUNK("Chunk selected (%s0)", "WorldEdit.Selection"),
    SELECTION_CHUNKS("Chunks selected (%s0) - (%s1)", "WorldEdit.Selection"),
    SELECTION_CONTRACT("Region contracted %s0 blocks.", "WorldEdit.Selection"),
    SELECTION_COUNT("Counted %s0 blocks.", "WorldEdit.Selection"),
    SELECTION_DISTR("# total blocks: %s0", "WorldEdit.Selection"),
    SELECTION_EXPAND("Region expanded %s0 blocks", "WorldEdit.Selection"),
    SELECTION_EXPAND_VERT("Region expanded %s0 blocks (top to bottom)", "WorldEdit.Selection"),
    SELECTION_INSET("Region inset", "WorldEdit.Selection"),
    SELECTION_OUTSET("Region outset", "WorldEdit.Selection"),
    SELECTION_SHIFT("Region shifted", "WorldEdit.Selection"),
    SELECTION_CLEARED("Selection cleared", "WorldEdit.Selection"),

    WORLD_IS_LOADED("The world shouldn't be in use when executing. Unload the world, or use use -f to override (save first)", "WorldEdit.Anvil"),

    BRUSH_RESET("Reset your brush. (SHIFT + Click)", "WorldEdit.Brush"),
    BRUSH_NONE("You aren't holding a brush!", "WorldEdit.Brush"),
    BRUSH_SCROLL_ACTION_SET("Set scroll action to %s0", "WorldEdit.Brush"),
    BRUSH_SCROLL_ACTION_UNSET("Removed scroll action", "WorldEdit.Brush"),
    BRUSH_VISUAL_MODE_SET("Set visual mode to %s0", "WorldEdit.Brush"),
    BRUSH_TARGET_MODE_SET("Set target mode to %s0", "WorldEdit.Brush"),
    BRUSH_TARGET_MASK_SET("Set target mask to %s0", "WorldEdit.Brush"),
    BRUSH_TARGET_OFFSET_SET("Set target offset to %s0", "WorldEdit.Brush"),
    BRUSH_EQUIPPED("Equipped brush %s0", "WorldEdit.Brush"),
    BRUSH_TRY_OTHER("There are other more suitable brushes e.g.,\n - //br height [radius=5] [#clipboard|file=null] [rotation=0] [yscale=1.00]", "WorldEdit.Brush"),
    BRUSH_COPY("Left click the base of an object to copy, right click to paste. Increase the brush radius if necessary.", "WorldEdit.Brush"),
    BRUSH_HEIGHT_INVALID("Invalid height map file (%s0)", "WorldEdit.Brush"),
    BRUSH_SMOOTH("Note: Use the blend brush if you want to smooth overhangs or caves.", "WorldEdit.Brush"),
    BRUSH_SPLINE("Click to add a point, click the same spot to finish", "WorldEdit.Brush"),
    BRUSH_LINE_PRIMARY("Added point %s0, click another position to create the line", "WorldEdit.Brush"),
    BRUSH_CATENARY_DIRECTION("Added point %s0, click the direction you want to create the spline", "WorldEdit.Brush"),
    BRUSH_LINE_SECONDARY("Created spline", "WorldEdit.Brush"),
    BRUSH_SPLINE_PRIMARY_2("Added position, Click the same spot to join!", "WorldEdit.Brush"),
    BRUSH_SPLINE_SECONDARY_ERROR("Not enough positions set!", "WorldEdit.Brush"),
    BRUSH_SPLINE_SECONDARY("Created spline", "WorldEdit.Brush"),
    BRUSH_SIZE("Brush size set", "WorldEdit.Brush"),
    BRUSH_RANGE("Brush size set", "WorldEdit.Brush"),
    BRUSH_MASK_DISABLED("Brush mask disabled", "WorldEdit.Brush"),
    BRUSH_MASK("Brush mask set", "WorldEdit.Brush"),
    BRUSH_SOURCE_MASK_DISABLED("Brush source mask disabled", "WorldEdit.Brush"),
    BRUSH_SOURCE_MASK("Brush source mask set", "WorldEdit.Brush"),
    BRUSH_TRANSFORM_DISABLED("Brush transform disabled", "WorldEdit.Brush"),
    BRUSH_TRANSFORM("Brush transform set", "WorldEdit.Brush"),
    BRUSH_MATERIAL("Brush material set", "WorldEdit.Brush"),

    ROLLBACK_ELEMENT("Undoing %s0", "WorldEdit.Rollback"),

    TOOL_INSPECT("Inspect tool bound to %s0.", "WorldEdit.Tool"),
    TOOL_INSPECT_INFO("%s0 changed %s1 to %s2 %s3 ago", "WorldEdit.Tool"),
    TOOL_INSPECT_INFO_FOOTER("Total: %s0 changes", "WorldEdit.Tool"),
    TOOL_NONE("Tool unbound from your current item.", "WorldEdit.Tool"),
    TOOL_INFO("Info tool bound to %s0.", "WorldEdit.Tool"),
    TOOL_TREE("Tree tool bound to %s0.", "WorldEdit.Tool"),
    TOOL_TREE_ERROR_BLOCK("A tree can't go here", "WorldEdit.Tool"),
    TOOL_TREE_ERROR("Tree type %s0 is unknown.", "WorldEdit.Tool"),
    TOOL_REPL("Block replacer tool bound to %s0.", "WorldEdit.Tool"),
    TOOL_CYCLER("Block data cycler tool bound to %s0.", "WorldEdit.Tool"),
    TOOL_FLOOD_FILL("Block flood fill tool bound to %s0.", "WorldEdit.Tool"),
    TOOL_RANGE_ERROR("Maximum range: %s0.", "WorldEdit.Tool"),
    TOOL_RADIUS_ERROR("Maximum allowed brush radius: %s0.", "WorldEdit.Tool"),
    TOOL_DELTREE("Floating tree remover tool bound to %s0.", "WorldEdit.Tool"),
    TOOL_DELTREE_ERROR("That's not a tree", "WorldEdit.Tool"),
    TOOL_DELTREE_FLOATING_ERROR("That's not a floating tree", "WorldEdit.Tool"),
    TOOL_FARWAND("Far wand tool bound to %s0.", "WorldEdit.Tool"),
    TOOL_LRBUILD_BOUND("Long-range building tool bound to %s0.", "WorldEdit.Tool"),
    TOOL_LRBUILD_INFO("Left-click set to %s0; right-click set to %s1.", "WorldEdit.Tool"),
    SUPERPICKAXE_ENABLED("Super Pickaxe enabled.", "WorldEdit.Tool"),
    SUPERPICKAXE_DISABLED("Super Pickaxe disabled.", "WorldEdit.Tool"),
    SUPERPICKAXE_AREA_ENABLED("Mode changed. Left click with a pickaxe. // to disable.", "WorldEdit.Tool"),

    SNAPSHOT_LOADED("Snapshot '%s0' loaded; now restoring...", "WorldEdit.Snapshot"),
    SNAPSHOT_SET("Snapshot set to: %s0", "WorldEdit.Snapshot"),
    SNAPSHOT_NEWEST("Now using newest snapshot.", "WorldEdit.Snapshot"),
    SNAPSHOT_LIST_HEADER("Snapshots for world (%s0):", "WorldEdit.Snapshot"),
    SNAPSHOT_LIST_FOOTER("Use /snap use [snapshot] or /snap use latest.", "WorldEdit.Snapshot"),
    SNAPSHOT_NOT_CONFIGURED("Snapshot/backup restore is not configured.", "WorldEdit.Snapshot"),
    SNAPSHOT_NOT_AVAILABLE("No snapshots are available. See console for details.", "WorldEdit.Snapshot"),
    SNAPSHOT_NOT_FOUND_WORLD("No snapshots were found for this world.", "WorldEdit.Snapshot"),
    SNAPSHOT_NOT_FOUND("No snapshots were found.", "WorldEdit.Snapshot"),
    SNAPSHOT_INVALID_INDEX("Invalid index, must be equal or higher then 1.", "WorldEdit.Snapshot"),
    SNAPSHOT_ERROR_DATE("Could not detect the date inputted.", "WorldEdit.Snapshot"),
    SNAPSHOT_ERROR_RESTORE("Errors prevented any blocks from being restored.", "WorldEdit.Snapshot"),
    SNAPSHOT_ERROR_RESTORE_CHUNKS("No chunks could be loaded. (Bad archive?)", "WorldEdit.Snapshot"),

    BIOME_LIST_HEADER("Biomes (page %s0/%s1):", "WorldEdit.Biome"),
    BIOME_CHANGED("Biomes were changed in %s0 columns.", "WorldEdit.Biome"),

    FAST_ENABLED("Fast mode enabled. History and edit restrictions will be bypassed.", "WorldEdit.General"),
    FAST_DISABLED("Fast mode disabled", "WorldEdit.General"),

    PLACE_ENABLED("Now placing at pos #1.", "WorldEdit.General"),
    PLACE_DISABLED("Now placing at the block you stand in.", "WorldEdit.General"),

    KILL_SUCCESS("Killed %s0 entities in a radius of %s1.", "WorldEdit.Utility"),
    NOTHING_CONFIRMED("You have no actions pending confirmation.", "WorldEdit.Utility"),


    SCHEMATIC_PROMPT_CLEAR("You may want to use %s0 to clear your current clipboard first", "Worldedit.Schematic"),
    SCHEMATIC_SHOW("Displaying %s0 schematics from %s1:\n" +
            " - Left click a structure to set your clipboard\n" +
            " - Right click to add a structure to your multi-clipboard\n" +
            " - Use %s2 to go back to the world", "Worldedit.Schematic"),
    SCHEMATIC_FORMAT("Available formats (Name: Lookup names)", "Worldedit.Schematic"),
    SCHEMATIC_MOVE_EXISTS("%s0 already exists", "Worldedit.Schematic"),
    SCHEMATIC_MOVE_SUCCESS("%s0 -> %s1", "Worldedit.Schematic"),
    SCHEMATIC_MOVE_FAILED("%s0 no moved: %s1", "Worldedit.Schematic"),
    SCHEMATIC_LOADED("%s0 loaded. Paste it with //paste", "Worldedit.Schematic"),
    SCHEMATIC_SAVED("%s0 saved.", "Worldedit.Schematic"),
    SCHEMATIC_PAGE("Page must be %s", "WorldEdit.Schematic"),
    SCHEMATIC_NONE("No files found.", "WorldEdit.Schematic"),
    SCHEMATIC_LIST("Available files (Filename: Format) [%s0/%s1]:", "Worldedit.Schematic"),
    SCHEMATIC_LIST_ELEM(" - %s0 - %s1", "Worldedit.Schematic"),

    CLIPBOARD_URI_NOT_FOUND("You do not have %s0 loaded", "WorldEdit.Clipboard"),
    CLIPBOARD_CLEARED("Clipboard cleared", "WorldEdit.Clipboard"),
    CLIPBOARD_INVALID_FORMAT("Unknown clipboard format:  %s0", "WorldEdit.Clipboard"),

    VISITOR_BLOCK("%s0 blocks affected", "WorldEdit.Visitor"),
    VISITOR_ENTITY("%s0 entities affected", "WorldEdit.Visitor"),
    VISITOR_FLAT("%s0 columns affected", "WorldEdit.Visitor"),

    SELECTOR_FUZZY_POS1("Region set and expanded from %s0 %s1.", "WorldEdit.Selector"),
    SELECTOR_FUZZY_POS2("Added expansion of %s0 %s1.", "WorldEdit.Selector"),
    SELECTOR_POS("pos%s0 set to %s1 (%s2).", "WorldEdit.Selector"),
    SELECTOR_CENTER("Center set to %s0 (%s1).", "WorldEdit.Selector"),
    SELECTOR_RADIUS("Radius set to %s0 (%s1).", "WorldEdit.Selector"),
    SELECTOR_EXPANDED("Expanded region to %s0 (%s1)", "WorldEdit.Selector"),
    SELECTOR_INVALID_COORDINATES("Invalid coordinates %s0", "WorldEdit.Selector"),
    SELECTOR_ALREADY_SET("Position already set.", "WorldEdit.Selector"),
    SELECTOR_SET_DEFAULT("Your default region selector is now %s0.", "WorldEdit.Selector"),

    TIMEZONE_SET("Timezone set for this session to: %s0", "WorldEdit.Timezone"),
    TIMEZONE_DISPLAY("The current time in that timezone is:  %s0", "WorldEdit.Timezone"),

    BLOCK_CYCLER_CANNOT_CYCLE("That block's data cannot be cycled!", "WorldEdit.Cycler"),
    BLOCK_CYCLER_LIMIT("Max blocks change limit reached.", "WorldEdit.Cycler"),
    BLOCK_CYCLER_NO_PERM("You are not permitted to cycle the data value of that block.", "WorldEdit.Cycler"),

    COMMAND_INVALID_SYNTAX("The command was not used properly (no more help available).", "WorldEdit.Command"),

    COMMAND_CLARIFYING_BRACKET("Added clarifying bracket for %s0", "WorldEdit.Help"),
    HELP_SUGGEST("Couldn't find %s0. Maybe try one of %s1 ?", "WorldEdit.Help"),
    HELP_HEADER_CATEGORIES("Command Types", "WorldEdit.Help"),
    HELP_HEADER_SUBCOMMANDS("Subcommands", "WorldEdit.Help"),
    HELP_HEADER_COMMAND("Help for: %s0", "WorldEdit.Help"),
    HELP_ITEM_ALLOWED("%s0 - %s1", "WorldEdit.Help"),
    HELP_ITEM_DENIED("%s0 - %s1", "WorldEdit.Help"),
    HELP_HEADER("Help: page %s0/%s1", "WorldEdit.Help"),
    HELP_FOOTER("Wiki: https://git.io/vSKE5", "WorldEdit.Help"),
    PAGE_FOOTER("Use %s0 to go to the next page", "WorldEdit.Utility"),

    PROGRESS_MESSAGE("%s1/%s0 (%s2%) @%s3cps %s4s left", "Progress"),
    PROGRESS_FINISHED("[ Done! ]", "Progress"),

    COMMAND_SYNTAX("Usage: %s0", "Error"),
    NO_PERM("You are lacking the permission node: %s0", "Error"),
    BLOCK_NOT_ALLOWED("You are not allowed to use", "Error"),
    SETTING_DISABLE("Lacking setting: %s0", "Error"),
    BRUSH_NOT_FOUND("Available brushes: %s0", "Error"),
    BRUSH_INCOMPATIBLE("Brush not compatible with this version", "Error"),
    SCHEMATIC_NOT_FOUND("Schematic not found: %s0", "Error"),
    NO_REGION("You have no current allowed region", "Error"),
    NO_MASK("You have no current mask set", "Error"),
    NOT_PLAYER("You must be a player to perform this action!", "Error"),
    PLAYER_NOT_FOUND("Player not found: %s0", "Error"),
    OOM(
            "[Critical] Detected low memory i.e. < 1%. We will take the following actions:\n - Terminate WE block placement\n - Clear WE history\n - Unload non essential chunks\n - Kill entities\n - Garbage collect\nIgnore this if trying to crash server.\nNote: Low memory is likely (but not necessarily) caused by WE",
            "Error"),

    WORLDEDIT_SOME_FAILS("%s0 blocks weren't placed because they were outside your allowed region.", "Error"),
    WORLDEDIT_SOME_FAILS_BLOCKBAG("Missing blocks: %s0", "Error"),

    WORLDEDIT_CANCEL_COUNT("Cancelled %s0 edits.", "Cancel"),
    WORLDEDIT_CANCEL_REASON_CONFIRM("Use //confirm to execute %s2", "Cancel"),
    WORLDEDIT_CANCEL_REASON_CONFIRM_REGION("Your selection is large (%s0 -> %s1, containing %s3 blocks). Use //confirm to execute %s2", "Cancel"),
    WORLDEDIT_CANCEL_REASON("Your WorldEdit action was cancelled: %s0.", "Cancel"),
    WORLDEDIT_CANCEL_REASON_MANUAL("Manual cancellation", "Cancel"),
    WORLDEDIT_CANCEL_REASON_LOW_MEMORY("Low memory", "Cancel"),
    WORLDEDIT_CANCEL_REASON_MAX_CHANGES("Too many blocks changed", "Cancel"),
    WORLDEDIT_CANCEL_REASON_MAX_CHECKS("Too many block checks", "Cancel"),
    WORLDEDIT_CANCEL_REASON_MAX_TILES("Too many blockstates", "Cancel"),
    WORLDEDIT_CANCEL_REASON_MAX_ENTITIES("Too many entities", "Cancel"),
    WORLDEDIT_CANCEL_REASON_MAX_ITERATIONS("Max iterations", "Cancel"),
    WORLDEDIT_CANCEL_REASON_OUTSIDE_LEVEL("Outside world", "Cancel"),
    WORLDEDIT_CANCEL_REASON_OUTSIDE_REGION("Outside allowed region (bypass with /wea, or disable `region-restrictions` in config.yml)", "Cancel"),
    WORLDEDIT_CANCEL_REASON_NO_REGION("No allowed region (bypass with /wea, or disable `region-restrictions` in config.yml)", "Cancel"),
    WORLDEDIT_FAILED_LOAD_CHUNK("Skipped loading chunk: %s0;%s1. Try increasing chunk-wait.", "Cancel"),

    ASCEND_FAIL("No free spot above you found.", "Navigation"),
    ASCENDED_PLURAL("Ascended %s0 levels.", "Navigation"),
    ASCENDED_SINGULAR("Ascended a level.", "Navigation"),
    UNSTUCK("There you go!", "Navigation"),
    DESCEND_FAIL("No free spot below you found.", "Navigation"),
    DESCEND_PLURAL("Descended %s0 levels.", "Navigation"),
    DESCEND_SINGULAR("Descended a level.", "Navigation"),
    WHOOSH("Whoosh!", "Navigation"),
    POOF("Poof!", "Navigation"),
    THRU_FAIL("No free spot ahead of you found.", "Navigation"),
    NO_BLOCK("No block in sight! (or too far)", "Navigation"),
    UP_FAIL("You would hit something above you.", "Navigation"),

    SEL_CUBOID("Cuboid: left click for point 1, right click for point 2", "Selection"),
    SEL_CUBOID_EXTEND("Cuboid: left click for a starting point, right click to extend", "Selection"),
    SEL_2D_POLYGON("2D polygon selector: Left/right click to add a point.", "Selection"),
    SAL_ELLIPSOID("Ellipsoid selector: left click=center, right click to extend", "Selection"),
    SEL_SPHERE("Sphere selector: left click=center, right click to set radius", "Selection"),
    SEL_CYLINDRICAL("Cylindrical selector: Left click=center, right click to extend.", "Selection"),
    SEL_MAX("%s0 points maximum.", "Selection"),
    SEL_FUZZY("Fuzzy selector: Left click to select all contingent blocks, right click to add. To select an air cavity, use //pos1.", "Selection"),
    SEL_CONVEX_POLYHEDRAL("Convex polyhedral selector: Left click=First vertex, right click to add more.", "Selection"),
    SEL_LIST("For a list of selection types use: //sel list", "Selection"),
    SEL_MODES("Select one of the modes below:", "Selection"),

    SCRIPTING_NO_PERM("You do not have permission to execute this craft script", "WorldEdit.Scripting"),
    SCRIPTING_CS("Use /cs with a script name first.", "WorldEdit.Scripting"),
    SCRIPTING_ERROR("An error occured while executing a craft script", "WorldEdit.Scripting"),



    TIP_SEL_LIST("Tip: See the different selection modes with //sel list", "Tips"),
    TIP_SELECT_CONNECTED("Tip: Select all connected blocks with //sel fuzzy", "Tips"),
    TIP_SET_POS1("Tip: Use pos1 as a pattern with //set pos1", "Tips"),
    TIP_FARWAND("Tip: Select distant points with //farwand", "Tips"),
    TIP_DISCORD("Need help using FAWE? https://discord.gg/ngZCzbU", "Tips"),

    // cut
    TIP_LAZYCUT("Tip: It is safer to use //lazycut", "Tips"),
    // set
    TIP_FAST("Tip: Set fast and without undo using //fast", "Tips"),
    TIP_CANCEL("Tip: You can //cancel an edit in progress", "Tips"),
    TIP_MASK("Tip: Set a global destination mask with /gmask", "Tips"),
    TIP_MASK_ANGLE("Tip: Replace upward slopes of 3-20 blocks using //replace /[-20][-3] bedrock", "Tips"),
    TIP_SET_LINEAR("Tip: Set blocks linearly with //set #l3d[wood,bedrock]", "Tips"),
    TIP_SURFACE_SPREAD("Tip: Spread a flat surface with //set #surfacespread[5][0][5][#existing]", "Tips"),
    TIP_SET_HAND("Tip: Use your current hand with //set hand", "Tips"),

    // replace
    TIP_REPLACE_REGEX("Tip: Replace using regex: //replace .*_log <pattern>", "Tips"),
    TIP_REPLACE_REGEX_2("Tip: Replace using regex: //replace .*stairs[facing=(north|south)] <pattern>", "Tips"),
    TIP_REPLACE_REGEX_3("Tip: Replace using operators: //replace water[level>2] sand", "Tips"),
    TIP_REPLACE_REGEX_4("Tip: Replace using operators: //replace true *[waterlogged=false]", "Tips"),
    TIP_REPLACE_REGEX_5("Tip: Replace using operators: //replace true *[level-=1]", "Tips"),

    TIP_REPLACE_ID("Tip: Replace only the block id: //replace woodenstair #id[cobblestair]", "Tips"),
    TIP_REPLACE_LIGHT("Tip: Remove light sources with //replace #brightness[1][15] 0", "Tips"),
    TIP_TAB_COMPLETE("Tip: The replace command supports tab completion", "Tips"),

    // clipboard
    TIP_FLIP("Tip: Mirror with //flip", "Tips"),
    TIP_DEFORM("Tip: Reshape with //deform", "Tips"),
    TIP_TRANSFORM("Tip: Set a transform with //gtransform", "Tips"),
    TIP_COPYPASTE("Tip: Paste on click with //br copypaste", "Tips"),
    TIP_SOURCE_MASK("Tip: Set a source mask with /gsmask <mask>", "Tips"),
    TIP_REPLACE_MARKER("Tip: Replace a block with your full clipboard using //replace wool #fullcopy", "Tips"),
    TIP_PASTE("Tip: Place with //paste", "Tips"),
    TIP_LAZYCOPY("Tip: lazycopy is faster", "Tips"),
    TIP_DOWNLOAD("Tip: Try out //download", "Tips"),
    TIP_ROTATE("Tip: Orientate with //rotate", "Tips"),
    TIP_COPY_PATTERN("Tip: To use as a pattern try #copy", "Tips"),

    // regen
    TIP_REGEN_0("Tip: Use a biome with /regen [biome]", "Tips"),
    TIP_REGEN_1("Tip: Use a seed with /regen [biome] [seed]", "Tips"),

    TIP_BIOME_PATTERN("Tip: The #biome[forest] pattern can be used in any command", "Tips"),
    TIP_BIOME_MASK("Tip: Restrict to a biome with the `$jungle` mask", "Tips"),;

    /**
     * Translated
     */
    private String translatedMessage;
    /**
     * Default
     */
    private String defaultMessage;
    /**
     * What locale category should this translation fall under
     */
    private String category;

    /**
     * Constructor
     *
     * @param defaultMessage default
     */
    BBC(String defaultMessage, String category) {
        this.defaultMessage = defaultMessage;
        setTranslated(defaultMessage);
        this.category = category.toLowerCase(Locale.ROOT);
    }

    public String format(Object... args) {
        String m = this.translatedMessage;
        for (int i = args.length - 1; i >= 0; i--) {
            if (args[i] == null) {
                continue;
            }
            m = m.replace("%s" + i, args[i].toString());
        }
        if (args.length > 0) {
            m = m.replace("%s", args[0].toString());
        }
        return m;
    }

    public static void load(File file) {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            final YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            final Set<String> keys = yml.getKeys(true);
            final EnumSet<BBC> all = EnumSet.allOf(BBC.class);
            final HashSet<String> allNames = new HashSet<>();
            final HashSet<String> allCats = new HashSet<>();
            final HashSet<String> toRemove = new HashSet<>();
            for (BBC c : all) {
                allNames.add(c.name());
                allCats.add(c.category.toLowerCase());
            }
            final EnumSet<BBC> captions = EnumSet.noneOf(BBC.class);
            boolean changed = false;
            for (String key : keys) {
                final Object value = yml.get(key);
                if (value instanceof MemorySection) {
                    continue;
                }
                final String[] split = key.split("\\.");
                final String node = split[split.length - 1].toUpperCase(Locale.ROOT);
                final BBC caption = allNames.contains(node) ? valueOf(node) : null;
                if (caption != null) {
                    if (!split[0].equalsIgnoreCase(caption.category)) {
                        changed = true;
                        yml.set(key, null);
                        yml.set(caption.category + "." + caption.name().toLowerCase(Locale.ROOT), value);
                    }
                    captions.add(caption);
                    caption.setTranslated((String) value);
                } else {
                    toRemove.add(key);
                }
            }
            for (String remove : toRemove) {
                changed = true;
                yml.set(remove, null);
            }
            for (BBC caption : all) {
                if (!captions.contains(caption)) {
                    changed = true;
                    yml.set(caption.category + "." + caption.name().toLowerCase(Locale.ROOT), caption.defaultMessage);
                }
            }
            if (changed) {
                yml.save(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setTranslated(String msg) {
        this.translatedMessage = msg;
    }

    @Override
    public String toString() {
        return s();
    }

    public boolean isEmpty() {
        return length() == 0;
    }

    public int length() {
        return toString().length();
    }

    public String s() {
        return this.translatedMessage;
    }

    public String original() {
        return defaultMessage;
    }

    public String getCategory() {
        return this.category;
    }

    public BBC or(BBC... others) {
        int index = ThreadLocalRandom.current().nextInt(others.length + 1);
        return index == 0 ? this : others[index - 1];
    }

    public void send(Object actor, Object... args) {
        if (isEmpty()) {
            return;
        }
        if (actor == null) {
            getLogger(BBC.class).debug(this.format(args));
        } else {
            try {
                Method method = actor.getClass().getMethod("print", String.class);
                method.setAccessible(true);
                method.invoke(actor, this.format(args));
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void send(Actor player, Object... args) {
        if (isEmpty()) {
            return;
        }
        if (player == null) {
            getLogger(BBC.class).debug(this.format(args));
        } else {
            player.print(this.format(args));
        }
    }

    /**
     * Colorize a component with legacy color codes
     * @param parent
     * @param locale
     * @return Component
     */
    public static Component color(Component component, Locale locale) {
        return color(WorldEditText.format(component, locale));
    }

    public static Component color(Component parent) {
        if (parent instanceof TextComponent) {
            TextComponent text = (TextComponent) parent;
            String content = text.content();
            if (content.indexOf('&') != -1) {
                Component legacy = LegacyComponentSerializer.legacy().deserialize(content, '&');
                legacy = legacy.style(parent.style());
                if (!parent.children().isEmpty()) {
                    parent = TextComponent.builder().append(legacy).append(parent.children()).build();
                } else {
                    parent = legacy;
                }
            }
        }
        List<Component> children = parent.children();
        if (!children.isEmpty()) {
            for (int i = 0; i < children.size(); i++) {
                Component child = children.get(i);
                Component coloredChild = color(child);
                if (coloredChild != child) {
                    if (!(children instanceof ArrayList)) {
                        children = new ArrayList<>(children);
                    }
                    children.set(i, coloredChild);
                }
            }
            if (children instanceof ArrayList) {
                parent = parent.children(children);
            }
        }
        return parent;
    }

}
