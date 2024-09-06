package com.vagdedes.spartan.listeners.protocol.bukkit.replicated.partly.completed;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.standalone.Packet_LagCompensation;
import org.bukkit.entity.Player;

public class Packet_Status extends PacketAdapter {

    public Packet_Status() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Server.LOGIN
        );
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();

        if (ProtocolLib.isTemporary(player)) {
            return;
        }
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);

        if (protocol.spartanPlayer.bedrockPlayer) {
            return;
        }
        Packet_LagCompensation.newPacket(player.getEntityId());
    }

}