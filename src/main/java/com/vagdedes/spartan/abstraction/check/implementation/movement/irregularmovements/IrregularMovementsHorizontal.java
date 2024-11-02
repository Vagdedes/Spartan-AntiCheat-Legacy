package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.ImplementedDetectionExecutor;
import com.vagdedes.spartan.abstraction.player.PlayerTrackers;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;

public class IrregularMovementsHorizontal extends DetectionExecutor {

    private final DetectionExecutor horizontal_known, horizontal_abstract;

    IrregularMovementsHorizontal(CheckExecutor executor) {
        super(executor, null, true);
        this.horizontal_known = new ImplementedDetectionExecutor(this, "horizontal_known", true);
        this.horizontal_abstract = new ImplementedDetectionExecutor(this, "horizontal_abstract", true);
    }

    void run(boolean knownBox, boolean knownOldBox, double box, double oldBox, double vertical) {
        if (!player.trackers.has(PlayerTrackers.TrackerType.BUBBLE_WATER, "soul-sand")) {
            int airTicks = player.movement.getTicksOnAir();

            if (airTicks > 0
                    && !BlockUtils.canClimb(player.movement.getLocation().getBlock().getType(), false)
                    && !BlockUtils.canClimb(player.movement.getEventFromLocation().getBlock().getType(), false)) {
                if (knownBox || knownOldBox) {
                    int limit = PlayerUtils.getJumpTicks(0) * (player.isBedrockPlayer() ? PlayerUtils.getJumpTicks(0) : 1);

                    if (airTicks > limit
                            && !player.isOnGround(true)) {
                        this.horizontal_known.cancel(
                                "type: horizontal(known)"
                                        + ", box: " + box
                                        + ", old-box: " + oldBox
                                        + ", vertical: " + vertical
                                        + ", air-ticks: " + airTicks
                                        + ", limit: " + limit,
                                airTicks / (double) limit,
                                player.movement.getDetectionLocation(),
                                0,
                                true
                        );
                    }
                } else {
                    this.horizontal_abstract.cancel(
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
