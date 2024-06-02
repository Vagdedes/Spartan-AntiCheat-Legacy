package com.vagdedes.spartan.abstraction.check.implementation.player;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanBlock;
import com.vagdedes.spartan.compatibility.manual.abilities.AureliumSkills;
import com.vagdedes.spartan.compatibility.manual.abilities.mcMMO;
import com.vagdedes.spartan.compatibility.manual.building.MineBomb;
import com.vagdedes.spartan.compatibility.manual.building.TreeFeller;
import com.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.CombatUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class NoSwing extends CheckExecutor {

    private static final int refreshTime = 30 * 20;
    private long breakTime, animationTime;
    private final Cooldowns cooldowns;
    private final Buffer buffer;

    public NoSwing(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player);
        this.breakTime = Long.MAX_VALUE;
        this.animationTime = Long.MAX_VALUE;
        this.cooldowns = new Cooldowns(player);
        this.buffer = new Buffer(player);
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
            SpartanBlock block = new SpartanBlock(((BlockBreakEvent) object).getBlock());

            if (!BlockUtils.isSensitive(player, block.getLocation().getBlock().material)
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
                    || player.hasAttackCooldown()
                    || player.trackers.has(Trackers.TrackerType.TRIDENT)) {
                return;
            }
            LivingEntity entity = ((PlayerAttackEvent) object).getTarget();

            if (!CombatProcessing.canCheck(player, entity)) {
                return;
            }
            if (cooldowns.canDo("damage")) {
                cooldowns.add("damage", 20);

                if (this.animationTime == Long.MAX_VALUE) {
                    if (buffer.count("damage", refreshTime) >= 3) {
                        buffer.remove("damage");
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

    @Override
    protected void cannotHandle(boolean cancelled, Object object) {
        if (object instanceof PlayerInteractEvent) {
            PlayerInteractEvent e = (PlayerInteractEvent) object;

            if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                this.resetBreakData();
            }
        } else if (object instanceof PlayerAnimationEvent) {
           this.resetAnimationData();
        }
    }

    private void runDamage(LivingEntity entity) {
        if (!hackType.getCheck().getBooleanOption("check_damage", true)
                || player.hasAttackCooldown()
                || player.trackers.has(Trackers.TrackerType.TRIDENT)) {
            return;
        }
        if (cooldowns.canDo("damage")) {
            cooldowns.add("damage", 20);

            if (this.animationTime == Long.MAX_VALUE) {
                if (buffer.count("damage", refreshTime) >= 3) {
                    buffer.remove("damage");
                    cancel("type: damage"
                            + ", entity: " + CombatUtils.entityToString(entity)
                            + ", item: " + BlockUtils.materialToString(player.getItemInHand().getType()));
                }
            } else {
                resetAnimationData();
            }
        }
    }

    private void resetAnimationData() {
        this.animationTime = Long.MAX_VALUE;
    }

    private void resetBreakData() {
        this.breakTime = Long.MAX_VALUE;
    }

    @Override
    public boolean canRun() {
        return !Vehicles.has(player, Vehicles.DRILL)
                && !mcMMO.hasGeneralAbility(player);
    }

}
