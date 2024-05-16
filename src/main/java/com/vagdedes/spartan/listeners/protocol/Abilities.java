package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.listeners.protocol.modules.AbilitiesContainer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Abilities extends PacketAdapter {

    public static Map<Player, AbilitiesContainer> playerAbilitiesContainer = new ConcurrentHashMap<>();
    public Abilities() {
        super(Register.plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.ABILITIES);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
       // player.sendMessage(event.getPacket().getType().toString());
    }

}
