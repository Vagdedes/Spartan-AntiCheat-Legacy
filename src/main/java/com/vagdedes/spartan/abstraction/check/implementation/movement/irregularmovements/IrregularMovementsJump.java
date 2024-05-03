package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class IrregularMovementsJump extends DetectionExecutor {

    private static final Map<Short, Collection<Double>> heights = new LinkedHashMap<>();

    static {
        Collection<Double> motions = PlayerUtils.calculateFallMotions(
                PlayerUtils.jumpAcceleration,
                PlayerUtils.airDrag,
                PlayerUtils.airAcceleration,
                PlayerUtils.terminalVelocity
        );


        for (short i = Short.MIN_VALUE; i < Short.MAX_VALUE; i++) {
            Collection<Double> heights = new ArrayList<>();

            for (double motion : motions) {
                System.out.println(motion);
            }
        }
    }

    IrregularMovementsJump(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run() {
        if (hackType.getCheck().getBooleanOption("check_jump", true)) {
            runAsync(() -> {
                player.debug(false, false, true,
                        1
                );
            });
        }
    }
}
