package com.vagdedes.spartan.abstraction.check.implementation.inventory;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TPS;
import me.vagdedes.spartan.system.Enums;

public class ItemDrops extends CheckExecutor {

    private long time;
    private final Buffer buffer;

    public ItemDrops(SpartanPlayer player) {
        super(Enums.HackType.ItemDrops, player);
        this.buffer = new Buffer(player);
    }

    @Override
    protected void runInternal(boolean cancelled) {
        long timePassed = System.currentTimeMillis() - time;
        this.time = System.currentTimeMillis();

        if (timePassed <= TPS.tickTime
                && buffer.count("", 10) == 7) {
            buffer.remove("");
            cancel("type: default, ms: " + timePassed, 1.0, null, 10);
        }
    }

    @Override
    protected boolean canRun() {
        return !player.isDead();
    }
}
