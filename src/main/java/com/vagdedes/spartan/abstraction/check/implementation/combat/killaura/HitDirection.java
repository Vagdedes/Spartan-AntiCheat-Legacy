package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.implementation.combat.killaura.movedirection.AttackData;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.utils.math.RayUtils;
import org.bukkit.entity.Entity;

import java.util.LinkedList;

public class HitDirection extends DetectionExecutor {

    HitDirection(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }


    void runDamage(Entity target) {

    }

}
