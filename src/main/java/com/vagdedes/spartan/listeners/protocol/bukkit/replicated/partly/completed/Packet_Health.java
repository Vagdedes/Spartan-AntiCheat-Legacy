package com.vagdedes.spartan.listeners.protocol.bukkit.replicated.partly.completed;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.listeners.bukkit.protocol.replicated.Event_Health;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class Packet_Health extends PacketAdapter {

    public Packet_Health() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Server.ENTITY_STATUS,
                PacketType.Play.Server.UPDATE_HEALTH
        );
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Entity entity = event.getPacket().getEntityModifier(event).read(0);

        if (entity instanceof Player) {
            Player player = (Player) entity;

            if (event.getPacketType() == PacketType.Play.Server.ENTITY_STATUS) {
                FoodLevelChangeEvent bukkitEvent = new FoodLevelChangeEvent(
                        player,
                        0 // todo
                );
                bukkitEvent.setCancelled(event.isCancelled());
                Event_Health.food(bukkitEvent);
            } else if (event.getPacketType() == PacketType.Play.Server.UPDATE_HEALTH) {
                EntityRegainHealthEvent bukkitEvent = new EntityRegainHealthEvent(
                        entity,
                        0,
                        null // todo
                );
                bukkitEvent.setCancelled(event.isCancelled());
                Event_Health.regen(bukkitEvent);
            }
        }
    }

}