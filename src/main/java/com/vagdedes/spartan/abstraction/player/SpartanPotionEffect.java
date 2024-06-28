package com.vagdedes.spartan.abstraction.player;

import com.vagdedes.spartan.functionality.server.TPS;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SpartanPotionEffect {

    static Map<PotionEffectType, SpartanPotionEffect> mapFromBukkit(Map<PotionEffectType, SpartanPotionEffect> map,
                                                                    SpartanPlayer player,
                                                                    Collection<PotionEffect> collection) {
        for (PotionEffect effect : collection) {
            map.put(effect.getType(), new SpartanPotionEffect(player, effect));
        }
        return map;
    }

    static List<SpartanPotionEffect> listFromBukkit(List<SpartanPotionEffect> list,
                                                    SpartanPlayer player,
                                                    Collection<PotionEffect> collection) {
        for (PotionEffect effect : collection) {
            list.add(new SpartanPotionEffect(player, effect));
        }
        return list;
    }

    // Separator

    private final SpartanPlayer parent;
    public final PotionEffect bukkitEffect;
    private final long expiration;

    public SpartanPotionEffect(SpartanPlayer player, PotionEffect effect) {
        this.parent = player;
        this.bukkitEffect = effect;
        this.expiration = TPS.getTick(player) + effect.getDuration();
    }

    public long ticksPassed() {
        long remaining = this.expiration - TPS.getTick(this.parent);
        return remaining >= 0 ? 0 : Math.abs(remaining);
    }

    public boolean isActive() {
        return this.expiration - TPS.getTick(this.parent) >= 0;
    }
}
