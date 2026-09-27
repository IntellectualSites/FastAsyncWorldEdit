/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.bukkit.adapter.ext.fawe.v1_21_R1;


/**
 * Dedicated class to map all names that we use.
 *
 * <p>
 * Overloads are split into multiple fields, as they <em>CAN</em> have different obfuscated names.
 * </p>
 */
public final class StaticRefraction {
    public static final String GET_CHUNK_FUTURE_MAIN_THREAD = "getChunkFutureMainThread";
    public static final String MAIN_THREAD_PROCESSOR = "mainThreadProcessor";
    public static final String NEXT_TICK_TIME = "nextTickTime";
    public static final String GET_BLOCK_STATE = "getBlockState";
    /**
     * {@code addFreshEntityWithPassengers(Entity entity)}.
     */
    public static final String ADD_FRESH_ENTITY_WITH_PASSENGERS_ENTITY = "addFreshEntityWithPassengers";
    /**
     * {@code addFreshEntityWithPassengers(Entity entity, CreatureSpawnEvent.SpawnReason reason)}.
     */
    public static final String ADD_FRESH_ENTITY_WITH_PASSENGERS_ENTITY_SPAWN_REASON =
        "addFreshEntityWithPassengers";
    /**
     * {@code addFreshEntity(Entity entity)}.
     */
    public static final String ADD_FRESH_ENTITY = "addFreshEntity";
    /**
     * {@code addFreshEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason)}.
     */
    public static final String ADD_FRESH_ENTITY_SPAWN_REASON = "addFreshEntity";
    /**
     * {@code getBlockEntity(BlockPos blockPos)}.
     */
    public static final String GET_BLOCK_ENTITY = "getBlockEntity";
    /**
     * {@code setBlock(BlockPos blockPos, BlockState blockState, int flags)}.
     */
    public static final String SET_BLOCK = "setBlock";
    /**
     * {@code setBlock(BlockPos blockPos, BlockState blockState, int flags, int maxUpdateDepth)}.
     */
    public static final String SET_BLOCK_MAX_UPDATE = "setBlock";
    public static final String REMOVE_BLOCK = "removeBlock";
    /**
     * {@code destroyBlock(BlockPos blockPos, boolean drop)}.
     */
    public static final String DESTROY_BLOCK = "destroyBlock";
    /**
     * {@code destroyBlock(BlockPos blockPos, boolean drop, Entity breakingEntity)}.
     */
    public static final String DESTROY_BLOCK_BREAKING_ENTITY = "destroyBlock";
    /**
     * {@code destroyBlock(BlockPos blockPos, boolean drop, Entity breakingEntity, int maxUpdateDepth)}.
     */
    public static final String DESTROY_BLOCK_BREAKING_ENTITY_MAX_UPDATE = "destroyBlock";
}
