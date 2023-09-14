package me.vagdedes.spartan.checks.combat.fastClicks;

import me.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import me.vagdedes.spartan.features.protections.LagLeniencies;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.handlers.tracking.ClickData;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.objects.system.HackPrevention;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.event.block.Action;

public class FastClicks {

    static final Enums.HackType check = Enums.HackType.FastClicks;
    private static Check checkObject = check.getCheck();

    static final int
            defaultClicksPerSecondLimit = 15,
            suspiciousClicksPerSecond = 10,
            suspiciousMillisecondCorrelation = 9;

    public static void run(SpartanPlayer player, Action action) {
        if (canDo(player, action)) {
            checkObject = check.getCheck();
            ClickData clickData = player.getClickData();

            int limit = checkObject.getNumericalOption("clicks_per_second_limit", defaultClicksPerSecondLimit),
                    latencyExtra = AlgebraUtils.integerRound(Math.min(LagLeniencies.getDelay(player), 5)),
                    extraLimit = Math.max(limit - defaultClicksPerSecondLimit, 0);
            double globalAverage = ResearchEngine.getCPS()[1];

            if (limit > 0) {
                if (globalAverage > limit) {
                    limit = AlgebraUtils.integerCeil(globalAverage) + 1;
                }
                limit += latencyExtra;
                ClicksPerSecond.run(player, clickData, limit, latencyExtra, globalAverage);
            }
            runDetection(
                    "check_click_frequency",
                    () -> ClickFrequency.run(player, clickData, latencyExtra, extraLimit, globalAverage)
            );
            runDetection(
                    "check_click_pattern",
                    () -> ClickPattern.run(player, clickData, latencyExtra, extraLimit, globalAverage)
            );
        }
    }

    private static void runDetection(String option, Runnable runnable) {
        if (checkObject.getBooleanOption(option, true)) {
            runnable.run();
        }
    }

    static void violate(SpartanPlayer player, String detection, String information) {
        new HackPrevention(player, check, information);

        if (detection != null) {
            player.getViolations(check).process();
            checkObject.addDisabledUser(
                    player.getUniqueId(),
                    check.toString(),
                    detection,
                    20
            );
        }
    }

    private static boolean canDo(SpartanPlayer player, Action action) {
        return action == Action.LEFT_CLICK_AIR
                && player.canRunCheck(check, true) && player.getVehicle() == null
                && !player.getHandlers().has(Handlers.HandlerType.BlockBreak)
                && !PlayerData.isPickaxeItem(player.getItemInHand().getType())
                && (!check.getCheck().getBooleanOption("combat_only_mode", false) || PlayerData.isInActivePlayerCombat(player))
                && !PrinterMode.canCancel(player);
    }
}
