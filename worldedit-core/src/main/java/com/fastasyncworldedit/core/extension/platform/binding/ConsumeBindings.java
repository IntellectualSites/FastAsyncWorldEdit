package com.fastasyncworldedit.core.extension.platform.binding;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.util.MainUtil;
import com.fastasyncworldedit.core.util.image.ImageUtil;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.annotation.Confirm;
import com.sk89q.worldedit.command.util.annotation.Preload;
import com.sk89q.worldedit.command.util.annotation.Time;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Identifiable;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import org.enginehub.piston.exception.StopExecutionException;
import org.enginehub.piston.inject.InjectedValueAccess;

import java.util.UUID;

@SuppressWarnings("unused")
public class ConsumeBindings extends Bindings {

    private final PlatformCommandManager manager;

    public ConsumeBindings(WorldEdit worldEdit, PlatformCommandManager manager) {
        super(worldEdit);
        this.manager = manager;
    }

    @Time
    @Binding
    public Long time(Actor actor, String argument) {
        return MainUtil.timeToSec(argument) * 1000;
    }

    @Binding
    @Confirm(Confirm.Processor.REGION)
    public int regionMultiple(Actor actor, InjectedValueAccess context, @Selection Region region, String argument) {
        try {
            int times = (int) Expression.compile(argument).evaluate();
            return Confirm.Processor.REGION.check(actor, context, times);
        } catch (Throwable t) {
            if (t instanceof StopExecutionException) { // Maintain throw from Confirm#check
                throw t;
            } else {
                throw new InputParseException(TextComponent.of(t.getMessage()), t);
            }
        }
    }

    @Binding
    @Confirm(Confirm.Processor.RADIUS)
    public Integer radiusInteger(Actor actor, InjectedValueAccess context, String argument) {
        try {
            int times = (int) Expression.compile(argument).evaluate();
            return Confirm.Processor.RADIUS.check(actor, context, times);
        } catch (Throwable t) {
            if (t instanceof StopExecutionException) { // Maintain throw from Confirm#check
                throw t;
            } else {
                throw new InputParseException(TextComponent.of(t.getMessage()), t);
            }
        }
    }

    @Binding
    @Confirm(Confirm.Processor.LIMIT)
    public Integer limitInteger(Actor actor, InjectedValueAccess context, String argument) {
        try {
            int times = (int) Expression.compile(argument).evaluate();
            return Confirm.Processor.LIMIT.check(actor, context, times);
        } catch (Throwable t) {
            if (t instanceof StopExecutionException) { // Maintain throw from Confirm#check
                throw t;
            } else {
                throw new InputParseException(TextComponent.of(t.getMessage()), t);
            }
        }
    }

    @Binding
    @Confirm(Confirm.Processor.RADIUS)
    public Double radiusDouble(Actor actor, InjectedValueAccess context, String argument) {
        try {
            double times = Expression.compile(argument).evaluate();
            return Confirm.Processor.RADIUS.check(actor, context, times);
        } catch (Throwable t) {
            if (t instanceof StopExecutionException) { // Maintain throw from Confirm#check
                throw t;
            } else {
                throw new InputParseException(TextComponent.of(t.getMessage()), t);
            }
        }
    }

    @Binding
    @Confirm(Confirm.Processor.LIMIT)
    public Double limitDouble(Actor actor, InjectedValueAccess context, String argument) {
        try {
            double times = Expression.compile(argument).evaluate();
            return Confirm.Processor.LIMIT.check(actor, context, times);
        } catch (Throwable t) {
            if (t instanceof StopExecutionException) { // Maintain throw from Confirm#check
                throw t;
            } else {
                throw new InputParseException(TextComponent.of(t.getMessage()), t);
            }
        }
    }

    @Binding
    @Confirm(Confirm.Processor.RADIUS)
    public BlockVector2 radiusVec2(Actor actor, InjectedValueAccess context, String argument) {
        BlockVector2 radius = manager.parseConverter(argument, context, BlockVector2.class);
        double length = radius.length();
        Confirm.Processor.RADIUS.check(actor, context, length);
        return radius;
    }

    @Binding
    @Confirm(Confirm.Processor.RADIUS)
    public BlockVector3 radiusVec3(Actor actor, InjectedValueAccess context, String argument) {
        BlockVector3 radius = manager.parseConverter(argument, context, BlockVector3.class);
        double length = radius.length();
        Confirm.Processor.RADIUS.check(actor, context, length);
        return radius;
    }

    @Binding
    @Preload(Preload.PreloadCheck.PRELOAD)
    public void checkPreload(Actor actor, InjectedValueAccess context) {
        Preload.PreloadCheck.PRELOAD.preload(actor, context);
    }

    @Binding
    @Preload(Preload.PreloadCheck.NEVER)
    public void neverPreload(Actor actor, InjectedValueAccess context) {
    }

    @Binding
    public UUID playerUUID(Actor actor, String argument) {
        if (argument.equals("me")) {
            return actor.getUniqueId();
        }
        if (argument.equals("*")) {
            return Identifiable.EVERYONE;
        }
        if (argument.equalsIgnoreCase("console") || argument.equalsIgnoreCase("server")) {
            return Identifiable.CONSOLE;
        }
        UUID uuid;
        if (argument.length() > 16) {
            uuid = UUID.fromString(argument);
        } else {
            uuid = Fawe.platform().getUUID(argument);
        }
        if (uuid == null) {
            throw new InputParseException(Caption.of("fawe.error.player.not.found", TextComponent.of(argument)));
        }
        return uuid;
    }


    @Binding
    public ProvideBindings.ImageUri getImage(String argument) {
        return new ProvideBindings.ImageUri(ImageUtil.getImageURI(argument));
    }

    @Binding
    public BlockType blockType(Actor actor, String argument) {
        return blockState(actor, argument).getBlockType();
    }

    @Binding
    public BlockStateHolder blockStateHolder(Actor actor, String argument) {
        return blockState(actor, argument);
    }

    @Binding
    public BlockState blockState(Actor actor, String argument) {
        return baseBlock(actor, argument).toBlockState();
    }

    @Binding
    public BaseBlock baseBlock(Actor actor, String argument) {
        ParserContext parserContext = new ParserContext();
        parserContext.setActor(actor);
        parserContext.setTryLegacy(actor.getLimit().ALLOW_LEGACY);
        if (actor instanceof Entity) {
            Extent extent = ((Entity) actor).getExtent();
            if (extent instanceof World) {
                parserContext.setWorld((World) extent);
            }
        }
        parserContext.setSession(getWorldEdit().getSessionManager().get(actor));
        try {
            return getWorldEdit().getBlockFactory().parseFromInput(argument, parserContext);
        } catch (NoMatchException e) {
            throw new InputParseException(TextComponent.of(e.getMessage()));
        }
    }

}
