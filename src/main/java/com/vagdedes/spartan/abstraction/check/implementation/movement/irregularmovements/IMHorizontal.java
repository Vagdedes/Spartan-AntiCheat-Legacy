package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;

public class IMHorizontal extends ProbabilityDetection {

    private final CheckDetection horizontal_known, horizontal_abstract;

    IMHorizontal(CheckRunner executor) {
        super(executor, null, Check.DetectionType.BUKKIT, null, true);
        this.horizontal_known = new ImplementedProbabilityDetection(this, "horizontal_known", true);
        this.horizontal_abstract = new ImplementedProbabilityDetection(this, "horizontal_abstract", true);
    }

    final void run(boolean knownBox, boolean knownOldBox, double box, double oldBox, double vertical) {
        if (this.protocol.spartan.getVehicle() == null
                && !this.protocol.spartan.trackers.has(PlayerTrackers.TrackerType.BUBBLE_WATER, "soul-sand")) {
            int airTicks = this.protocol.spartan.movement.getTicksOnAir();

            if (airTicks > 0
                    && !BlockUtils.canClimb(new SpartanLocation(this.protocol.getLocation()).getBlock().getType(), false)
                    && !BlockUtils.canClimb(new SpartanLocation(this.protocol.getFromLocation()).getBlock().getType(), false)) {
                if (knownBox || knownOldBox) {
                    int limit = PlayerUtils.getJumpTicks(0) * (this.protocol.spartan.isBedrockPlayer() ? PlayerUtils.getJumpTicks(0) : 1);

                    if (airTicks > limit
                            && !this.protocol.spartan.isOnGround(true)) {
                        this.horizontal_known.cancel(
                                "type: horizontal(known)"
                                        + ", box: " + box
                                        + ", old-box: " + oldBox
                                        + ", vertical: " + vertical
                                        + ", air-ticks: " + airTicks
                                        + ", limit: " + limit,
                                this.protocol.getFromLocation(),
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
                            this.protocol.getFromLocation(),
                            0,
                            true
                    );
                }
            }
        }
    }

}
