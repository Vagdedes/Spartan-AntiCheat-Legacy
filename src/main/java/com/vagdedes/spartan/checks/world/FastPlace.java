package com.vagdedes.spartan.checks.world;

import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import com.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.functionality.protections.LagLeniencies;
import com.vagdedes.spartan.functionality.synchronicity.cloud.CloudFeature;
import com.vagdedes.spartan.handlers.stability.ResearchEngine;
import com.vagdedes.spartan.objects.data.Buffer;
import com.vagdedes.spartan.objects.data.Timer;
import com.vagdedes.spartan.objects.replicates.SpartanBlock;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
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
    public boolean handle(Object object) {
        if (canDo()) {
            SpartanBlock b = new SpartanBlock(player, ((BlockPlaceEvent) object).getBlock());

            if (!BlockUtils.isScaffoldingBlock(b.getType())) {
                Material material = b.getType();

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

                        if (time <= 50L) {
                            String detection = "type: fast";

                            if (!ResearchEngine.hasCompetitiveCPS()
                                    && !CloudFeature.isInformationCancelled(hackType, detection)
                                    && !player.isBedrockPlayer()
                                    && hackType.getCheck().getBooleanOption("check_fast", true)
                                    && !Compatibility.CompatibilityType.VeinMiner.isFunctional()
                                    && !LagLeniencies.hasHighLatency(player, LagLeniencies.unplayablePing) // Building
                                    && buffer.start("fast", ticks) >= (LagLeniencies.hasHighLatency(player, LagLeniencies.averagePing) ? 8 : 4)) { // Building
                                cancel(detection + ", ms: " + time + ", block: " + BlockUtils.materialToString(material),
                                        null, ticks);
                            }
                        } else if (time <= 100L) {
                            if (buffer.start("medium", ticks) >= 7) {
                                cancel("type: medium, ms: " + time + ", block: " + BlockUtils.materialToString(material),
                                        null, ticks);
                            }
                        } else if (time <= 150L) {
                            if (buffer.start("slow", ticks) >= 8) {
                                cancel("type: slow, ms: " + time + ", block: " + BlockUtils.materialToString(material),
                                        null, ticks);
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
    public boolean run() {
        return false;
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true)
                && !player.wasFlying()
                && !PrinterMode.canCancel(player)
                && !Vehicles.has(player, new String[]{"drill", "tractor"});
    }
}
