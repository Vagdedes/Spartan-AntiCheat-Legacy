package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.*;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.packets.Container;
import com.vagdedes.spartan.abstraction.player.packets.IPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public class Move extends PacketAdapter {

    public Move() {
        super(Register.plugin, ListenerPriority.LOWEST,
                        PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK,
                        PacketType.Play.Client.LOOK, PacketType.Play.Client.FLYING, PacketType.Play.Client.GROUND);
    }
    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        IPlayer iPlayer = Container.getIPlayer(player);
        movePacket(event, iPlayer);

        if (!invalidTeleport(iPlayer.getLocation()) && iPlayer.isOnLoadStatus()) {
            iPlayer.setOnLoadStatus(false);
        } else if (!invalidTeleport(iPlayer.getLocation())
                        || (invalidTeleport(iPlayer.getLocation()) && !iPlayer.isOnLoadStatus())) {
            Shared.move(new PlayerMoveEvent(player, iPlayer.getFrom(), iPlayer.getLocation()));
        }

    }
    private static void movePacket(PacketEvent event, IPlayer iPlayer) {
        PacketContainer packet = event.getPacket();
        PacketType type = event.getPacket().getType();
        Player player = event.getPlayer();

        iPlayer.setFrom(iPlayer.getLocation());

        if (type == PacketType.Play.Client.LOOK) {
            Location location = iPlayer.getLocation();
            location.setYaw(packet.getFloat().read(0));
            location.setPitch(packet.getFloat().read(1));
            iPlayer.setLocation(location);
        } else if (type == PacketType.Play.Client.POSITION_LOOK) {
            Location location = readLocation(event);
            location.setYaw(packet.getFloat().read(0));
            location.setPitch(packet.getFloat().read(1));
            iPlayer.setLocation(location);
        } else if (type == PacketType.Play.Client.POSITION) {
            Location location = readLocation(event);
            Location iPlayerLocation = iPlayer.getLocation();
            location.setYaw(iPlayerLocation.getYaw());
            location.setPitch(iPlayerLocation.getPitch());
            iPlayer.setLocation(location);
        }
        iPlayer.setOnGround(onGroundPacketLevel(event));
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