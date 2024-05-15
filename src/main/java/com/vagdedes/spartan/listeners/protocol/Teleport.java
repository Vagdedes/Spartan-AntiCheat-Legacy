package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.listeners.omnibridge.MovementEvents;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Teleport extends PacketAdapter {

    public static Map<Player, Location> lastTeleport = new ConcurrentHashMap<>();

    public Teleport() {
        super(Register.plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.POSITION);
    }
    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        PlayerTeleportEvent playerTeleportEvent =
                        new PlayerTeleportEvent(player,
                                        Move.positionManager.get(player),
                                        Move.readMovePacket(event));
        MovementEvents.teleport(playerTeleportEvent);


    }
}