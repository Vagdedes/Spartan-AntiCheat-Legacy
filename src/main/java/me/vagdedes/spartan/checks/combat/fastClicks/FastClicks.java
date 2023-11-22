package me.vagdedes.spartan.checks.combat.fastClicks;

import me.vagdedes.spartan.abstraction.CheckExecutor;
import me.vagdedes.spartan.abstraction.DetectionExecutor;
import me.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import me.vagdedes.spartan.functionality.protections.LagLeniencies;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.data.Clicks;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class FastClicks extends CheckExecutor {

    private final ClicksPerSecond clicksPerSecond;
    private final ClickPattern clickPattern;
    private final ClickFrequency clickFrequency;

    public FastClicks(SpartanPlayer player) {
        super(Enums.HackType.FastClicks, player, 3);
        this.addDetections(new DetectionExecutor[]{
                new ClicksPerSecond(this),
                new ClickPattern(this),
                new ClickFrequency(this)
        });
        clicksPerSecond = (ClicksPerSecond) detections[0];
        clickPattern = (ClickPattern) detections[1];
        clickFrequency = (ClickFrequency) detections[2];
    }

    static final int
            defaultClicksPerSecondLimit = 15,
            suspiciousClicksPerSecond = 10,
            suspiciousMillisecondCorrelation = 9;

    private void runDetection(String option, Runnable runnable) {
        if (hackType.getCheck().getBooleanOption(option, true)) {
            runnable.run();
        }
    }

    void violate(String detection, String information) {
        cancel(information);

        if (detection != null) {
            player.getViolations(hackType).process();
            hackType.getCheck().addDisabledUser(
                    player.getUniqueId(),
                    hackType.toString(),
                    detection,
                    20
            );
        }
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true) && player.getVehicle() == null
                && !player.getHandlers().has(Handlers.HandlerType.SensitiveBlockBreak)
                && !PlayerData.isPickaxeItem(player.getItemInHand().getType())
                && (!hackType.getCheck().getBooleanOption("combat_only_mode", false) || PlayerData.isInActivePlayerCombat(player))
                && !PrinterMode.canCancel(player);
    }

    @Override
    public boolean run() {
        return false;
    }

    @Override
    public boolean handle(Object object) {
        if (canDo()
                && ((PlayerInteractEvent) object).getAction() == Action.LEFT_CLICK_AIR) {
            Clicks clickData = player.getClicks();
            int limit = hackType.getCheck().getNumericalOption("clicks_per_second_limit", defaultClicksPerSecondLimit),
                    latencyExtra = AlgebraUtils.integerRound(Math.min(LagLeniencies.getDelay(player), 5)),
                    extraLimit = Math.max(limit - defaultClicksPerSecondLimit, 0);
            double globalAverage = ResearchEngine.getAverageCPS();

            if (limit > 0) {
                if (globalAverage > limit) {
                    limit = AlgebraUtils.integerCeil(globalAverage) + 1;
                }
                limit += latencyExtra;
                clicksPerSecond.run(clickData, limit, latencyExtra, globalAverage);
            }
            runDetection(
                    "check_click_frequency",
                    () -> clickFrequency.run(clickData, latencyExtra, extraLimit, globalAverage)
            );
            runDetection(
                    "check_click_pattern",
                    () -> clickPattern.run(clickData, latencyExtra, extraLimit, globalAverage)
            );
        }
        return false;
    }
}
