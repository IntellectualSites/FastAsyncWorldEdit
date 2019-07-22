package com.boydti.fawe.config;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.configuration.MemorySection;
import com.boydti.fawe.configuration.file.YamlConfiguration;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal3;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.chat.Message;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sk89q.worldedit.extension.platform.Actor;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public enum BBC {

    /*
     * Things to note about this class:
     * Can use multiple arguments %s, %s1, %s2, %s3 etc
     */
    PREFIX("&8(&4&lFAWE&8)&r&7", "Info"),
    FILE_DELETED("%s0 has been deleted.", "Info"),
    SCHEMATIC_PASTING("&7The schematic is pasting. This cannot be undone.", "Info"),
    LIGHTING_PROPOGATE_SELECTION("&7Lighting has been propogated in %s0 chunks. (Note: To remove light use //removelight)", "Info"),
    UPDATED_LIGHTING_SELECTION("&7Lighting has been updated in %s0 chunks. (It may take a second for the packets to send)", "Info"),
    SET_REGION("&7Selection set to your current allowed region", "Info"),
    WORLDEDIT_COMMAND_LIMIT("&7Please wait until your current action completes", "Info"),
    WORLDEDIT_DELAYED("&7Please wait while we process your FAWE action...", "Info"),
    WORLDEDIT_RUN("&7Apologies for the delay. Now executing: %s", "Info"),
    WORLDEDIT_COMPLETE("&7Edit completed.", "Info"),
    REQUIRE_SELECTION_IN_MASK("&7%s of your selection is not within your mask. You can only make edits within allowed regions.", "Info"),
    WORLDEDIT_VOLUME("&7You cannot select a volume of %current%. The maximum volume you can modify is %max%.", "Info"),
    WORLDEDIT_ITERATIONS("&7You cannot iterate %current% times. The maximum number of iterations allowed is %max%.", "Info"),
    WORLDEDIT_UNSAFE("&7Access to that command has been blocked", "Info"),
    WORLDEDIT_DANGEROUS_WORLDEDIT("&cProcessed unsafe edit at %s0 by %s1", "Info"),
    WORLDEDIT_EXTEND("&cYour edit may have extended outside your allowed region.", "Error"),
    WORLDEDIT_TOGGLE_TIPS_ON("&7Disabled FAWE tips.", "Info"),
    WORLDEDIT_TOGGLE_TIPS_OFF("&7Enabled FAWE tips.", "Info"),

    WORLDEDIT_BYPASSED("&7Currently bypassing FAWE restriction.", "Info"),
    WORLDEDIT_UNMASKED("&6Your FAWE edits are now unrestricted.", "Info"),

    WORLDEDIT_RESTRICTED("&6Your FAWE edits are now restricted.", "Info"),
    WORLDEDIT_OOM_ADMIN("&cPossible options:\n&8 - &7//fast\n&8 - &7Do smaller edits\n&8 - &7Allocate more memory\n&8 - &7Disable `max-memory-percent`", "Info"),
    COMPRESSED("History compressed. Saved ~ %s0b (%s1x smaller)", "Info"),

    WEB_UNAUTHORIZED("Only links from the configured web host is allowed: %s0", "Error"),
    ACTION_COMPLETE("Action completed in %s0 seconds", "Info"),
    GENERATING_LINK("Uploading %s, please wait...", "Web"),
    GENERATING_LINK_FAILED("&cFailed to generate download link!", "Web"),
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
    NAVIGATION_WAND_ERROR("&cNothing to pass through", "WorldEdit.Navigation"),

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
    BRUSH_TRY_OTHER("&cThere are other more suitable brushes e.g.\n&8 - &7//br height [radius=5] [#clipboard|file=null] [rotation=0] [yscale=1.00]", "WorldEdit.Brush"),
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
    TOOL_INSPECT_INFO("&7%s0 changed %s1 to %s2 %s3 ago", "WorldEdit.Tool"),
    TOOL_INSPECT_INFO_FOOTER("&6Total: &7%s0 changes", "WorldEdit.Tool"),
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


    SCHEMATIC_PROMPT_CLEAR("&7You may want to use &c%s0 &7to clear your current clipboard first", "Worldedit.Schematic"),
    SCHEMATIC_SHOW("&7Displaying &a%s0&7 schematics from &a%s1&7:\n" +
            "&8 - &aLeft click &7a structure to set your clipboard\n" +
            "&8 - &aRight click &7to add a structure to your multi-clipboard\n" +
            "&8 - &7Use &a%s2&7 to go back to the world", "Worldedit.Schematic"),
    SCHEMATIC_FORMAT("Available formats (Name: Lookup names)", "Worldedit.Schematic"),
    SCHEMATIC_MOVE_EXISTS("&c%s0 already exists", "Worldedit.Schematic"),
    SCHEMATIC_MOVE_SUCCESS("&a%s0 -> %s1", "Worldedit.Schematic"),
    SCHEMATIC_MOVE_FAILED("&a%s0 no moved: %s1", "Worldedit.Schematic"),
    SCHEMATIC_LOADED("%s0 loaded. Paste it with //paste", "Worldedit.Schematic"),
    SCHEMATIC_SAVED("%s0 saved.", "Worldedit.Schematic"),
    SCHEMATIC_PAGE("Page must be %s", "WorldEdit.Schematic"),
    SCHEMATIC_NONE("No files found.", "WorldEdit.Schematic"),
    SCHEMATIC_LIST("Available files (Filename: Format) [%s0/%s1]:", "Worldedit.Schematic"),
    SCHEMATIC_LIST_ELEM("&8 - &a%s0 &8- &7%s1", "Worldedit.Schematic"),

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
    BLOCK_CYCLER_NO_PERM("&cYou are not permitted to cycle the data value of that block.", "WorldEdit.Cycler"),

    COMMAND_INVALID_SYNTAX("The command was not used properly (no more help available).", "WorldEdit.Command"),

    COMMAND_CLARIFYING_BRACKET("&7Added clarifying bracket for &c%s0", "WorldEdit.Help"),
    HELP_SUGGEST("&7Couldn't find %s0. Maybe try one of &c%s1 &7?", "WorldEdit.Help"),
    HELP_HEADER_CATEGORIES("Command Types", "WorldEdit.Help"),
    HELP_HEADER_SUBCOMMANDS("Subcommands", "WorldEdit.Help"),
    HELP_HEADER_COMMAND("&cHelp for: &7%s0", "WorldEdit.Help"),
    HELP_ITEM_ALLOWED("&a%s0&8 - &7%s1", "WorldEdit.Help"),
    HELP_ITEM_DENIED("&c%s0&8 - &7%s1", "WorldEdit.Help"),
    HELP_HEADER("Help: page %s0/%s1", "WorldEdit.Help"),
    HELP_FOOTER("&7Wiki: https://git.io/vSKE5", "WorldEdit.Help"),
    PAGE_FOOTER("Use %s0 to go to the next page", "WorldEdit.Utility"),

    PROGRESS_MESSAGE("%s1/%s0 (%s2%) @%s3cps %s4s left", "Progress"),
    PROGRESS_FINISHED("[ Done! ]", "Progress"),

    COMMAND_SYNTAX("&cUsage: &7%s0", "Error"),
    NO_PERM("&cYou are lacking the permission node: %s0", "Error"),
    BLOCK_NOT_ALLOWED("You are not allowed to use", "Error"),
    SETTING_DISABLE("&cLacking setting: %s0", "Error"),
    BRUSH_NOT_FOUND("&cAvailable brushes: %s0", "Error"),
    BRUSH_INCOMPATIBLE("&cBrush not compatible with this version", "Error"),
    SCHEMATIC_NOT_FOUND("&cSchematic not found: &7%s0", "Error"),
    NO_REGION("&cYou have no current allowed region", "Error"),
    NO_MASK("&cYou have no current mask set", "Error"),
    NOT_PLAYER("&cYou must be a player to perform this action!", "Error"),
    PLAYER_NOT_FOUND("&cPlayer not found:&7 %s0", "Error"),
    OOM(
            "&8[&cCritical&8] &cDetected low memory i.e. < 1%. We will take the following actions:\n&8 - &7Terminate WE block placement\n&8 - &7Clear WE history\n&8 - &7Unload non essential chunks\n&8 - &7Kill entities\n&8 - &7Garbage collect\n&cIgnore this if trying to crash server.\n&7Note: Low memory is likely (but not necessarily) caused by WE",
            "Error"),

    WORLDEDIT_SOME_FAILS("&c%s0 blocks weren't placed because they were outside your allowed region.", "Error"),
    WORLDEDIT_SOME_FAILS_BLOCKBAG("&cMissing blocks: %s0", "Error"),

    WORLDEDIT_CANCEL_COUNT("&cCancelled %s0 edits.", "Cancel"),
    WORLDEDIT_CANCEL_REASON_CONFIRM("&7Your selection is large (&c%s0 &7-> &c%s1&7, containing &c%s3&7 blocks). Use &c//confirm &7to execute &c%s2", "Cancel"),
    WORLDEDIT_CANCEL_REASON("&cYour WorldEdit action was cancelled:&7 %s0&c.", "Cancel"),
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
    WORLDEDIT_FAILED_LOAD_CHUNK("&cSkipped loading chunk: &7%s0;%s1&c. Try increasing chunk-wait.", "Cancel"),

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
    SEL_ELLIPSIOD("Ellipsoid selector: left click=center, right click to extend", "Selection"),
    SEL_SPHERE("Sphere selector: left click=center, right click to set radius", "Selection"),
    SEL_CYLINDRICAL("Cylindrical selector: Left click=center, right click to extend.", "Selection"),
    SEL_MAX("%s0 points maximum.", "Selection"),
    SEL_FUZZY("Fuzzy selector: Left click to select all contingent blocks, right click to add. To select an air cavity, use //pos1.", "Selection"),
    SEL_CONVEX_POLYHEDRAL("Convex polyhedral selector: Left click=First vertex, right click to add more.", "Selection"),
    SEL_LIST("For a list of selection types use:&c //sel list", "Selection"),
    SEL_MODES("Select one of the modes below:", "Selection"),

    SCRIPTING_NO_PERM("&cYou do not have permission to execute this craft script", "WorldEdit.Scripting"),
    SCRIPTING_CS("Use /cs with a script name first.", "WorldEdit.Scripting"),
    SCRIPTING_ERROR("An error occured while executing a craft script", "WorldEdit.Scripting"),

    TIP_SEL_LIST("Tip: See the different selection modes with &c//sel list", "Tips"),
    TIP_SELECT_CONNECTED("Tip: Select all connected blocks with //sel fuzzy", "Tips"),
    TIP_SET_POS1("Tip: Use pos1 as a pattern with &c//set pos1", "Tips"),
    TIP_FARWAND("Tip: Select distant points with &c//farwand", "Tips"),
    TIP_DISCORD("Need help using FAWE? https://discord.gg/ngZCzbU", "Tips"),

    // cut
    TIP_LAZYCUT("&7Tip: It is safer to use &c//lazycut", "Tips"),
    // set
    TIP_FAST("&7Tip: Set fast and without undo using &c//fast", "Tips"),
    TIP_CANCEL("&7Tip: You can &c//cancel &7an edit in progress", "Tips"),
    TIP_MASK("&7Tip: Set a global destination mask with &c/gmask", "Tips"),
    TIP_MASK_ANGLE("Tip: Replace upward slopes of 3-20 blocks using&c //replace /[-20][-3] bedrock", "Tips"),
    TIP_SET_LINEAR("&7Tip: Set blocks linearly with&c //set #l3d[wood,bedrock]", "Tips"),
    TIP_SURFACE_SPREAD("&7Tip: Spread a flat surface with&c //set #surfacespread[5][0][5][#existing]", "Tips"),
    TIP_SET_HAND("&7Tip: Use your current hand with &c//set hand", "Tips"),

    // replace
    TIP_REPLACE_REGEX("&7Tip: Replace using regex:&c //replace .*_log <pattern>", "Tips"),
    TIP_REPLACE_REGEX_2("&7Tip: Replace using regex:&c //replace .*stairs[facing=(north|south)] <pattern>", "Tips"),
    TIP_REPLACE_REGEX_3("&7Tip: Replace using operators:&c //replace water[level>2] sand", "Tips"),
    TIP_REPLACE_REGEX_4("&7Tip: Replace using operators:&c //replace true *[waterlogged=false]", "Tips"),
    TIP_REPLACE_REGEX_5("&7Tip: Replace using operators:&c //replace true *[level-=1]", "Tips"),

    TIP_REPLACE_ID("&7Tip: Replace only the block id:&c //replace woodenstair #id[cobblestair]", "Tips"),
    TIP_REPLACE_LIGHT("Tip: Remove light sources with&c //replace #brightness[1][15] 0", "Tips"),
    TIP_TAB_COMPLETE("Tip: The replace command supports tab completion", "Tips"),

    // clipboard
    TIP_FLIP("Tip: Mirror with &c//flip", "Tips"),
    TIP_DEFORM("Tip: Reshape with &c//deform", "Tips"),
    TIP_TRANSFORM("Tip: Set a transform with &c//gtransform", "Tips"),
    TIP_COPYPASTE("Tip: Paste on click with &c//br copypaste", "Tips"),
    TIP_SOURCE_MASK("Tip: Set a source mask with &c/gsmask <mask>&7", "Tips"),
    TIP_REPLACE_MARKER("Tip: Replace a block with your full clipboard using &c//replace wool #fullcopy", "Tips"),
    TIP_PASTE("Tip: Place with &c//paste", "Tips"),
    TIP_LAZYCOPY("Tip: lazycopy is faster", "Tips"),
    TIP_DOWNLOAD("Tip: Try out &c//download", "Tips"),
    TIP_ROTATE("Tip: Orientate with &c//rotate", "Tips"),
    TIP_COPY_PATTERN("Tip: To use as a pattern try &c#copy", "Tips"),

    // regen
    TIP_REGEN_0("Tip: Use a biome with /regen [biome]", "Tips"),
    TIP_REGEN_1("Tip: Use a seed with /regen [biome] [seed]", "Tips"),

    TIP_BIOME_PATTERN("Tip: The &c#biome[forest]&7 pattern can be used in any command", "Tips"),
    TIP_BIOME_MASK("Tip: Restrict to a biome with the `$jungle` mask", "Tips"),;


    private static final HashMap<String, String> replacements = new HashMap<>();
    static {
        for (final char letter : "1234567890abcdefklmnor".toCharArray()) {
            replacements.put("&" + letter, "\u00a7" + letter);
        }
        replacements.put("\\\\n", "\n");
        replacements.put("\\n", "\n");
        replacements.put("&-", "\n");
    }
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
    BBC(final String defaultMessage, final String category) {
        this.defaultMessage = defaultMessage;
        this.translatedMessage = defaultMessage;
        this.category = category.toLowerCase();
    }

    public String f(final Object... args) {
        return format(args);
    }

    public String format(final Object... args) {
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

    public static void load(final File file) {
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
            for (final BBC c : all) {
                allNames.add(c.name());
                allCats.add(c.category.toLowerCase());
            }
            final EnumSet<BBC> captions = EnumSet.noneOf(BBC.class);
            boolean changed = false;
            for (final String key : keys) {
                final Object value = yml.get(key);
                if (value instanceof MemorySection) {
                    continue;
                }
                final String[] split = key.split("\\.");
                final String node = split[split.length - 1].toUpperCase();
                final BBC caption = allNames.contains(node) ? valueOf(node) : null;
                if (caption != null) {
                    if (!split[0].equalsIgnoreCase(caption.category)) {
                        changed = true;
                        yml.set(key, null);
                        yml.set(caption.category + "." + caption.name().toLowerCase(), value);
                    }
                    captions.add(caption);
                    caption.translatedMessage = (String) value;
                } else {
                    toRemove.add(key);
                }
            }
            for (final String remove : toRemove) {
                changed = true;
                yml.set(remove, null);
            }
            for (final BBC caption : all) {
                if (!captions.contains(caption)) {
                    changed = true;
                    yml.set(caption.category + "." + caption.name().toLowerCase(), caption.defaultMessage);
                }
                caption.translatedMessage = StringMan.replaceFromMap(caption.translatedMessage, replacements);
            }
            if (changed) {
                yml.save(file);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
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

    public static String color(String string) {
        return StringMan.replaceFromMap(string, replacements);
    }

    public static String stripColor(String string) {

        return StringMan.removeFromSet(string, replacements.values());
    }

    public String s() {
        return this.translatedMessage;
    }

    public Message m(Object... args) {
        return new Message(this, args);
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

    public void send(Object actor, final Object... args) {
        if (isEmpty()) {
            return;
        }
        if (actor == null) {
            Fawe.debug(this.format(args));
        } else {
            try {
                Method method = actor.getClass().getMethod("print", String.class);
                method.setAccessible(true);
                method.invoke(actor, (PREFIX.isEmpty() ? "" : PREFIX.s() + " ") + this.format(args));
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getPrefix() {
        return (PREFIX.isEmpty() ? "" : PREFIX.s() + " ");
    }

    public void send(final FawePlayer<?> player, final Object... args) {
        if (isEmpty()) {
            return;
        }
        if (player == null) {
            Fawe.debug(this.format(args));
        } else {
            player.sendMessage((PREFIX.isEmpty() ? "" : PREFIX.s() + " ") + this.format(args));
        }
    }
    public void send(final Actor player, final Object... args) {
        if (isEmpty()) {
            return;
        }
        if (player == null) {
            Fawe.debug(this.format(args));
        } else {
            player.print(this.format(args));
        }
    }

    public static char getCode(String name) {
        switch (name) {
            case "BLACK":
                return '0';
            case "DARK_BLUE":
                return '1';
            case "DARK_GREEN":
                return '2';
            case "DARK_AQUA":
                return '3';
            case "DARK_RED":
                return '4';
            case "DARK_PURPLE":
                return '5';
            case "GOLD":
                return '6';
            case "GRAY":
                return '7';
            case "DARK_GRAY":
                return '8';
            case "BLUE":
                return '9';
            case "GREEN":
                return 'a';
            case "AQUA":
                return 'b';
            case "RED":
                return 'c';
            case "LIGHT_PURPLE":
                return 'd';
            case "YELLOW":
                return 'e';
            case "WHITE":
                return 'f';
            case "OBFUSCATED":
                return 'k';
            case "BOLD":
                return 'l';
            case "STRIKETHROUGH":
                return 'm';
            case "UNDERLINE":
                return 'n';
            case "ITALIC":
                return 'o';
            default:
            case "RESET":
                return 'r';
        }
    }

    public static String getColorName(char code) {
        switch (code) {
            case '0':
                return "BLACK";
            case '1':
                return "DARK_BLUE";
            case '2':
                return "DARK_GREEN";
            case '3':
                return "DARK_AQUA";
            case '4':
                return "DARK_RED";
            case '5':
                return "DARK_PURPLE";
            case '6':
                return "GOLD";
            case '7':
                return "GRAY";
            case '8':
                return "DARK_GRAY";
            case '9':
                return "BLUE";
            case 'a':
                return "GREEN";
            case 'b':
                return "AQUA";
            case 'c':
                return "RED";
            case 'd':
                return "LIGHT_PURPLE";
            case 'e':
                return "YELLOW";
            case 'f':
                return "WHITE";
            case 'k':
                return "OBFUSCATED";
            case 'l':
                return "BOLD";
            case 'm':
                return "STRIKETHROUGH";
            case 'n':
                return "UNDERLINE";
            case 'o':
                return "ITALIC";
            case 'r':
                return "RESET";
            default:
                return "GRAY";
        }
    }

    private static Object[] append(StringBuilder builder, Map<String, Object> obj, String color, Map<String, Boolean> properties) {
        Object[] style = new Object[] { color, properties };
        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            switch (entry.getKey()) {
                case "text":
                    String text = (String) entry.getValue();
                    String newColor = (String) obj.get("color");
                    String newBold = (String) obj.get("bold");
                    int index = builder.length();
                    if (!Objects.equals(color, newColor)) {
                        style[0] = newColor;
                        char code = BBC.getCode(newColor.toUpperCase());
                        builder.append('\u00A7').append(code);
                    }
                    for (Map.Entry<String, Object> entry2 : obj.entrySet()) {
                        if (StringMan.isEqualIgnoreCaseToAny(entry2.getKey(), "bold", "italic", "underlined", "strikethrough", "obfuscated")) {
                            boolean newValue = Boolean.parseBoolean((String) entry2.getValue());
                            if (properties.put(entry2.getKey(), newValue) != newValue) {
                                if (newValue) {
                                    char code = BBC.getCode(entry2.getKey().toUpperCase());
                                    builder.append('\u00A7').append(code);
                                } else {
                                    builder.insert(index, '\u00A7').append('r');
                                    if (Objects.equals(color, newColor) && newColor != null) {
                                        builder.append('\u00A7').append(BBC.getCode(newColor.toUpperCase()));
                                    }
                                }
                            }
                        }
                    }
                    builder.append(text);
                    break;
                case "extra":
                    List<Map<String, Object>> list = (List<Map<String, Object>>) entry.getValue();
                    for (Map<String, Object> elem : list) {
                        elem.putIfAbsent("color", obj.get("color"));
                        for (Map.Entry<String, Object> entry2 : obj.entrySet()) {
                            if (StringMan.isEqualIgnoreCaseToAny(entry2.getKey(), "bold", "italic", "underlined", "strikethrough", "obfuscated")) {
                                elem.putIfAbsent(entry2.getKey(), entry2.getValue());
                            }
                        }
                        style = append(builder, elem, (String) style[0], (Map) style[1]);
                    }
            }
        }
        return style;
    }

    public static String jsonToString(String text) {
        Gson gson = new Gson();
        StringBuilder builder = new StringBuilder();
        Map<String, Object> obj = gson.fromJson(text, new TypeToken<Map<String, Object>>() {}.getType());
        HashMap<String, Boolean> properties = new HashMap<>();
        properties.put("bold", false);
        properties.put("italic", false);
        properties.put("underlined", false);
        properties.put("strikethrough", false);
        properties.put("obfuscated", false);
        append(builder, obj, null, properties);
        return builder.toString();
    }

    /**
     * @param m
     * @param runPart Part, Color, NewLine
     */
    public static void splitMessage(String m, RunnableVal3<String, String, Boolean> runPart) {
        m = color(m);
        String color = "GRAY";
        boolean newline = false;
        for (String line : m.split("\n")) {
            boolean hasColor = line.charAt(0) == '\u00A7';
            String[] splitColor = line.split("\u00A7");
            for (String part : splitColor) {
                if (hasColor) {
                    color = getColorName(part.charAt(0));
                    part = part.substring(1);
                }
                runPart.run(part, color, newline);
                hasColor = true;
            }
            newline = true;
        }
    }
}
