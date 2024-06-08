package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.packets.Container;
import com.vagdedes.spartan.abstraction.player.packets.IPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

public class ServerPosition extends PacketAdapter {

    public ServerPosition() {
        super(Register.plugin, ListenerPriority.HIGHEST,
                        PacketType.Play.Server.POSITION, PacketType.Play.Server.RESPAWN);
    }
    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packetContainer = event.getPacket();
        Player player = event.getPlayer();
        IPlayer iPlayer = Container.getIPlayer(player);
        if (packetContainer.getType() == PacketType.Play.Server.POSITION) {
            Location teleportLocation = Move.readLocation(event);
            iPlayer.getLocation().add(teleportLocation);
            Shared.teleport(new PlayerTeleportEvent(player, iPlayer.getFrom(), iPlayer.getLocation()));
        }
        if (packetContainer.getType() == PacketType.Play.Server.RESPAWN) {
            iPlayer.setLocation(new Location(player.getWorld(), 0, 0, 0));
        }
    }

}
