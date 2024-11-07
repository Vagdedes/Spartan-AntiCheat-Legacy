package com.vagdedes.spartan.abstraction.check.implementation.player;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.ImplementedDetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.manual.abilities.AureliumSkills;
import com.vagdedes.spartan.compatibility.manual.abilities.mcMMO;
import com.vagdedes.spartan.compatibility.manual.building.MineBomb;
import com.vagdedes.spartan.compatibility.manual.building.TreeFeller;
import com.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import com.vagdedes.spartan.functionality.server.TPS;
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

    private final DetectionExecutor breaking, damage;
    private static final int refreshTime = 30 * 20;
    private long breakTime, animationTime;
    private final Buffer.IndividualBuffer damageBuffer;
    private long checkDamage;

    public NoSwing(Enums.HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.breakTime = Long.MAX_VALUE;
        this.animationTime = Long.MAX_VALUE;
        this.damageBuffer = new Buffer.IndividualBuffer();
        this.breaking = new ImplementedDetectionExecutor(this, "breaking", true);
        this.damage = new ImplementedDetectionExecutor(this, "damage", true);
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
            if (MineBomb.isUsing(this.protocol().spartanPlayer)
                    || AureliumSkills.isUsing(this.protocol().spartanPlayer)) {
                return;
            }
            Block block = ((BlockBreakEvent) object).getBlock();

            if (!BlockUtils.isSensitive(this.protocol().spartanPlayer, block.getType())
                    && !TreeFeller.canCancel(this.protocol().spartanPlayer, block)
                    && System.currentTimeMillis() - breakTime > TPS.tickTime) {
                if (this.animationTime == Long.MAX_VALUE) {
                    this.breaking.cancel(
                            "type: breaking"
                                    + ", block: " + BlockUtils.blockToString(block)
                                    + ", item: " + BlockUtils.materialToString(this.protocol().spartanPlayer.getItemInHand().getType()));
                } else {
                    resetAnimationData();
                }
            }
        } else if (object instanceof PlayerAttackEvent) {
            if (this.protocol().spartanPlayer.getAttackCooldown() != 1.0f
                    || this.protocol().spartanPlayer.trackers.has(PlayerTrackers.TrackerType.TRIDENT)) {
                return;
            }
            LivingEntity entity = ((PlayerAttackEvent) object).target;

            if (!CombatUtils.canCheck(this.protocol().spartanPlayer, entity)) {
                return;
            }
            long time = System.currentTimeMillis();

            if (checkDamage < time) {
                checkDamage = time + (20L * TPS.tickTime);

                if (this.animationTime == Long.MAX_VALUE) {
                    if (damageBuffer.count(1, refreshTime) >= 3) {
                        damageBuffer.reset();
                        this.damage.cancel(
                                "type: damage"
                                        + ", entity: " + CombatUtils.entityToString(entity)
                                        + ", item: " + BlockUtils.materialToString(this.protocol().spartanPlayer.getItemInHand().getType())
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
        return !Vehicles.has(this.protocol().spartanPlayer, Vehicles.DRILL)
                && !mcMMO.hasGeneralAbility(this.protocol().spartanPlayer);
    }

}
