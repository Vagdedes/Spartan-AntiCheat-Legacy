package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetectionExecutor;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;

public class IrregularMovementsHorizontal extends ProbabilityDetectionExecutor {

    private final CheckDetection horizontal_known, horizontal_abstract;

    IrregularMovementsHorizontal(CheckRunner executor) {
        super(executor, null, true);
        this.horizontal_known = new ImplementedProbabilityDetection(this, "horizontal_known", true);
        this.horizontal_abstract = new ImplementedProbabilityDetection(this, "horizontal_abstract", true);
    }

    void run(boolean knownBox, boolean knownOldBox, double box, double oldBox, double vertical) {
        if (!this.protocol().spartan.trackers.has(PlayerTrackers.TrackerType.BUBBLE_WATER, "soul-sand")) {
            int airTicks = this.protocol().spartan.movement.getTicksOnAir();

            if (airTicks > 0
                    && !BlockUtils.canClimb(this.protocol().spartan.movement.getLocation().getBlock().getType(), false)
                    && !BlockUtils.canClimb(this.protocol().spartan.movement.getEventFromLocation().getBlock().getType(), false)) {
                if (knownBox || knownOldBox) {
                    int limit = PlayerUtils.getJumpTicks(0) * (this.protocol().spartan.isBedrockPlayer() ? PlayerUtils.getJumpTicks(0) : 1);

                    if (airTicks > limit
                            && !this.protocol().spartan.isOnGround(true)) {
                        this.horizontal_known.cancel(
                                "type: horizontal(known)"
                                        + ", box: " + box
                                        + ", old-box: " + oldBox
                                        + ", vertical: " + vertical
                                        + ", air-ticks: " + airTicks
                                        + ", limit: " + limit,
                                airTicks / (double) limit,
                                this.protocol().spartan.movement.getDetectionLocation(),
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
                            this.protocol().spartan.movement.getDetectionLocation(),
                            0,
                            true
                    );
                }
            }
        }
    }
}
