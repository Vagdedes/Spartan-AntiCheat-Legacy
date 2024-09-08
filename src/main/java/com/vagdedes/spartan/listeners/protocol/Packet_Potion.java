package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.Event_Potion;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPotionEffectEvent;

public class Packet_Potion extends PacketAdapter {

    public Packet_Potion() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Server.ENTITY_EFFECT,
                PacketType.Play.Server.REMOVE_ENTITY_EFFECT
        );
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Entity entity = event.getPacket().getEntityModifier(event).read(0);

        if (entity instanceof Player) {
            Player player = (Player) entity;
            SpartanProtocol protocol = SpartanBukkit.getProtocol(player);

            if (protocol.spartanPlayer.bedrockPlayer) {
                return;
            }
            EntityPotionEffectEvent bukkitEvent = new EntityPotionEffectEvent(
                    player,
                    null, // todo
                    null, // todo
                    null, // todo
                    null, // todo
                    true // todo
            );
            bukkitEvent.setCancelled(event.isCancelled());
            Event_Potion.event(bukkitEvent);
        }
    }
}
