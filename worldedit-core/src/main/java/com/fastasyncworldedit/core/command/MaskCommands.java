// TODO: Ping @MattBDev to reimplement (or remove because this class is stupid) 2020-02-04
//package com.fastasyncworldedit.core.command;
//
//import com.boydti.fawe.object.mask.AdjacentAnyMask;
//import com.boydti.fawe.object.mask.AdjacentMask;
//import com.boydti.fawe.object.mask.AngleMask;
//import com.boydti.fawe.object.mask.BiomeMask;
//import com.boydti.fawe.object.mask.BlockLightMask;
//import com.boydti.fawe.object.mask.BrightnessMask;
//import com.boydti.fawe.object.mask.ExtremaMask;
//import com.boydti.fawe.object.mask.LightMask;
//import com.boydti.fawe.object.mask.OpacityMask;
//import com.boydti.fawe.object.mask.ROCAngleMask;
//import com.boydti.fawe.object.mask.RadiusMask;
//import com.boydti.fawe.object.mask.RandomMask;
//import com.boydti.fawe.object.mask.SimplexMask;
//import com.boydti.fawe.object.mask.SkyLightMask;
//import com.boydti.fawe.object.mask.SurfaceMask;
//import com.boydti.fawe.object.mask.WallMask;
//import com.boydti.fawe.function.mask.XAxisMask;
//import com.boydti.fawe.function.mask.YAxisMask;
//import com.boydti.fawe.function.mask.ZAxisMask;
//import com.sk89q.worldedit.IncompleteRegionException;
//import com.sk89q.worldedit.LocalSession;
//import com.sk89q.worldedit.WorldEdit;
//import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
//import com.sk89q.worldedit.entity.Player;
//import com.sk89q.worldedit.extent.Extent;
//import com.sk89q.worldedit.function.mask.BlockMaskBuilder;
//import com.sk89q.worldedit.function.mask.ExistingBlockMask;
//import com.sk89q.worldedit.function.mask.ExpressionMask;
//import com.sk89q.worldedit.function.mask.Mask;
//import com.sk89q.worldedit.function.mask.MaskIntersection;
//import com.sk89q.worldedit.function.mask.MaskUnion;
//import com.sk89q.worldedit.function.mask.Masks;
//import com.sk89q.worldedit.function.mask.OffsetMask;
//import com.sk89q.worldedit.function.mask.RegionMask;
//import com.sk89q.worldedit.function.mask.SolidBlockMask;
//import com.sk89q.worldedit.internal.expression.Expression;
//import com.sk89q.worldedit.internal.expression.ExpressionEnvironment;
//import com.sk89q.worldedit.internal.expression.ExpressionException;
//import com.sk89q.worldedit.math.BlockVector3;
//import com.sk89q.worldedit.math.Vector3;
//import com.sk89q.worldedit.regions.shape.WorldEditExpressionEnvironment;
//import com.sk89q.worldedit.session.request.RequestSelection;
//import com.sk89q.worldedit.world.biome.BiomeType;
//import org.enginehub.piston.annotation.Command;
//import org.enginehub.piston.annotation.CommandContainer;
//import org.enginehub.piston.annotation.param.Arg;
//import org.enginehub.piston.annotation.param.Switch;
//
////@Command(aliases = {"masks"},
////        desc = "Help for the various masks. [More Info](https://git.io/v9r4K)",
////        descFooter = "Masks determine if a block can be placed\n" +
////                " - Use [brackets] for arguments\n" +
////                " - Use , to OR multiple\n" +
////                " - Use & to AND multiple\n" +
////                "e.g. >[stone,dirt],#light[0][5],$jungle\n" +
////                "More Info: https://git.io/v9r4K"
////)
//@CommandContainer//(superTypes = CommandPermissionsConditionGenerator.Registration.class)
//public class MaskCommands {
//    private final WorldEdit worldEdit;
//
//    public MaskCommands(WorldEdit worldEdit) {
//        this.worldEdit = worldEdit;
//    }
//    @Command(
//            name = "#light",
//            desc = "Restrict to specific light levels"
//    )
//    public Mask light(Extent extent, @Arg(name="mine", desc = "min light") double minInt, @Arg(name="mine", desc = "max light") double maxInt) {
//        return new LightMask(extent, (int) minInt, (int) maxInt);
//    }
//
//    @Command(
//            name = "#skylight",
//            desc = "Restrict to specific sky light levels"
//    )
//    public Mask skylight(Extent extent, @Arg(name="mine", desc = "min light") double minInt, @Arg(name="mine", desc = "max light") double maxInt) {
//        return new SkyLightMask(extent, (int) minInt, (int) maxInt);
//    }
//
//    @Command(
//            name = "#blocklight",
//            aliases = {"#emittedlight"},
//            desc = "Restrict to specific block light levels"
//    )
//    public Mask blocklight(Extent extent, @Arg(name="mine", desc = "min light") double minInt, @Arg(name="mine", desc = "max light") double maxInt) {
//        return new BlockLightMask(extent, (int) minInt, (int) maxInt);
//    }
//
//    @Command(
//            name = "#opacity",
//            desc = "Restrict to specific opacity levels"
//    )
//    public Mask opacity(Extent extent, @Arg(name="mine", desc = "min light") double minInt, @Arg(name="mine", desc = "max light") double maxInt) {
//        return new OpacityMask(extent, (int) minInt, (int) maxInt);
//    }
//
//    @Command(
//            name = "#brightness",
//            desc = "Restrict to specific block brightness"
//    )
//    public Mask brightness(Extent extent, @Arg(name="mine", desc = "min light") double minInt, @Arg(name="mine", desc = "max light") double maxInt) {
//        return new BrightnessMask(extent, (int) minInt, (int) maxInt);
//    }
//
//    @Command(
//            name = "#haslight",
//            desc = "Restricts to blocks with light (sky or emitted)"
//    )
//    public Mask haslight(Extent extent) {
//        return new LightMask(extent, 1, Integer.MAX_VALUE);
//    }
//
//    @Command(
//            name = "#nolight",
//            desc = "Restrict to blocks without light (sky or emitted)"
//    )
//    public Mask nolight(Extent extent) {
//        return new LightMask(extent, 0, 0);
//    }

//    @Command(
//            name = "#iddata",
//            desc = "Restrict to initial block id and data"
//    )
//    public Mask iddata(Extent extent) {
//        return new IdDataMask(extent);
//    }
//}
