package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.listeners.SharedEvents;
import com.vagdedes.spartan.listeners.protocol.modules.DeprecateTypes;
import com.vagdedes.spartan.listeners.protocol.modules.RotationData;
import com.vagdedes.spartan.utils.math.RayUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


public class Move extends PacketAdapter {

    public static Map<Player, Location> positionManager = new ConcurrentHashMap<>();
    public static Map<Player, Boolean> groundManager = new ConcurrentHashMap<>();
    public static Map<Player, Boolean> spawnStatus = new ConcurrentHashMap<>();
    public static Map<Player, RotationData> lastRotation = new ConcurrentHashMap<>();

    public Move() {
        super(Register.plugin, ListenerPriority.LOWEST, PacketType.Play.Client.getInstance().values());
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
            double xz = to.getX() + to.getZ();
            if ((xz == 17 || xz == 0 && spawnStatus.containsKey(player) && spawnStatus.get(player))
            ) {
                return;
            } else {
                spawnStatus.put(player, false);
            }
            if (Teleport.lastTeleport.containsKey(player)) {
                Teleport.lastTeleport.remove(player);
                return;
            }
            importRotation(player, to);
            positionManager.putIfAbsent(player, to);
            Location from = positionManager.get(player);
            importRotation(player, from);
            boolean ground = packet.getBooleans().read(0);
            groundManager.put(player, ground);
            PlayerMoveEvent playerMoveEvent = new PlayerMoveEvent(
                    player, from, to
            );
            if (Join.canCheck(player) && !spawnStatus.get(player)) {
                SharedEvents.move(playerMoveEvent);
            }
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
    public static void importRotation(Player player, Location l) {
        l.setYaw(lastRotation.get(player).yaw());
        l.setPitch(lastRotation.get(player).pitch());
    }

    public static boolean onGroundPacketLevel(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        return packet.getBooleans().read(0);
    }

    public static Location getLocation(SpartanPlayer spartanPlayer) {
        if (positionManager.containsKey(spartanPlayer.getPlayer())) {
            return positionManager.get(spartanPlayer.getPlayer());
        } else {
            return spartanPlayer.getPlayer().getLocation();
        }
    }

    public static Boolean isOnGround(SpartanPlayer spartanPlayer) {
        Player player = spartanPlayer.getPlayer();
        if (groundManager.containsKey(player)) {
            return groundManager.get(player);
        } else {
            return player.isOnGround();
        }
    }
    private static boolean rumiaShield(Player player, Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double y = RayUtils.scaleVal(to.getY() - from.getY(), 2);
        double speed = RayUtils.scaleVal(Math.sqrt(dx * dx + dz * dz), 2);
        return  y == -0.09;
    }
}