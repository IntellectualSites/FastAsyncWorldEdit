package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R3;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FaweMutableBlockPlaceContext extends BlockPlaceContext {

    private static final BlockHitResult DEFAULT_BLOCK_HIT = new BlockHitResult(Vec3.ZERO, Direction.NORTH, BlockPos.ZERO, false);
    private final ServerLevel level;
    private BlockHitResult hitResult = null;
    private Direction direction = null;
    private BlockPos relativePos;

    @SuppressWarnings("DataFlowIssue")
    public FaweMutableBlockPlaceContext(ServerLevel level) {
        super(
                level,
                null,
                null,
                null,
                DEFAULT_BLOCK_HIT

        );
        this.level = level;
        this.replaceClicked = false;
    }

    public FaweMutableBlockPlaceContext withSetting(BlockHitResult hitResult, Direction direction) {
        this.hitResult = hitResult;
        this.direction = direction;
        this.relativePos = hitResult.getBlockPos().relative(hitResult.getDirection());
        return this;
    }

    @Override
    @Nonnull
    public BlockPos getClickedPos() {
        return this.relativePos;
    }

    @Override
    @Nonnull
    public Direction getClickedFace() {
        return this.hitResult.getDirection();
    }

    @Override
    @Nonnull
    public Vec3 getClickLocation() {
        return this.hitResult.getLocation();
    }

    @Override
    public boolean isInside() {
        return this.hitResult.isInside();
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public ItemStack getItemInHand() {
        return ItemStack.EMPTY;
    }

    @Nullable
    @Override
    public Player getPlayer() {
        return null;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public InteractionHand getHand() {
        return null;
    }

    @Override
    @Nonnull
    public Level getLevel() {
        return this.level;
    }

    @Override
    @Nonnull
    public Direction getHorizontalDirection() {
        return this.direction.getAxis() == Direction.Axis.Y ? Direction.NORTH : this.direction;
    }

    @Override
    public boolean isSecondaryUseActive() {
        return false;
    }

    @Override
    public float getRotation() {
        return (float) (this.direction.get2DDataValue() * 90);
    }

    @Override
    public boolean canPlace() {
        return this.getLevel().getBlockState(this.getClickedPos()).canBeReplaced(this);
    }

    @Override
    public boolean replacingClickedOnBlock() {
        return false;
    }

    @Override
    @Nonnull
    public Direction getNearestLookingDirection() {
        return direction;
    }

    @Override
    @Nonnull
    public Direction getNearestLookingVerticalDirection() {
        return direction.getAxis() == Direction.Axis.Y ? Direction.UP : Direction.DOWN;
    }

    @Override
    @Nonnull
    public Direction[] getNearestLookingDirections() {
        return new Direction[]{direction};
    }

}
