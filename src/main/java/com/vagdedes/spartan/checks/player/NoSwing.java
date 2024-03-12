package com.vagdedes.spartan.checks.player;

import com.frash23.smashhit.AsyncPreDamageEvent;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.data.Timer;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.AureliumSkills;
import com.vagdedes.spartan.compatibility.manual.abilities.mcmmo.mcMMO;
import com.vagdedes.spartan.compatibility.manual.building.MineBomb;
import com.vagdedes.spartan.compatibility.manual.building.TreeFeller;
import com.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import com.vagdedes.spartan.compatibility.semi.QuickShop;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class NoSwing extends CheckExecutor {

    private static final int refreshTime = 30 * 20;
    private static final Enums.HackType check = Enums.HackType.NoSwing;

    public NoSwing(SpartanPlayer player) {
        super(check, player);
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerInteractEvent) {
            PlayerInteractEvent e = (PlayerInteractEvent) object;

            if (e.getAction() == Action.LEFT_CLICK_BLOCK
                    && player.canRunCheck(hackType, false)) {
                player.getTimer(hackType).set("break");
            }
        } else if (object instanceof PlayerItemHeldEvent) {
            player.getViolations(hackType).addDisableCause(hackType.toString(), null, 10);
        } else if (object instanceof PlayerAnimationEvent) {
            if (!player.canRunCheck(hackType, true)
                    || ((PlayerAnimationEvent) object).getAnimationType() != PlayerAnimationType.ARM_SWING) {
                clearData();
            } else {
                player.getTimer(hackType).set("animation");
            }
        } else if (object instanceof BlockBreakEvent) {
            if (QuickShop.isEnabled()
                    || !canDo()
                    || !hackType.getCheck().getBooleanOption("check_breaking", true)
                    || player.hasPotionEffect(PotionEffectType.FAST_DIGGING)
                    || MineBomb.canUse(player) || AureliumSkills.canCancel(player)) {
                clearData();
                return false;
            }
            SpartanBlock block = new SpartanBlock(player, ((BlockBreakEvent) object).getBlock());

            if (!BlockUtils.isSensitive(player, block.getLocation())
                    && !TreeFeller.canCancel(player, block)) {
                ItemStack itemStack = player.getItemInHand();

                if (itemStack.getEnchantmentLevel(Enchantment.DIG_SPEED) < 4) {
                    Material material = itemStack.getType();

                    if (!PlayerData.isAxeItem(material)
                            && !PlayerData.isPickaxeItem(material)
                            && !PlayerData.isSpadeItem(material)
                            && !BlockUtils.isSensitive(player, material)) {
                        Timer timer = player.getTimer(hackType);

                        if (timer.get("break") > 50L) {
                            if (!timer.has("animation")) {
                                cancel(
                                        "type: break"
                                                + ", block: " + BlockUtils.blockToString(block)
                                                + ", item: " + BlockUtils.materialToString(material));
                            } else {
                                clearData();
                            }
                        }
                    } else {
                        clearData();
                    }
                } else {
                    clearData();
                }
            }
        } else if (object instanceof EntityDamageByEntityEvent) {
            runDamage((LivingEntity) ((EntityDamageByEntityEvent) object).getEntity());
        } else if (Compatibility.CompatibilityType.SmashHit.isFunctional()) {
            runDamage((LivingEntity) ((AsyncPreDamageEvent) object).getEntity());
        }
        return false;
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        return false;
    }

    private void runDamage(LivingEntity entity) {
        if (!canDo() || !hackType.getCheck().getBooleanOption("check_damage", true) || player.hasAttackCooldown()
                || player.getEnemiesNumber(CombatUtils.maxHitDistance, true) > 3
                || player.getHandlers().has(Handlers.HandlerType.Trident) || CombatUtils.isNewPvPMechanic(player, entity) || CombatUtils.hasBlockBehind(player, entity)) {
            clearData();
            return;
        }
        String key = "damage";
        Cooldowns cooldowns = player.getCooldowns(hackType);

        if (!cooldowns.canDo(key)) {
            return;
        }
        cooldowns.add(key, 20);

        if (!player.getTimer(hackType).has("animation")) {
            Buffer buffer = player.getBuffer(hackType);

            if (cooldowns.canDo("first")) {
                cooldowns.add("first", refreshTime);
                buffer.remove(key);
                return;
            }
            if (buffer.start(key, refreshTime) >= 2) {
                buffer.remove(key);
                cancel("type: damage"
                        + ", entity: " + CombatUtils.entityToString(entity)
                        + ", item: " + BlockUtils.materialToString(player.getItemInHand().getType()));
            }
        } else {
            clearData();
        }
    }

    private void clearData() {
        player.getTimer(hackType).remove("animation");
    }

    @Override
    public boolean canDo() {
        return player.canRunCheck(hackType, true)
                && !Vehicles.has(player, "drill")
                && !mcMMO.hasAbility(player);
    }
}
