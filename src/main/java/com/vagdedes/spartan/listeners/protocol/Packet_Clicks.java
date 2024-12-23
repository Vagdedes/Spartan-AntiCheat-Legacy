package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.utils.java.OverflowMap;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class Packet_Clicks extends PacketAdapter {

    private final Map<UUID, Boolean> diggingPlayers = new OverflowMap<>(
            new LinkedHashMap<>(),
            1_024
    );

    public Packet_Clicks() {
        super(Register.plugin, ListenerPriority.NORMAL, PacketType.Play.Client.ARM_ANIMATION, PacketType.Play.Client.BLOCK_DIG);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        UUID uuid = ProtocolLib.getUUID(player);

        if (event.getPacketType() == PacketType.Play.Client.ARM_ANIMATION) {
            if (!diggingPlayers.containsKey(uuid)) {
                handleLeftClick(player);
            }
        } else if (event.getPacketType() == PacketType.Play.Client.BLOCK_DIG) {
            int digType = event.getPacket().getPlayerDigTypes().readSafely(0).ordinal();
            if (digType == 0) {
                diggingPlayers.put(uuid, true);
            } else if (digType == 2) {
                diggingPlayers.remove(uuid);
            }
        }
    }

    private void handleLeftClick(Player player) {
        //player.sendMessage("Left-click detected without digging.");
    }
}
