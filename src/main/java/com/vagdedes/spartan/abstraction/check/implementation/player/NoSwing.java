package com.vagdedes.spartan.abstraction.check.implementation.player;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedDetection;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.compatibility.manual.abilities.AureliumSkills;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
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

public class NoSwing extends CheckRunner {

    private final CheckDetection breaking, damage;
    private static final int refreshTime = 30 * 20;
    private long breakTime, animationTime;
    private final Buffer.IndividualBuffer damageBuffer;
    private long checkDamage;

    public NoSwing(Enums.HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.breakTime = Long.MAX_VALUE;
        this.animationTime = Long.MAX_VALUE;
        this.damageBuffer = new Buffer.IndividualBuffer();
        this.breaking = new ImplementedDetection(this, null, null, "breaking", true);
        this.damage = new ImplementedDetection(this, null, null, "damage", true);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerInteractEvent) {
            PlayerInteractEvent e = (PlayerInteractEvent) object;

            if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                Block block = e.getClickedBlock();

                if (block == null
                        || !ItemsAdder.is(block)) {
                    this.breakTime = System.currentTimeMillis();
                }
            }
        } else if (object instanceof PlayerAnimationEvent) {
            this.animationTime = System.currentTimeMillis();
        } else if (object instanceof BlockBreakEvent) {
            this.breaking.call(() -> {
                if (MineBomb.isUsing(this.protocol.bukkitExtra)
                        || AureliumSkills.isUsing(this.protocol)) {
                    return;
                }
                BlockBreakEvent e = (BlockBreakEvent) object;
                Block block = e.getBlock();

                if (!BlockUtils.isSensitive(this.protocol, block.getType())
                        && !TreeFeller.canCancel(block)
                        && !ItemsAdder.is(block)
                        && System.currentTimeMillis() - breakTime > TPS.tickTime) {
                    if (this.animationTime == Long.MAX_VALUE) {
                        this.breaking.cancel(
                                "breaking"
                                        + ", block: " + BlockUtils.blockToString(block)
                                        + ", item: " + BlockUtils.materialToString(this.protocol.bukkitExtra.getItemInHand().getType()));

                        if (this.breaking.prevent()) {
                            e.setCancelled(true);
                        }
                    } else {
                        resetAnimationData();
                    }
                }
            });
        } else if (object instanceof PlayerAttackEvent) {
            this.damage.call(() -> {
                if (this.protocol.bukkitExtra.getAttackCooldown() != 1.0f) {
                    return;
                }
                LivingEntity entity = ((PlayerAttackEvent) object).target;

                if (!CombatUtils.canCheck(this.protocol, entity)) {
                    return;
                }
                long time = System.currentTimeMillis();

                if (checkDamage < time) {
                    checkDamage = time + (20L * TPS.tickTime);

                    if (this.animationTime == Long.MAX_VALUE) {
                        if (damageBuffer.count(1, refreshTime) >= 3) {
                            damageBuffer.reset();
                            this.damage.cancel(
                                    "damage"
                                            + ", entity: " + CombatUtils.entityToString(entity)
                                            + ", item: " + BlockUtils.materialToString(this.protocol.bukkitExtra.getItemInHand().getType())
                            );
                        }
                    } else {
                        resetAnimationData();
                    }
                }
            });
        }
    }

    private void resetAnimationData() {
        this.animationTime = Long.MAX_VALUE;
    }

    @Override
    public boolean canRun() {
        return !Vehicles.has(this.protocol, Vehicles.DRILL);
    }

}
