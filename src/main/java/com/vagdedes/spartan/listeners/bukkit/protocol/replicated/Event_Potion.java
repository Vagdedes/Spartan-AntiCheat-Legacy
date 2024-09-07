package com.vagdedes.spartan.listeners.bukkit.protocol.replicated;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;

public class Event_Potion implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public static void event(EntityPotionEffectEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            Player n = (Player) entity;
            SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

            // Objects
            p.setStoredPotionEffects(n.getActivePotionEffects());
        }
    }

}
