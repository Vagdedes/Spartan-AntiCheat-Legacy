package me.vagdedes.spartan.checks.combat.criticals;

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.profiling.PlayerProfile;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class CriticalsUtils {

    static final Map<UUID, List<CriticalsItem>> memory = new LinkedHashMap<>(Config.getMaxPlayers());

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

    public static void cache(UUID uuid, Material itemType, double damage) {
        itemType = manipulateItem(itemType);
        List<CriticalsItem> list = memory.get(uuid);

        if (list == null) {
            list = new ArrayList<>();
            list.add(new CriticalsUtils.CriticalsItem(itemType, damage));
            memory.put(uuid, list);
        } else {
            list.add(new CriticalsUtils.CriticalsItem(itemType, damage));
        }
    }

    static double getExpectedDamage(Material itemType) {
        if (memory.size() > 0) {
            itemType = manipulateItem(itemType);
            double collection = 0.0;
            int total = 0;
            Set<UUID> providers = new HashSet<>();

            for (Map.Entry<UUID, List<CriticalsUtils.CriticalsItem>> entry : memory.entrySet()) {
                UUID uuid = entry.getKey();
                SpartanPlayer player = SpartanBukkit.getPlayer(uuid);

                if (player != null && canCollectFrom(player)) {
                    for (CriticalsUtils.CriticalsItem object : entry.getValue()) {
                        if (object.material == itemType) {
                            collection += object.damage;
                            total++;
                            providers.add(uuid);
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
