package com.boydti.fawe.bukkit.adapter.mc1_14.test;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class TestChunkPacketSend {
    public TestChunkPacketSend(Plugin plugin) {
        // Disable all sound effects
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(
                new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.MAP_CHUNK) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        if (event.getPacketType() != PacketType.Play.Server.MAP_CHUNK) {
                            return;
                        }
                        PacketContainer packet = event.getPacket();
                        StructureModifier<Byte> bytes = packet.getBytes();
                        StructureModifier<WrappedBlockData> blockData = packet.getBlockData();
                        List<WrappedBlockData> values = blockData.getValues();
                    }
                });
    }
}
