package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura.movedirection;

import org.bukkit.entity.Entity;

public class AttackHandle {

    private final Entity target;
    private final long timeMillis;

    public AttackHandle(Entity target, long timeMillis) {
        this.timeMillis = timeMillis;
        this.target = target;
    }

    public Entity getTarget() {
        return target;
    }

    public long getTimeMillis() {
        return timeMillis;
    }
}
