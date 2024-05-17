package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.listeners.protocol.modules.AbilitiesContainer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Join extends PacketAdapter implements Listener {

    public static Map<Player, Boolean> canCheck = new ConcurrentHashMap<>();

    public Join() {
        super(Register.plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.LOGIN);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        if (packet.getType() == PacketType.Play.Server.LOGIN) {
            Player player = event.getPlayer();
            canCheck.put(player, false);
            Move.spawnStatus.put(player, true);
            EntityAction.playerAbilitiesContainer.put(player, new AbilitiesContainer(false, false, false));
        }
    }

    public static boolean canCheck(Player player) {
        return canCheck.get(player);
    }
}
