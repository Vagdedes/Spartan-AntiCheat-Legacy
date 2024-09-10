package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.Event_Teleport;
import org.bukkit.entity.Player;

public class Packet_ServerInput extends PacketAdapter {


    public Packet_ServerInput() {
        super(
                Register.plugin,
                ListenerPriority.NORMAL,
                PacketType.Play.Server.UPDATE_HEALTH
        );
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);

        if (protocol.spartanPlayer.bedrockPlayer) {
            return;
        }
        PacketContainer packet = event.getPacket();

        if (packet.getType().equals(PacketType.Play.Server.UPDATE_HEALTH)) {
            if (packet.getFloat().read(0) <= 0.0F) {
                Event_Teleport.death(event.getPlayer(), true);
            }
        }
    }

}
