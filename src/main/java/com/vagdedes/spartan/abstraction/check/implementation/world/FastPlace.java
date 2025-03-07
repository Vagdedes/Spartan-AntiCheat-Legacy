package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedDetection;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.event.CBlockPlaceEvent;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.compatibility.Compatibility;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import com.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Material;

public class FastPlace extends CheckRunner {

    private final CheckDetection fast, medium, slow;
    private long time;
    private final Buffer.IndividualBuffer fastBuffer, mediumBuffer, slowBuffer;

    public FastPlace(HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.fast = new ImplementedDetection(this, null, null, "fast", true);
        this.medium = new ImplementedDetection(this, null, null, "medium", true);
        this.slow = new ImplementedDetection(this, null, null, "slow", true);
        this.fastBuffer = new Buffer.IndividualBuffer();
        this.mediumBuffer = new Buffer.IndividualBuffer();
        this.slowBuffer = new Buffer.IndividualBuffer();
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof CBlockPlaceEvent) {
            CBlockPlaceEvent event = (CBlockPlaceEvent) object;

            if (!ItemsAdder.is(event.placedBlock)
                    && !BlockUtils.isScaffoldingBlock(event.placedBlock.getType())) {
                Material material = event.placedBlock.getType();

                if (material != Material.FIRE
                        && material != Material.AIR

                        && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                        || material != Material.CAVE_AIR
                        && material != Material.VOID_AIR)) {
                    long timePassed = System.currentTimeMillis() - time;
                    time = System.currentTimeMillis();
                    int ticks = 20;

                    if (timePassed <= 150L) {
                        this.slow.call(() -> {
                            if (slowBuffer.count(1, ticks) >= 9) {
                                this.slow.cancel(
                                        "slow"
                                                + ", ms: " + timePassed
                                                + ", block: " + BlockUtils.materialToString(material),
                                        null,
                                        ticks
                                );
                            }
                        });
                        if (timePassed <= 100L) {
                            this.medium.call(() -> {
                                if (mediumBuffer.count(1, ticks) >= 7) {
                                    this.medium.cancel(
                                            "medium"
                                                    + ", ms: " + timePassed
                                                    + ", block: " + BlockUtils.materialToString(material),
                                            null,
                                            ticks
                                    );
                                }
                            });
                            this.fast.call(() -> {
                                if (timePassed <= 50L
                                        && !this.protocol.isBedrockPlayer()
                                        && !Compatibility.CompatibilityType.VEIN_MINER.isFunctional()
                                        && fastBuffer.count(1, ticks) >= 5) {
                                    this.fast.cancel(
                                            "fast"
                                                    + ", ms: " + timePassed
                                                    + ", block: " + BlockUtils.materialToString(material),
                                            null,
                                            ticks
                                    );
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    @Override
    protected boolean canRun() {
        return !this.protocol.bukkitExtra.movement.isFlying()
                && !PrinterMode.isUsing(this.protocol)
                && !Vehicles.has(this.protocol, new String[]{Vehicles.DRILL, Vehicles.TRACTOR});
    }
}
