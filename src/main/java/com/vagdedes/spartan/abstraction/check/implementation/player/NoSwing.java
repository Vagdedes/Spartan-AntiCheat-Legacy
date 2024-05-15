package com.vagdedes.spartan.abstraction.check.implementation.player;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.data.Timer;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.AureliumSkills;
import com.vagdedes.spartan.compatibility.manual.abilities.mcMMO;
import com.vagdedes.spartan.compatibility.manual.building.MineBomb;
import com.vagdedes.spartan.compatibility.manual.building.TreeFeller;
import com.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.CombatUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class NoSwing extends CheckExecutor {

    private static final int refreshTime = 30 * 20;

    public NoSwing(SpartanPlayer player) {
        super(Enums.HackType.NoSwing, player);
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerInteractEvent) {
            PlayerInteractEvent e = (PlayerInteractEvent) object;

            if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                this.getTimer().set("break");
            }
        } else if (object instanceof PlayerAnimationEvent) {
            this.getTimer().set("animation");
        } else if (object instanceof BlockBreakEvent) {
            if (!hackType.getCheck().getBooleanOption("check_breaking", true)
                    || MineBomb.isUsing(player)
                    || AureliumSkills.isUsing(player)) {
                return;
            }
            SpartanBlock block = new SpartanBlock(player, ((BlockBreakEvent) object).getBlock());

            if (!BlockUtils.isSensitive(player, block.getLocation().getBlock().material)
                    && !TreeFeller.canCancel(player, block)) {
                Timer timer = this.getTimer();

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
        }
    }

    private void runDamage(LivingEntity entity) {
        if (!hackType.getCheck().getBooleanOption("check_damage", true)
                || player.hasAttackCooldown()
                || player.getTrackers().has(Trackers.TrackerType.TRIDENT)) {
            return;
        }
        Cooldowns cooldowns = this.getCooldowns();

        if (cooldowns.canDo("damage")) {
            cooldowns.add("damage", 20);

            if (!this.getTimer().has("animation")) {
                Buffer buffer = this.getBuffer();

                if (buffer.count("damage", refreshTime) >= 3) {
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
        this.getTimer().remove("animation");
    }

    @Override
    public boolean canRun() {
        return !Vehicles.has(player, "drill")
                && !mcMMO.hasGeneralAbility(player);
    }

}
