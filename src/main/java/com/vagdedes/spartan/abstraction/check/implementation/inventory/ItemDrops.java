package com.vagdedes.spartan.abstraction.check.implementation.inventory;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Timer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TPS;
import me.vagdedes.spartan.system.Enums;

public class ItemDrops extends CheckExecutor {

    public ItemDrops(SpartanPlayer player) {
        super(Enums.HackType.ItemDrops, player);
    }

    @Override
    public void runInternal(boolean cancelled) {
        Timer timer = this.getTimer();
        long time = timer.get("");

        if (time <= TPS.tickTime) {
            Buffer buffer = this.getBuffer();

            if (buffer.count("", 10) == 7) {
                buffer.remove("");
                cancel("type: default, ms: " + time, 1.0, null, 10);
            }
        }
        timer.set("");
    }

    @Override
    protected boolean canRun() {
        return !player.isDead();
    }
}
