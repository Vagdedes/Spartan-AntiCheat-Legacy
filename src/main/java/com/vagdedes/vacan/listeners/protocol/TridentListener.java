package com.vagdedes.vacan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.vacan.Register;
import com.vagdedes.vacan.abstraction.event.CPlayerRiptideEvent;
import com.vagdedes.vacan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.vacan.functionality.concurrent.CheckThread;
import com.vagdedes.vacan.functionality.server.PluginBase;
import com.vagdedes.vacan.listeners.bukkit.TridentEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class TridentListener extends PacketAdapter {

    public TridentListener() {
        super(
                Register.plugin,
                ListenerPriority.LOWEST,
                PacketType.Play.Client.USE_ITEM,
                PacketType.Play.Client.BLOCK_DIG
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (!event.isCancelled()) { // PlayerRiptideEvent does not implement cancellable
            Player player = event.getPlayer();
            PlayerProtocol protocol = PluginBase.getProtocol(player);
            if (protocol.isBedrockPlayer()) {
                return;
            }
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType().equals(Material.TRIDENT)) {
                double r = Math.toRadians(protocol.getLocation().getYaw());
                CheckThread.run(() -> TridentEvent.event(
                        new CPlayerRiptideEvent(
                                player,
                                item,
                                new Vector(-Math.sin(r), protocol.getLocation().getPitch() / 90, Math.cos(r))
                        ),
                        true
                ));
            }
        }
    }

}