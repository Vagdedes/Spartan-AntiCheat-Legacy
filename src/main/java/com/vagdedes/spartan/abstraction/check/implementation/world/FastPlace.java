package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.ImplementedDetectionExecutor;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import com.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;

public class FastPlace extends CheckExecutor {

    private final DetectionExecutor fast, medium, slow;
    private long time;
    private final Buffer.IndividualBuffer fastBuffer, mediumBuffer, slowBuffer;

    public FastPlace(HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.fast = new ImplementedDetectionExecutor(this, "fast", true);
        this.medium = new ImplementedDetectionExecutor(this, "medium", true);
        this.slow = new ImplementedDetectionExecutor(this, "slow", true);
        this.fastBuffer = new Buffer.IndividualBuffer();
        this.mediumBuffer = new Buffer.IndividualBuffer();
        this.slowBuffer = new Buffer.IndividualBuffer();
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        Block b = ((BlockPlaceEvent) object).getBlock();

        if (!BlockUtils.isScaffoldingBlock(b.getType())) {
            Material material = b.getType();

            if (material != Material.FIRE
                    && material != Material.AIR

                    && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                    || material != Material.CAVE_AIR
                    && material != Material.VOID_AIR)) {
                long timePassed = System.currentTimeMillis() - time;
                time = System.currentTimeMillis();
                int ticks = 20;

                if (timePassed <= 150L) {
                    if (slowBuffer.count(1, ticks) >= 8) {
                        this.slow.cancel(
                                "type: slow"
                                        + ", ms: " + timePassed
                                        + ", block: " + BlockUtils.materialToString(material),
                                1.0,
                                null,
                                ticks
                        );
                    }
                    if (timePassed <= 100L) {
                        if (mediumBuffer.count(1, ticks) >= 7) {
                            this.medium.cancel(
                                    "type: medium"
                                            + ", ms: " + timePassed
                                            + ", block: " + BlockUtils.materialToString(material),
                                    1.0,
                                    null,
                                    ticks
                            );
                        }
                        if (timePassed <= 50L
                                && !protocol.spartanPlayer.isBedrockPlayer()
                                && !Compatibility.CompatibilityType.VEIN_MINER.isFunctional()
                                && fastBuffer.count(1, ticks) >= 4) {
                            this.fast.cancel(
                                    "type: fast"
                                            + ", ms: " + timePassed
                                            + ", block: " + BlockUtils.materialToString(material),
                                    1.0,
                                    null,
                                    ticks
                            );
                        }
                    }
                }
            }
        }
    }

    @Override
    protected boolean canRun() {
        return !protocol.spartanPlayer.movement.isFlying()
                && !PrinterMode.isUsing(protocol.spartanPlayer)
                && !Vehicles.has(protocol.spartanPlayer, new String[]{Vehicles.DRILL, Vehicles.TRACTOR});
    }
}
