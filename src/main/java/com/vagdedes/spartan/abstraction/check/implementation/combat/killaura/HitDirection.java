package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.utils.math.RayUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class HitDirection extends DetectionExecutor {

    HitDirection(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run() {
        // todo
    }

}
