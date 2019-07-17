package com.sk89q.worldedit.command;

import com.boydti.fawe.object.extent.Linear3DTransform;
import com.boydti.fawe.object.extent.LinearTransform;
import com.boydti.fawe.object.extent.OffsetExtent;
import com.boydti.fawe.object.extent.PatternTransform;
import com.boydti.fawe.object.extent.RandomOffsetTransform;
import com.boydti.fawe.object.extent.RandomTransform;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.object.extent.ScaleTransform;
import com.boydti.fawe.object.extent.TransformExtent;
import com.boydti.fawe.util.ExtentTraverser;
import org.enginehub.piston.annotation.Command;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.util.command.parametric.Optional;
import java.util.Set;

@Command(aliases = {"transforms"},
        desc = "Help for the various transforms. [More Info](https://git.io/v9KHO)",
        descFooter = "Transforms modify how a block is placed\n" +
                " - Use [brackets] for arguments\n" +
                " - Use , to OR multiple\n" +
                " - Use & to AND multiple\n" +
                "More Info: https://git.io/v9KHO"
)
public class TransformCommands extends MethodCommands {
    public TransformCommands(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Command(
            name = "#linear",
            aliases = {"#l"},
            desc = "Sequentially pick from a list of transform"
)
    public ResettableExtent linear(Actor actor, LocalSession session, @Arg(name = "other", desc = "ResettableExtent", def = "#null") ResettableExtent other) {
        if (other instanceof RandomTransform) {
            Set<ResettableExtent> extents = ((RandomTransform) other).getExtents();
            return new LinearTransform(extents.toArray(new ResettableExtent[extents.size()]));
        }
        return other;
    }

    @Command(
            name = "#linear3d",
            aliases = {"#l3d"},
            desc = "Use the x,y,z coordinate to pick a transform from the list"
)
    public ResettableExtent linear3d(Actor actor, LocalSession session, @Arg(name = "other", desc = "ResettableExtent", def = "#null") ResettableExtent other) {
        if (other instanceof RandomTransform) {
            Set<ResettableExtent> extents = ((RandomTransform) other).getExtents();
            return new Linear3DTransform(extents.toArray(new ResettableExtent[extents.size()]));
        }
        return other;
    }

    @Command(
            name = "#pattern",
            desc = "Always use a specific pattern"
)
    public ResettableExtent pattern(Actor actor, LocalSession session, Pattern pattern, @Arg(name = "other", desc = "ResettableExtent", def = "#null") ResettableExtent other) {
        return new PatternTransform(other, pattern);
    }

    @Command(
            name = "#offset",
            desc = "Offset transform"
)
    public ResettableExtent offset(Actor actor, LocalSession session, double x, double y, double z, @Arg(name = "other", desc = "ResettableExtent", def = "#null") ResettableExtent other) {
        return new OffsetExtent(other, (int) x, (int) y, (int) z);
    }

    @Command(
            name = "#spread",
            aliases = {"#randomoffset"},
            desc = "Random offset transform"
)
    public ResettableExtent randomoffset(Actor actor, LocalSession session, double x, double y, double z, @Arg(name = "other", desc = "ResettableExtent", def = "#null") ResettableExtent other) {
        return new RandomOffsetTransform(other, (int) x, (int) y, (int) z);
    }

    @Command(
            name = "#scale",
            desc = "All changes will be scaled"
)
    public ResettableExtent scale(Actor actor, LocalSession session, double x, double y, double z, @Arg(name = "other", desc = "ResettableExtent", def = "#null") ResettableExtent other) {
        return new ScaleTransform(other, x, y, z);
    }

    @Command(
            name = "#rotate",
            desc = "All changes will be rotate around the initial position"
)
    public ResettableExtent rotate(Player player, LocalSession session, double x, double y, double z, @Arg(name = "other", desc = "ResettableExtent", def = "#null") ResettableExtent other) {
        ExtentTraverser traverser = new ExtentTraverser(other).find(TransformExtent.class);
        BlockTransformExtent affine = (TransformExtent) (traverser != null ? traverser.get() : null);
        if (affine == null) {
            other = affine = new TransformExtent(other);
        }
        AffineTransform transform = (AffineTransform) affine.getTransform();
        transform = transform.rotateX(x);
        transform = transform.rotateY(y);
        transform = transform.rotateZ(z);
        affine.setTransform(transform);
        return (ResettableExtent) other;
    }
}
