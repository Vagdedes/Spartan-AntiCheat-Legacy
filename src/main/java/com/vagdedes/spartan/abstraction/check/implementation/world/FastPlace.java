package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Timer;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import com.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Material;
import org.bukkit.event.block.BlockPlaceEvent;

public class FastPlace extends CheckExecutor {

    private static final HackType check = HackType.FastPlace;

    public FastPlace(SpartanPlayer player) {
        super(check, player);
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        if (canDo()) {
            SpartanBlock b = new SpartanBlock(player, ((BlockPlaceEvent) object).getBlock());

            if (!BlockUtils.isScaffoldingBlock(b.material)) {
                Material material = b.material;

                if (material != Material.FIRE
                        && material != Material.AIR

                        && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                        || material != Material.CAVE_AIR
                        && material != Material.VOID_AIR)) {
                    Timer timer = player.getTimer(hackType);
                    long time = player.getTimer(hackType).get("time");

                    if (timer.has(time)) {
                        Buffer buffer = player.getBuffer(hackType);
                        int ticks = 20;

                        if (time <= 150L) {
                            if (buffer.start("slow", ticks, Check.detectionMeasurementTicks) >= 0.4) {
                                cancel("type: slow, ms: " + time + ", block: " + BlockUtils.materialToString(material),
                                        null, ticks);
                            }
                            if (time <= 100L) {
                                if (buffer.start("medium", ticks, Check.detectionMeasurementTicks) >= 0.35) {
                                    cancel("type: medium, ms: " + time + ", block: " + BlockUtils.materialToString(material),
                                            null, ticks);
                                }
                                if (time <= 50L) {
                                    String detection = "type: fast";

                                    if (!CloudBase.isInformationCancelled(hackType, detection)
                                            && !player.bedrockPlayer
                                            && hackType.getCheck().getBooleanOption("check_fast", true)
                                            && !Compatibility.CompatibilityType.VeinMiner.isFunctional()
                                            && buffer.start("fast", ticks, Check.detectionMeasurementTicks) >= 0.2) {
                                        cancel(detection + ", ms: " + time + ", block: " + BlockUtils.materialToString(material),
                                                null, ticks);
                                    }
                                }
                            }
                        }
                    }
                    timer.set("time");
                }
            }
        }
        return false;
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        return false;
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true)
                && !player.movement.isFlying()
                && !PrinterMode.canCancel(player)
                && !Vehicles.has(player, new String[]{"drill", "tractor"});
    }
}
