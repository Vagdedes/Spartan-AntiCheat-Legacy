package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.ImplementedDetectionExecutor;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
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
