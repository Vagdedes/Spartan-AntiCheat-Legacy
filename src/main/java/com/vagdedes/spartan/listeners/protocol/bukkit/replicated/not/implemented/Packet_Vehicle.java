package com.vagdedes.spartan.listeners.protocol.bukkit.replicated.not.implemented;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class Packet_Vehicle extends PacketAdapter {

    public Packet_Vehicle() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Client.STEER_VEHICLE,
                PacketType.Play.Server.ATTACH_ENTITY
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE) {
            Player player = event.getPlayer();
            boolean dismount = event.getPacket().getBooleans().read(1);  // The dismount flag

            if (dismount) {
                SpartanPlayer spartanPlayer = SpartanBukkit.getProtocol(player).spartanPlayer;
                spartanPlayer.trackers.add(Trackers.TrackerType.VEHICLE, "exit", 5);
            }
        }
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.ATTACH_ENTITY) {
            Entity entity = event.getPacket().getEntityModifier(event).read(1);
            if (entity instanceof Player) {
                SpartanPlayer spartanPlayer = SpartanBukkit.getProtocol((Player) entity).spartanPlayer;
                spartanPlayer.trackers.add(Trackers.TrackerType.VEHICLE, "enter", 5);
            }
        }
    }
}