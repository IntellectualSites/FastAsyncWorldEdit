/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.command.composition;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.extent.FaweRegionExtent;
import com.boydti.fawe.util.MainUtil;

import com.google.common.base.Joiner;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.Lists;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.Contextual;
import com.sk89q.worldedit.function.EditContext;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.command.argument.CommandArgs;
import com.sk89q.worldedit.util.command.composition.CommandExecutor;
import com.sk89q.worldedit.util.command.composition.SimpleCommand;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.lang.reflect.Field;
import java.util.List;

public class SelectionCommand extends SimpleCommand<Operation> {

    private final CommandExecutor<Contextual<? extends Operation>> delegate;
    private final String permission;

    public SelectionCommand(CommandExecutor<Contextual<? extends Operation>> delegate, String permission) {
        checkNotNull(delegate, "delegate");
        checkNotNull(permission, "permission");
        this.delegate = delegate;
        this.permission = permission;
        addParameter(delegate);
    }

    @Override
    public Operation call(CommandArgs args, CommandLocals locals) throws CommandException {
        if (!testPermission(locals)) {
            throw new CommandPermissionsException();
        }

        Contextual<? extends Operation> operationFactory = delegate.call(args, locals);

        Actor actor = locals.get(Actor.class);
        if (actor instanceof Player) {
            try {
                Player player = (Player) actor;
                LocalSession session = WorldEdit.getInstance().getSessionManager().get(player);
                Region selection = session.getSelection(player.getWorld());

                EditSession editSession = session.createEditSession(player);
                editSession.enableStandardMode();
                locals.put(EditSession.class, editSession);
                session.tellVersion(player);

                EditContext editContext = new EditContext();
                editContext.setDestination(locals.get(EditSession.class));
                editContext.setRegion(selection);
                editContext.setSession(session);

                Operation operation = operationFactory.createFromContext(editContext);
                // Shortcut
                if (selection instanceof CuboidRegion && editSession.hasFastMode() && operation instanceof RegionVisitor) {
                    CuboidRegion cuboid = (CuboidRegion) selection;
                    RegionFunction function = ((RegionVisitor) operation).function;
                    RegionWrapper current = new RegionWrapper(cuboid.getMinimumPoint(), cuboid.getMaximumPoint());
                    FaweRegionExtent regionExtent = editSession.getRegionExtent();

                    if (function instanceof BlockReplace && regionExtent == null || regionExtent.isGlobal()) {
                        try {
                            BlockReplace replace = ((BlockReplace) function);
                            Field field = replace.getClass().getDeclaredField("pattern");
                            field.setAccessible(true);
                            Pattern pattern = (Pattern) field.get(replace);
                            if (pattern instanceof BlockStateHolder) {
                                BlockStateHolder block = ((BlockStateHolder) pattern);
                                final FaweQueue queue = editSession.getQueue();
                                final int minY = cuboid.getMinimumY();
                                final int maxY = cuboid.getMaximumY();

                                final FaweChunk<?> fc = queue.getFaweChunk(0, 0);
                                fc.fillCuboid(0, 15, minY, maxY, 0, 15, block.getInternalId());
                                fc.optimize();

                                // [chunkx, chunkz, pos1x, pos1z, pos2x, pos2z, isedge]
                                MainUtil.chunkTaskSync(current, new RunnableVal<int[]>() {
                                    @Override
                                    public void run(int[] value) {
                                        FaweChunk newChunk;
                                        if (value[6] == 0) {
                                            newChunk = fc.copy(true);
                                            newChunk.setLoc(queue, value[0], value[1]);
                                        } else {
                                            int bx = value[2] & 15;
                                            int tx = value[4] & 15;
                                            int bz = value[3] & 15;
                                            int tz = value[5] & 15;
                                            if (bx == 0 && tx == 15 && bz == 0 && tz == 15) {
                                                newChunk = fc.copy(true);
                                                newChunk.setLoc(queue, value[0], value[1]);
                                            } else {
                                                newChunk = queue.getFaweChunk(value[0], value[1]);
                                                newChunk.fillCuboid(value[2] & 15, value[4] & 15, minY, maxY, value[3] & 15, value[5] & 15, block.getInternalId());
                                            }
                                        }
                                        newChunk.addToQueue();
                                    }
                                });
                                queue.enqueue();
                                BBC.OPERATION.send(actor, BBC.VISITOR_BLOCK.format(cuboid.getArea()));
                                queue.flush();
                                return null;
                            }
                        } catch (Throwable e) {
                            MainUtil.handleError(e);
                        }
                    }
                }
                Operations.completeBlindly(operation);

                List<String> messages = Lists.newArrayList();
                operation.addStatusMessages(messages);
                if (messages.isEmpty()) {
                    BBC.OPERATION.send(actor, 0);
                } else {
                    BBC.OPERATION.send(actor, Joiner.on(", ").join(messages));
                }

                return operation;
            } catch (IncompleteRegionException e) {
                WorldEdit.getInstance().getPlatformManager().getCommandManager().getExceptionConverter().convert(e);
                return null;
            }
        } else {
            throw new CommandException("This command can only be used by players.");
        }
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    protected boolean testPermission0(CommandLocals locals) {
        return locals.get(Actor.class).hasPermission(permission);
    }

}
