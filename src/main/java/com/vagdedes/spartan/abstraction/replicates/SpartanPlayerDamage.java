package com.vagdedes.spartan.abstraction.replicates;

import com.vagdedes.spartan.functionality.server.TPS;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class SpartanPlayerDamage {

    private final SpartanPlayer parent;
    public final long tick;
    public final EntityDamageEvent event;
    public final SpartanLocation location;

    SpartanPlayerDamage(SpartanPlayer player,
                        EntityDamageEvent event,
                        SpartanLocation location,
                        long tick) {
        this.parent = player;
        this.tick = tick;
        this.event = event;
        this.location = location;
    }

    SpartanPlayerDamage(SpartanPlayer player,
                        EntityDamageEvent event,
                        SpartanLocation location) {
        this(player, event, location, TPS.getTick(player));
    }

    public EntityDamageByEntityEvent getEntityDamageByEntityEvent() {
        return event instanceof EntityDamageByEntityEvent
                ? (EntityDamageByEntityEvent) this.event
                : null;
    }

    public long ticksPassed() {
        return TPS.getTick(this.parent) - tick;
    }

    public boolean isPropellingProjectile() { // todo cover fishing rods
        if (this.ticksPassed() <= TPS.maximum
                && this.location.getPitch() <= -60.0f
                && this.event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) this.event;

            if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                Player p = this.parent.getPlayer();
                return p != null && event.getDamager().equals(p);
            }
        }
        return false;
    }
}
