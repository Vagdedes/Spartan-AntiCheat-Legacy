package me.vagdedes.spartan.checks.player;

import me.vagdedes.spartan.compatibility.manual.abilities.AureliumSkills;
import me.vagdedes.spartan.compatibility.manual.abilities.mcmmo.mcMMO;
import me.vagdedes.spartan.compatibility.manual.building.MineBomb;
import me.vagdedes.spartan.compatibility.manual.building.TreeFeller;
import me.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import me.vagdedes.spartan.compatibility.semi.QuickShop;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.data.Timer;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.HackPrevention;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class NoSwing {

    private static final int refreshTime = 30 * 20;
    private static final Enums.HackType check = Enums.HackType.NoSwing;

    // Detections

    public static void runAnimation(SpartanPlayer p, PlayerAnimationType animation) {
        if (!p.canRunCheck(check, true) || animation != PlayerAnimationType.ARM_SWING) {
            clearData(p);
        } else {
            p.getTimer(check).set("animation");
        }
    }

    public static void runDamage(SpartanPlayer p, LivingEntity entity) {
        if (!canDo(p) || !check.getCheck().getBooleanOption("check_damage", true) || p.hasAttackCooldown()
                || PlayerData.getEntitiesNumber(p, CombatUtils.maxHitDistance, false, 4) > 3
                || p.getHandlers().has(Handlers.HandlerType.Trident) || CombatUtils.isNewPvPMechanic(p, entity) || CombatUtils.hasBlockBehind(p, entity)) {
            clearData(p);
            return;
        }
        String key = "damage";
        Cooldowns cooldowns = p.getCooldowns(check);

        if (!cooldowns.canDo(key)) {
            return;
        }
        cooldowns.add(key, 20);

        if (!p.getTimer(check).has("animation")) {
            Buffer buffer = p.getBuffer(check);

            if (cooldowns.canDo("first")) {
                cooldowns.add("first", refreshTime);
                buffer.remove(key);
                return;
            }
            if (buffer.start(key, refreshTime) >= 2) {
                buffer.remove(key);
                new HackPrevention(p, check,
                        "type: damage"
                                + ", entity: " + CombatUtils.entityToString(entity)
                                + ", item: " + BlockUtils.materialToString(p.getItemInHand().getType()));
            }
        } else {
            clearData(p);
        }
    }

    public static void runBreak(SpartanPlayer p, SpartanBlock b) {
        if (QuickShop.isEnabled() || !canDo(p) || !check.getCheck().getBooleanOption("check_breaking", true)
                || p.hasPotionEffect(PotionEffectType.FAST_DIGGING) || BlockUtils.isSensitive(p, b.getLocation())
                || TreeFeller.canCancel(p, b) || MineBomb.canUse(p) || AureliumSkills.canCancel(p)) {
            clearData(p);
            return;
        }
        ItemStack itemStack = p.getItemInHand();

        if (itemStack.getEnchantmentLevel(Enchantment.DIG_SPEED) < 4) {
            Material material = itemStack.getType();

            if (!PlayerData.isAxeItem(material)
                    && !PlayerData.isPickaxeItem(material)
                    && !PlayerData.isSpadeItem(material)
                    && !BlockUtils.isSensitive(p, material)) {
                Timer timer = p.getTimer(check);

                if (timer.get("break") > 50L) {
                    if (!timer.has("animation")) {
                        new HackPrevention(p, check,
                                "type: break"
                                        + ", block: " + BlockUtils.blockToString(b)
                                        + ", item: " + BlockUtils.materialToString(material));
                    } else {
                        clearData(p);
                    }
                }
            } else {
                clearData(p);
            }
        } else {
            clearData(p);
        }
    }

    // Handlers

    private static void clearData(SpartanPlayer p) {
        p.getTimer(check).remove("animation");
    }

    public static void runItemChange(SpartanPlayer p) {
        check.getCheck().addDisabledUser(p.getUniqueId(), check.toString(), 10);
    }

    public static void runInteract(SpartanPlayer p, Action action) {
        if (action == Action.LEFT_CLICK_BLOCK && p.canRunCheck(check, false)) {
            p.getTimer(check).set("break");
        }
    }

    // Utils

    private static boolean canDo(SpartanPlayer p) {
        return p.canRunCheck(check, true)
                && !Vehicles.has(p, "drill")
                && !mcMMO.hasAbility(p);
    }
}
