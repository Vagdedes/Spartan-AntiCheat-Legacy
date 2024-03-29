package com.vagdedes.spartan.checks.combat.criticals;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Decimals;
import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.identifiers.complex.predictable.BouncingBlocks;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.gameplay.PatternUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Criticals extends CheckExecutor {

    static final HackType check = HackType.Criticals;

    public Criticals(SpartanPlayer player) {
        super(check, player);
    }

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

    public static void clear() {
        CriticalsUtils.memory.clear();
    }

    // Detections

    private void checkDamage(double damage, LivingEntity entity) {
        if (hackType.getCheck().getBooleanOption("check_damage", true)
                && !player.bedrockPlayer
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
                            CriticalsUtils.cache(player.name, itemType, originalDamage);
                            AntiCheatLogs.logInfo(player.name + CriticalsUtils.criticalHitMessage + BlockUtils.materialToString(itemType) + " and " + originalDamage + " damage", false);
                        }
                    }

                    // Detection
                    if (expectedDamage > 0.0 && player.movement.getTicksOnAir() == 0) {
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
                            cancel("type: damage, amount: " + damage
                                    + ", expected-amount: " + expectedDamage + ", additional-amount: " + additional
                                    + ", ground: " + (vanillaGround + "/" + customGround) + ", item: " + BlockUtils.materialToString(itemType)
                                    + ", old-air-ticks: " + player.movement.getOldTicksOnAir() + ", enchantments: " + (!hasEnchantments ? "none" : "[" + enchantmentsString + "]"));
                        }
                    }
                }
            }
        }
    }

    private void checkLocation() {
        if (hackType.getCheck().getBooleanOption("check_location", true)
                && !player.isOnGround()
                && player.movement.getLocation().getY() % 1.0 == 0.0) {
            float fallDistance = player.getFallDistance();

            if (fallDistance > 0.0f
                    && player.getBuffer(hackType).start("location=attempts", 14 * 11) >= 2) {
                cancel("type: location, fall-distance: " + fallDistance);
            }
        }
    }

    private void checkMiniJump() {
        if (hackType.getCheck().getBooleanOption("check_mini_jump", true)
                && !player.movement.isSneaking()
                && isBlockFree(1.0)) {
            float fallDistance = player.getFallDistance();

            if (fallDistance > 0.0f && fallDistance <= 0.07f) {
                SpartanLocation location = player.movement.getLocation();

                if (PlayerUtils.isOnGround(player, location, 0, -fallDistance, 0)
                        || PlayerUtils.isOnGround(player, location, 0, -(fallDistance + 0.1), 0)) {
                    cancel("type: mini-jump, fall-distance: " + fallDistance);
                }
            }
        }
    }

    private void checkPosition() {
        if (hackType.getCheck().getBooleanOption("check_position", true)
                && PlayerUtils.getPotionLevel(player, PotionEffectType.JUMP) < 250 // Error Jump Effect
                && isBlockFree(BlockUtils.hitbox_max)
                && !player.isOnGround()
                && !player.isOnGroundCustom()) {
            Decimals decimals = player.getDecimals(hackType);
            SpartanLocation location = player.movement.getLocation();
            double box = location.getY() - location.getBlockY();

            if (player.movement.getTicksOnAir() >= 20 && box >= 0.15 && box <= 0.75) {
                double oldBox = decimals.get("position");

                if (decimals.has(oldBox) && oldBox == box) {
                    cancel("type: position, box: " + box);
                }
            }
            decimals.set("position", box);
        }
    }

    private boolean isBlockFree(double box) {
        if (player.getCooldowns(hackType).canDo("block-free=" + box)) {
            boolean onGroundCustom = player.isOnGroundCustom();

            if (PatternUtils.isBlockPattern(new double[][]{
                    {box, 0, box},
                    {box, 1, box},
                    {box, 2, box}
            }, player.movement.getLocation(), true, BlockUtils.solid, BlockUtils.semi_solid)) {
                player.getCooldowns(hackType).add("block-free=" + box, onGroundCustom ? 40 : 60);
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean canDo() {
        if (player.canRunCheck(hackType, true)
                && !player.movement.wasFlying()
                && !player.movement.wasInLiquids()
                && !player.hasPotionEffect(PotionEffectType.BLINDNESS)
                && player.getBlocksOffGround(3) <= 2
                && !player.getHandlers().has(Handlers.HandlerType.Vehicle)
                && !BouncingBlocks.isBelow(player)) {
            SpartanLocation location = player.movement.getLocation();
            return !PatternUtils.isBlockPattern(aboveBlock, location, true, BlockUtils.solid, BlockUtils.semi_solid)
                    && !PatternUtils.isBlockPattern(web, location, true, BlockUtils.web);
        }
        return false;
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        if (!player.hasAttackCooldown()
                && !player.movement.isSprinting()) {
            checkPosition(); // Always first
            checkMiniJump();
            checkLocation();
        }
        return false;
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        if (object instanceof Boolean) {
            if ((boolean) object) {
                if (!CriticalsUtils.canCollectFrom(player)) {
                    CriticalsUtils.memory.remove(player.name);
                }
            } else {
                CriticalsUtils.memory.remove(player.name);
            }
        } else if (canDo()) {
            Object[] objects = (Object[]) object;
            runInternal(cancelled);
            checkDamage((double) objects[0], (LivingEntity) objects[1]);
        }
        return false;
    }
}
