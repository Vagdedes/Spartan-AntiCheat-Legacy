package me.vagdedes.spartan.checks.combat.criticals;

import me.vagdedes.spartan.features.configuration.AntiCheatLogs;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.handlers.identifiers.complex.predictable.BouncingBlocks;
import me.vagdedes.spartan.handlers.stability.Moderation;
import me.vagdedes.spartan.objects.data.Decimals;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPrevention;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.utils.gameplay.PatternUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Criticals {

    static final HackType check = HackType.Criticals;

    private static final double[][]
            web = new double[][]{
            {BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 1, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 2, BlockUtils.hitbox_max}
    };

    private static final double[] aboveBlock = new double[]{
            BlockUtils.hitbox_max, 2.0, BlockUtils.hitbox_max
    };

    // Handlers

    public static void clear() {
        CriticalsUtils.memory.clear();
    }

    public static void remove(SpartanPlayer p, boolean leave) {
        if (leave) {
            if (!CriticalsUtils.canCollectFrom(p)) {
                CriticalsUtils.memory.remove(p.getUniqueId());
            }
        } else {
            CriticalsUtils.memory.remove(p.getUniqueId());
        }
    }

    public static void run(SpartanPlayer p, SpartanLocation location, double damage, LivingEntity entity) {
        if (canDo(p, location)) {
            if (!p.hasAttackCooldown() && !p.isSprinting()) {
                float fallDistance = p.getFallDistance();
                checkPosition(p, location); // Always first
                checkMiniJump(p, location, fallDistance);
                checkLocation(p, location, fallDistance);
            }
            checkDamage(p, damage, entity);
        }
    }

    // Detections

    private static void checkDamage(SpartanPlayer player, double damage, LivingEntity entity) {
        if (check.getCheck().getBooleanOption("check_damage", true)
                && !player.isBedrockPlayer()
                && !player.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE)
                && !CombatUtils.isNewPvPMechanic(player, entity)) {
            double[] widthAndHeight = CombatUtils.getWidthAndHeight(entity);

            if (!CombatUtils.isEntityTall(widthAndHeight[1]) && !CombatUtils.isEntityWide(widthAndHeight[0])) { // Non-player like entities seem to be problematic
                ItemStack itemStack = player.getItemInHand();

                if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_11) || !itemStack.containsEnchantment(Enchantment.SWEEPING_EDGE)) {
                    Material itemType = itemStack.getType();
                    boolean attackCooldown = player.hasAttackCooldown(),
                            isEmptyItem = itemType == Material.AIR,
                            vanillaGround = player.isOnGround(),
                            customGround = player.isOnGroundCustom();
                    double expectedDamage = isEmptyItem ? 1 : CriticalsUtils.getExpectedDamage(itemType),
                            sharpnessDamage = CriticalsUtils.calculateDamage(Enchantment.DAMAGE_ALL, itemStack),
                            smiteDamage = CriticalsUtils.calculateDamage(Enchantment.DAMAGE_UNDEAD, itemStack);

                    // Calculation
                    if (!isEmptyItem) {
                        double damageDecimals = damage - Math.floor(damage);
                        itemType = CriticalsUtils.manipulateItem(itemType);

                        if (vanillaGround
                                && customGround
                                && CriticalsUtils.canCollectFrom(player)

                                && (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) ? !attackCooldown : // Collect data only when there is no attack cooldown
                                damageDecimals == 0.0 || damageDecimals == 0.5)) { // When method is not available, check the validity of the damage
                            double originalDamage = damage - sharpnessDamage - smiteDamage;
                            CriticalsUtils.cache(player.getName(), itemType, originalDamage);
                            AntiCheatLogs.silentLogInfo(player.getName() + Moderation.criticalHitMessage + BlockUtils.materialToString(itemType) + " and " + originalDamage + " damage");
                        }
                    }

                    // Detection
                    if (expectedDamage > 0.0 && player.getTicksOnAir() == 0) {
                        double additional = sharpnessDamage + smiteDamage;

                        if (((expectedDamage * 1.5) + additional - damage) == 0.0) {
                            Set<Map.Entry<Enchantment, Integer>> enchantments =
                                    !isEmptyItem && itemStack.hasItemMeta() && itemStack.getItemMeta().hasEnchants() ?
                                            itemStack.getEnchantments().entrySet() : new HashSet<>();
                            StringBuilder enchantmentsString = new StringBuilder();
                            boolean hasEnchantments = enchantments.size() > 0;

                            if (hasEnchantments) {
                                for (Map.Entry<Enchantment, Integer> enchantment : enchantments) {
                                    String name = enchantment.getKey().getName().toLowerCase().replace("_", "/");
                                    enchantmentsString.append(name).append("-").append(enchantment.getValue()).append(", ");
                                }
                                enchantmentsString = new StringBuilder(enchantmentsString.substring(0, enchantmentsString.length() - 2));
                            }
                            new HackPrevention(player, check, "type: damage, amount: " + damage
                                    + ", expected-amount: " + expectedDamage + ", additional-amount: " + additional
                                    + ", ground: " + (vanillaGround + "/" + customGround) + ", item: " + BlockUtils.materialToString(itemType)
                                    + ", old-air-ticks: " + player.getOldTicksOnAir() + ", enchantments: " + (!hasEnchantments ? "none" : "[" + enchantmentsString + "]"));
                        }
                    }
                }
            }
        }
    }

    private static void checkLocation(SpartanPlayer p, SpartanLocation location, float fallDistance) {
        if (check.getCheck().getBooleanOption("check_location", true)
                && !p.isOnGround() && fallDistance > 0.0f && location.getY() % 1.0 == 0.0
                && p.getBuffer(check).start("location=attempts", 14 * 11) >= 2) {
            new HackPrevention(p, check, "type: location, fall-distance: " + fallDistance);
        }
    }

    private static void checkMiniJump(SpartanPlayer p, SpartanLocation location, float fallDistance) {
        if (check.getCheck().getBooleanOption("check_mini_jump", true)
                && fallDistance > 0.0f && fallDistance <= 0.07f
                && !p.isSneaking() && p.getSneakingCount() == 0
                && isBlockFree(p, location, 1.0)

                && (PlayerData.isOnGround(p, location, 0, -fallDistance, 0)
                || PlayerData.isOnGround(p, location, 0, -(fallDistance + 0.1), 0))) {
            new HackPrevention(p, check, "type: mini-jump, fall-distance: " + fallDistance);
        }
    }

    private static void checkPosition(SpartanPlayer p, SpartanLocation location) {
        if (check.getCheck().getBooleanOption("check_position", true)
                && PlayerData.getPotionLevel(p, PotionEffectType.JUMP) < 250 // Error Jump Effect
                && isBlockFree(p, location, BlockUtils.hitbox_max)
                && !p.isOnGround()
                && !p.isOnGroundCustom()) {
            Decimals decimals = p.getDecimals(check);
            double box = location.getY() - location.getBlockY();

            if (p.getTicksOnAir() >= 20 && box >= 0.15 && box <= 0.75) {
                double oldBox = decimals.get("position");

                if (decimals.has(oldBox) && oldBox == box) {
                    new HackPrevention(p, check, "type: position, box: " + box);
                }
            }
            decimals.set("position", box);
        }
    }

    private static boolean isBlockFree(SpartanPlayer p, SpartanLocation loc, double box) {
        if (p.getCooldowns(check).canDo("block-free=" + box)) {
            boolean onGroundCustom = p.isOnGroundCustom();

            if (PatternUtils.isBlockPattern(new double[][]{
                    {box, 0, box},
                    {box, 1, box},
                    {box, 2, box}
            }, loc, true, BlockUtils.solid, BlockUtils.semi_solid)) {
                p.getCooldowns(check).add("block-free=" + box, onGroundCustom ? 40 : 60);
                return false;
            }
            return true;
        }
        return false;
    }

    private static boolean canDo(SpartanPlayer p, SpartanLocation location) {
        return p.canRunCheck(check, true)
                && !p.wasFlying()
                && !p.wasInLiquids()
                && !p.hasPotionEffect(PotionEffectType.BLINDNESS)
                && p.getBlocksOffGround(3, true, true) <= 2
                && !p.getHandlers().has(Handlers.HandlerType.Vehicle)
                && !BouncingBlocks.isBelow(p)
                && !PatternUtils.isBlockPattern(aboveBlock, location, true, BlockUtils.solid, BlockUtils.semi_solid)
                && !PatternUtils.isBlockPattern(web, location, true, BlockUtils.web);
    }
}
