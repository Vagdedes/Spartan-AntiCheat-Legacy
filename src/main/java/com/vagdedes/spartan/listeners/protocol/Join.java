package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.SharedEvents;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Join extends PacketAdapter implements Listener {

    public static Map<Player, Boolean> canCheck = new ConcurrentHashMap<>();

    public Join() {
        super(Register.plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.LOGIN);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        canCheck.put(player, false);
        PacketContainer packet = event.getPacket();

        if (packet.getType() == PacketType.Play.Server.LOGIN) {

        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Join(PlayerJoinEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        SharedEvents.join(p, e);
        canCheck.put(e.getPlayer(), true);
    }

    public static boolean canCheck(Player player) {
        return canCheck.get(player);
    }
}
