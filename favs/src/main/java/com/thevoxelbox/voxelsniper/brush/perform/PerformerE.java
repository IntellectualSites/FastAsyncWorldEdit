/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thevoxelbox.voxelsniper.brush.perform;

import org.bukkit.ChatColor;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;



/* The m/i/c system of naming performers: <placement-option>[replacement-option][extras]
 *
 * placement-option is mandatory and can be material(m) [for /v], ink(i) [for /vi] or combo(c) [for both]
 * replacement-option is optional and can be m [for /vr], i [for /vir] or c [for both]
 * extras is optional and can be update(u) [for graphical glitch], physics(p) [for no-phys] or up [for both]
 *
 * new extra: n = no undo
 *
 * The main benefit of this system is that it provides the least possible number of characters in the parameters
 * while guaranteeing that all sensible combinations will be made.  Additionally, the names will be VERY consistent
 *
 * EX Old System: /b b isrcup (use /v, /vi, /vr and /vir, update graphics and no physics)
 * EX New System: /b b ccup   (two characters shorter, good because snipers have been complaining about keystrokes)
 *
 */

/* This enum is getting REALLY Long, would it be possible to algorithmically generate the full performer
 * from the pieces? So if the performer name is of the for m*, you'll setTypeId whereas if it is of the
 * form c* you'd setTypeIdAndPropertyId?  Similarly, if the performer is of the form *p, any setTypeId's or setTypeIdAndPropertyId's
 * will be set to false instead of true? The middle bits might be tougher, being of the form _m* perhaps?
 * Regex to the rescue, am I right? - Giltwist
 */

public enum PerformerE {

    MATERIAL(pMaterial.class, "m", "material"),
    MATERIAL_NOPHYS(pMaterialNoPhys.class, "mp", "mat-nophys"),
    MAT_MAT(pMatMat.class, "mm", "mat-mat"),
    MAT_MAT_NOPHYS(pMatMatNoPhys.class, "mmp", "mat-mat-nophys"),
    MAT_INK(pMatInk.class, "mi", "mat-ink"),
    MAT_INK_NOPHYS(pMatInkNoPhys.class, "mip", "mat-ink-nophys"),
    MAT_COMBO(pMatCombo.class, "mc", "mat-combo"),
    MAT_COMBO_NOPHYS(pMatComboNophys.class, "mcp", "mat-combo-nophys"),
    INK(pInk.class, "i", "ink"),
    INK_MAT(pInkMat.class, "im", "ink-mat"),
    INK_INK(pInkInk.class, "ii", "ink-ink"),
    INK_COMBO(pInkCombo.class, "ic", "ink-combo"),
    INK_INK_NOPHYS(pInkInkNoPhys.class, "iip", "ink-ink-nophys"),
    INK_NOPHYS(pInkNoPhys.class, "ip", "ink-nophys"),
    INK_MAT_NOPHYS(pInkMatNoPhys.class, "imp", "ink-mat-nophys"),
    INK_COMBO_NOPHYS(pInkComboNoPhys.class, "icp", "ink-combo-nophys"),
    COMBO(pCombo.class, "c", "combo"),
    COMBO_NOPHYS(pComboNoPhys.class, "cp", "combo-nophys"),
    COMBO_MAT(pComboMat.class, "cm", "combo-mat"),
    COMBO_MAT_NOPHYS(pComboMatNoPhys.class, "cmp", "combo-mat-nophys"),
    COMBO_INK(pComboInk.class, "ci", "combo-ink"),
    COMBO_INK_NOPHYS(pComboInkNoPhys.class, "cip", "combo-ink-nophys"),
    COMBO_COMBO(pComboCombo.class, "cc", "combo-combo"),
    COMBO_COMBO_NOPHYS(pComboComboNoPhys.class, "ccp", "combo-combo-nophys"),
    EXCLUDE_MATERIAL(pExcludeMat.class, "xm", "exclude-mat"),
    EXCLUDE_INK(pExcludeInk.class, "xi", "exclude-ink"),
    EXCLUDE_COMBO(pExcludeCombo.class, "xc", "exclude-combo"),
    INCLUDE_MATERIAL(pIncludeMat.class, "nm", "include-mat"),
    INCLUDE_INK(pIncludeInk.class, "ni", "include-ink"),
    INCLUDE_COMBO(pIncludeCombo.class, "nc", "include-combo"),
    MAT_NO_UNDO(pNoUndo.class, "noundo", "noundo");
    //Other Performers which don't exist yet but are required for a full set of possibilities that actually could potentially do something:
    //List does not include any no-physics, unless materials are being placed (or combo), or any update unless ink is being placed (or combo) -Gavjenks

    //MAT_MAT_UPDATE(           pMatMatUpdate.class,            "mmu",          "mat-mat-update"    ),      //              place mat, replace mat, graphical update
    //MAT_COMBO_UPDATE(         pMatComboUpdate.class,          "mcu",          "mat-combo-update"  ),      //              place mat, replace combo, graphical update
    //MAT_COMBO_NOPHYS_UPDATE(  pMatComboNoPhysUpdate.class,    "mcup",         "mat-combo-update-nophys"), //              place mat, replace combo, update, no physics
    //MAT_INK_UPDATE(           pMatInkUpdate.class,            "miu",          "mat-ink-update"),          //              place mat, replace ink, graphical update
    //MAT_INK_NOPHYS_UPDATE(    pMatInkNoPhysUpdate.class,      "miup",         "mat-ink-update-nophys"),   //              place mat, replace ink, graphical update no physics
    //INK_MAT_UPDATE(           pInkMatUpdate.class,            "imu",          "ink-mat-update"),          //              place ink, replace mat, graphical update
    //INK_INK_UPDATE(           pInkInkUpdate.class,            "iiu",          "ink-ink-update"),          //              place ink, replace ink, graphical update
    //INK_COMBO_UPDATE(         pInkComboUpdate.class,          "icu",          "ink-combo-update"),        //              place ink, replace combo, graphical update
    //COMBO_MAT_UPDATE(         pComboMatUpdate.class,          "cmu",          "combo-mat-update"),        //              place combo, replace mat, graphical update
    //COMBO_MAT_NOPHYS_UPDATE(  pComboMatNoPhysUpdate.class,    "cmup",         "combo-mat-update-nophys"), //              place combo, replace mat, graphical update, no physics
    //COMBO_INK_UPDATE(         pComboInkUpdate.class,          "ciu",          "combo-ink-update"),        //              place combo, replace ink, graphical update
    //COMBO_INK_NOPHYS_UPDATE(  pComboInkNoPhysUpdate.class,    "ciup",         "combo-ink-update-nophys"), //              place combo, replace ink, graphical update, no physics
    //COMBO_COMBO_UPDATE(       pComboComboUpdate.class,        "ccu",          "combo-combo-update"),      //              place combo, replace combo, graphical update
    //COMBO_COMBO_NOPHYS_UPDATE(pComboComboNoPhysUpdate.class,  "ccup",         "combo-combo-update-nophys"),//             place combo, replace combo, graphical update, no physics

    public static String performer_list_short = "";
    public static String performer_list_long = "";
    private static Map<String, vPerformer> performers;
    private static Map<String, String> long_names;

    static {
        performers = new TreeMap<>();
        long_names = new TreeMap<>();

        for (PerformerE pe : values()) {
            performers.put(pe.short_name, pe.getPerformer());
            long_names.put(pe.long_name, pe.short_name);
            performer_list_short = performer_list_short + ChatColor.GREEN + pe.short_name + ChatColor.RED + ", ";
            performer_list_long = performer_list_long + ChatColor.GREEN + pe.long_name + ChatColor.RED + ", ";
        }
        performer_list_short = performer_list_short.substring(0, performer_list_short.length() - 2);
        performer_list_long = performer_list_long.substring(0, performer_list_long.length() - 2);
    }

    private Class<? extends vPerformer> pclass;
    private String short_name;
    private String long_name;

    PerformerE(Class<? extends vPerformer> c, String s, String l) {
        pclass = c;
        short_name = s;
        long_name = l;
    }

    public static vPerformer getPerformer(String s) {
        if (performers.containsKey(s)) {
            return performers.get(s);
        } else {
            return performers.get(long_names.get(s));
        }
    }

    public static boolean has(String s) {
        return performers.containsKey(s);
    }

    private vPerformer getPerformer() {
        vPerformer p;
        try {
            try {
                p = pclass.getConstructor().newInstance();
                return p;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | IllegalArgumentException ex) {
                Logger.getLogger(PerformerE.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (NoSuchMethodException | SecurityException ex) {
            Logger.getLogger(PerformerE.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
