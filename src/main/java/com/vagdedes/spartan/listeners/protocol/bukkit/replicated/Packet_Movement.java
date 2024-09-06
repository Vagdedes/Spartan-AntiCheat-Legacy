package com.vagdedes.spartan.listeners.protocol.bukkit.replicated;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.ReplicatedHandlers;
import com.vagdedes.spartan.utils.minecraft.protocol.ProtocolTools;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public class Packet_Movement extends PacketAdapter {

    public Packet_Movement() {
        super(
                Register.plugin,
                ListenerPriority.LOWEST,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.LOOK,
                PacketType.Play.Client.GROUND
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        long time = System.currentTimeMillis();
        SpartanProtocol protocol = SpartanBukkit.getProtocol(event.getPlayer());

        if (protocol.spartanPlayer.bedrockPlayer) {
            return;
        }
        if (System.currentTimeMillis() - protocol.mcClient.lastPacketTime > 4) {
            protocol.mcClient.verifiedPacket++;
        }
        protocol.mcClient.lastPacketTime = time;
        movePacket(event, protocol);
    }

    private static void movePacket(PacketEvent event, SpartanProtocol protocol) {
        PacketContainer packet = event.getPacket();
        PacketType type = event.getPacket().getType();
        protocol.setOnGround(ProtocolTools.onGroundPacketLevel(event));

        /*if (!protocol.teleportEngine.isEmpty()) {
            for (TeleportData teleportData : protocol.teleportEngine) {
                Location teleportLocation = protocol.getLocation().clone().add(teleportData.getLocation());
                Location teleportSilentLocation = teleportData.getLocation();
                int hl = protocol.getLocation().toVector().hashCode();
                int ht = teleportLocation.toVector().hashCode();
                int hSt = teleportSilentLocation.toVector().hashCode();
                if (hl == ht) {
                    protocol.setLocation(teleportLocation);
                    protocol.teleportEngine.remove(teleportData);
                } else if (hl == hSt) {
                    protocol.setLocation(teleportSilentLocation);
                    protocol.teleportEngine.remove(teleportData);
                }
            }
        }*/

        if (type.equals(PacketType.Play.Client.LOOK)) {
            Player player = event.getPlayer();
            Location from = protocol.getLocation().clone();
            protocol.getLocation().setYaw(packet.getFloat().read(0));
            protocol.getLocation().setPitch(packet.getFloat().read(1));
            protocol.getLocation().setWorld(player.getWorld());

            if (!from.getWorld().equals(protocol.getLocation().getWorld())
                    || ProtocolTools.invalidTeleport(protocol.getLocation())) {
                protocol.spartanPlayer.resetTrackers();
            } else if (!protocol.loaded) {
                protocol.loaded = true;
            } else {
                ReplicatedHandlers.movement(
                        new PlayerMoveEvent(
                                player,
                                from,
                                protocol.getLocation()
                        ),
                        true
                );
            }
        } else if (type.equals(PacketType.Play.Client.POSITION)
                || type.equals(PacketType.Play.Client.POSITION_LOOK)) {
            Player player = event.getPlayer();
            Location from = protocol.getLocation().clone(),
                    location = ProtocolTools.readLocation(event);
            protocol.getLocation().setX(location.getX());
            protocol.getLocation().setY(location.getY());
            protocol.getLocation().setZ(location.getZ());
            protocol.getLocation().setWorld(player.getWorld());

            if (type.equals(PacketType.Play.Client.POSITION_LOOK)) {
                protocol.getLocation().setYaw(packet.getFloat().read(0));
                protocol.getLocation().setPitch(packet.getFloat().read(1));
            }
            if (!from.getWorld().equals(location.getWorld())
                    || ProtocolTools.invalidTeleport(location)) {
                protocol.spartanPlayer.resetTrackers();
            } else if (!protocol.loaded) {
                protocol.loaded = true;
            } else {
                ReplicatedHandlers.movement(
                        new PlayerMoveEvent(
                                player,
                                from,
                                protocol.getLocation()
                        ),
                        true
                );
            }
        }
    }

}