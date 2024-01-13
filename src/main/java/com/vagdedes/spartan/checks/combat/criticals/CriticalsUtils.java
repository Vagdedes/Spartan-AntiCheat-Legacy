package com.vagdedes.spartan.checks.combat.criticals;

import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.handlers.stability.ResearchEngine;
import com.vagdedes.spartan.objects.profiling.PlayerProfile;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class CriticalsUtils {

    static final Map<String, List<CriticalsItem>> memory = new LinkedHashMap<>(Config.getMaxPlayers());

    // Objects

    static class CriticalsItem {

        final Material material;
        final double damage;

        CriticalsItem(Material material, double damage) {
            this.material = material;
            this.damage = damage;
        }
    }

    // Cache

    static boolean canCollectFrom(SpartanPlayer player) {
        if (!player.isBedrockPlayer()) {
            PlayerProfile profile = player.getProfile();
            return !profile.isSuspectedOrHacker(Criticals.check)
                    && !profile.wasTesting();
        }
        return false;
    }

    static Material manipulateItem(Material itemType) {
        return !BlockUtils.isSolid(itemType) && !itemType.isEdible() ? itemType : Material.AIR;
    }

    static double calculateDamage(Enchantment enchantment, ItemStack itemStack) {
        int level = itemStack.getEnchantmentLevel(enchantment);

        if (level > 0) {
            if (enchantment == Enchantment.DAMAGE_ALL) {
                return 0.5 + (level * 0.5);
            } else if (enchantment == Enchantment.DAMAGE_UNDEAD) {
                return level * 2.5;
            }
        }
        return 0.0;
    }

    public static void cache(String name, Material itemType, double damage) {
        itemType = manipulateItem(itemType);
        List<CriticalsItem> list = memory.get(name);

        if (list == null) {
            list = new ArrayList<>();
            list.add(new CriticalsUtils.CriticalsItem(itemType, damage));
            memory.put(name, list);
        } else {
            list.add(new CriticalsUtils.CriticalsItem(itemType, damage));
        }
    }

    static double getExpectedDamage(Material itemType) {
        if (!memory.isEmpty()) {
            itemType = manipulateItem(itemType);
            double collection = 0.0;
            int total = 0;
            Set<String> providers = new HashSet<>();

            for (Map.Entry<String, List<CriticalsUtils.CriticalsItem>> entry : memory.entrySet()) {
                String name = entry.getKey();
                SpartanPlayer player = SpartanBukkit.getPlayer(name);

                if (player != null && canCollectFrom(player)) {
                    for (CriticalsUtils.CriticalsItem object : entry.getValue()) {
                        if (object.material == itemType) {
                            collection += object.damage;
                            total++;
                            providers.add(name);
                        }
                    }
                }
            }

            if (providers.size() >= ResearchEngine.profileRequirement) {
                return Math.round(collection / ((double) total));
            }
        }
        return 0.0;
    }
}
