package com.vagdedes.spartan.abstraction.replicates;

import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Boss;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public class SpartanPlayerDamage {

    private final SpartanPlayer parent;
    public final long tick;
    public final EntityDamageEvent event;
    public final SpartanLocation location;
    private final ItemStack activeItem;
    public final boolean boss;

    SpartanPlayerDamage(SpartanPlayer player,
                        EntityDamageEvent event,
                        SpartanLocation location,
                        long tick) {
        this.parent = player;
        this.tick = tick;
        this.event = event;
        this.location = location;

        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent actualEvent = (EntityDamageByEntityEvent) this.event;

            if (actualEvent.getDamager() instanceof LivingEntity) {
                this.activeItem = ((LivingEntity) actualEvent.getDamager()).getActiveItem();
                this.boss = actualEvent.getDamager() instanceof Boss;
            } else if (actualEvent.getDamager() instanceof Projectile) {
                Projectile projectile = (Projectile) actualEvent.getDamager();

                if (projectile.getShooter() instanceof LivingEntity) {
                    LivingEntity shooter = (LivingEntity) projectile.getShooter();
                    this.activeItem = shooter.getActiveItem();
                    this.boss = shooter instanceof Boss;
                } else {
                    this.activeItem = null;
                    this.boss = false;
                }
            } else {
                this.activeItem = null;
                this.boss = false;
            }
        } else {
            this.activeItem = null;
            this.boss = false;
        }

        if (this.activeItem != null) {
            int level = this.activeItem.getEnchantmentLevel(Enchantment.KNOCKBACK);

            if (level > 2) {
                this.parent.getHandlers().add(
                        Handlers.HandlerType.Velocity,
                        AlgebraUtils.integerRound(Math.log(level))
                );
            }
        }
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

    public ItemStack getActiveItem() {
        return this.activeItem == null
                ? new ItemStack(Material.AIR)
                : this.activeItem;
    }

    public boolean isPropellingProjectile() { // todo cover fishing rods
        if (this.ticksPassed() <= TPS.maximum
                && this.parent.movement.getTicksOnAir() <= TPS.maximum
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

    public boolean hasBigKnockBack() {
        int time = (int) TPS.maximum * 2;
        return this.ticksPassed() <= time
                && this.parent.movement.getTicksOnAir() <= time
                && (this.boss
                || this.activeItem != null
                && this.activeItem.containsEnchantment(Enchantment.KNOCKBACK));
    }

    public boolean isSignificant(long ticksPassed) {
        switch (this.event.getCause()) {
            case ENTITY_ATTACK:
            case PROJECTILE:
                return this.ticksPassed() <= ticksPassed;
            default:
                return false;
        }
    }

    public boolean isSubtle(long ticksPassed) {
        if (this.parent.getHandlers().has(Handlers.HandlerType.Floor)) {
            return true;
        } else {
            EntityDamageEvent.DamageCause cause = this.event.getCause();

            switch (cause) {
                case STARVATION:
                case LIGHTNING:
                case THORNS:
                case VOID:
                case POISON:
                case WITHER:
                case CONTACT:
                case FALL:
                    return this.ticksPassed() <= ticksPassed
                            && this.parent.movement.getTicksOnAir() <= TPS.maximum;
                default:
                    return cause == EntityDamageEvent.DamageCause.SUFFOCATION
                            && !this.parent.isOutsideOfTheBorder(0.0)
                            || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)
                            && cause == EntityDamageEvent.DamageCause.WORLD_BORDER
                            || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_10)
                            && cause == EntityDamageEvent.DamageCause.HOT_FLOOR
                            || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_11)
                            && cause == EntityDamageEvent.DamageCause.MAGIC;
            }
        }
    }

    public boolean isExplosion(long ticksPassed) {
        switch (this.event.getCause()) {
            case ENTITY_EXPLOSION:
            case BLOCK_EXPLOSION:
                return this.ticksPassed() <= ticksPassed;
            default:
                return false;
        }
    }
}
