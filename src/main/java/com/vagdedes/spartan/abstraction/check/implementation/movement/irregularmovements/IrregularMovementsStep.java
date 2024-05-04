package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import org.bukkit.entity.Entity;

import java.util.LinkedHashMap;
import java.util.Map;

public class IrregularMovementsStep extends DetectionExecutor {

    private static final Map<Integer, Boolean> combination = new LinkedHashMap<>();

    static {
        for (double before : GroundUtils.getHeights()) {
            for (double after : GroundUtils.getHeights()) {
                int hash = hash(before, after);

                if (after == before) {
                    combination.put(hash, true);
                } else if (after > before) {
                    combination.put(hash, after - before <= GroundUtils.maxPlayerStep);
                } else {
                    combination.put(hash, before - after <= GroundUtils.minPossibleStep);
                }
            }
        }
    }

    private static int hash(double before, double after) {
        return (Double.hashCode(before) * SpartanBukkit.hashCodeMultiplier)
                + Double.hashCode(after);
    }

    IrregularMovementsStep(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run(boolean knownBox, boolean knownOldBox, double box, double oldBox, double vertical) {
        if (hackType.getCheck().getBooleanOption("check_step", true)) {
            runAsync(() -> {
                if (knownBox && knownOldBox) {
                    int hash = hash(oldBox, box);
                    Entity vehicle = player.getVehicle();

                    if (vehicle == null) {
                        if (combination.get(hash)) {
                            if (vertical > GroundUtils.maxPlayerStep) {
                                cancel(
                                        "type: step(high)"
                                                + ", old-box: " + oldBox
                                                + ", box: " + box
                                                + ", vertical: " + vertical,
                                        player.movement.getDetectionLocation(),
                                        0,
                                        true
                                );
                            }
                        } else if (vertical < 0.0 || vertical == Math.abs(box - oldBox)) {
                            cancel(
                                    "type: step(combination)"
                                            + ", old-box: " + oldBox
                                            + ", box: " + box
                                            + ", vertical: " + vertical,
                                    player.movement.getDetectionLocation(),
                                    0,
                                    true
                            );
                        }
                    } else if (vertical > 1.0) {
                        cancel(
                                "type: step(entity)"
                                        + ", old-box: " + oldBox
                                        + ", box: " + box
                                        + ", vertical: " + vertical,
                                player.movement.getDetectionLocation(),
                                0,
                                true
                        );
                    }
                } else {

                }
            });
        }
    }
}
