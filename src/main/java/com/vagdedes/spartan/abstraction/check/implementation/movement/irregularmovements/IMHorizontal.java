package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedDetection;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;

public class IMHorizontal extends CheckDetection {

    private final CheckDetection horizontal_known, horizontal_abstract;

    IMHorizontal(CheckRunner executor) {
        super(executor, null, Check.DetectionType.BUKKIT, null, true);
        this.horizontal_known = new ImplementedDetection(this, "horizontal_known", true);
        this.horizontal_abstract = new ImplementedDetection(this, "horizontal_abstract", true);
    }

    void run(boolean knownBox, boolean knownOldBox, double box, double oldBox, double vertical) {
        this.call(() -> {
            if (!this.protocol.bukkitExtra.movement.wasGliding()
                    && this.protocol.bukkitExtra.getVehicle() == null
                    && !this.protocol.bukkitExtra.trackers.has(PlayerTrackers.TrackerType.BUBBLE_WATER, "soul-sand")) {
                int airTicks = this.protocol.bukkitExtra.movement.getTicksOnAir();

                if (airTicks > 0
                        && !BlockUtils.canClimb(new SpartanLocation(this.protocol.getLocation()).getBlock().getType(), false)
                        && !BlockUtils.canClimb(new SpartanLocation(this.protocol.getFromLocation()).getBlock().getType(), false)) {
                    if (knownBox || knownOldBox) {
                        int limit = PlayerUtils.getJumpTicks(0) * (this.protocol.bukkitExtra.isBedrockPlayer() ? PlayerUtils.getJumpTicks(0) : 1);

                        if (airTicks > limit
                                && !this.protocol.isOnGround()) {
                            this.horizontal_known.cancel(
                                    "horizontal(known)"
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
                                "horizontal(abstract)"
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
        });
    }

}
