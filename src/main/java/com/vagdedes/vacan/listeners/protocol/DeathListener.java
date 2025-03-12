package com.vagdedes.vacan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.vacan.Register;
import com.vagdedes.vacan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.vacan.functionality.server.PluginBase;
import com.vagdedes.vacan.listeners.bukkit.DeathEvent;

public class DeathListener extends PacketAdapter {

    public static final PacketType[] packetTypes = new PacketType[]{
            PacketType.Play.Server.UPDATE_HEALTH
    };

    public DeathListener() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                packetTypes
        );
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PlayerProtocol protocol = PluginBase.getProtocol(event.getPlayer());

        if (protocol.isBedrockPlayer()) {
            return;
        }
        PacketContainer packet = event.getPacket();

        if (packet.getType().equals(PacketType.Play.Server.UPDATE_HEALTH)
                && packet.getFloat().read(0) <= 0.0F) {
            DeathEvent.event(event.getPlayer(), true, event);
            protocol.useItemPacket = false;
        }
    }

}
