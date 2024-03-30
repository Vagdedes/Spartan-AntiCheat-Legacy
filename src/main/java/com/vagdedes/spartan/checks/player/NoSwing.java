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
import com.vagdedes.spartan.compatibility.manual.abilities.mcMMO;
import com.vagdedes.spartan.compatibility.manual.building.MineBomb;
import com.vagdedes.spartan.compatibility.manual.building.TreeFeller;
import com.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;

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
        } else if (object instanceof PlayerAnimationEvent) {
            player.getTimer(hackType).set("animation");
        } else if (object instanceof BlockBreakEvent) {
            if (!canDo()
                    || !hackType.getCheck().getBooleanOption("check_breaking", true)
                    || MineBomb.canUse(player)
                    || AureliumSkills.canCancel(player)) {
                return false;
            }
            SpartanBlock block = new SpartanBlock(player, ((BlockBreakEvent) object).getBlock());

            if (!BlockUtils.isSensitive(player, block.getLocation())
                    && !TreeFeller.canCancel(player, block)) {
                Timer timer = player.getTimer(hackType);

                if (timer.get("break") > TPS.tickTime) {
                    if (!timer.has("animation")) {
                        cancel(
                                "type: break"
                                        + ", block: " + BlockUtils.blockToString(block)
                                        + ", item: " + BlockUtils.materialToString(player.getItemInHand().getType()));
                    } else {
                        clearData();
                    }
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
        if (!canDo()
                || !hackType.getCheck().getBooleanOption("check_damage", true)
                || player.hasAttackCooldown()
                || player.getHandlers().has(Handlers.HandlerType.Trident)
                || CombatUtils.isNewPvPMechanic(player, entity)) {
            return;
        }
        Cooldowns cooldowns = player.getCooldowns(hackType);

        if (cooldowns.canDo("damage")) {
            cooldowns.add("damage", 20);

            if (!player.getTimer(hackType).has("animation")) {
                Buffer buffer = player.getBuffer(hackType);

                if (buffer.start("damage", refreshTime) >= 3) {
                    buffer.remove("damage");
                    cancel("type: damage"
                            + ", entity: " + CombatUtils.entityToString(entity)
                            + ", item: " + BlockUtils.materialToString(player.getItemInHand().getType()));
                }
            } else {
                clearData();
            }
        }
    }

    private void clearData() {
        player.getTimer(hackType).remove("animation");
    }

    @Override
    public boolean canDo() {
        return player.canRunCheck(hackType, true)
                && !Vehicles.has(player, "drill")
                && !mcMMO.hasGeneralAbility(player);
    }

}
