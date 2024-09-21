package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.player.PlayerTrackers;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;

public class IrregularMovementsHorizontal extends DetectionExecutor {

    IrregularMovementsHorizontal(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run(boolean knownBox, boolean knownOldBox, double box, double oldBox, double vertical) {
        if (hackType.getCheck().getBooleanOption("check_horizontal", true)
                && !player.trackers.has(PlayerTrackers.TrackerType.BUBBLE_WATER, "soul-sand")) {
            int airTicks = player.movement.getTicksOnAir();

            if (airTicks > 0
                    && !BlockUtils.canClimb(player.movement.getLocation().getBlock().material, false)
                    && !BlockUtils.canClimb(player.movement.getEventFromLocation().getBlock().material, false)) {
                if (knownBox || knownOldBox) {
                    int limit = PlayerUtils.getJumpTicks(0) * (player.isBedrockPlayer() ? PlayerUtils.getJumpTicks(0) : 1);

                    if (airTicks > limit
                            && !player.isOnGround(true)) {
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
        }
    }
}
