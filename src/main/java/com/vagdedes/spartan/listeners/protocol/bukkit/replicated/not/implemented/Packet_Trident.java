package com.vagdedes.spartan.listeners.protocol.bukkit.replicated.not.implemented;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.TridentUse;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Packet_Trident extends PacketAdapter {

    public Packet_Trident() {
        super(
                Register.plugin,
                ListenerPriority.LOWEST,
                PacketType.Play.Client.USE_ITEM
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        EnumWrappers.Hand hand = event.getPacket().getHands().read(0);

        ItemStack item = player.getInventory().getItem(hand.ordinal());

        if (item != null && item.getType().equals(Material.TRIDENT)) {
            boolean inWater = player.isInWater();
            boolean inRain = player.getWorld().hasStorm() && player.getLocation().getBlockY() >= player.getWorld().getHighestBlockYAt(player.getLocation());

            if (inWater || inRain) {
                TridentUse.run(SpartanBukkit.getProtocol(player).spartanPlayer);
            }
        }
    }
}