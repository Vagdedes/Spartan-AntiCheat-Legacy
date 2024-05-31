package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.player.SpartanLocation;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;

public class IrregularMovementsHorizontal extends DetectionExecutor {

    IrregularMovementsHorizontal(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run(boolean knownBox, boolean knownOldBox, double box, double oldBox, double vertical) {
        if (hackType.getCheck().getBooleanOption("check_horizontal", true)) {
            int airTicks = player.movement.getTicksOnAir();
            SpartanLocation to = player.movement.getEventToLocation(),
                    from = player.movement.getEventFromLocation();

            runAsync(() -> {
                if (airTicks > 0
                        && !BlockUtils.canClimb(to.getBlock().material, false)
                        && !BlockUtils.canClimb(from.getBlock().material, false)) {
                    if (knownBox || knownOldBox) {
                        int limit = PlayerUtils.getJumpTicks(0) * (player.bedrockPlayer ? 2 : 1);

                        if (airTicks > limit) {
                            cancel(
                                    "type: horizontal(known)"
                                            + ", box: " + box
                                            + ", old-box: " + oldBox
                                            + ", vertical: " + vertical
                                            + ", air-ticks: " + airTicks
                                            + ", limit: " + limit,
                                    airTicks / TPS.maximum,
                                    player.movement.getDetectionLocation(),
                                    0,
                                    true
                            );
                        }
                    } else {
                        cancel(
                                "type: horizontal(abstract)"
                                        + ", box: " + box
                                        + ", old-box: " + oldBox
                                        + ", vertical: " + vertical
                                        + ", air-ticks: " + airTicks,
                                airTicks / TPS.maximum,
                                player.movement.getDetectionLocation(),
                                0,
                                true
                        );
                    }
                }
            });
        }
    }
}
