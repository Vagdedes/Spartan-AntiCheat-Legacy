package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.profiling.PlayerFight;
import com.vagdedes.spartan.utils.math.RayUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Vector2f;
import org.joml.Vector3d;

import static java.lang.Math.PI;

public class FightAnalysis extends DetectionExecutor {

    FightAnalysis(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run(PlayerFight fight) {
        if (hackType.getCheck().getBooleanOption("check_fight_analysis", true)) {
            runAsync(() -> {
                // todo
            });
        }
    }
}
