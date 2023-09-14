package me.vagdedes.spartan.checks.implementation.world;

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
import me.vagdedes.spartan.objects.system.HackPrevention;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import org.bukkit.Material;

public class FastPlace {

    private static final HackType check = HackType.FastPlace;

    public static void run(SpartanPlayer p, SpartanBlock b, boolean fast, boolean medium, boolean slow) {
        if (!p.canRunCheck(check, true) || p.wasFlying()
                || PrinterMode.canCancel(p) || BlockUtils.isScaffoldingBlock(b.getType())
                || Vehicles.has(p, new String[]{"drill", "tractor"})) {
            return;
        }
        Material material = b.getType();

        if (material == Material.FIRE || material == Material.AIR
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && (material == Material.CAVE_AIR || material == Material.VOID_AIR)) {
            return;
        }
        Timer timer = p.getTimer(check);
        long time = p.getTimer(check).get("time");

        if (timer.has(time)) {
            Buffer buffer = p.getBuffer(check);
            int ticks = 20;

            if (time <= 50L) {
                if (fast
                        && !ResearchEngine.hasCompetitiveCPS()
                        && !p.isBedrockPlayer()
                        && check.getCheck().getBooleanOption("check_fast", true)
                        && !Compatibility.CompatibilityType.VeinMiner.isFunctional()
                        && !LagLeniencies.hasHighLatency(p, LagLeniencies.unplayablePing) // Building
                        && buffer.start("fast", ticks) >= (LagLeniencies.hasHighLatency(p, LagLeniencies.averagePing) ? 8 : 4)) { // Building
                    new HackPrevention(p, check, "type: fast, ms: " + time + ", block: " + BlockUtils.materialToString(material),
                            null, ticks);
                }
            } else if (time <= 100L) {
                if (medium && buffer.start("medium", ticks) >= 7) {
                    new HackPrevention(p, check, "type: medium, ms: " + time + ", block: " + BlockUtils.materialToString(material),
                            null, ticks);
                }
            } else if (time <= 150L) {
                if (slow && buffer.start("slow", ticks) >= 8) {
                    new HackPrevention(p, check, "type: slow, ms: " + time + ", block: " + BlockUtils.materialToString(material),
                            null, ticks);
                }
            }
        }
        timer.set("time");
    }
}
