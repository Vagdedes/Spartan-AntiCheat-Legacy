package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.omnibridge.MovementEvents;
import com.vagdedes.spartan.listeners.protocol.modules.DeprecateTypes;
import com.vagdedes.spartan.listeners.protocol.modules.RotationData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


public class Move extends PacketAdapter {

    public static Map<Player, Location> positionManager = new ConcurrentHashMap<>();
    public static Map<Player, Boolean> groundManager = new ConcurrentHashMap<>();
    public static Map<Player, RotationData> lastRotation = new ConcurrentHashMap<>();
    public Move() {
        super(Register.plugin, ListenerPriority.LOWEST, PacketType.Play.Client.getInstance());
    }
    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PacketContainer packet = event.getPacket();
        if (packet.getType() == PacketType.Play.Client.POSITION
                        || packet.getType() == PacketType.Play.Client.POSITION_LOOK
                        || packet.getType() == PacketType.Play.Client.LOOK) {
            // CPlayerPacket
            Location to = readMovePacket(event);
            positionManager.putIfAbsent(player, to);
            boolean ground = packet.getBooleans().read(0);
            groundManager.put(player, ground);
            Location from = positionManager.get(player);
            PlayerMoveEvent playerMoveEvent = new PlayerMoveEvent(
                            player, from, to
            );
            if (Join.canCheck(player)) { MovementEvents.Move(playerMoveEvent); };
            positionManager.put(player, to);
            // Action
        } else if (Objects.equals(packet.getType().name(), DeprecateTypes.GROUND.name())
                        || Objects.equals(packet.getType().name(), DeprecateTypes.FLYING.name())) {
            boolean ground = packet.getBooleans().read(0);
        }

    }
    public static Location readMovePacket(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        Player player = event.getPlayer();
        if (event.getPacket().getType() == PacketType.Play.Client.LOOK
                        || event.getPacket().getType() == PacketType.Play.Client.POSITION_LOOK) {
            lastRotation.put(event.getPlayer(), new RotationData(
                            packet.getFloat().read(0), packet.getFloat().read(1))
            );
        }
        if (event.getPacket().getType() == PacketType.Play.Client.LOOK) {
            return positionManager.get(player);
        } else {
            return new Location(
                            player.getWorld(),
                            packet.getDoubles().read(0),
                            packet.getDoubles().read(1),
                            packet.getDoubles().read(2)
            );
        }
    }
    public static boolean onGroundPacketLevel(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        return packet.getBooleans().read(0);
    }
    public static SpartanLocation getLocation(SpartanPlayer spartanPlayer) {
        Player player = spartanPlayer.getPlayer();
        SpartanLocation location = new SpartanLocation(spartanPlayer, positionManager.get(player));
        location.setYaw(lastRotation.get(player).yaw());
        location.setPitch(lastRotation.get(player).pitch());
        return location;
    }
    public static Boolean isOnGround(SpartanPlayer spartanPlayer) {
        Player player = spartanPlayer.getPlayer();
        return groundManager.get(player);
    }
}