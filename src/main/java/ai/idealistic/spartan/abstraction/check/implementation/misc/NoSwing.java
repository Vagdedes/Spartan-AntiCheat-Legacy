package ai.idealistic.spartan.abstraction.check.implementation.misc;

import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.check.CheckEnums.HackType;
import ai.idealistic.spartan.abstraction.check.CheckRunner;
import ai.idealistic.spartan.abstraction.check.definition.ImplementedDetection;
import ai.idealistic.spartan.abstraction.data.Buffer;
import ai.idealistic.spartan.abstraction.event.PlayerAttackEvent;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.abstraction.world.ServerBlock;
import ai.idealistic.spartan.compatibility.Compatibility;
import ai.idealistic.spartan.compatibility.manual.abilities.AureliumSkills;
import ai.idealistic.spartan.compatibility.manual.abilities.ItemsAdder;
import ai.idealistic.spartan.compatibility.manual.building.MineBomb;
import ai.idealistic.spartan.compatibility.manual.building.TreeFeller;
import ai.idealistic.spartan.compatibility.manual.entity.Vehicles;
import ai.idealistic.spartan.functionality.server.PluginBase;
import ai.idealistic.spartan.functionality.tracking.CheckConditions;
import ai.idealistic.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerAnimationEvent;

public class NoSwing extends CheckRunner {

    private final CheckDetection breaking, damage;
    private long lastAnimationTime;
    private final Buffer.IndividualBuffer damageBuffer, breakBuffer;

    public NoSwing(HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.lastAnimationTime = 0L;
        this.damageBuffer = new Buffer.IndividualBuffer();
        this.breakBuffer = new Buffer.IndividualBuffer();
        this.breaking = new ImplementedDetection(this, null, null, "breaking", true);
        this.damage = new ImplementedDetection(this, null, null, "damage", true);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerAnimationEvent) {
            this.lastAnimationTime = System.currentTimeMillis();
        } else if (object instanceof BlockBreakEvent) {
            this.breaking.call(() -> {
                if (MineBomb.isUsing(this.protocol)
                        || AureliumSkills.isUsing(this.protocol)) {
                    return;
                }
                BlockBreakEvent e = (BlockBreakEvent) object;
                org.bukkit.block.Block block = e.getBlock();
                org.bukkit.Material itemInHand = this.protocol.getItemInHand().getType();

                PluginBase.runTask(block, () -> {
                    if (!BlockUtils.isSensitive(this.protocol, new ServerBlock(block).getType())
                            && !TreeFeller.canCancel(block)
                            && !ItemsAdder.is(block)) {

                        long timeSinceAnimation = System.currentTimeMillis() - this.lastAnimationTime;

                        // Safely absorbs out-of-order packet batching up to 150ms
                        if (timeSinceAnimation > 150L) {
                            if (breakBuffer.count(1, 20) >= 3) {
                                this.breaking.cancel(
                                        "breaking"
                                                + ", block: " + BlockUtils.blockToString(block)
                                                + ", item: " + BlockUtils.materialToString(itemInHand)
                                                + ", ms-since-anim: " + timeSinceAnimation
                                );

                                if (this.breaking.prevent()) {
                                    e.setCancelled(true);
                                }
                            }
                        } else {
                            breakBuffer.decrease(0.25);
                        }
                    }
                });
            });
        } else if (object instanceof PlayerAttackEvent) {
            this.damage.call(() -> {
                LivingEntity entity = ((PlayerAttackEvent) object).target;

                if (!CheckConditions.canCheckCombat(this.protocol, entity)) {
                    return;
                }

                long timeSinceAnimation = System.currentTimeMillis() - this.lastAnimationTime;

                // Removed cooldown requirement to catch 15+ CPS KillAura modules
                if (timeSinceAnimation > 150L) {
                    if (damageBuffer.count(1, 20) >= 4) {
                        this.damage.cancel(
                                "damage"
                                        + ", entity: " + entityToString(entity)
                                        + ", item: " + BlockUtils.materialToString(this.protocol.getItemInHand().getType())
                                        + ", ms-since-anim: " + timeSinceAnimation
                        );
                    }
                } else {
                    damageBuffer.decrease(0.5);
                }
            });
        }
    }

    @Override
    public boolean canRun() {
        return !Vehicles.has(this.protocol, Vehicles.DRILL)
                && !Compatibility.CompatibilityType.ADVANCED_ENCHANTMENTS.isFunctional();
    }

    private static String entityToString(Entity e) {
        return e.getType().toString().toLowerCase().replace("_", "-");
    }

}