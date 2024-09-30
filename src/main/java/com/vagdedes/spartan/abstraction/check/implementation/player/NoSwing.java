package com.vagdedes.spartan.abstraction.check.implementation.player;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.player.PlayerTrackers;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.AureliumSkills;
import com.vagdedes.spartan.compatibility.manual.abilities.mcMMO;
import com.vagdedes.spartan.compatibility.manual.building.MineBomb;
import com.vagdedes.spartan.compatibility.manual.building.TreeFeller;
import com.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import com.vagdedes.spartan.utils.minecraft.entity.CombatUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class NoSwing extends CheckExecutor {

    private static final int refreshTime = 30 * 20;
    private long breakTime, animationTime;
    private final Buffer.IndividualBuffer damageBuffer;
    private long checkDamage;

    public NoSwing(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player, 0);
        this.breakTime = Long.MAX_VALUE;
        this.animationTime = Long.MAX_VALUE;
        this.damageBuffer = new Buffer.IndividualBuffer();
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerInteractEvent) {
            PlayerInteractEvent e = (PlayerInteractEvent) object;

            if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                this.breakTime = System.currentTimeMillis();
            }
        } else if (object instanceof PlayerAnimationEvent) {
            this.animationTime = System.currentTimeMillis();
        } else if (object instanceof BlockBreakEvent) {
            if (!hackType.getCheck().getBooleanOption("check_breaking", true)
                    || MineBomb.isUsing(player)
                    || AureliumSkills.isUsing(player)) {
                return;
            }
            Block block = ((BlockBreakEvent) object).getBlock();

            if (!BlockUtils.isSensitive(player, block.getType())
                    && !TreeFeller.canCancel(player, block)
                    && System.currentTimeMillis() - breakTime > TPS.tickTime) {
                if (this.animationTime == Long.MAX_VALUE) {
                    cancel(
                            "type: break"
                                    + ", block: " + BlockUtils.blockToString(block)
                                    + ", item: " + BlockUtils.materialToString(player.getItemInHand().getType()));
                } else {
                    resetAnimationData();
                }
            }
        } else if (object instanceof PlayerAttackEvent) {
            if (!hackType.getCheck().getBooleanOption("check_damage", true)
                    || player.getAttackCooldown() != 1.0f
                    || player.trackers.has(PlayerTrackers.TrackerType.TRIDENT)) {
                return;
            }
            LivingEntity entity = ((PlayerAttackEvent) object).target;

            if (!CombatProcessing.canCheck(player, entity)) {
                return;
            }
            long time = System.currentTimeMillis();

            if (checkDamage < time) {
                checkDamage = time + (20L * TPS.tickTime);

                if (this.animationTime == Long.MAX_VALUE) {
                    if (damageBuffer.count(1, refreshTime) >= 3) {
                        damageBuffer.reset();
                        cancel(
                                "type: damage"
                                        + ", entity: " + CombatUtils.entityToString(entity)
                                        + ", item: " + BlockUtils.materialToString(player.getItemInHand().getType())
                        );
                    }
                } else {
                    resetAnimationData();
                }
            }
        }
    }

    private void resetAnimationData() {
        this.animationTime = Long.MAX_VALUE;
    }

    @Override
    public boolean canRun() {
        return !Vehicles.has(player, Vehicles.DRILL)
                && !mcMMO.hasGeneralAbility(player);
    }

}
