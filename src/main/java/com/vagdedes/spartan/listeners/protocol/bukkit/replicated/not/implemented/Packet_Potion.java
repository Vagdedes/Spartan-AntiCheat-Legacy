package com.vagdedes.spartan.listeners.protocol.bukkit.replicated.not.implemented;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;

public class Packet_Potion extends PacketAdapter {

    public Packet_Potion() {
        super(Register.plugin, ListenerPriority.HIGHEST,
                        PacketType.Play.Server.ENTITY_EFFECT,
                        PacketType.Play.Server.REMOVE_ENTITY_EFFECT);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Entity entity = event.getPacket().getEntityModifier(event).read(0);

        if (entity instanceof Player) {
            Player player = (Player) entity;
            SpartanPlayer spartanPlayer = SpartanBukkit.getProtocol(player).spartanPlayer;

            Collection<PotionEffect> activeEffects = player.getActivePotionEffects();
            spartanPlayer.setStoredPotionEffects(activeEffects);
        }
    }
}
