package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.potion.PotionEffectType;
import org.mozilla.javascript.ast.Jump;

import java.util.*;

public class IrregularMovementsJump extends DetectionExecutor {

    private static final Map<Integer, Set<Double>> heights = new LinkedHashMap<>();

    static {
        for (int jumpEffect = 0; jumpEffect < 255; jumpEffect++) {
            Set<Double> collection = new HashSet<>();
            double height = 0.0;

            for (double motion : PlayerUtils.getJumpMotions(jumpEffect)) {
                height += motion;
                collection.add(AlgebraUtils.removeBase(height));
            }
            heights.put(jumpEffect, collection);
        }
    }

    IrregularMovementsJump(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run(double vertical, double box) {
        if (hackType.getCheck().getBooleanOption("check_jump", true)) {
            runAsync(() -> {
                player.debug(true, false, true,
                        2,
                        heights.get(PlayerUtils.getPotionLevel(player, PotionEffectType.JUMP)).contains(box)
                );
            });
        }
    }
}
