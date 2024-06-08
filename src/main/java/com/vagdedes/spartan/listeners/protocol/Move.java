package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public class Move extends PacketAdapter {

    public Move() {
        super(
                Register.plugin,
                ListenerPriority.LOWEST,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.LOOK,
                PacketType.Play.Client.FLYING,
                PacketType.Play.Client.GROUND
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);
        movePacket(event, protocol);

        if (!invalidTeleport(protocol.getLocation()) && protocol.isOnLoadStatus()) {
            protocol.setOnLoadStatus(false);
        } else if (!invalidTeleport(protocol.getLocation())
                || (invalidTeleport(protocol.getLocation()) && !protocol.isOnLoadStatus())) {
            Shared.move(new PlayerMoveEvent(player, protocol.getFrom(), protocol.getLocation()));
        }
    }

    private static void movePacket(PacketEvent event, SpartanProtocol protocol) {
        PacketContainer packet = event.getPacket();
        PacketType type = event.getPacket().getType();
        protocol.setFrom(protocol.getLocation());

        if (type == PacketType.Play.Client.LOOK) {
            Location location = protocol.getLocation();
            location.setYaw(packet.getFloat().read(0));
            location.setPitch(packet.getFloat().read(1));
            protocol.setLocation(location);
        } else if (type == PacketType.Play.Client.POSITION_LOOK) {
            Location location = readLocation(event);
            location.setYaw(packet.getFloat().read(0));
            location.setPitch(packet.getFloat().read(1));
            protocol.setLocation(location);
        } else if (type == PacketType.Play.Client.POSITION) {
            Location location = readLocation(event);
            Location iPlayerLocation = protocol.getLocation();
            location.setYaw(iPlayerLocation.getYaw());
            location.setPitch(iPlayerLocation.getPitch());
            protocol.setLocation(location);
        }
        protocol.setOnGround(onGroundPacketLevel(event));
    }

    public static Location readLocation(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        return new Location(
                event.getPlayer().getWorld(),
                packet.getDoubles().read(0),
                packet.getDoubles().read(1),
                packet.getDoubles().read(2)
        );
    }

    public static boolean onGroundPacketLevel(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        return packet.getBooleans().read(0);
    }

    private static boolean invalidTeleport(Location location) {
        return location.getX() + location.getZ() == 17;
    }

}