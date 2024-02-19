package com.vagdedes.spartan.checks.combat.fastClicks;

import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.abstraction.DetectionExecutor;
import com.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import com.vagdedes.spartan.objects.data.Clicks;
import com.vagdedes.spartan.objects.data.Handlers;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.system.Enums;
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
            player.getViolations(hackType).addDisableCause(
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
                && !PrinterMode.canCancel(player);
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        return false;
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        if (canDo()
                && ((PlayerInteractEvent) object).getAction() == Action.LEFT_CLICK_AIR) {
            Clicks clickData = player.getClicks();
            int limit = hackType.getCheck().getNumericalOption("clicks_per_second_limit", defaultClicksPerSecondLimit),
                    extraLimit = Math.max(limit - defaultClicksPerSecondLimit, 0);

            if (limit > 0) {
                clicksPerSecond.run(clickData, limit);
            }
            runDetection(
                    "check_click_frequency",
                    () -> clickFrequency.run(clickData, extraLimit)
            );
            runDetection(
                    "check_click_pattern",
                    () -> clickPattern.run(clickData, extraLimit)
            );
        }
        return false;
    }
}
