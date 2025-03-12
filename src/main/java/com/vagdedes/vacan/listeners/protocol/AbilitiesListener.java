package com.vagdedes.vacan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.vacan.Register;
import com.vagdedes.vacan.functionality.server.PluginBase;
import org.bukkit.entity.Player;

public class AbilitiesListener extends PacketAdapter {

    public AbilitiesListener() {
        super(Register.plugin, ListenerPriority.NORMAL,
                PacketType.Play.Client.ABILITIES);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        if (player.getAllowFlight()) {
            PluginBase.getProtocol(player).flyingTicks = 2;
        }
    }

}