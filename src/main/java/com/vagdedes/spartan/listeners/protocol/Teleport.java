package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.listeners.SharedEvents;
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
        Location currentLocation = Move.positionManager.get(player);
        Location newLocation = Move.readMovePacket(event);

        // Check if the worlds are the same before adding the locations
        if (currentLocation != null && currentLocation.getWorld().equals(newLocation.getWorld())) {
            Location finalLocation = newLocation.add(currentLocation);
            PlayerTeleportEvent playerTeleportEvent = new PlayerTeleportEvent(player, currentLocation, finalLocation);
            lastTeleport.put(player, finalLocation);
            SharedEvents.teleport(playerTeleportEvent);
        }
    }

}