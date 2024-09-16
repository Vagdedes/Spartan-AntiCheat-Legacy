package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanBlock;
import com.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import com.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Material;
import org.bukkit.event.block.BlockPlaceEvent;

public class FastPlace extends CheckExecutor {

    private long time;
    private final Buffer.IndividualBuffer fastBuffer, mediumBuffer, slowBuffer;

    public FastPlace(HackType hackType, SpartanPlayer player) {
        super(hackType, player, 0);
        this.fastBuffer = new Buffer.IndividualBuffer();
        this.mediumBuffer = new Buffer.IndividualBuffer();
        this.slowBuffer = new Buffer.IndividualBuffer();
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        SpartanBlock b = new SpartanBlock(((BlockPlaceEvent) object).getBlock());

        if (!BlockUtils.isScaffoldingBlock(b.material)) {
            Material material = b.material;

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
                        cancel(
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
                            cancel(
                                    "type: medium"
                                            + ", ms: " + timePassed
                                            + ", block: " + BlockUtils.materialToString(material),
                                    1.0,
                                    null,
                                    ticks
                            );
                        }
                        if (timePassed <= 50L) {
                            String detection = "type: fast";

                            if (!player.bedrockPlayer
                                    && hackType.getCheck().getBooleanOption("check_fast", true)
                                    && !Compatibility.CompatibilityType.VEIN_MINER.isFunctional()
                                    && fastBuffer.count(1, ticks) >= 4) {
                                cancel(
                                        detection
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
    }

    @Override
    protected boolean canRun() {
        return !player.movement.isFlying()
                && !PrinterMode.isUsing(player)
                && !Vehicles.has(player, new String[]{Vehicles.DRILL, Vehicles.TRACTOR});
    }
}
