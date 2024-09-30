package com.vagdedes.spartan.abstraction.player;

import com.vagdedes.spartan.functionality.server.TPS;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SpartanPotionEffect {

    public final PotionEffect bukkitEffect;
    private final long expiration;

    public SpartanPotionEffect(PotionEffect effect) {
        this.bukkitEffect = effect;
        this.expiration = System.currentTimeMillis() + (effect.getDuration() * TPS.tickTime);
    }

    public long timePassed() {
        long remaining = this.expiration - System.currentTimeMillis();
        return remaining >= 0 ? 0 : Math.abs(remaining);
    }

    public boolean isActive() {
        return this.expiration - System.currentTimeMillis() >= 0;
    }
}
