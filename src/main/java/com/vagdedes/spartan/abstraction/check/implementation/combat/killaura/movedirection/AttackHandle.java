package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura.movedirection;

import org.bukkit.entity.Entity;

public class AttackHandle {

    private final Entity target;
    private final long timeMillis;

    AttackHandle(Entity target, long timeMillis) {
        this.timeMillis = timeMillis;
        this.target = target;
    }

    Entity getTarget() {
        return target;
    }

    long getTimeMillis() {
        return timeMillis;
    }
}
