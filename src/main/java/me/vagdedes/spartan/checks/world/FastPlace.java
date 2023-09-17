package me.vagdedes.spartan.checks.world;

import me.vagdedes.spartan.abstraction.CheckExecutor;
import me.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import me.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.protections.LagLeniencies;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Timer;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
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
                            if (!ResearchEngine.hasCompetitiveCPS()
                                    && !player.isBedrockPlayer()
                                    && hackType.getCheck().getBooleanOption("check_fast", true)
                                    && !Compatibility.CompatibilityType.VeinMiner.isFunctional()
                                    && !LagLeniencies.hasHighLatency(player, LagLeniencies.unplayablePing) // Building
                                    && buffer.start("fast", ticks) >= (LagLeniencies.hasHighLatency(player, LagLeniencies.averagePing) ? 8 : 4)) { // Building
                                cancel("type: fast, ms: " + time + ", block: " + BlockUtils.materialToString(material),
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
