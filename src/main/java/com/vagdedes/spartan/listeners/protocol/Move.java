package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.listeners.protocol.modules.DeprecateTypes;
import com.vagdedes.spartan.listeners.protocol.modules.PlayerStayEvent;
import com.vagdedes.spartan.listeners.protocol.modules.RotationData;
import com.vagdedes.spartan.utils.math.RayUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


public class Move {

    public static Map<Player, Location> positionManager = new ConcurrentHashMap<>();
    public static Map<Player, Boolean> groundManager = new ConcurrentHashMap<>();
    public static Map<Player, RotationData> lastRotation = new ConcurrentHashMap<>();
    public static Map<Player, Boolean> toggleSpawn = new ConcurrentHashMap<>();


    public static void registerPacketListeners(ProtocolManager protocolManager) {
        PacketListener serverSideListener = new PacketAdapter(Register.plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.POSITION) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (positionManager.containsKey(event.getPlayer()) && !toggleSpawn.get(event.getPlayer())) {
                    positionManager.put(event.getPlayer(),
                                    positionManager.get(event.getPlayer()).add(readMovePacket(event)));
                }
            }
        };

        PacketListener clientSideListener = new PacketAdapter(Register.plugin, ListenerPriority.LOWEST, PacketType.Play.Client.getInstance().values()) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                moving(event);
            }
        };

        protocolManager.addPacketListener(serverSideListener);
        protocolManager.addPacketListener(clientSideListener);
    }
    public static void moving(PacketEvent event) {
        Player player = event.getPlayer();
        PacketContainer packet = event.getPacket();
        if (packet.getType() == PacketType.Play.Client.POSITION
                || packet.getType() == PacketType.Play.Client.POSITION_LOOK
                || packet.getType() == PacketType.Play.Client.LOOK) {
            Location to = readMovePacket(event);
            double xz = to.getX() + to.getZ();
            if (xz == 17) {
                if (toggleSpawn.get(player)) {
                    Move.positionManager.put(event.getPlayer(), new Location(event.getPlayer().getWorld(), 8.5, 0, 8.5));
                    return;
                }
            } else if (xz == 0) {
                if (toggleSpawn.get(player)) {
                    Move.positionManager.put(event.getPlayer(), new Location(event.getPlayer().getWorld(), 0, 0, 0));
                    return;
                }
            } else if (toggleSpawn.get(player)) {
                toggleSpawn.put(player, false);
                positionManager.put(player, to);
                return;
            }
            importRotation(player, to);
            Location from = positionManager.get(player);
            importRotation(player, from);
            boolean ground = packet.getBooleans().read(0);
            groundManager.put(player, ground);
            PlayerMoveEvent playerMoveEvent = new PlayerMoveEvent(
                    player, from, to
            );
            positionManager.put(player, to);
            Shared.move(playerMoveEvent);
            // Action
        } else if (Objects.equals(packet.getType().name(), DeprecateTypes.GROUND.name())
                || Objects.equals(packet.getType().name(), DeprecateTypes.FLYING.name())) {
            boolean ground = packet.getBooleans().read(0);
            groundManager.put(player, ground);
            Shared.stay(new PlayerStayEvent(ground, player));
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
        if (positionManager.containsKey(spartanPlayer.getInstance())) {
            Location l = positionManager.get(spartanPlayer.getInstance());
            if (lastRotation.containsKey(spartanPlayer.getInstance())) {
                l.setYaw(lastRotation.get(spartanPlayer.getInstance()).yaw());
                l.setPitch(lastRotation.get(spartanPlayer.getInstance()).pitch());
            }
            return l;
        } else {
            return spartanPlayer.getInstance().getLocation();
        }
    }
    public static Location getLocation(Player player) {
        if (positionManager.containsKey(player)) {
            Location l = positionManager.get(player);
            if (lastRotation.containsKey(player)) {
                l.setYaw(lastRotation.get(player).yaw());
                l.setPitch(lastRotation.get(player).pitch());
            }
            return l;
        } else {
            return player.getLocation();
        }
    }

    public static Boolean isOnGround(SpartanPlayer spartanPlayer) {
        Player player = spartanPlayer.getInstance();
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