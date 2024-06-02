package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import org.bukkit.entity.Entity;

public class HitDirection extends DetectionExecutor {

    HitDirection(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }


    void runDamage(Entity target) {

    }

}
