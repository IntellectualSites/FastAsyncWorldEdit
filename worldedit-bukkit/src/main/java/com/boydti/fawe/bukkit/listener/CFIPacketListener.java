package com.boydti.fawe.bukkit.listener;

import com.boydti.fawe.command.CFICommands;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal3;
import com.boydti.fawe.object.brush.visualization.VirtualWorld;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.platform.BlockInteractEvent;
import com.sk89q.worldedit.event.platform.Interaction;
import com.sk89q.worldedit.extension.platform.PlatformManager;
import com.sk89q.worldedit.math.BlockVector3;

import com.sk89q.worldedit.util.formatting.text.TextComponent.Builder;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.List;

import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

/**
 * The CFIPacketListener handles packets for editing the VirtualWorld
 * The generator is a virtual world which only the creator can see
 *  - The virtual world is displayed inside the current world
 *  - Block/Chunk/Movement packets need to be handled properly
 */
public class CFIPacketListener implements Listener {

    private final Plugin plugin;
    private final ProtocolManager protocolmanager;

    public CFIPacketListener(Plugin plugin) {
        this.plugin = plugin;
        this.protocolmanager = ProtocolLibrary.getProtocolManager();

        // TODO NOT IMPLEMENTED
//        // Direct digging to the virtual world
//        registerBlockEvent(PacketType.Play.Client.BLOCK_DIG, false, new RunnableVal3<PacketEvent, VirtualWorld, BlockVector3>() {
//            @Override
//            public void run(Builder event, URI gen, String pt) {
//                try {
//                    Player plr = event.getPlayer();
//                    BlockVector3 realPos = pt.add(gen.getOrigin().toBlockPoint());
//                    if (!sendBlockChange(plr, gen, pt, Interaction.HIT)) {
//                        gen.setBlock(pt, BlockTypes.AIR.getDefaultState());
//                    }
//                } catch (WorldEditException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//        // Direct placing to the virtual world
//        RunnableVal3<PacketEvent, VirtualWorld, BlockVector3> placeTask = new RunnableVal3<PacketEvent, VirtualWorld, BlockVector3>() {
//            @Override
//            public void run(Builder event, URI gen, String pt) {
//                try {
//                    Player plr = event.getPlayer();
//                    List<EnumWrappers.Hand> hands = event.getPacket().getHands().getValues();
//
//                    EnumWrappers.Hand enumHand = hands.isEmpty() ? EnumWrappers.Hand.MAIN_HAND : hands.get(0);
//                    PlayerInventory inv = plr.getInventory();
//                    ItemStack hand = enumHand == EnumWrappers.Hand.MAIN_HAND ? inv.getItemInMainHand() : inv.getItemInOffHand();
//                    if (hand.getType().isBlock()) {
//                        Material type = hand.getType();
//                        switch (type) {
//                            case AIR:
//                            case CAVE_AIR:
//                            case VOID_AIR:
//                                break;
//                            default: {
//                                BlockStateHolder block = BukkitAdapter.asBlockState(hand);
//                                if (block != null) {
//                                    gen.setBlock(pt, block);
//                                    return;
//                                }
//                            }
//                        }
//                    }
//                    pt = getRelPos(event, gen);
//                    sendBlockChange(plr, gen, pt, Interaction.OPEN);
//                } catch (WorldEditException e) {
//                    e.printStackTrace();
//                }
//            }
//        };
//        registerBlockEvent(PacketType.Play.Client.BLOCK_PLACE, true, placeTask);
//        registerBlockEvent(PacketType.Play.Client.USE_ITEM, true, placeTask);
//
//        // Cancel block change packets where the real world overlaps with the virtual one
//        registerBlockEvent(PacketType.Play.Server.BLOCK_CHANGE, false, new RunnableVal3<PacketEvent, VirtualWorld, BlockVector3>() {
//            @Override
//            public void run(Builder event, URI gen, String pt) {
//                // Do nothing
//            }
//        });
//
//        // Modify chunk packets where the real world overlaps with the virtual one
//        protocolmanager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.MAP_CHUNK) {
//            @Override
//            public void onPacketSending(PacketEvent event) {
//                if (!event.isServerPacket()) return;
//
//                VirtualWorld gen = getGenerator(event);
//                if (gen != null) {
//                    BlockVector3 origin = gen.getOrigin().toBlockPoint();
//                    PacketContainer packet = event.getPacket();
//                    StructureModifier<Integer> ints = packet.getIntegers();
//                    int cx = ints.read(0);
//                    int cz = ints.read(1);
//
//                    int ocx = origin.getBlockX() >> 4;
//                    int ocz = origin.getBlockZ() >> 4;
//
//                    if (gen.contains(BlockVector3.at((cx - ocx) << 4, 0, (cz - ocz) << 4))) {
//                        event.setCancelled(true);
//
//                        Player plr = event.getPlayer();
//
//                        FaweQueue queue = SetQueue.IMP.getNewQueue(plr.getWorld().getName(), true, false);
//
//                        FaweChunk toSend = gen.getSnapshot(cx - ocx, cz - ocz);
//                        toSend.setLoc(gen, cx, cz);
//                        queue.sendChunkUpdate(toSend, FawePlayer.wrap(plr));
//                    }
//                }
//            }
//        });

        // The following few listeners are to ignore block collisions where the virtual and real world overlap

        protocolmanager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_VELOCITY) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (!event.isServerPacket()) return;

                Player player = event.getPlayer();
                Location pos = player.getLocation();
                VirtualWorld gen = getGenerator(event);
                if (gen != null) {
                    BlockVector3 origin = gen.getOrigin().toBlockPoint();
                    BlockVector3 pt = BlockVector3.at(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());

                    StructureModifier<Integer> ints = event.getPacket().getIntegers();
                    int id = ints.read(0);
                    int mx = ints.read(1);
                    int my = ints.read(2);
                    int mz = ints.read(3);

                    if (gen.contains(pt.subtract(origin)) && mx == 0 && my == 0 && mz == 0) {
                        event.setCancelled(true);
                    }
                }
            }
        });

        protocolmanager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.POSITION) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (!event.isServerPacket()) return;

                Player player = event.getPlayer();
                Location pos = player.getLocation();
                VirtualWorld gen = getGenerator(event);
                if (gen != null) {
                    BlockVector3 origin = gen.getOrigin().toBlockPoint();
                    BlockVector3 from = BlockVector3.at(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());

                    PacketContainer packet = event.getPacket();
                    StructureModifier<Double> doubles = packet.getDoubles();
                    BlockVector3 to = BlockVector3.at(doubles.read(0), doubles.read(1), doubles.read(2));
                    if (gen.contains(to.subtract(origin)) && from.distanceSq(to) < 8) {
                        int id = packet.getIntegers().read(0);
                        PacketContainer reply = new PacketContainer(PacketType.Play.Client.TELEPORT_ACCEPT);
                        reply.getIntegers().write(0, id);
                        try {
                            protocolmanager.recieveClientPacket(player, reply);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                        event.setCancelled(true);
                    }
                }
            }
        });

        protocolmanager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (!event.isServerPacket()) return;

                VirtualWorld gen = getGenerator(event);
                if (gen != null) {
                    PacketContainer packet = event.getPacket();
                    ChunkCoordIntPair chunk = packet.getChunkCoordIntPairs().read(0);
                    BlockVector3 origin = gen.getOrigin().toBlockPoint();
                    int cx = chunk.getChunkX() - (origin.getBlockX() >> 4);
                    int cz = chunk.getChunkZ() - (origin.getBlockX() >> 4);
                    if (gen.contains(BlockVector3.at(cx << 4, 0, cz << 4))) {
                        event.setCancelled(true);
                    }
                }
            }
        });
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        VirtualWorld gen = getGenerator(player);
        if (gen != null) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to.getWorld().equals(from.getWorld()) && to.distanceSquared(from) < 8) {
                event.setTo(player.getLocation());
                event.setCancelled(true);
                player.setVelocity(player.getVelocity());
            }
        }
    }

    private boolean sendBlockChange(Player plr, VirtualWorld gen, BlockVector3 pt, Interaction action) {
        PlatformManager platform = WorldEdit.getInstance().getPlatformManager();
        com.sk89q.worldedit.entity.Player actor = FawePlayer.wrap(plr).getPlayer();
        com.sk89q.worldedit.util.Location location = new com.sk89q.worldedit.util.Location(actor.getWorld(), pt.toVector3());
        BlockInteractEvent toCall = new BlockInteractEvent(actor, location, action);
        platform.handleBlockInteract(toCall);
        if (toCall.isCancelled() || action == Interaction.OPEN) {
            BlockVector3 realPos = pt.add(gen.getOrigin().toBlockPoint());
            BlockStateHolder block = gen.getBlock(pt);
            sendBlockChange(plr, realPos, block);
            return true;
        }
        return false;
    }

    private void sendBlockChange(Player plr, BlockVector3 pt, BlockStateHolder block) {
        plr.sendBlockChange(new Location(plr.getWorld(), pt.getBlockX(), pt.getBlockY(), pt.getBlockZ()), BukkitAdapter.adapt(block));
    }

    private VirtualWorld getGenerator(PacketEvent event) {
        return getGenerator(event.getPlayer());
    }

    private VirtualWorld getGenerator(Player player) {
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        VirtualWorld vw = fp.getSession().getVirtualWorld();
        if (vw != null) return vw;
        CFICommands.CFISettings settings = fp.getMeta("CFISettings");
        if (settings != null && settings.hasGenerator() && settings.getGenerator().hasPacketViewer()) {
            return settings.getGenerator();
        }
        return null;
    }

    private BlockVector3 getRelPos(PacketEvent event, VirtualWorld generator) {
        PacketContainer packet = event.getPacket();
        StructureModifier<BlockPosition> position = packet.getBlockPositionModifier();
        BlockPosition loc = position.readSafely(0);
        if (loc == null) return null;
        BlockVector3 origin = generator.getOrigin().toBlockPoint();
        return BlockVector3.at(loc.getX() - origin.getBlockX(), loc.getY() - origin.getBlockY(), loc.getZ() - origin.getBlockZ());
    }

    private void handleBlockEvent(PacketEvent event, boolean relative, RunnableVal3<PacketEvent, VirtualWorld, BlockVector3> task) {
        VirtualWorld gen = getGenerator(event);
        if (gen != null) {
            BlockVector3 pt = getRelPos(event, gen);
            if (pt != null) {
                if (relative) pt = getRelative(event, pt);
                if (gen.contains(pt)) {
                    event.setCancelled(true);
                    task.run(event, gen, pt);
                }
            }
        }
    }

    private void registerBlockEvent(PacketType type, boolean relative, RunnableVal3<PacketEvent, VirtualWorld, BlockVector3> task) {
        protocolmanager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, type) {
            @Override
            public void onPacketReceiving(final PacketEvent event) {
                if (type.isClient() || event.isServerPacket()) handleBlockEvent(event, relative, task);
            }

            @Override
            public void onPacketSending(PacketEvent event) {
                onPacketReceiving(event);
            }
        });
    }

    private BlockVector3 getRelative(PacketEvent container, BlockVector3 pt) {
        PacketContainer packet = container.getPacket();
        StructureModifier<EnumWrappers.Direction> dirs = packet.getDirections();
        EnumWrappers.Direction dir = dirs.readSafely(0);
        if (dir == null) return pt;
        switch (dir.ordinal()) {
            case 0: return pt.add(0, -1, 0);
            case 1: return pt.add(0, 1, 0);
            case 2: return pt.add(0, 0, -1);
            case 3: return pt.add(0, 0, 1);
            case 4: return pt.add(-1, 0, 0);
            case 5: return pt.add(1, 0, 0);
            default: return pt;
        }
    }
}
